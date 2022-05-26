package org.partiql.lang.eval.evaluatortestframework

import org.junit.jupiter.api.fail
import org.partiql.lang.ION
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.TypingMode
import org.partiql.lang.eval.UndefinedVariableBehavior
import org.partiql.lang.planner.EvaluatorOptions
import org.partiql.lang.planner.MetadataResolver
import org.partiql.lang.planner.PassResult
import org.partiql.lang.planner.PlannerPipeline
import org.partiql.lang.planner.ResolutionResult
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
            typedOpBehavior(co.typedOpBehavior)
            projectionIteration(co.projectionIteration)
        }

        @Suppress("DEPRECATION")
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

            // Create a fake MetadataResolver implementation which defines any global that is also defined in the
            // session.
            metadataResolver(
                object : MetadataResolver {
                    override fun resolveVariable(bindingName: BindingName): ResolutionResult {
                        val boundValue = session.globals[bindingName]
                        return if (boundValue != null) {
                            // There is no way to tell the actual name of the global variable as it exists
                            // in session.globals (case may differ).  For now we simply have to use binding.name
                            // as the uniqueId of the variable, however, this is not desirable in production
                            // scenarios.  At minimum, the name of the variable in its original letter-case should be
                            // used.
                            ResolutionResult.GlobalVariable(bindingName.name)
                        } else {
                            ResolutionResult.Undefined
                        }
                    }
                }
            )
        }

        return object : AbstractPipeline {
            override val typingMode: TypingMode
                get() = evaluatorOptions.typingMode

            override fun evaluate(query: String): ExprValue {
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
