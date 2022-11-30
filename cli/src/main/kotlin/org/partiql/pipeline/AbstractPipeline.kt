/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.pipeline

import com.amazon.ion.IonSystem
import com.amazon.ion.system.IonSystemBuilder
import org.partiql.cli.Pipeline
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.compiler.PartiQLCompilerBuilder
import org.partiql.lang.compiler.PartiQLCompilerPipeline
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.eval.ProjectionIterationBehavior
import org.partiql.lang.eval.ThunkOptions
import org.partiql.lang.eval.TypedOpBehavior
import org.partiql.lang.eval.TypingMode
import org.partiql.lang.eval.UndefinedVariableBehavior
import org.partiql.lang.planner.GlobalResolutionResult
import org.partiql.lang.planner.GlobalVariableResolver
import org.partiql.lang.planner.PartiQLPlannerBuilder
import org.partiql.lang.syntax.Parser
import org.partiql.lang.syntax.PartiQLParserBuilder
import java.time.ZoneOffset

/**
 * A means by which we can run both the EvaluatingCompiler and PartiQLCompilerPipeline
 */
internal sealed class AbstractPipeline(open val options: PipelineOptions) {

    abstract fun compile(input: String, session: EvaluationSession): PartiQLResult

    companion object {
        internal fun create(options: PipelineOptions): AbstractPipeline = when (options.pipeline) {
            Pipeline.STANDARD -> PipelineStandard(options)
            Pipeline.EXPERIMENTAL -> PipelineExperimental(options)
        }
        internal fun convertExprValue(value: ExprValue): PartiQLResult {
            return PartiQLResult.Value(value)
        }
        internal fun standard(): AbstractPipeline {
            return create(PipelineOptions())
        }
    }

    data class PipelineOptions(
        val pipeline: Pipeline = Pipeline.STANDARD,
        val ion: IonSystem = IonSystemBuilder.standard().build(),
        val parser: Parser = PartiQLParserBuilder.standard().build(),
        val typedOpBehavior: TypedOpBehavior = TypedOpBehavior.HONOR_PARAMETERS,
        val projectionIterationBehavior: ProjectionIterationBehavior = ProjectionIterationBehavior.FILTER_MISSING,
        val undefinedVariableBehavior: UndefinedVariableBehavior = UndefinedVariableBehavior.ERROR,
        val typingMode: TypingMode = TypingMode.LEGACY,
        val functions: List<(ExprValueFactory) -> ExprFunction> = emptyList()
    )

    /**
     * Wraps the EvaluatingCompiler
     */
    internal class PipelineStandard(options: PipelineOptions) : AbstractPipeline(options) {

        private val compileOptions = CompileOptions.build {
            typedOpBehavior(options.typedOpBehavior)
            projectionIteration(options.projectionIterationBehavior)
            undefinedVariable(options.undefinedVariableBehavior)
            typingMode(options.typingMode)
        }

        private val compilerPipeline = CompilerPipeline.build(options.ion) {
            options.functions.forEach { functionBlock ->
                val function = functionBlock.invoke(valueFactory)
                addFunction(function)
            }
            compileOptions(compileOptions)
            sqlParser(options.parser)
        }

        override fun compile(input: String, session: EvaluationSession): PartiQLResult {
            val exprValue = compilerPipeline.compile(input).eval(session)
            return PartiQLResult.Value(exprValue)
        }
    }

    /**
     * Wraps the PartiQLCompilerPipeline
     */
    class PipelineExperimental(options: PipelineOptions) : AbstractPipeline(options) {

        private val evaluatorOptions = org.partiql.lang.planner.EvaluatorOptions.Builder()
            .defaultTimezoneOffset(ZoneOffset.UTC)
            .projectionIteration(options.projectionIterationBehavior)
            .thunkOptions(ThunkOptions.standard())
            .typingMode(options.typingMode)
            .typedOpBehavior(options.typedOpBehavior)
            .build()

        private val plannerOptions = org.partiql.lang.planner.PartiQLPlanner.Options(
            allowedUndefinedVariables = true,
            typedOpBehavior = options.typedOpBehavior
        )

        override fun compile(input: String, session: EvaluationSession): PartiQLResult {
            val globalVariableResolver = createGlobalVariableResolver(session)
            val pipeline = PartiQLCompilerPipeline(
                parser = options.parser,
                planner = PartiQLPlannerBuilder.standard()
                    .options(plannerOptions)
                    .globalVariableResolver(globalVariableResolver)
                    .build(),
                compiler = PartiQLCompilerBuilder.standard()
                    .ionSystem(options.ion)
                    .options(evaluatorOptions)
                    .build(),
            )
            return pipeline.compile(input).eval(session)
        }

        private fun createGlobalVariableResolver(session: EvaluationSession) = GlobalVariableResolver {
            val value = session.globals[it]
            if (value != null) {
                GlobalResolutionResult.GlobalVariable(it.name)
            } else {
                GlobalResolutionResult.Undefined
            }
        }
    }
}
