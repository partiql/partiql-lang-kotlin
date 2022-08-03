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
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.Expression
import org.partiql.lang.eval.ThunkReturnTypeAssertions
import org.partiql.lang.eval.builtins.DynamicLookupExprFunction
import org.partiql.lang.eval.builtins.createBuiltinFunctions
import org.partiql.lang.eval.builtins.storedprocedure.StoredProcedure
import org.partiql.lang.eval.physical.PhysicalBexprToThunkConverter
import org.partiql.lang.eval.physical.PhysicalExprToThunkConverter
import org.partiql.lang.eval.physical.PhysicalExprToThunkConverterImpl
import org.partiql.lang.eval.physical.PhysicalPlanThunk
import org.partiql.lang.eval.physical.operators.DEFAULT_RELATIONAL_OPERATOR_FACTORIES
import org.partiql.lang.eval.physical.operators.RelationalOperatorFactory
import org.partiql.lang.eval.physical.operators.RelationalOperatorFactoryKey
import org.partiql.lang.planner.transforms.normalize
import org.partiql.lang.planner.transforms.toDefaultPhysicalPlan
import org.partiql.lang.planner.transforms.toLogicalPlan
import org.partiql.lang.planner.transforms.toResolvedPlan
import org.partiql.lang.syntax.Parser
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.syntax.SyntaxException
import org.partiql.lang.types.CustomType

/**
 * Represents a pass over the physical plan that accepts a physical plan and returns a modified
 * physical plan.
 *
 * Passes accept as input a [PartiqlPhysical.Plan] which is cloned & modified in some way before being returned.
 * A second input to the pass is an instance of [ProblemHandler], which can be used to report semantic errors and
 * warnings to the query author.
 *
 * Examples of passes:
 *
 * - Select optimal physical operator implementations.
 * - Push down predicates or projections.
 * - Convert filter predicates to index lookups.
 * - Fold constants.
 * - And many, many others, some will be specific to the application embedding PartiQL.
 *
 * Notes on exceptions and semantic problems:
 *
 * - The passes may throw any exception, however these will always abort query planning and bypass the user-friendly
 * error reporting ([ProblemHandler]) mechanisms used for
 * [syntax and semantic errors](https://www.educative.io/edpresso/what-is-the-difference-between-syntax-and-semantic-errors)
 * - Use the [ProblemHandler] to report semantic errors and warnings in the query to the query author.
 *
 * @see [ProblemHandler.handleProblem]
 * @see [Problem]
 * @see [Problem.details]
 * @see [org.partiql.lang.errors.ProblemSeverity]
 */
interface PartiqlPhysicalPass {
    val passName: String
    fun rewrite(inputPlan: PartiqlPhysical.Plan, problemHandler: ProblemHandler): PartiqlPhysical.Plan
}

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
     * and calling [GlobalVariableResolver.resolveGlobal] to obtain unique identifiers global values such as tables that
     * are specific to the application embedding PartiQL, and optionally converts undefined variables to dynamic
     * lookups.
     * - Converts the logical plan to a physical plan with `(impl default)` operators.
     *
     * @param query The text of the SQL statement or expression to be planned.
     * @return [PlannerPassResult.Success] containing an instance of [PartiqlPhysical.Statement] and any applicable warnings
     * if planning was successful or [PlannerPassResult.Error] if not.
     */
    fun plan(query: String): PlannerPassResult<PartiqlPhysical.Plan>

    /**
     * Compiles the previously planned [PartiqlPhysical.Statement] instance.
     *
     * @param physicalPlan The physical query plan.
     * @return [PlannerPassResult.Success] containing an instance of [PartiqlPhysical.Statement] and any applicable warnings
     * if compilation was successful or [PlannerPassResult.Error] if not.
     */
    fun compile(physicalPlan: PartiqlPhysical.Plan): PlannerPassResult<QueryPlan>

    /**
     * Plans and compiles a query.
     *
     * @param query The text of the SQL statement or expression to be planned and compiled.
     * @return [PlannerPassResult.Success] containing an instance of [PartiqlPhysical.Statement] and any applicable warnings
     * if compiling and planning was successful or [PlannerPassResult.Error] if not.
     */
    fun planAndCompile(query: String): PlannerPassResult<QueryPlan> =
        when (val planResult = plan(query)) {
            is PlannerPassResult.Error -> PlannerPassResult.Error(planResult.errors)
            is PlannerPassResult.Success -> {
                when (val compileResult = compile(planResult.output)) {
                    is PlannerPassResult.Error -> compileResult
                    is PlannerPassResult.Success -> PlannerPassResult.Success(
                        compileResult.output,
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
    class Builder(val valueFactory: ExprValueFactory) {
        private var parser: Parser? = null
        private var evaluatorOptions: EvaluatorOptions? = null
        private val customFunctions: MutableMap<String, ExprFunction> = HashMap()
        private var customDataTypes: List<CustomType> = listOf()
        private val customProcedures: MutableMap<String, StoredProcedure> = HashMap()
        private val physicalPlanPasses = ArrayList<PartiqlPhysicalPass>()
        private val physicalOperatorFactories = ArrayList<RelationalOperatorFactory>()
        private var globalVariableResolver: GlobalVariableResolver = emptyGlobalsResolver()
        private var allowUndefinedVariables: Boolean = false
        private var enableLegacyExceptionHandling: Boolean = false
        private var plannerEventCallback: PlannerEventCallback? = null

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
         *
         * This function is marked as internal to prevent it from being used outside the tests in this
         * project--it will be replaced during implementation of the open type system.
         * https://github.com/partiql/partiql-lang-kotlin/milestone/4
         */
        internal fun addFunction(function: ExprFunction): Builder = this.apply {
            customFunctions[function.signature.name] = function
        }

        /**
         * Add custom types to CAST/IS operators to.
         *
         * Built-in types will take precedence over custom types in case of a name collision.
         *
         * This function is marked as internal to prevent it from being used outside the tests in this
         * project--it will be replaced during implementation of the open type system.
         * https://github.com/partiql/partiql-lang-kotlin/milestone/4
         */
        internal fun customDataTypes(customTypes: List<CustomType>) = this.apply {
            customDataTypes = customTypes
        }

        /**
         * Add a custom stored procedure which will be callable by the compiled queries.
         *
         * Stored procedures added here will replace any built-in procedure with the same name.
         * This function is marked as internal to prevent it from being used outside the tests in this
         * project--it will be replaced during implementation of the open type system.
         * https://github.com/partiql/partiql-lang-kotlin/milestone/4
         */
        internal fun addProcedure(procedure: StoredProcedure): Builder = this.apply {
            customProcedures[procedure.signature.name] = procedure
        }

        /**
         * Adds a pass over the physical algebra. The pass will be invoked every time a query is planned with the
         * [PlannerPipeline] returned from the [build] function.
         *
         * Passes accept a [PartiqlPhysical.Plan] as input and return a rewritten [PartiqlPhysical.Plan] as output.
         * The input of every pass is the output of the previous pass, starting with the default physical plan which is
         * a direct conversion of the resolved logical plan to the physical, with default operator implementations
         * specified.   Passes are invoked in the order they are added.
         *
         * If any pass invokes [ProblemHandler.handleProblem] with a [Problem] that is an error (see
         * [org.partiql.lang.errors.ProblemSeverity]), planning will be aborted immediately after the pass returns and
         * the list of errors and warnings will be returned to the caller.  [Problem]s that are warnings do not
         * cause query planning to be aborted but are returned to the caller when planning has finished.
         *
         * @see [toDefaultPhysicalPlan].
         * @see [PartiqlPhysicalPass]
         * @see [org.partiql.lang.errors.ProblemDetails.severity]
         * @see [org.partiql.lang.errors.ProblemSeverity]
         */
        fun addPhysicalPlanPass(pass: PartiqlPhysicalPass) = this.apply {
            physicalPlanPasses.add(pass)
        }

        /**
         * Helper function for [addPhysicalPlanPass] to reduce syntactic overhead.
         *
         * - [name] the name of the pass--passed to [PlannerEventCallback] as [PlannerEvent.eventName].  Naming
         * convention is `lower_snake_case`.
         * - [passBody] a closure which actually performs the rewrite within the pass.  See [PartiqlPhysicalPass].
         */
        fun addPhysicalPlanPass(
            name: String,
            passBody: (PartiqlPhysical.Plan, ProblemHandler) -> PartiqlPhysical.Plan
        ) = this.apply {
            physicalPlanPasses.add(
                object : PartiqlPhysicalPass {
                    override val passName = name

                    override fun rewrite(
                        inputPlan: PartiqlPhysical.Plan,
                        problemHandler: ProblemHandler
                    ): PartiqlPhysical.Plan =
                        passBody(inputPlan, problemHandler)
                }
            )
        }

        /**
         * Makes an instance of [RelationalOperatorFactory] available during plan compilation.
         *
         * To actually be used, operator implementations must be selected during a pass over the physical plan.
         * See [addPhysicalPlanPass].
         */
        fun addRelationalOperatorFactory(factory: RelationalOperatorFactory) = this.apply {
            physicalOperatorFactories.add(factory)
        }

        /**
         * Adds the [GlobalVariableResolver] for global variables (usually tables).
         *
         * [globalVariableResolver] is queried during query planning to fetch unique ids for global variables.
         */
        fun globalVariableResolver(bindings: GlobalVariableResolver): Builder = this.apply {
            this.globalVariableResolver = bindings
        }

        /**
         * Sets a flag indicating if undefined variables are allowed.
         *
         * When allowed, undefined variables are rewritten to dynamic lookups.  This is intended to provide a migration
         * path for legacy PartiQL customers who depend on dynamic lookup of undefined variables to use the query
         * planner & phys. algebra. New customers should not enable this.
         */
        fun allowUndefinedVariables(allow: Boolean = true): Builder = this.apply {
            this.allowUndefinedVariables = allow
        }

        /**
         * Prevents [SqlException] that occur during compilation from being converted into [Problem]s.
         *
         * This is for compatibility with the legacy unit test suite, which hasn't been updated to handle
         * [Problem]s yet.
         */
        internal fun enableLegacyExceptionHandling(): Builder = this.apply {
            enableLegacyExceptionHandling = true
        }

        /**
         * If set, invoked after every phase of planning is completed.
         *
         * **CAUTION:* [PlannerEvent] instances passed to [cb] may contain sensitive information contained within user
         * queries, particularly within filter predicates.  It may be necessary to redact these statements before
         * logging to persistent storage.  The [org.partiql.lang.passes.redact] function is supplied to redact SQL
         * queries but no facility is provided by PartiQL to redact ASTs or plans yet.  Such redaction must currently
         * be provided by the embedding PartiQL application.
         */
        fun plannerEventCallback(cb: PlannerEventCallback): Builder = this.apply {
            plannerEventCallback = cb
        }

        /** Builds the actual implementation of [PlannerPipeline]. */
        fun build(): PlannerPipeline {
            val compileOptionsToUse = evaluatorOptions ?: EvaluatorOptions.standard()

            when (compileOptionsToUse.thunkOptions.thunkReturnTypeAssertions) {
                ThunkReturnTypeAssertions.DISABLED -> { /* take no action */ }
                ThunkReturnTypeAssertions.ENABLED -> error(
                    "TODO: Support ThunkReturnTypeAssertions.ENABLED " +
                        "need a static type pass first)"
                )
            }

            // check for duplicate operator factories.  Unlike [ExprFunctions], we do not allow the default
            // operator implementations to be overridden.
            val allPhysicalOperatorFactories = (DEFAULT_RELATIONAL_OPERATOR_FACTORIES + physicalOperatorFactories).apply {
                groupBy { it.key }.entries.firstOrNull { it.value.size > 1 }?.let {
                    throw IllegalArgumentException(
                        "More than one BindingsOperatorFactory for ${it.key.operator} " +
                            "named '${it.value}' was specified."
                    )
                }
            }

            val builtinFunctions = createBuiltinFunctions(valueFactory) + DynamicLookupExprFunction()
            val builtinFunctionsMap = builtinFunctions.associateBy {
                it.signature.name
            }

            // customFunctions must be on the right side of + here to ensure that they overwrite any
            // built-in functions with the same name.
            val allFunctionsMap = builtinFunctionsMap + customFunctions
            return PlannerPipelineImpl(
                valueFactory = valueFactory,
                parser = parser ?: SqlParser(valueFactory.ion, this.customDataTypes),
                evaluatorOptions = compileOptionsToUse,
                functions = allFunctionsMap,
                customDataTypes = customDataTypes,
                procedures = customProcedures,
                physicalPlanPasses = physicalPlanPasses,
                bindingsOperatorFactories = allPhysicalOperatorFactories.associateBy { it.key },
                globalVariableResolver = globalVariableResolver,
                allowUndefinedVariables = allowUndefinedVariables,
                enableLegacyExceptionHandling = enableLegacyExceptionHandling,
                plannerEventCallback = plannerEventCallback
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
    val bindingsOperatorFactories: Map<RelationalOperatorFactoryKey, RelationalOperatorFactory>,
    val globalVariableResolver: GlobalVariableResolver,
    val allowUndefinedVariables: Boolean,
    val enableLegacyExceptionHandling: Boolean,
    val physicalPlanPasses: List<PartiqlPhysicalPass>,
    val plannerEventCallback: PlannerEventCallback?,
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

    override fun plan(query: String): PlannerPassResult<PartiqlPhysical.Plan> {
        val ast = try {
            plannerEventCallback.doEvent("parse_sql", query) {
                parser.parseAstStatement(query)
            }
        } catch (ex: SyntaxException) {
            val problem = Problem(
                SourceLocationMeta(
                    ex.errorContext[Property.LINE_NUMBER]?.longValue() ?: -1,
                    ex.errorContext[Property.COLUMN_NUMBER]?.longValue() ?: -1
                ),
                PlanningProblemDetails.ParseError(ex.generateMessageNoLocation())
            )
            return PlannerPassResult.Error(listOf(problem))
        }
        // Now run the AST thru each pass until we arrive at the physical algebra.

        // logical plan -> resolved logical plan
        val problemHandler = ProblemCollector()
        // Normalization--synthesizes any unspecified `AS` aliases, converts `SELECT *` to `SELECT f.*[, ...]` ...
        val normalizedAst = plannerEventCallback.doEvent("normalize_ast", ast) {
            ast.normalize()
        }

        // ast -> logical plan
        val logicalPlan = plannerEventCallback.doEvent("ast_to_logical", normalizedAst) {
            normalizedAst.toLogicalPlan(problemHandler)
        }

        if (problemHandler.hasErrors) {
            return PlannerPassResult.Error(problemHandler.problems)
        }

        // logical plan -> resolved logical plan
        val resolvedLogicalPlan = plannerEventCallback.doEvent("logical_to_logical_resolved", logicalPlan) {
            logicalPlan.toResolvedPlan(problemHandler, globalVariableResolver, allowUndefinedVariables)
        }

        // If there are unresolved variables after attempting to resolve variables, then we can't proceed.
        if (problemHandler.hasErrors) {
            return PlannerPassResult.Error(problemHandler.problems)
        }

        // Possible future passes:
        // - type checking and inferencing?
        // - constant folding
        // - common sub-expression removal
        // - push down predicates & projections on top of their scan nodes.
        // - customer supplied rewrites of resolved logical plan.

        // resolved logical plan -> physical plan.
        // this will give all relational operators `(impl default)`.
        val defaultPhysicalPlan = plannerEventCallback.doEvent(
            "logical_resolved_to_default_physical",
            resolvedLogicalPlan
        ) {
            resolvedLogicalPlan.toDefaultPhysicalPlan(problemHandler)
        }

        if (problemHandler.hasErrors) {
            return PlannerPassResult.Error(problemHandler.problems)
        }

        val finalPlan = physicalPlanPasses
            .fold(defaultPhysicalPlan) { accumulator: PartiqlPhysical.Plan, current: PartiqlPhysicalPass ->
                val passResult = plannerEventCallback.doEvent(
                    "custom_physical_plan_pass_${current.passName}",
                    accumulator
                ) {
                    current.rewrite(accumulator, problemHandler)
                }

                // stop planning if this pass resulted in any errors.
                if (problemHandler.hasErrors) {
                    return PlannerPassResult.Error(problemHandler.problems)
                }
                passResult
            }
        // If we reach this far, we're successful.  If there were any problems at all, they were just warnings.
        return PlannerPassResult.Success(finalPlan, problemHandler.problems)
    }

    override fun compile(physicalPlan: PartiqlPhysical.Plan): PlannerPassResult<QueryPlan> =
        plannerEventCallback.doEvent("compile", physicalPlan) {
            // PhysicalBExprToThunkConverter and PhysicalExprToThunkConverterImpl are mutually recursive therefore
            // we have to fall back on mutable variables to allow them to reference each other.
            var exprConverter: PhysicalExprToThunkConverterImpl? = null

            val bexperConverter = PhysicalBexprToThunkConverter(
                valueFactory = this.valueFactory,
                exprConverter = object : PhysicalExprToThunkConverter {
                    override fun convert(expr: PartiqlPhysical.Expr): PhysicalPlanThunk =
                        exprConverter!!.convert(expr)
                },
                relationalOperatorFactory = bindingsOperatorFactories
            )

            exprConverter = PhysicalExprToThunkConverterImpl(
                valueFactory = valueFactory,
                functions = functions,
                customTypedOpParameters = customTypedOpParameters,
                procedures = procedures,
                evaluatorOptions = evaluatorOptions,
                bexperConverter = bexperConverter
            )

            val expression = when {
                enableLegacyExceptionHandling -> exprConverter.compile(physicalPlan)
                else -> {
                    // Legacy exception handling is disabled, convert any [SqlException] into a Problem and return
                    // PassResult.Error.
                    try {
                        exprConverter.compile(physicalPlan)
                    } catch (e: SqlException) {
                        val problem = Problem(
                            SourceLocationMeta(
                                e.errorContext[Property.LINE_NUMBER]?.longValue() ?: -1,
                                e.errorContext[Property.COLUMN_NUMBER]?.longValue() ?: -1
                            ),
                            PlanningProblemDetails.CompileError(e.generateMessageNoLocation())
                        )
                        return@doEvent PlannerPassResult.Error(listOf(problem))
                    }
                }
            }

            val queryPlan = when (physicalPlan.stmt) {
                is PartiqlPhysical.Statement.DmlQuery ->
                    QueryPlan { session -> expression.eval(session).toDmlCommand() }
                is PartiqlPhysical.Statement.Query, is PartiqlPhysical.Statement.Exec ->
                    QueryPlan { session -> QueryResult.Value(expression.eval(session)) }
            }
            PlannerPassResult.Success(queryPlan, listOf())
        }
}
