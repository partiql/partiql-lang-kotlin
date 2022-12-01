package org.partiql.lang.planner.transforms.optimizations

import org.junit.jupiter.api.Assertions.assertEquals
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.planner.PartiQLPhysicalPass
import org.partiql.lang.planner.transforms.PLAN_VERSION_NUMBER

/** A test case for [PartiQLPhysicalPass] implementations that work on expressions in the [PartiqlPhysical] domain. */
data class PhysicalPlanPassExprTestCase(
    private val inputExpr: PartiqlPhysical.Expr,
    private val expectedOutputExpr: PartiqlPhysical.Expr
) {
    fun runTest(pass: PartiQLPhysicalPass) {
        val expectedOutputPlan = makeFakePlan(expectedOutputExpr)
        val inputPlan = makeFakePlan(inputExpr)

        val actualOutputPlan = pass.apply(
            inputPlan,
            object : ProblemHandler {
                override fun handleProblem(problem: Problem) {
                    error("no errors were expected")
                }
            }
        )

        assertEquals(expectedOutputPlan, actualOutputPlan)
    }

    private fun makeFakePlan(expr: PartiqlPhysical.Expr) = PartiqlPhysical.build {
        plan(
            query(expr),
            version = PLAN_VERSION_NUMBER
        )
    }
}
