package org.partiql.lang.compiler

import com.amazon.ionelement.api.ionInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.partiql.annotations.ExperimentalPartiQLCompilerPipeline
import org.partiql.errors.Problem
import org.partiql.errors.ProblemDetails
import org.partiql.errors.ProblemLocation
import org.partiql.errors.ProblemSeverity
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.PartiQLException
import org.partiql.lang.eval.physical.SetVariableFunc
import org.partiql.lang.eval.physical.operators.RelationExpressionAsync
import org.partiql.lang.eval.physical.operators.ScanRelationalOperatorFactoryAsync
import org.partiql.lang.eval.physical.operators.ValueExpressionAsync
import org.partiql.lang.eval.physical.sourceLocationMetaOrUnknown
import org.partiql.lang.planner.PartiQLPhysicalPass
import org.partiql.lang.planner.PartiQLPlanner
import org.partiql.lang.planner.PlannerEventCallback
import org.partiql.lang.planner.PlanningProblemDetails
import org.partiql.lang.planner.createFakeGlobalsResolver
import org.partiql.lang.planner.transforms.DEFAULT_IMPL_NAME
import org.partiql.lang.planner.transforms.PLAN_VERSION_NUMBER

internal fun createFakeErrorProblem(sourceLocationMeta: SourceLocationMeta): Problem {
    data class FakeProblemDetails(
        override val severity: ProblemSeverity = ProblemSeverity.ERROR,
        override val message: String = "Ack, the query author presented us with a logical conundrum!"
    ) : ProblemDetails

    return Problem(
        sourceLocationMeta.toProblemLocation(),
        FakeProblemDetails()
    )
}

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPartiQLCompilerPipeline::class)
class PartiQLCompilerPipelineAsyncSmokeTests {
    private fun createPlannerPipelineAsyncForTest(
        allowUndefinedVariables: Boolean,
        plannerEventCallback: PlannerEventCallback?,
        block: PartiQLCompilerPipelineAsync.Builder.() -> Unit = { }
    ) = PartiQLCompilerPipelineAsync.build {
        planner.options(
            PartiQLPlanner.Options(
                allowedUndefinedVariables = allowUndefinedVariables,
            )
        ).callback {
            plannerEventCallback?.invoke(it)
        }.globalVariableResolver(createFakeGlobalsResolver("Customer" to "fake_uid_for_Customer"))
        block()
    }

    @Test
    fun `happy path`() = runTest {
        var pecCallbacks = 0
        val plannerEventCallback: PlannerEventCallback = { _ ->
            pecCallbacks++
        }

        val pipeline = createPlannerPipelineAsyncForTest(allowUndefinedVariables = true, plannerEventCallback = plannerEventCallback)

        // the constructed ASTs are tested separately, here we check the compile function does not throw any exception.
        assertDoesNotThrow {
            pipeline.compile("SELECT c.* FROM Customer AS c WHERE c.primaryKey = 42")
        }

        // pec should be called once for each pass in the planner:
        // - normalize ast
        // - ast -> logical
        // - logical -> logical resolved
        // - logical resolved -> default physical
        assertEquals(4, pecCallbacks)
    }

    @Test
    fun `undefined variable`() = runTest {
        val qp = createPlannerPipelineAsyncForTest(allowUndefinedVariables = false, plannerEventCallback = null)

        val error = assertThrows<PartiQLException> {
            qp.compile("SELECT undefined.* FROM Customer AS c")
        }

        // TODO: We use string comparison until we finalized the error reporting mechanism for PartiQLCompilerPipeline
        assertEquals(
            listOf(Problem(ProblemLocation(1, 8, 9), PlanningProblemDetails.UndefinedVariable("undefined", caseSensitive = false))).toString(),
            error.message
        )
    }

    @Test
    fun `physical plan pass - happy path`() = runTest {
        val qp = createPlannerPipelineAsyncForTest(allowUndefinedVariables = false, plannerEventCallback = null) {
            planner.physicalPlannerPasses(
                listOf(
                    PartiQLPhysicalPass { plan, _ ->
                        assertEquals(createFakePlan(1), plan)
                        createFakePlan(2)
                    },
                    PartiQLPhysicalPass { plan, _ ->
                        assertEquals(createFakePlan(2), plan)
                        createFakePlan(3)
                    },
                    PartiQLPhysicalPass { plan, _ ->
                        assertEquals(createFakePlan(3), plan)
                        createFakePlan(4)
                    },
                )
            )
        }

        assertDoesNotThrow {
            qp.compile("1")
        }
    }

    private fun createFakePlan(number: Int) =
        PartiqlPhysical.build {
            plan(
                stmt = query(lit(ionInt(number.toLong()))),
                version = PLAN_VERSION_NUMBER
            )
        }

    @Test
    fun `physical plan pass - first user pass sends semantic error`() = runTest {
        val qp = createPlannerPipelineAsyncForTest(allowUndefinedVariables = false, plannerEventCallback = null) {
            planner.physicalPlannerPasses(
                listOf(
                    PartiQLPhysicalPass { plan, problemHandler ->
                        problemHandler.handleProblem(
                            createFakeErrorProblem(plan.stmt.metas.sourceLocationMetaOrUnknown)
                        )
                        plan
                    },
                    PartiQLPhysicalPass { _, _ ->
                        error(
                            "This pass should not be reached due to an error being sent to to the problem handler " +
                                "in the previous pass"
                        )
                    },
                    PartiQLPhysicalPass { plan, _ ->
                        assertEquals(createFakePlan(3), plan)
                        createFakePlan(4)
                    },
                )
            )
        }
        val expectedError = createFakeErrorProblem(SourceLocationMeta(1, 1, 57))

        val error = assertThrows<PartiQLException> {
            qp.compile(
                // the actual expression doesn't matter as long as it doesn't have an error detected by a built-in pass
                "'the meaning of life, the universe, and everything is 42'"
            )
        }

        // TODO: We use string comparison until we finalized the error reporting mechanism for PartiQLCompilerPipeline
        assertEquals(listOf(expectedError).toString(), error.message)
    }

    @Test
    fun `duplicate physical operator factories are blocked`() {
        // This will duplicate the default async scan operator factory.
        val fakeOperator = object : ScanRelationalOperatorFactoryAsync(DEFAULT_IMPL_NAME) {
            override fun create(
                impl: PartiqlPhysical.Impl,
                expr: ValueExpressionAsync,
                setAsVar: SetVariableFunc,
                setAtVar: SetVariableFunc?,
                setByVar: SetVariableFunc?
            ): RelationExpressionAsync {
                TODO("doesn't matter won't be invoked")
            }
        }

        assertThrows<IllegalStateException> {
            PartiQLCompilerPipelineAsync.build {
                compiler.customOperatorFactories(
                    listOf(fakeOperator)
                )
            }
        }
    }
}
