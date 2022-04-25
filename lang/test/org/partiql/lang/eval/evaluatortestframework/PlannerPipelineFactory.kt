package org.partiql.lang.eval.evaluatortestframework

import org.junit.jupiter.api.fail
import org.partiql.lang.ION
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.TypingMode
import org.partiql.lang.eval.UndefinedVariableBehavior
import org.partiql.lang.planner.EvaluatorOptions
import org.partiql.lang.planner.PassResult
import org.partiql.lang.planner.PlannerPipeline
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

internal class PlannerPipelineFactory : PipelineFactory {

    override val pipelineName: String
        get() = "PlannerPipeline (and Physical Plan Evaluator)"

    override val target: EvaluatorTestTarget
        get() = EvaluatorTestTarget.PLANNER_PIPELINE

    override fun createPipeline(
        evaluatorTestDefinition: EvaluatorTestDefinition,
        forcePermissiveMode: Boolean
    ): AbstractPipeline {

        // Construct a legacy CompilerPipeline
        val compilerPipeline = evaluatorTestDefinition.createCompilerPipeline(forcePermissiveMode)

        // Convert it to a PlannerPipeline (to avoid having to refactor many tests cases to use
        // PlannerPipeline.Builder and EvaluatorOptions.Builder.
        val co = compilerPipeline.compileOptions

        assertNotEquals(
            co.undefinedVariable, UndefinedVariableBehavior.MISSING,
            "The planner and physical plan evaluator do not support UndefinedVariableBehavior.MISSING. " +
                "Please set target = EvaluatorTestTarget.COMPILER_PIPELINE for this test."
        )

        assertNull(
            compilerPipeline.globalTypeBindings,
            "The planner and physical plan evaluator do not support globalTypeBindings (yet)" +
                "Please set target = EvaluatorTestTarget.COMPILER_PIPELINE for this test."
        )

        val evaluatorOptions = EvaluatorOptions.build {
            typingMode(co.typingMode)
            thunkOptions(co.thunkOptions)
            defaultTimezoneOffset(co.defaultTimezoneOffset)
            typedOpBehavior(co.typedOpBehavior)
            projectionIteration(co.projectionIteration)
        }

        val plannerPipeline = PlannerPipeline.build(ION) {
            // this is for support of the existing test suite and may not be desirable for all future tests.
            allowUndefinedVariables(true)

            customDataTypes(compilerPipeline.customDataTypes)

            compilerPipeline.functions.values.forEach { this.addFunction(it) }
            compilerPipeline.procedures.values.forEach { this.addProcedure(it) }

            evaluatorOptions(evaluatorOptions)

            // For compatibility with the unit test suite, prevent the planner from catching SqlException during query
            // compilation and converting them into Problems
            enableLegacyExceptionHandling()
        }

        return object : AbstractPipeline {
            override val typingMode: TypingMode
                get() = evaluatorOptions.typingMode

            override fun evaluate(query: String, session: EvaluationSession): ExprValue {
                when (val planningResult = plannerPipeline.planAndCompile(query)) {
                    is PassResult.Error -> {
                        fail("Query compilation unexpectedly failed: ${planningResult.errors}")
                    }
                    is PassResult.Success -> {
                        return planningResult.result.eval(session)
                    }
                }
            }
        }
    }
}
