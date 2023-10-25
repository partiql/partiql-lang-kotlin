package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.TestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectQueryOutputType
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.runTest
import org.partiql.types.StaticType

class InferencerTrimFunctionTests {
    @ParameterizedTest
    @MethodSource("parametersForTrimFunctionTests")
    fun trimFunctionTests(tc: TestCase) = runTest(tc)

    companion object {
        private fun createTrimTestCases(
            toRemove: StaticType? = null,
            target: StaticType,
            result: StaticType
        ) =
            listOf("both", "leading", "trailing", "").map {
                TestCase(
                    when {
                        it.isBlank() && toRemove == null -> "trim($target)"
                        else -> "trim($it ${toRemove ?: ""} from $target)"
                    },
                    when {
                        it.isBlank() && toRemove == null -> "trim(y)"
                        else -> "trim($it ${if (toRemove == null) ' ' else 'x'} from y)"
                    },
                    when (toRemove) {
                        null -> mapOf("y" to target)
                        else -> mapOf("x" to toRemove, "y" to target)
                    },
                    handler = expectQueryOutputType(result)
                )
            }

        @JvmStatic
        @Suppress("unused")
        fun parametersForTrimFunctionTests() = listOf(
            createTrimTestCases(
                target = StaticType.STRING,
                result = StaticType.STRING
            ),
            createTrimTestCases(
                toRemove = StaticType.STRING,
                target = StaticType.STRING,
                result = StaticType.STRING
            ),
            createTrimTestCases(
                target = StaticType.unionOf(StaticType.STRING, StaticType.INT),
                result = StaticType.unionOf(StaticType.STRING, StaticType.MISSING)
            ),
            createTrimTestCases(
                target = StaticType.ANY,
                result = StaticType.unionOf(StaticType.STRING, StaticType.MISSING, StaticType.NULL)
            ),
            createTrimTestCases(
                toRemove = StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL),
                target = StaticType.unionOf(StaticType.INT, StaticType.STRING),
                result = StaticType.unionOf(StaticType.STRING, StaticType.MISSING)
            ),
            createTrimTestCases(
                toRemove = StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL, StaticType.NULL),
                target = StaticType.unionOf(StaticType.INT, StaticType.STRING, StaticType.BOOL, StaticType.MISSING),
                result = StaticType.unionOf(StaticType.STRING, StaticType.MISSING, StaticType.NULL)
            ),
            createTrimTestCases(
                toRemove = StaticType.ANY,
                target = StaticType.ANY,
                result = StaticType.unionOf(StaticType.STRING, StaticType.MISSING, StaticType.NULL)
            )
        ).flatten() + listOf(
            TestCase(
                "Leading is treated as keyword",
                "trim(leading from 'target')",
                mapOf(
                    "leading" to StaticType.BOOL
                ),
                handler = expectQueryOutputType(StaticType.STRING)
            )
        )
    }
}
