package org.partiql.lang.planner.transforms.optimizations

import org.junit.jupiter.api.Assertions
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.planner.PartiQLPlannerPass
import org.partiql.lang.planner.transforms.PLAN_VERSION_NUMBER

/** A test case for [PartiQLPlannerPass.Physical] implementations that work on expressions in the [PartiqlPhysical] domain. */
data class PhysicalPlanPassBexprTestCase(
    private val inputBexpr: PartiqlPhysical.Bexpr,
    private val expectedOutputBexpr: PartiqlPhysical.Bexpr
) {
    fun runTest(pass: PartiQLPlannerPass.Physical) {
        val expectedOutputPlan = makeFakePlan(expectedOutputBexpr)
        val inputPlan = makeFakePlan(inputBexpr)

        val actualOutputPlan = pass.apply(
            inputPlan,
            object : ProblemHandler {
                override fun handleProblem(problem: Problem) {
                    error("no errors were expected")
                }
            }
        )

        Assertions.assertEquals(expectedOutputPlan, actualOutputPlan)
    }

    /** Reduces boilerplate when specifying expected plans. */
    private fun makeFakePlan(bexpr: PartiqlPhysical.Bexpr) =
        PartiqlPhysical.build {
            plan(
                query(
                    bindingsToValues(
                        localId(0), // doesn't matter can be any variable
                        bexpr
                    )
                ),
                version = PLAN_VERSION_NUMBER
            )
        }
}
