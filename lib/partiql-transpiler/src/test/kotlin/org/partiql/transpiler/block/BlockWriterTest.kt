package org.partiql.transpiler.block

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class BlockWriterTest {

    @ParameterizedTest(name = "write #{index}")
    @MethodSource("writeCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun write(case: Case) = case.assert()

    @ParameterizedTest(name = "format #{index}")
    @MethodSource("formatCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun format(case: Case) = case.assert()

    companion object {

        private fun block(): Block {

            return NIL + "aaa[" + NL + nest {
                NIL + "bbbbb[" + NL + nest {
                    NIL + "ccc," + NL + "dd" + NL
                } + "]," + NL + "eee," + NL + "ffff[" + NL + nest {
                    NIL + "gg," + NL + "hhh," + NL + "ii" + NL
                } + "]" + NL
            } + "]"
        }

        @JvmStatic
        fun writeCases() = listOf(
            write("aaa[bbbbb[ccc,dd],eee,ffff[gg,hhh,ii]]") { block() },
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

        private fun format(expected: String, block: () -> Block) = Case(block(), expected, BlockWriter::format)

        private fun write(expected: String, block: () -> Block) = Case(block(), expected, BlockWriter::write)

        private fun r(text: String) = Block.Text(text)
    }

    class Case(
        private val input: Block,
        private val expected: String,
        private val action: (Block) -> String,
    ) {

        fun assert() {
            val actual = action(input)
            Assertions.assertEquals(expected, actual)
        }
    }
}
