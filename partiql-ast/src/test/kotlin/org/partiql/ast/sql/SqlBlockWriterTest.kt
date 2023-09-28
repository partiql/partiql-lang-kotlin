package org.partiql.ast.sql

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class SqlBlockWriterTest {

    @ParameterizedTest(name = "write #{index}")
    @MethodSource("onelineCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun write(case: Case) = case.assert()

    @ParameterizedTest(name = "format #{index}")
    @MethodSource("formatCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun format(case: Case) = case.assert()

    companion object {

        private fun block(): SqlBlock {

            return NIL + "aaa[" + NL + nest {
                NIL + "bbbbb[" + NL + nest {
                    NIL + "ccc," + NL + "dd" + NL
                } + "]," + NL + "eee," + NL + "ffff[" + NL + nest {
                    NIL + "gg," + NL + "hhh," + NL + "ii" + NL
                } + "]" + NL
            } + "]"
        }

        @JvmStatic
        fun onelineCases() = listOf(
            oneline("aaa[bbbbb[ccc,dd],eee,ffff[gg,hhh,ii]]") { block() },
        )

        @JvmStatic
        fun formatCases() = listOf(
            format(
                """
                    |aaa[
                    |  bbbbb[
                    |    ccc,
                    |    dd
                    |  ],
                    |  eee,
                    |  ffff[
                    |    gg,
                    |    hhh,
                    |    ii
                    |  ]
                    |]
                    """.trimMargin()
            ) { block() }
        )

        private fun format(expected: String, block: () -> SqlBlock) = Case(block(), expected, SqlLayout.DEFAULT::format)

        private fun oneline(expected: String, block: () -> SqlBlock) = Case(block(), expected, SqlLayout.ONELINE::format)

        private fun r(text: String) = SqlBlock.Text(text)
    }

    class Case(
        private val input: SqlBlock,
        private val expected: String,
        private val action: (SqlBlock) -> String,
    ) {

        fun assert() {
            val actual = action(input)
            Assertions.assertEquals(expected, actual)
        }
    }
}
