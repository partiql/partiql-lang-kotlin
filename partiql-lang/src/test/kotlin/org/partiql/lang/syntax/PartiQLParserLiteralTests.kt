package org.partiql.lang.syntax

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class PartiQLParserLiteralTests : PartiQLParserTestBase() {

    override val targets: Array<ParserTarget> = arrayOf(ParserTarget.EXPERIMENTAL)

    @ParameterizedTest
    @MethodSource("cases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testAll(case: Case) {
        assertExpression(
            source = case.input,
            expectedPigAst = case.expect,
        )
        Long.MAX_VALUE
    }

    companion object {

        @JvmStatic
        fun cases() = listOf(
            Case(
                input = "1",
                expect = "(lit 1)"
            ),
            Case(
                input = "+-1",
                expect = "(lit -1)"
            ),
            Case(
                input = "-+1",
                expect = "(lit -1)"
            ),
            Case(
                input = "-+-1",
                expect = "(lit 1)"
            ),
            Case(
                input = "+++1",
                expect = "(lit 1)"
            ),
            Case(
                input = "-1",
                expect = "(lit -1)"
            ),
            Case(
                input = "+1",
                expect = "(lit 1)"
            ),
            Case(
                input = "9223372036854775808", // Long.MAX_VALUE + 1
                expect = "(lit 9223372036854775808)"
            ),
            Case(
                input = "-9223372036854775809", // Long.MIN_VALUE - 1
                expect = "(lit -9223372036854775809)"
            ),
            Case(
                input = "+9223372036854775808",
                expect = "(lit 9223372036854775808)"
            ),
        )
    }

    class Case(
        @JvmField val input: String,
        @JvmField val expect: String,
    )
}
