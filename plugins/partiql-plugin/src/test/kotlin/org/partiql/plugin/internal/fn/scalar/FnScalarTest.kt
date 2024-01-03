package org.partiql.plugin.internal.fn.scalar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.boolValue
import org.partiql.value.int8Value

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
class FnScalarTest {

    @ParameterizedTest
    @MethodSource("fnGt")
    @Execution(ExecutionMode.CONCURRENT)
    fun testGt(case: Case) = case.assert()

    companion object {

        @JvmStatic
        fun fnGt() = listOf(
            FnGt0.tests {
                case(arrayOf(int8Value(1), int8Value(0)), result = boolValue(true))
                case(arrayOf(int8Value(0), int8Value(1)), result = boolValue(false))
                case(arrayOf(int8Value(0), int8Value(0)), result = boolValue(false))
                case(arrayOf(int8Value(null), int8Value(0)), result = boolValue(null))
            }
        ).flatten()

        private fun PartiQLFunction.Scalar.tests(block: CasesBuilder.() -> Unit): List<Case> {
            val builder = CasesBuilder(this)
            builder.block()
            return builder.cases
        }
    }

    class CasesBuilder(private val fn: PartiQLFunction.Scalar) {

        val cases: MutableList<Case> = mutableListOf()

        fun case(args: Array<PartiQLValue>, result: PartiQLValue) {
            val case = Case(fn, args, result)
            cases.add(case)
        }
    }

    class Case(
        private val fn: PartiQLFunction.Scalar,
        private val args: Array<PartiQLValue>,
        private val expected: PartiQLValue,
    ) {

        fun assert() {
            val actual = fn.invoke(args)
            assertEquals(expected, actual)
        }
    }
}
