package org.partiql.planner

import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionSymbol
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.planner.transforms.PlanningProblemDetails

/**
 * Query planning primarily consists of AST traversals and rewrites.  Each of those are thoroughly tested separately,
 * so the tests here will focus on functionality that is specific to the planner itself, such as the ability of the
 * planner to parse & send the resulting AST thru all of the passes, and to stop planning when encountering semantic
 * problems such as undefined variables.
 */
class QueryPlannerImplIntegrationTests {
    private val ion = IonSystemBuilder.standard().build()

    private val globalBindings = createFakeGlobalBindings("Customer")

    @Test
    fun `happy path`() {
        val qp = createQueryPlanner(ion, allowUndefinedVariables = true, globalBindings)
        val result = qp.plan("SELECT c.* FROM Customer AS c WHERE c.primaryKey = 42")
        println(result)

        assertEquals(
            result,
            PlanningResult.Success(
                physicalPlan = PartiqlPhysical.build {
                    query(
                        mapValues(
                            exp = localId("c", 0),
                            query = filter(
                                i = impl("default"),
                                predicate = eq(
                                    operands0 = path(localId("c", 0), pathExpr(lit(ionString("primaryKey")), caseInsensitive())),
                                    operands1 = lit(ionInt(42))
                                ),
                                source = scan(
                                    i = impl("default"),
                                    expr = globalId("Customer", "fake_uid_for_Customer"),
                                    asDecl = varDecl("c", 0)
                                )
                            )
                        )
                    )
                },
                warnings = emptyList()
            )
        )
    }

    @Test
    fun `undefined variable`() {
        val qp = createQueryPlanner(ion, allowUndefinedVariables = false, globalBindings)
        val result = qp.plan("SELECT undefined.* FROM Customer AS c")
        assertEquals(
            PlanningResult.Error(
                listOf(problem(1, 8, PlanningProblemDetails.UndefinedVariable("undefined", caseSensitive = false)))
            ),
            result
        )
    }
}