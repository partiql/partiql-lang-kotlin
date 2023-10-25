package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.TestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectQueryOutputType
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.runTest
import org.partiql.types.ListType
import org.partiql.types.StaticType

class InferencerSimplePathsOnSequencesTests {
    @ParameterizedTest
    @MethodSource("parametersForSimplePathsOnSequences")
    fun simplePathsOnSequences(tc: TestCase) = runTest(tc)

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun parametersForSimplePathsOnSequences(): List<TestCase> {
            val INT_TYPES = setOf(StaticType.INT, StaticType.INT2, StaticType.INT4, StaticType.INT8)
            val incompatibleTypeForIndex = StaticType.ALL_TYPES.filter { it !in INT_TYPES }.map {
                TestCase(
                    "simple path for lists a[b] -- b is not INT type",
                    "a[b]",
                    mapOf(
                        "a" to ListType(elementType = StaticType.STRING),
                        "b" to it
                    ),
                    handler = expectQueryOutputType(StaticType.MISSING)
                )
            }

            val validIndexType = StaticType.ALL_TYPES.map { elementType ->
                INT_TYPES.map { intType ->
                    TestCase(
                        "a[b] -- valid type for 'b' varies element type in a ($elementType)",
                        "a[b]",
                        mapOf(
                            "a" to ListType(elementType = elementType),
                            "b" to intType
                        ),
                        handler = expectQueryOutputType(elementType)
                    )
                }
            }.flatten()

            return incompatibleTypeForIndex + validIndexType
        }
    }
}
