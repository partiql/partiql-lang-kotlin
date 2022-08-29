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

@file:Suppress("DEPRECATION") // We don't need warnings about deprecated ExprNode.

package org.partiql.lang

import com.amazon.ion.IonSystem
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.toAstStatement
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.*
import org.partiql.lang.eval.EvaluatingCompiler
import org.partiql.lang.eval.builtins.createBuiltinFunctions
import org.partiql.lang.eval.builtins.storedprocedure.StoredProcedure
import org.partiql.lang.eval.visitors.PipelinedVisitorTransform
import org.partiql.lang.eval.visitors.StaticTypeInferenceVisitorTransform
import org.partiql.lang.eval.visitors.StaticTypeVisitorTransform
import org.partiql.lang.ots_work.plugins.standard.plugin.BehaviorWhenDivisorIsZero
import org.partiql.lang.ots_work.plugins.standard.plugin.StandardPlugin
import org.partiql.lang.ots_work.plugins.standard.plugin.TypedOpBehavior
import org.partiql.lang.ots_work.stscore.ScalarTypeSystem
import org.partiql.lang.syntax.Parser
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.types.CustomType
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.interruptibleFold

/**
 * Contains all information needed for processing steps.
 */
data class StepContext(
    /** The instance of [ExprValueFactory] that is used by the pipeline. */
    val valueFactory: ExprValueFactory,

    /** The compilation options. */
    val compileOptions: CompileOptions,

    /**
     * Returns a list of all functions which are available for execution.
     * Includes built-in functions as well as custom functions added while the [CompilerPipeline]
     * was being built.
     */
    val functions: @JvmSuppressWildcards Map<String, ExprFunction>,

    /**
     * Returns a list of all stored procedures which are available for execution.
     * Only includes the custom stored procedures added while the [CompilerPipeline] was being built.
     */
    val procedures: @JvmSuppressWildcards Map<String, StoredProcedure>
)

/**
 * [ProcessingStep] functions accept an [PartiqlAst.Statement] and [StepContext] as an arguments and processes them in some
 * way and then returns either the original [PartiqlAst.Statement] or a modified [PartiqlAst.Statement].
 */
typealias ProcessingStep = (PartiqlAst.Statement, StepContext) -> PartiqlAst.Statement

/**
 * [CompilerPipeline] is the main interface for compiling PartiQL queries into instances of [Expression] which
 * can be executed.
 *
 * The provided builder companion creates an instance of [CompilerPipeline] that is NOT thread safe and should NOT be
 * used to compile queries concurrently. If used in a multithreaded application, use one instance of [CompilerPipeline]
 * per thread.
 */
interface CompilerPipeline {
    val scalarTypeSystem: ScalarTypeSystem

    val valueFactory: ExprValueFactory

    /** The compilation options. */
    val compileOptions: CompileOptions

    /**
     * Returns a list of all functions which are available for execution.
     * Includes built-in functions as well as custom functions added while the [CompilerPipeline]
     * was being built.
     */
    val functions: @JvmSuppressWildcards Map<String, ExprFunction>

    /**
     * Returns list of custom data types that are available in typed operators (i.e CAST/IS).
     *
     * This does not include core PartiQL parameters.
     */
    val customDataTypes: List<CustomType>

    /**
     * Returns a list of all stored procedures which are available for execution.
     * Only includes the custom stored procedures added while the [CompilerPipeline] was being built.
     */
    val procedures: @JvmSuppressWildcards Map<String, StoredProcedure>

    /**
     * The configured global type bindings.
     */
    val globalTypeBindings: Bindings<StaticType>?

    /** Compiles the specified PartiQL query using the configured parser. */
    fun compile(query: String): Expression

    @Deprecated("ExprNode is deprecated. Please use PIG generated AST. ")
    /** Compiles the specified [ExprNode] instance. */
    fun compile(query: ExprNode): Expression

    /** Compiles the specified [PartiqlAst.Statement] instance. */
    fun compile(query: PartiqlAst.Statement): Expression

    companion object {
        /** Kotlin style builder for [CompilerPipeline].  If calling from Java instead use [builder]. */
        fun build(ion: IonSystem, block: Builder.() -> Unit) = build(ExprValueFactory.standard(ion), block)

        /** Kotlin style builder for [CompilerPipeline].  If calling from Java instead use [builder]. */
        fun build(valueFactory: ExprValueFactory, block: Builder.() -> Unit) = Builder(valueFactory).apply(block).build()

        /** Fluent style builder.  If calling from Kotlin instead use the [build] method. */
        @JvmStatic
        fun builder(ion: IonSystem): Builder = builder(ExprValueFactory.standard(ion))

        /** Fluent style builder.  If calling from Kotlin instead use the [build] method. */
        @JvmStatic
        fun builder(valueFactory: ExprValueFactory): Builder = Builder(valueFactory)

        /** Returns an implementation of [CompilerPipeline] with all properties set to their defaults. */
        @JvmStatic
        fun standard(ion: IonSystem): CompilerPipeline = standard(ExprValueFactory.standard(ion))

        /** Returns an implementation of [CompilerPipeline] with all properties set to their defaults. */
        @JvmStatic
        fun standard(valueFactory: ExprValueFactory): CompilerPipeline = builder(valueFactory).build()
    }

    /**
     * An implementation of the builder pattern for instances of [CompilerPipeline]. The created instance of
     * [CompilerPipeline] is NOT thread safe and should NOT be used to compile queries concurrently. If used in a
     * multithreaded application, use one instance of [CompilerPipeline] per thread.
     */
    class Builder(val valueFactory: ExprValueFactory) {
        private var parser: Parser? = null
        private var compileOptions: CompileOptions? = null
        private val customFunctions: MutableMap<String, ExprFunction> = HashMap()
        private var customDataTypes: List<CustomType> = listOf()
        private val customProcedures: MutableMap<String, StoredProcedure> = HashMap()
        private val preProcessingSteps: MutableList<ProcessingStep> = ArrayList()
        private var globalTypeBindings: Bindings<StaticType>? = null
        private var scalarTypeSystem: ScalarTypeSystem? = null

        /**
         * Specifies the [Parser] to be used to turn an PartiQL query into an instance of [PartiqlAst].
         * The default is [SqlParser].
         */
        fun sqlParser(p: Parser): Builder = this.apply { parser = p }

        /**
         * The options to be used during compilation. The default is [CompileOptions.standard].
         *
         */
        fun compileOptions(options: CompileOptions): Builder = this.apply { compileOptions = options }

        /**
         * A nested builder for compilation options. The default is [CompileOptions.standard].
         *
         * Avoid the use of this overload if calling from Java and instead use the overload accepting an instance
         * of [CompileOptions].
         *
         * There is no need to call [Builder.build] when using this method.
         */
        fun compileOptions(block: CompileOptions.Builder.() -> Unit): Builder = compileOptions(CompileOptions.build(block))

        /**
         * Add a custom function which will be callable by the compiled queries.
         *
         * Functions added here will replace any built-in function with the same name.
         */
        fun addFunction(function: ExprFunction): Builder = this.apply { customFunctions[function.signature.name] = function }

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
        fun addProcedure(procedure: StoredProcedure): Builder = this.apply { customProcedures[procedure.signature.name] = procedure }

        /** Adds a preprocessing step to be executed after parsing but before compilation. */
        fun addPreprocessingStep(step: ProcessingStep): Builder = this.apply { preProcessingSteps.add(step) }

        /** Adds the [Bindings<StaticType>] for global variables. */
        fun globalTypeBindings(bindings: Bindings<StaticType>): Builder = this.apply { this.globalTypeBindings = bindings }

        fun scalarTypeSystem(scalarTypeSystem: ScalarTypeSystem): Builder = this.apply { this.scalarTypeSystem = scalarTypeSystem }

        /** Builds the actual implementation of [CompilerPipeline]. */
        fun build(): CompilerPipeline {
            val compileOptionsToUse = compileOptions ?: CompileOptions.standard()
            val behaviorWhenDivisorIsZero = when (compileOptionsToUse.typingMode){
                TypingMode.LEGACY -> BehaviorWhenDivisorIsZero.ERROR
                TypingMode.PERMISSIVE -> BehaviorWhenDivisorIsZero.MISSING
            }

            val scalarTypeSystemToUse = scalarTypeSystem ?: ScalarTypeSystem(
                StandardPlugin(
                    typedOpBehavior = TypedOpBehavior.LEGACY,
                    behaviorWhenDivisorIsZero = behaviorWhenDivisorIsZero
                )
            )

            when (compileOptionsToUse.thunkOptions.thunkReturnTypeAssertions) {
                ThunkReturnTypeAssertions.DISABLED -> { /* intentionally blank */ }
                ThunkReturnTypeAssertions.ENABLED -> {
                    check(this.globalTypeBindings != null) {
                        "EvaluationTimeTypeChecks.ENABLED does not work if globalTypeBindings have not been specified"
                    }
                }
            }

            val builtinFunctions = createBuiltinFunctions(valueFactory).associateBy {
                it.signature.name
            }

            // customFunctions must be on the right side of + here to ensure that they overwrite any
            // built-in functions with the same name.
            val allFunctions = builtinFunctions + customFunctions

            return CompilerPipelineImpl(
                valueFactory = valueFactory,
                parser = parser ?: SqlParser(valueFactory.ion, customDataTypes),
                compileOptions = compileOptionsToUse,
                functions = allFunctions,
                customDataTypes = customDataTypes,
                procedures = customProcedures,
                preProcessingSteps = preProcessingSteps,
                globalTypeBindings = globalTypeBindings,
                scalarTypeSystem = scalarTypeSystemToUse
            )
        }
    }
}

internal class CompilerPipelineImpl(
    override val valueFactory: ExprValueFactory,
    private val parser: Parser,
    override val compileOptions: CompileOptions,
    override val functions: Map<String, ExprFunction>,
    override val customDataTypes: List<CustomType>,
    override val procedures: Map<String, StoredProcedure>,
    private val preProcessingSteps: List<ProcessingStep>,
    override val globalTypeBindings: Bindings<StaticType>?,
    override val scalarTypeSystem: ScalarTypeSystem
) : CompilerPipeline {

    private val compiler = EvaluatingCompiler(
        valueFactory,
        functions,
        customDataTypes.map { customType ->
            (customType.aliases + customType.name).map { alias ->
                Pair(alias.toLowerCase(), customType.typedOpParameter)
            }
        }.flatten().toMap(),
        procedures,
        compileOptions,
        scalarTypeSystem
    )

    override fun compile(query: String): Expression = compile(parser.parseAstStatement(query))

    override fun compile(query: ExprNode): Expression = compile(query.toAstStatement())

    override fun compile(query: PartiqlAst.Statement): Expression {
        val context = StepContext(valueFactory, compileOptions, functions, procedures)

        val preProcessedQuery = executePreProcessingSteps(query, context)

        val transforms = PipelinedVisitorTransform(
            *listOfNotNull(
                listOf(compileOptions.visitorTransformMode.createVisitorTransform()),
                // if [typeBindings] was specified, enable [StaticTypeVisitorTransform] and [StaticTypeInferenceVisitorTransform].
                when (globalTypeBindings) {
                    null -> null
                    else -> {
                        listOf(
                            StaticTypeVisitorTransform(valueFactory.ion, globalTypeBindings),
                            StaticTypeInferenceVisitorTransform(
                                globalBindings = globalTypeBindings,
                                customFunctionSignatures = functions.values.map { it.signature },
                                customTypedOpParameters = customDataTypes.map { customType ->
                                    (customType.aliases + customType.name).map { alias ->
                                        Pair(alias.toLowerCase(), customType.typedOpParameter)
                                    }
                                }.flatten().toMap(),
                                scalarTypeSystem = scalarTypeSystem
                            )
                        )
                    }
                }
            ).flatten().toTypedArray()
        )

        val queryToCompile = transforms.transformStatement(preProcessedQuery)

        return compiler.compile(queryToCompile)
    }

    internal fun executePreProcessingSteps(query: PartiqlAst.Statement, context: StepContext) = preProcessingSteps
        .interruptibleFold(query) { currentAstStatement, step -> step(currentAstStatement, context) }
}
