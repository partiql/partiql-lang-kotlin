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

package org.partiql.lang

import com.amazon.ion.IonSystem
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.EvaluatingCompiler
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.Expression
import org.partiql.lang.eval.builtins.createBuiltinFunctions
import org.partiql.lang.syntax.Parser
import org.partiql.lang.syntax.SqlParser

/**
 * Contains all of the information needed for processing steps.
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
    val functions: @JvmSuppressWildcards Map<String, ExprFunction>
)

/**
 * [ProcessingStep] functions accept an [ExprNode] and [StepContext] as an arguments and processes them in some
 * way and then returns either the original [ExprNode] or a modified [ExprNode].
 */
typealias ProcessingStep = (ExprNode, StepContext) -> ExprNode

/**
 * [CompilerPipeline] is the main interface for compiling PartiQL queries into instances of [Expression] which
 * can be executed.
 */
interface CompilerPipeline  {
    val valueFactory: ExprValueFactory

    /** The compilation options. */
    val compileOptions: CompileOptions

    /**
     * Returns a list of all functions which are available for execution.
     * Includes built-in functions as well as custom functions added while the [CompilerPipeline]
     * was being built.
     */
    val functions: @JvmSuppressWildcards Map<String, ExprFunction>

    /** Compiles the specified PartiQL query using the configured parser. */
    fun compile(query: String): Expression

    /** Compiles the specified [ExprNode] instance. */
    fun compile(query: ExprNode): Expression

    companion object {

        /** Kotlin style builder for [CompilerPipeline].  If calling from Java instead use [builder]. */
        fun build(ion: IonSystem, block: Builder.() -> Unit) = build(ExprValueFactory.standard(ion), block)

        /** Kotlin style builder for [CompilerPipeline].  If calling from Java instead use [builder]. */
        fun build(valueFactory: ExprValueFactory, block: Builder.() -> Unit) = Builder(valueFactory).apply(block).build()

        /** Fluent style builder.  If calling from Kotlin instead use the [build] method. */
        @JvmStatic
        fun builder(ion: IonSystem): CompilerPipeline.Builder = builder(ExprValueFactory.standard(ion))

        /** Fluent style builder.  If calling from Kotlin instead use the [build] method. */
        @JvmStatic
        fun builder(valueFactory: ExprValueFactory): CompilerPipeline.Builder = Builder(valueFactory)

        /** Returns an implementation of [CompilerPipeline] with all properties set to their defaults. */
        @JvmStatic
        fun standard(ion: IonSystem): CompilerPipeline = standard(ExprValueFactory.standard(ion))

        /** Returns an implementation of [CompilerPipeline] with all properties set to their defaults. */
        @JvmStatic
        fun standard(valueFactory: ExprValueFactory): CompilerPipeline =
            builder(valueFactory).build()
    }

    /** An implementation of the builder pattern for instances of [CompilerPipeline]. */
    class Builder(val valueFactory: ExprValueFactory) {
        private var parser: Parser? = null
        private var compileOptions: CompileOptions? = null
        private val customFunctions: MutableMap<String, ExprFunction> = HashMap()
        private val preProcessingSteps: MutableList<ProcessingStep> = ArrayList()

        /**
         * Specifies the [Parser] to be used to turn an PartiQL query into an instance of [ExprNode].
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
        fun addFunction(function: ExprFunction): Builder = this.apply { customFunctions[function.name] = function }

        /** Adds a preprocessing step to be executed after parsing but before compilation. */
        fun addPreprocessingStep(step: ProcessingStep): Builder = this.apply { preProcessingSteps.add(step) }

        /** Builds the actual implementation of [CompilerPipeline]. */
        fun build(): CompilerPipeline {
            val builtinFunctions = createBuiltinFunctions(valueFactory).associateBy { it.name }

            // customFunctions must be on the right side of + here to ensure that they overwrite any
            // built-in functions with the same name.
            val allFunctions = builtinFunctions + customFunctions

            return CompilerPipelineImpl(
                valueFactory,
                parser ?: SqlParser(valueFactory.ion),
                compileOptions ?: CompileOptions.standard(),
                allFunctions,
                preProcessingSteps)
        }
    }
}

private class CompilerPipelineImpl(
    override val valueFactory: ExprValueFactory,
    private val parser: Parser,
    override val compileOptions: CompileOptions,
    override val functions: Map<String, ExprFunction>,
    private val preProcessingSteps: List<ProcessingStep>
) : CompilerPipeline {

    private val compiler = EvaluatingCompiler(valueFactory, functions, compileOptions)

    override fun compile(query: String): Expression {
        @Suppress("DEPRECATION")
        return compile(parser.parseExprNode(query))
    }

    override fun compile(query: ExprNode): Expression {
        val context = StepContext(valueFactory, compileOptions, functions)

        val preProcessedQuery = preProcessingSteps.fold(query) { currentExprNode, step ->
            step(currentExprNode, context)
        }

        return compiler.compile(preProcessedQuery)
    }
}
