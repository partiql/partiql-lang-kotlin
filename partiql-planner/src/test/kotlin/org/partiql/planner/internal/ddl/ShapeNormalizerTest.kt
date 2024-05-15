
package org.partiql.planner.internal.ddl

import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.planner.internal.ir.DdlOp
import kotlin.test.assertEquals

// Those test validates constraint validation logic and shape normalization logic
internal class ShapeNormalizerTest {

    val typer = DDLTestBase.typer.DdlTyper()

    fun run(tc: DDLTestBase.TestCase) {
        if (tc.typed != null) {
            assertEquals(
                tc.typed,
                typer.visitDdlOpCreateTable(tc.untyped as DdlOp.CreateTable, emptyList())
            )
        } else {
            assertThrows<NotImplementedError> {
                typer.visitDdlOpCreateTable(tc.untyped as DdlOp.CreateTable, emptyList())
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
