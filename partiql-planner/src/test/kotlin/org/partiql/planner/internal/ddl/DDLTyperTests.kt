package org.partiql.planner.internal.ddl

import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.planner.internal.ir.statementDDL
import org.partiql.types.StaticType
import kotlin.test.assertEquals

internal class DDLTyperTests {
    val typer = DDLTestBase.typer
    fun run(tc: DDLTestBase.TestCase) {
        if (tc.resolved != null) {
            assertEquals(tc.resolved.shape, typer.resolveDdl(statementDDL(StaticType.ANY, tc.untyped)).shape)
        } else {
            assertThrows<IllegalArgumentException> {
                typer.resolveDdl(statementDDL(StaticType.ANY, tc.untyped))
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
