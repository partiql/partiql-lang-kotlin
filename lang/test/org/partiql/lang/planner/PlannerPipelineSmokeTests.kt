package org.partiql.lang.planner

import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.toIonValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.partiql.lang.ION
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.planner.transforms.PLAN_VERSION_NUMBER
import org.partiql.lang.planner.transforms.PlanningProblemDetails
import org.partiql.lang.util.SexpAstPrettyPrinter

/**
 * Query planning primarily consists of AST traversals and rewrites.  Each of those are thoroughly tested separately,
 * but it is still good to have a simple "smoke test" for the planner pipeline.
 */
class PlannerPipelineSmokeTests {
    private val ion = IonSystemBuilder.standard().build()

    @Suppress("DEPRECATION")
    private fun createPlannerPipelineForTest(allowUndefinedVariables: Boolean) = PlannerPipeline.build(ion) {
        allowUndefinedVariables(allowUndefinedVariables)
        metadataResolver(createFakeMetadataResolver("Customer" to "fake_uid_for_Customer"))
    }

    @Test
    fun `happy path`() {
        val pipeline = createPlannerPipelineForTest(allowUndefinedVariables = true)
        val result = pipeline.plan("SELECT c.* FROM Customer AS c WHERE c.primaryKey = 42")

        result as PassResult.Success
        println(SexpAstPrettyPrinter.format(result.result.toIonElement().asAnyElement().toIonValue(ION)))

        assertEquals(
            result,
            PassResult.Success(
                result = PartiqlPhysical.build {
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
                                        expr = globalId("fake_uid_for_Customer", caseInsensitive()),
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
        val qp = createPlannerPipelineForTest(allowUndefinedVariables = false)
        val result = qp.plan("SELECT undefined.* FROM Customer AS c")
        assertEquals(
            PassResult.Error<PartiqlPhysical.Statement>(
                listOf(problem(1, 8, PlanningProblemDetails.UndefinedVariable("undefined", caseSensitive = false)))
            ),
            result
        )
    }
}
