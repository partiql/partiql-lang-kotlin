/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.planner

import com.amazon.ion.IonSystem
import org.partiql.lang.SqlException
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemCollector
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.Expression
import org.partiql.lang.eval.ThunkReturnTypeAssertions
import org.partiql.lang.eval.builtins.createBuiltinFunctions
import org.partiql.lang.eval.builtins.storedprocedure.StoredProcedure
import org.partiql.lang.eval.physical.PhysicalExprToThunkConverterImpl
import org.partiql.lang.planner.transforms.PlanningProblemDetails
import org.partiql.lang.planner.transforms.normalize
import org.partiql.lang.planner.transforms.toLogical
import org.partiql.lang.planner.transforms.toPhysical
import org.partiql.lang.planner.transforms.toResolved
import org.partiql.lang.syntax.Parser
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.syntax.SyntaxException
import org.partiql.lang.types.CustomType
import org.partiql.lang.util.BuilderDsl

/*
Differences between CompilerPipeline and PlannerPipeline:

- CompilerPipeline's ProcessingSteps work only on AST.  PlannerPipeline does not depend on the AST at all.  The intent
is to free customers from the need to manipulate or even be aware of the AST to the extent possible.  PlannerPipeline
will eventually only support working on resolved logical plans and later, but not yet.
- PlannerPipeline does not yet to work with static types.

Why not add an option to enable the planner to be used with CompilerPipeline?  Some fundamental differences:
- global bindings are (eventually) a GlobalBindings instance instead of a Bindings<StaticType> (globalTypeBindings).
    - (ResolutionResult, returned from GlobalBindings, likely will eventually include a StaticType.)
- there is more complexity than is needed (i.e. AST processing steps)
*/

/**
 * [PlannerPipeline] is the main interface for planning and compiling PartiQL queries into instances of [Expression]
 * which can be executed.
 *
 * This class was originally derived from [org.partiql.lang.CompilerPipeline], which is the main compiler entry point
 * for the legacy AST compiler.  The main difference is that the logical and physical plans have taken the place of
 * PartiQL's AST, and that after parsing several passes over the AST are performed:
 *
 * - It is transformed into a logical plan
 * - Variables are resolved.
 * - It is converted into a physical plan.
 *
 * In the future additional passes will exist to include optimizations like predicate & projection push-down, and
 * will be extensible by customers who can include their own optimizations.
 *
 * Two basic scenarios for using this interface:
 *
 * 1. You want to plan and compile a query once, but don't care about re-using the plan across process instances.  In
 * this scenario, simply use the [planAndCompile] function to obtain an instance of [Expression] that can be
 * invoked directly to evaluate a query.
 * 2. You want to plan a query once and share a planned query across process boundaries.  In this scenario, use
 * [plan] to perform query planning and obtain an instance of [PartiqlPhysical.Statement] which can be serialized to
 * Ion text or binary format.  On the other side of the process boundary, use [compile] to turn the
 * [PartiqlPhysical.Statement] query plan into an [Expression]. Compilation itself should be relatively fast.
 *
 * The provided builder companion creates an instance of [PlannerPipeline] that is NOT thread safe and should NOT be
 * used to compile queries concurrently. If used in a multithreaded application, use one instance of [PlannerPipeline]
 * per thread.
 */
interface PlannerPipeline {
    val valueFactory: ExprValueFactory

    /**
     * Plans a query only but does not compile it.
     *
     * - Parses the specified SQL string, producing an AST.
     * - Converts the AST to a logical plan.
     * - Resolves all global and local variables in the logical plan, assigning unique indexes to local variables
     * and calling [GlobalBindings.resolve] of [globalBindings] to obtain PartiQL-service specific unique identifiers
     * of global values such as tables, and optionally converts undefined variables to dynamic lookups.
     * - Converts the AST to a physical plan with `(impl default)` operators.
     *
     * @param query The text of the SQL statement or expression to be planned.
     * @return [PassResult.Success] containing an instance of [PartiqlPhysical.Statement] and any applicable warnings
     * if planning was successful or [PassResult.Error] if not.
     */
    fun plan(query: String): PassResult<PartiqlPhysical.Statement>

    /**
     * Compiles the previously planned [PartiqlPhysical.Statement] instance.
     *
     * @param physcialPlan The physical query plan.
     * @return [PassResult.Success] containing an instance of [PartiqlPhysical.Statement] and any applicable warnings
     * if compilation was successful or [PassResult.Error] if not.
     */
    fun compile(physcialPlan: PartiqlPhysical.Statement): PassResult<Expression>

    /**
     * Plans and compiles a query.
     *
     * @param query The text of the SQL statement or expression to be planned and compiled.
     * @return [PassResult.Success] containing an instance of [PartiqlPhysical.Statement] and any applicable warnings
     * if compiling and planning was successful or [PassResult.Error] if not.
     */
    fun planAndCompile(query: String): PassResult<Expression> =
        when (val planResult = plan(query)) {
            is PassResult.Error -> PassResult.Error(planResult.errors)
            is PassResult.Success -> {
                when (val compileResult = compile(planResult.result)) {
                    is PassResult.Error -> compileResult
                    is PassResult.Success -> PassResult.Success(
                        compileResult.result,
                        // Need to include any warnings that may have been discovered during planning.
                        planResult.warnings + compileResult.warnings
                    )
                }
            }
        }

    @Suppress("DeprecatedCallableAddReplaceWith", "DEPRECATION")
    companion object {
        private const val WARNING = "WARNING: PlannerPipeline is EXPERIMENTAL and has incomplete language support! " +
            "For production use, see org.partiql.lang.CompilerPipeline which is stable and supports all PartiQL " +
            "features."

        /** Kotlin style builder for [PlannerPipeline].  If calling from Java instead use [builder]. */
        @Deprecated(WARNING)
        fun build(ion: IonSystem, block: Builder.() -> Unit) = build(ExprValueFactory.standard(ion), block)

        /** Kotlin style builder for [PlannerPipeline].  If calling from Java instead use [builder]. */
        @Deprecated(WARNING)
        fun build(valueFactory: ExprValueFactory, block: Builder.() -> Unit) = Builder(valueFactory).apply(block).build()

        /** Fluent style builder.  If calling from Kotlin instead use the [build] method. */
        @JvmStatic
        @Deprecated(WARNING)
        fun builder(ion: IonSystem): Builder = builder(ExprValueFactory.standard(ion))

        /** Fluent style builder.  If calling from Kotlin instead use the [build] method. */
        @JvmStatic
        @Deprecated(WARNING)
        fun builder(valueFactory: ExprValueFactory): Builder = Builder(valueFactory)

        /** Returns an implementation of [PlannerPipeline] with all properties set to their defaults. */
        @JvmStatic
        @Deprecated(WARNING)
        fun standard(ion: IonSystem): PlannerPipeline = standard(ExprValueFactory.standard(ion))

        /** Returns an implementation of [PlannerPipeline] with all properties set to their defaults. */
        @JvmStatic
        @Deprecated(WARNING)
        fun standard(valueFactory: ExprValueFactory): PlannerPipeline = builder(valueFactory).build()
    }

    /**
     * An implementation of the builder pattern for instances of [PlannerPipeline]. The created instance of
     * [PlannerPipeline] is NOT thread safe and should NOT be used to compile queries concurrently. If used in a
     * multithreaded application, use one instance of [PlannerPipeline] per thread.
     */
    @BuilderDsl
    class Builder(val valueFactory: ExprValueFactory) {
        private var parser: Parser? = null
        private var evaluatorOptions: EvaluatorOptions? = null
        private val customFunctions: MutableMap<String, ExprFunction> = HashMap()
        private var customDataTypes: List<CustomType> = listOf()
        private val customProcedures: MutableMap<String, StoredProcedure> = HashMap()
        private var globalBindings: GlobalBindings = emptyGlobalBindings()
        private var allowUndefinedVariables: Boolean = false
        private var enableLegacyExceptionHandling: Boolean = false

        /**
         * Specifies the [Parser] to be used to turn an PartiQL query into an instance of [PartiqlAst].
         * The default is [SqlParser].
         */
        fun sqlParser(p: Parser): Builder = this.apply {
            parser = p
        }

        /**
         * Options affecting evaluation-time behavior. The default is [EvaluatorOptions.standard].
         */
        fun evaluatorOptions(options: EvaluatorOptions): Builder = this.apply {
            evaluatorOptions = options
        }

        /**
         * A nested builder for compilation options. The default is [EvaluatorOptions.standard].
         *
         * Avoid the use of this overload if calling from Java and instead use the overload accepting an instance
         * of [EvaluatorOptions].
         *
         * There is no need to call [Builder.build] when using this method.
         */
        fun evaluatorOptions(block: EvaluatorOptions.Builder.() -> Unit): Builder =
            evaluatorOptions(EvaluatorOptions.build(block))

        /**
         * Add a custom function which will be callable by the compiled queries.
         *
         * Functions added here will replace any built-in function with the same name.
         */
        fun addFunction(function: ExprFunction): Builder = this.apply {
            customFunctions[function.signature.name] = function
        }

        /**
         * Add custom types to CAST/IS operators to.
         *
         * Built-in types will take precedence over custom types in case of a name collision.
         */
        fun customDataTypes(customTypes: List<CustomType>) = this.apply {
            customDataTypes = customTypes
        }

        /**
         * Add a custom stored procedure which will be callable by the compiled queries.
         *
         * Stored procedures added here will replace any built-in procedure with the same name.
         */
        fun addProcedure(procedure: StoredProcedure): Builder = this.apply {
            customProcedures[procedure.signature.name] = procedure
        }

        /**
         * Adds the [GlobalBindings] for global variables.
         *
         * [globalBindings] is queried during query planning to fetch database schema information.
         */
        fun globalBindings(bindings: GlobalBindings): Builder = this.apply {
            this.globalBindings = bindings
        }

        /**
         * Sets a flag indicating if undefined variables are allowed.
         *
         * When allowed, undefined variables are rewritten to dynamic lookups.
         */
        fun allowUndefinedVariables(allow: Boolean = true): Builder = this.apply {
            this.allowUndefinedVariables = allow
        }

        /**
         * Prevents [SqlException] that occur during compilation from being converted into [Problem]s.
         *
         * This is for compatibility with the unit test suite, which hasn't been updated to handle [Problem]s yet.
         */
        internal fun enableLegacyExceptionHandling(): Builder = this.apply {
            enableLegacyExceptionHandling = true
        }

        /** Builds the actual implementation of [PlannerPipeline]. */
        fun build(): PlannerPipeline {
            val compileOptionsToUse = evaluatorOptions ?: EvaluatorOptions.standard()

            when (compileOptionsToUse.thunkOptions.thunkReturnTypeAssertions) {
                ThunkReturnTypeAssertions.DISABLED -> { /* intentionally blank */ }
                ThunkReturnTypeAssertions.ENABLED -> error(
                    "TODO: Support ThunkReturnTypeAssertions.ENABLED " +
                        "need a static type pass first)"
                )
            }

            val builtinFunctions = createBuiltinFunctions(valueFactory).associateBy {
                it.signature.name
            }

            // customFunctions must be on the right side of + here to ensure that they overwrite any
            // built-in functions with the same name.
            val allFunctions = builtinFunctions + customFunctions

            return PlannerPipelineImpl(
                valueFactory = valueFactory,
                parser = parser ?: SqlParser(valueFactory.ion, this.customDataTypes),
                evaluatorOptions = compileOptionsToUse,
                functions = allFunctions,
                customDataTypes = customDataTypes,
                procedures = customProcedures,
                globalBindings = globalBindings,
                allowUndefinedVariables = allowUndefinedVariables,
                enableLegacyExceptionHandling = enableLegacyExceptionHandling
            )
        }
    }
}

internal class PlannerPipelineImpl(
    override val valueFactory: ExprValueFactory,
    private val parser: Parser,
    val evaluatorOptions: EvaluatorOptions,
    val functions: Map<String, ExprFunction>,
    val customDataTypes: List<CustomType>,
    val procedures: Map<String, StoredProcedure>,
    val globalBindings: GlobalBindings,
    val allowUndefinedVariables: Boolean,
    val enableLegacyExceptionHandling: Boolean
) : PlannerPipeline {

    init {
        when (evaluatorOptions.thunkOptions.thunkReturnTypeAssertions) {
            ThunkReturnTypeAssertions.DISABLED -> {
                /** intentionally blank. */
            }
            ThunkReturnTypeAssertions.ENABLED ->
                // Need a type inferencer pass on resolved logical algebra to support this.
                TODO("Support for EvaluatorOptions.thunkReturnTypeAsserts == ThunkReturnTypeAssertions.ENABLED")
        }
    }

    val customTypedOpParameters = customDataTypes.map { customType ->
        (customType.aliases + customType.name).map { alias ->
            Pair(alias.toLowerCase(), customType.typedOpParameter)
        }
    }.flatten().toMap()

    override fun plan(query: String): PassResult<PartiqlPhysical.Statement> {
        val ast = try {
            parser.parseAstStatement(query)
        } catch (ex: SyntaxException) {
            val problem = Problem(
                SourceLocationMeta(
                    ex.errorContext[Property.LINE_NUMBER]?.longValue() ?: -1,
                    ex.errorContext[Property.COLUMN_NUMBER]?.longValue() ?: -1
                ),
                PlanningProblemDetails.ParseError(ex.generateMessageNoLocation())
            )
            return PassResult.Error(listOf(problem))
        }
        // Now run the AST thru each pass until we arrive at the physical algebra.

        // Normalization--synthesizes any unspecified `AS` aliases, converts `SELECT *` to `SELECT f.*[, ...]` ...
        val normalizedAst = ast.normalize()

        // ast -> logical plan
        val logicalPlan = normalizedAst.toLogical()

        // logical plan -> resolved logical plan
        val problemHandler = ProblemCollector()
        val resolvedLogicalPlan = logicalPlan.toResolved(problemHandler, globalBindings, allowUndefinedVariables)
        // If there are unresolved variables after attempting to resolve variables, then we can't proceed.
        if (problemHandler.hasErrors) {
            return PassResult.Error(problemHandler.problems)
        }

        // Possible future passes:
        // - type checking and inferencing?
        // - constant folding
        // - common sub-expression removal
        // - push down predicates & projections on top of their scan nodes.
        // - customer supplied rewrites of resolved logical plan.

        // resolved logical plan -> physical plan.
        // this will give all relational operators `(impl default)`.
        val physicalPlan = resolvedLogicalPlan.toPhysical()

        // Future work: invoke passes to choose relational operator implementations other than `(impl default)`.
        // Future work: fully push down predicates and projections into their physical read operators.
        // Future work: customer supplied rewrites of phsyical plan

        // If we reach this far, we're successful.  If there were any problems at all, they were just warnings.
        return PassResult.Success(physicalPlan, problemHandler.problems)
    }

    override fun compile(physcialPlan: PartiqlPhysical.Statement): PassResult<Expression> {
        val compiler = PhysicalExprToThunkConverterImpl(
            valueFactory = valueFactory,
            functions = functions,
            customTypedOpParameters = customTypedOpParameters,
            procedures = procedures,
            evaluatorOptions = evaluatorOptions
        )

        val expression = when {
            enableLegacyExceptionHandling -> compiler.compile(physcialPlan)
            else -> {
                // Legacy exception handling is disabled, convert any [SqlException] into a Problem and return
                // PassResult.Error.
                try {
                    compiler.compile(physcialPlan)
                } catch (e: SqlException) {
                    val problem = Problem(
                        SourceLocationMeta(
                            e.errorContext[Property.LINE_NUMBER]?.longValue() ?: -1,
                            e.errorContext[Property.COLUMN_NUMBER]?.longValue() ?: -1
                        ),
                        PlanningProblemDetails.CompileError(e.generateMessageNoLocation())
                    )
                    return PassResult.Error(listOf(problem))
                }
            }
        }

        return PassResult.Success(expression, listOf())
    }
}
