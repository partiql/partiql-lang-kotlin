package org.partiql.planner.internal.ddl

import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.planner.internal.ir.PartiQLPlan
import org.partiql.planner.internal.ir.statementDDL
import org.partiql.planner.internal.transforms.PlanTransform
import org.partiql.types.StaticType
import kotlin.test.assertEquals

internal class DDLPlanTests {
    val transform = PlanTransform(setOf())
    val typer = DDLTestBase.typer
    fun run(tc: DDLTestBase.TestCase) {
        if (tc.publicPlan != null) {
            val typed = typer.resolve(statementDDL(StaticType.ANY, tc.untyped))
            val internal = PartiQLPlan(typed)
            assertEquals(tc.publicPlan, transform.transform(internal, {}).statement)
        } else {
            assertThrows<IllegalArgumentException> {
                val typed = typer.resolve(statementDDL(StaticType.ANY, tc.untyped))
                val internal = PartiQLPlan(typed)
                transform.transform(internal, {})
            }
        }
    }

    companion object {
        @JvmStatic
        fun testCases() = DDLTestBase.testCases()
    }

    @ParameterizedTest
    @MethodSource("testCases")
    fun test(tc: DDLTestBase.TestCase) = run(tc)
}
