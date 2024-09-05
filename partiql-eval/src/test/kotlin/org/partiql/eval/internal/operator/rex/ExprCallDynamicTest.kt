package org.partiql.eval.internal.operator.rex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.eval.value.Datum
import org.partiql.eval.value.Datum.bag
import org.partiql.eval.value.Datum.bool
import org.partiql.eval.value.Datum.integer
import org.partiql.eval.value.Datum.list
import org.partiql.eval.value.Datum.string
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

class ExprCallDynamicTest {

    @ParameterizedTest
    @MethodSource("sanityTestsCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun sanityTests(tc: DynamicTestCase) = tc.assert()

    public class DynamicTestCase(
        val lhs: Datum,
        val rhs: Datum,
        val expectedIndex: Int,
    ) {

        @OptIn(PartiQLValueExperimental::class)
        fun assert() {
            val expr = ExprCallDynamic(
                name = "example_function",
                candidateFns = candidates,
                args = arrayOf(ExprLit(lhs), ExprLit(rhs)),
            )
            val result = expr.eval(Environment.empty).check(PartiQLValueType.INT32)
            assertEquals(expectedIndex, result.int)
        }

        companion object {

            @OptIn(PartiQLValueExperimental::class)
            private val params = listOf(
                PartiQLValueType.LIST to PartiQLValueType.LIST, // Index 0
                PartiQLValueType.BAG to PartiQLValueType.BAG, // Index 1
                PartiQLValueType.INT8 to PartiQLValueType.INT8, // Index 2
                PartiQLValueType.INT16 to PartiQLValueType.INT16, // Index 3
                PartiQLValueType.INT32 to PartiQLValueType.INT32, // Index 4
                PartiQLValueType.INT64 to PartiQLValueType.INT64, // Index 5
                PartiQLValueType.STRING to PartiQLValueType.STRING, // Index 6
                PartiQLValueType.ANY to PartiQLValueType.LIST, // Index 7
                PartiQLValueType.BAG to PartiQLValueType.ANY, // Index 8
                PartiQLValueType.INT32 to PartiQLValueType.STRING, // Index 9
                PartiQLValueType.INT64 to PartiQLValueType.STRING, // Index 10
                PartiQLValueType.LIST to PartiQLValueType.ANY, // Index 11
                PartiQLValueType.ANY to PartiQLValueType.ANY, // Index 12
            )

            @OptIn(PartiQLValueExperimental::class)
            internal val candidates: Array<Fn> = params.mapIndexed { index, it ->
                object : Fn {
                    override val signature: FnSignature = FnSignature(
                        name = "example_function",
                        returns = PType.integer(),
                        parameters = listOf(
                            FnParameter("first", type = it.first.toPType()),
                            FnParameter("second", type = it.second.toPType()),
                        )
                    )

                    override fun invoke(args: Array<Datum>): Datum = integer(index)
                }
            }.toTypedArray()
        }
    }

    companion object {

        @OptIn(PartiQLValueExperimental::class)
        @JvmStatic
        fun sanityTestsCases() = listOf(
            DynamicTestCase(
                lhs = integer(20),
                rhs = integer(40),
                expectedIndex = 4
            ),
            DynamicTestCase(
                lhs = list(emptyList()),
                rhs = list(emptyList()),
                expectedIndex = 0
            ),
            DynamicTestCase(
                lhs = bag(emptyList()),
                rhs = bag(emptyList()),
                expectedIndex = 1
            ),
            DynamicTestCase(
                lhs = string("hello"),
                rhs = string("world"),
                expectedIndex = 6
            ),
            DynamicTestCase(
                lhs = string("hello"),
                rhs = list(emptyList()),
                expectedIndex = 7
            ),
            DynamicTestCase(
                lhs = bag(emptyList()),
                rhs = string("world"),
                expectedIndex = 8
            ),
            DynamicTestCase(
                lhs = integer(20),
                rhs = string("world"),
                expectedIndex = 9
            ),
            DynamicTestCase(
                lhs = list(emptyList()),
                rhs = string("hello"),
                expectedIndex = 11
            ),
            DynamicTestCase(
                lhs = bool(true),
                rhs = bool(false),
                expectedIndex = 12
            ),
        )
    }
}
