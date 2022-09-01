package org.partiql.lang.eval.evaluatortestframework

import org.junit.jupiter.api.fail
import org.partiql.lang.ION
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.TypingMode
import org.partiql.lang.eval.UndefinedVariableBehavior
import org.partiql.lang.planner.EvaluatorOptions
import org.partiql.lang.planner.GlobalResolutionResult
import org.partiql.lang.planner.PlannerPassResult
import org.partiql.lang.planner.PlannerPipeline
import org.partiql.lang.planner.QueryResult
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * Uses the test infrastructure (which is geared toward the legacy [org.partiql.lang.CompilerPipeline]) to create a
 * standard [org.partiql.lang.CompilerPipeline], then creates an equivalent [PlannerPipeline] which is wrapped in
 * an instance of [AbstractPipeline] and returned to the caller.
 *
 * Why?  Because the entire test infrastructure (and the many thousands of tests) are heavily dependent on
 * [org.partiql.lang.CompilerPipeline].
 *
 * TODO: When that class is deprecated or removed we'll want to change the test infrastructure to depend on the
 * [PlannerPipeline] instead.
 */
internal class PlannerPipelineFactory : PipelineFactory {

    override val pipelineName: String
        get() = "PlannerPipeline (and Physical Plan Evaluator)"

    override val target: EvaluatorTestTarget
        get() = EvaluatorTestTarget.PLANNER_PIPELINE

    override fun createPipeline(
        evaluatorTestDefinition: EvaluatorTestDefinition,
        session: EvaluationSession,
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
                "Please set target = EvaluatorTestTarget.COMPILER_PIPELINE for this test.\n" +
                "Test groupName: ${evaluatorTestDefinition.groupName}"
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
            projectionIteration(co.projectionIteration)
        }

        @Suppress("DEPRECATION")
        val plannerPipeline = PlannerPipeline.build(ION) {
            // this is for support of the existing test suite and may not be desirable for all future tests.
            allowUndefinedVariables(true)

            customDataTypes(compilerPipeline.customDataTypes)

            compilerPipeline.functions.forEach { this.addFunction(it) }
            compilerPipeline.procedures.values.forEach { this.addProcedure(it) }

            evaluatorOptions(evaluatorOptions)

            // For compatibility with the unit test suite, prevent the planner from catching SqlException during query
            // compilation and converting them into Problems
            enableLegacyExceptionHandling()

            // Create a fake GlobalVariableResolver implementation which defines any global that is also defined in the
            // session.
            globalVariableResolver { bindingName ->
                val boundValue = session.globals[bindingName]
                if (boundValue != null) {
                    // There is no way to tell the actual name of the global variable as it exists
                    // in session.globals (case may differ).  For now we simply have to use binding.name
                    // as the uniqueId of the variable, however, this is not desirable in production
                    // scenarios.  Ideally the name of the variable in the letter case of its declaration
                    // should be used.
                    GlobalResolutionResult.GlobalVariable(bindingName.name)
                } else {
                    GlobalResolutionResult.Undefined
                }
            }

            scalarTypeSystem(compilerPipeline.scalarTypeSystem)
        }

        return object : AbstractPipeline {
            override val typingMode: TypingMode
                get() = evaluatorOptions.typingMode

            override fun evaluate(query: String): ExprValue {
                when (val planningResult = plannerPipeline.planAndCompile(query)) {
                    is PlannerPassResult.Error -> {
                        fail("Query compilation unexpectedly failed: ${planningResult.errors}")
                    }
                    is PlannerPassResult.Success -> {
                        when (val queryResult = planningResult.output.eval(session)) {
                            is QueryResult.DmlCommand -> error("DML is not supported by test suite")
                            is QueryResult.Value -> return queryResult.value
                        }
                    }
                }
            }
        }
    }
}
