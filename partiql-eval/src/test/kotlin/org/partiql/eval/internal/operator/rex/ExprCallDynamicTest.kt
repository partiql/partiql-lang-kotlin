package org.partiql.eval.internal.operator.rex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.eval.value.Datum
import org.partiql.eval.value.Datum.bagValue
import org.partiql.eval.value.Datum.boolValue
import org.partiql.eval.value.Datum.int32Value
import org.partiql.eval.value.Datum.listValue
import org.partiql.eval.value.Datum.stringValue
import org.partiql.planner.internal.fn.Fn
import org.partiql.planner.internal.fn.FnParameter
import org.partiql.planner.internal.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

class ExprCallDynamicTest {

    @ParameterizedTest
    @MethodSource("sanityTestsCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun sanityTests(tc: DynamicTestCase) = tc.assert()

    public class DynamicTestCase @OptIn(PartiQLValueExperimental::class) constructor(
        val lhs: Datum,
        val rhs: Datum,
        val expectedIndex: Int,
    ) {

        @OptIn(PartiQLValueExperimental::class)
        fun assert() {
            val expr = ExprCallDynamic(
                name = "example_function",
                candidates = candidates,
                args = arrayOf(ExprLiteral(lhs), ExprLiteral(rhs)),
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
            internal val candidates = params.mapIndexed { index, it ->
                ExprCallDynamic.Candidate(
                    fn = object : Fn {
                        override val signature: FnSignature = FnSignature(
                            name = "example_function",
                            returns = PartiQLValueType.INT32,
                            parameters = listOf(
                                FnParameter("first", type = it.first),
                                FnParameter("second", type = it.second),
                            )
                        )

                        override fun invoke(args: Array<PartiQLValue>): PartiQLValue = int32Value(index).toPartiQLValue()
                    },
                    coercions = arrayOf(null, null)
                )
            }.toTypedArray()
        }
    }

    companion object {

        @OptIn(PartiQLValueExperimental::class)
        @JvmStatic
        fun sanityTestsCases() = listOf(
            DynamicTestCase(
                lhs = int32Value(20),
                rhs = int32Value(40),
                expectedIndex = 4
            ),
            DynamicTestCase(
                lhs = listValue(emptyList()),
                rhs = listValue(emptyList()),
                expectedIndex = 0
            ),
            DynamicTestCase(
                lhs = bagValue(emptyList()),
                rhs = bagValue(emptyList()),
                expectedIndex = 1
            ),
            DynamicTestCase(
                lhs = stringValue("hello"),
                rhs = stringValue("world"),
                expectedIndex = 6
            ),
            DynamicTestCase(
                lhs = stringValue("hello"),
                rhs = listValue(emptyList()),
                expectedIndex = 7
            ),
            DynamicTestCase(
                lhs = bagValue(emptyList()),
                rhs = stringValue("world"),
                expectedIndex = 8
            ),
            DynamicTestCase(
                lhs = int32Value(20),
                rhs = stringValue("world"),
                expectedIndex = 9
            ),
            DynamicTestCase(
                lhs = listValue(emptyList()),
                rhs = stringValue("hello"),
                expectedIndex = 11
            ),
            DynamicTestCase(
                lhs = boolValue(true),
                rhs = boolValue(false),
                expectedIndex = 12
            ),
        )
    }
}
