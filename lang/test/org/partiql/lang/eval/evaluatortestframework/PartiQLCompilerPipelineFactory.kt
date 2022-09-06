package org.partiql.lang.eval.evaluatortestframework

import org.partiql.lang.ION
import org.partiql.lang.compiler.PartiQLCompilerPipeline
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.eval.TypingMode
import org.partiql.lang.eval.UndefinedVariableBehavior
import org.partiql.lang.planner.EvaluatorOptions
import org.partiql.lang.planner.GlobalResolutionResult
import org.partiql.lang.planner.GlobalVariableResolver
import org.partiql.lang.planner.PartiQLPlanner
import org.partiql.lang.syntax.SqlParser
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * TODO delete this once evaluator tests are replaced by `partiql-tests`
 */
internal class PartiQLCompilerPipelineFactory : PipelineFactory {

    override val pipelineName: String = "PartiQLCompilerPipeline"

    override val target: EvaluatorTestTarget = EvaluatorTestTarget.PLANNER_PIPELINE

    override fun createPipeline(
        evaluatorTestDefinition: EvaluatorTestDefinition,
        session: EvaluationSession,
        forcePermissiveMode: Boolean
    ): AbstractPipeline {

        // Construct a legacy CompilerPipeline
        val legacyPipeline = evaluatorTestDefinition.createCompilerPipeline(forcePermissiveMode)
        val co = legacyPipeline.compileOptions

        assertNotEquals(
            co.undefinedVariable, UndefinedVariableBehavior.MISSING,
            "The planner and physical plan evaluator do not support UndefinedVariableBehavior.MISSING. " +
                "Please set target = EvaluatorTestTarget.COMPILER_PIPELINE for this test.\n" +
                "Test groupName: ${evaluatorTestDefinition.groupName}"
        )

        assertNull(
            legacyPipeline.globalTypeBindings,
            "The planner and evaluator do not currently support globalTypeBindings" +
                "Please set target = EvaluatorTestTarget.COMPILER_PIPELINE for this test."
        )

        val evaluatorOptions = EvaluatorOptions.build {
            typingMode(co.typingMode)
            thunkOptions(co.thunkOptions)
            defaultTimezoneOffset(co.defaultTimezoneOffset)
            typedOpBehavior(co.typedOpBehavior)
            projectionIteration(co.projectionIteration)
        }

        val globalVariableResolver = GlobalVariableResolver {
            val value = session.globals[it]
            if (value != null) {
                GlobalResolutionResult.GlobalVariable(it.name)
            } else {
                GlobalResolutionResult.Undefined
            }
        }

        val plannerOptions = PartiQLPlanner.Options(
            allowedUndefinedVariables = true
        )

        val pipeline = PartiQLCompilerPipeline.build {
            parser = SqlParser(ION, customTypes = legacyPipeline.customDataTypes)
            planner
                .withOptions(plannerOptions)
                .withGlobalVariableResolver(globalVariableResolver)
            compiler
                .withIonSystem(ION)
                .withOptions(evaluatorOptions)
                .withCustomTypes(legacyPipeline.customDataTypes)
                .withCustomFunctions(legacyPipeline.functions.values.toList())
                .withCustomProcedures(legacyPipeline.procedures.values.toList())
        }

        return object : AbstractPipeline {

            override val typingMode: TypingMode = evaluatorOptions.typingMode

            override fun evaluate(query: String): ExprValue {
                val statement = pipeline.compile(query)
                return when (val result = statement.eval(session)) {
                    is PartiQLResult.Delete,
                    is PartiQLResult.Insert -> error("DML is not supported by test suite")
                    is PartiQLResult.Value -> result.value
                }
            }
        }
    }
}
