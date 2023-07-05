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

package org.partiql.cli.pipeline

import com.amazon.ion.IonSystem
import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import com.amazon.ionelement.api.toIonValue
import org.partiql.annotations.ExperimentalPartiQLCompilerPipeline
import org.partiql.cli.Debug
import org.partiql.cli.functions.QueryDDB
import org.partiql.cli.functions.ReadFile_1
import org.partiql.cli.functions.ReadFile_2
import org.partiql.cli.functions.WriteFile_1
import org.partiql.cli.functions.WriteFile_2
import org.partiql.cli.utils.ServiceLoaderUtil
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.compiler.PartiQLCompilerBuilder
import org.partiql.lang.compiler.PartiQLCompilerPipeline
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
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
            PipelineType.STANDARD -> PipelineStandard(options)
            PipelineType.EXPERIMENTAL -> PipelineExperimental(options)
            PipelineType.DEBUG -> PipelineDebug(options)
        }
        internal fun convertExprValue(value: ExprValue): PartiQLResult {
            return PartiQLResult.Value(value)
        }
        internal fun standard(): AbstractPipeline {
            return create(PipelineOptions())
        }

        internal fun createPipelineOptions(
            pipeline: PipelineType,
            typedOpBehavior: TypedOpBehavior,
            projectionIteration: ProjectionIterationBehavior,
            undefinedVariable: UndefinedVariableBehavior,
            permissiveMode: TypingMode
        ): PipelineOptions {
            val ion = IonSystemBuilder.standard().build()
            val functions: List<ExprFunction> = listOf(
                ReadFile_1(ion),
                ReadFile_2(ion),
                WriteFile_1(ion),
                WriteFile_2(ion),
                QueryDDB(ion)
            ) + ServiceLoaderUtil.loadPlugins()
            val parser = PartiQLParserBuilder().build()
            return PipelineOptions(
                pipeline,
                ion,
                parser,
                typedOpBehavior,
                projectionIteration,
                undefinedVariable,
                permissiveMode,
                functions = functions
            )
        }
    }

    data class PipelineOptions(
        val pipeline: PipelineType = PipelineType.STANDARD,
        val ion: IonSystem = IonSystemBuilder.standard().build(),
        val parser: Parser = PartiQLParserBuilder.standard().build(),
        val typedOpBehavior: TypedOpBehavior = TypedOpBehavior.HONOR_PARAMETERS,
        val projectionIterationBehavior: ProjectionIterationBehavior = ProjectionIterationBehavior.FILTER_MISSING,
        val undefinedVariableBehavior: UndefinedVariableBehavior = UndefinedVariableBehavior.ERROR,
        val typingMode: TypingMode = TypingMode.LEGACY,
        val functions: List<ExprFunction> = emptyList()
    )

    internal enum class PipelineType {
        STANDARD,
        EXPERIMENTAL,
        DEBUG,
    }

    /**
     * Wrap the Main.kt debug function in a pipeline
     */
    internal class PipelineDebug(options: PipelineOptions) : AbstractPipeline(options) {

        override fun compile(input: String, session: EvaluationSession): PartiQLResult {
            val (message, status) = try {
                Debug.action(input, session) to 0L
            } catch (e: Exception) {
                e.stackTraceToString() to 1L
            }
            val value = ionStructOf(
                "message" to ionString(message),
                "status" to ionInt(status),
            )
            return PartiQLResult.Value(ExprValue.of(value.toIonValue(options.ion)))
        }
    }

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

        private val compilerPipeline = CompilerPipeline.build {
            options.functions.forEach { function ->
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
    @OptIn(ExperimentalPartiQLCompilerPipeline::class)
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
