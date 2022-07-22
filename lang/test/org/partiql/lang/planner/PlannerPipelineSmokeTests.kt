package org.partiql.lang.planner

import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.lang.ION
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemDetails
import org.partiql.lang.errors.ProblemSeverity
import org.partiql.lang.eval.physical.SetVariableFunc
import org.partiql.lang.eval.physical.operators.RelationExpression
import org.partiql.lang.eval.physical.operators.ScanRelationalOperatorFactory
import org.partiql.lang.eval.physical.operators.ValueExpression
import org.partiql.lang.eval.physical.sourceLocationMetaOrUnknown
import org.partiql.lang.planner.transforms.DEFAULT_IMPL_NAME
import org.partiql.lang.planner.transforms.PLAN_VERSION_NUMBER

/**
 * Query planning primarily consists of AST traversals and rewrites.  Each of those are thoroughly tested separately,
 * but it is still good to have a simple "smoke test" for the planner pipeline.
 */
class PlannerPipelineSmokeTests {

    @Suppress("DEPRECATION")
    private fun createPlannerPipelineForTest(
        allowUndefinedVariables: Boolean,
        plannerEventCallback: PlannerEventCallback?,
        block: PlannerPipeline.Builder.() -> Unit = { }
    ) = PlannerPipeline.build(ION) {
        allowUndefinedVariables(allowUndefinedVariables)
        globalVariableResolver(createFakeGlobalsResolver("Customer" to "fake_uid_for_Customer"))
        plannerEventCallback?.let { plannerEventCallback(it) }
        block()
    }

    @Test
    fun `happy path`() {
        var pecCallbacks = 0
        val plannerEventCallback: PlannerEventCallback = { _ -> pecCallbacks++ }

        val pipeline = createPlannerPipelineForTest(allowUndefinedVariables = true, plannerEventCallback = plannerEventCallback)

        val planResult = pipeline.plan("SELECT c.* FROM Customer AS c WHERE c.primaryKey = 42")
            as PlannerPassResult.Success<PartiqlPhysical.Plan>

        // we call compile even tho we do nothing with the result just to ensure the PlannerEventCallback is invoked
        // for the compile pass.
        pipeline.compile(planResult.output)

        // pec should be called once for each pass in the planner:
        // - parse
        // - normalize ast
        // - ast -> logical
        // - logical -> logical resolved
        // - logical resolved -> default physical
        // - compile
        assertEquals(6, pecCallbacks)

        assertEquals(
            planResult,
            PlannerPassResult.Success(
                output = PartiqlPhysical.build {
                    plan(
                        stmt = query(
                            bindingsToValues(
                                exp = struct(structFields(localId(0))),
                                query = filter(
                                    i = impl("default"),
                                    predicate = eq(
                                        operands0 = path(
                                            localId(0),
                                            pathExpr(lit(ionString("primaryKey")), caseInsensitive())
                                        ),
                                        operands1 = lit(ionInt(42))
                                    ),
                                    source = scan(
                                        i = impl("default"),
                                        expr = globalId("fake_uid_for_Customer"),
                                        asDecl = varDecl(0)
                                    )
                                )
                            )
                        ),
                        locals = listOf(localVariable("c", 0)),
                        version = PLAN_VERSION_NUMBER
                    )
                },
                warnings = emptyList()
            )
        )
    }

    @Test
    fun `undefined variable`() {
        val qp = createPlannerPipelineForTest(allowUndefinedVariables = false, plannerEventCallback = null)
        val result = qp.plan("SELECT undefined.* FROM Customer AS c")
        assertEquals(
            PlannerPassResult.Error<PartiqlPhysical.Statement>(
                listOf(problem(1, 8, PlanningProblemDetails.UndefinedVariable("undefined", caseSensitive = false)))
            ),
            result
        )
    }

    @Test
    fun `physical plan pass - happy path`() {
        val qp = createPlannerPipelineForTest(allowUndefinedVariables = false, plannerEventCallback = null) {
            addPhysicalPlanPass("fake pass 1") { inputPlan, _ ->
                // ensure we're getting the correct plan as input
                assertEquals(createFakePlan(1), inputPlan)

                // instead of doing something smart (this is after all a "smoke" test), just return a different
                // plan entirely.
                createFakePlan(2)
            }
            addPhysicalPlanPass("fake pass 2") { inputPlan, _ ->
                // second pass should get the output of the first pass as input
                assertEquals(createFakePlan(2), inputPlan)
                createFakePlan(3)
            }
            addPhysicalPlanPass("fake pass 3") { inputPlan, _ ->
                // third pass should get the output of the second pass as input
                assertEquals(createFakePlan(3), inputPlan)
                createFakePlan(4)
            }
        }
        val actualPlanResult = qp.plan("1")

        // final plan should be the output of the third pass.
        assertEquals(PlannerPassResult.Success(createFakePlan(4), emptyList()), actualPlanResult)
    }

    private fun createFakePlan(number: Int) =
        PartiqlPhysical.build {
            plan(
                stmt = query(lit(ionInt(number.toLong()))),
                version = PLAN_VERSION_NUMBER
            )
        }

    @Test
    fun `physical plan pass - first user pass sends semantic error`() {
        val qp = createPlannerPipelineForTest(allowUndefinedVariables = false, plannerEventCallback = null) {
            addPhysicalPlanPass("test_pass") { inputPlan, problemHandler ->
                problemHandler.handleProblem(
                    createFakeErrorProblem(inputPlan.stmt.metas.sourceLocationMetaOrUnknown)
                )
                inputPlan
            }

            addPhysicalPlanPass("test_pass_2") { _, _ ->
                error(
                    "This pass should not be reached due to an error being sent to to the problem handler " +
                        "in the previous pass"
                )
            }
        }
        val expectedError = createFakeErrorProblem(SourceLocationMeta(1, 1, 57))

        val actualPassResult = qp.plan(
            // the actual expression doesn't matter as long as it doesn't have an error detected by a built-in pass
            "'the meaning of life, the universe, and everything is 42'"
        )

        assertEquals(PlannerPassResult.Error<PartiqlPhysical.Plan>(listOf(expectedError)), actualPassResult)
    }

    private fun createFakeErrorProblem(sourceLocationMeta: SourceLocationMeta): Problem {
        data class FakeProblemDetails(
            override val severity: ProblemSeverity = ProblemSeverity.ERROR,
            override val message: String = "Ack, the query author presented us with a logical conundrum!"
        ) : ProblemDetails

        return Problem(
            sourceLocationMeta,
            FakeProblemDetails()
        )
    }

    @Test
    fun `duplicate physical operator factories are blocked`() {
        // This will duplicate the default scan operator factory.
        val fakeOperator = object : ScanRelationalOperatorFactory(DEFAULT_IMPL_NAME) {
            override fun create(
                impl: PartiqlPhysical.Impl,
                expr: ValueExpression,
                setAsVar: SetVariableFunc,
                setAtVar: SetVariableFunc?,
                setByVar: SetVariableFunc?
            ): RelationExpression {
                TODO("doesn't matter won't be invoked")
            }
        }

        assertThrows<IllegalArgumentException> {
            @Suppress("DEPRECATION") // don't warn about use of experimental APIs.
            PlannerPipeline.build(ION) {
                addRelationalOperatorFactory(fakeOperator)
            }
        }
    }
}
