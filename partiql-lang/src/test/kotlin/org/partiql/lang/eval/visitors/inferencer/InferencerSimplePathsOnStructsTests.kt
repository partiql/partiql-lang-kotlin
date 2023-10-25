package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.TestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectQueryOutputType
import org.partiql.types.StaticType
import org.partiql.types.StructType

class InferencerSimplePathsOnStructsTests {

    @ParameterizedTest
    @MethodSource("parametersForSimplePathsOnStructs")
    fun simplePathsOnStructs(tc: TestCase) = InferencerTestUtil.runTest(tc)

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun parametersForSimplePathsOnStructs(): List<TestCase> {
            val VALID_PATH_EXPR_SOURCES = setOf(StaticType.ANY, StaticType.LIST, StaticType.SEXP, StaticType.STRUCT)

            val incompatibleTypeForB =
                StaticType.ALL_TYPES.filter { it !in VALID_PATH_EXPR_SOURCES }
                    .flatMap { type ->
                        listOf(
                            TestCase(
                                "Simple path on struct: a.b.c",
                                "a.b.c",
                                mapOf(
                                    "a" to StructType(
                                        mapOf("b" to type, "c" to StaticType.INT)
                                    )
                                ),
                                handler = expectQueryOutputType(StaticType.MISSING)
                            ),
                            TestCase(
                                "Simple path on struct: a[b].c",
                                "a['b'].c",
                                mapOf(
                                    "a" to StructType(
                                        mapOf("b" to type, "c" to StaticType.INT)
                                    )
                                ),
                                handler = expectQueryOutputType(StaticType.MISSING)
                            )
                        )
                    }

            val bHasAnyType = StaticType.ALL_TYPES.flatMap {
                listOf(
                    TestCase(
                        "Simple path on struct: a.b.c",
                        "a.b.c",
                        mapOf(
                            "a" to StructType(
                                mapOf("b" to StaticType.ANY, "c" to it)
                            )
                        ),
                        handler = expectQueryOutputType(StaticType.ANY)
                    ),
                    TestCase(
                        "Simple path on struct: a['b'].c",
                        "a['b'].c",
                        mapOf(
                            "a" to StructType(
                                mapOf("b" to StaticType.ANY, "c" to it)
                            )
                        ),
                        handler = expectQueryOutputType(StaticType.ANY)
                    )
                )
            }

            val validTypeForB = StaticType.ALL_TYPES.flatMap {
                listOf(
                    TestCase(
                        "Simple path on struct: a.b",
                        "a.b",
                        mapOf(
                            "a" to StructType(
                                mapOf("b" to it)
                            )
                        ),
                        handler = expectQueryOutputType(it)
                    ),
                    TestCase(
                        "Simple path on struct: a['b']",
                        "a['b']",
                        mapOf(
                            "a" to StructType(
                                mapOf("b" to it)
                            )
                        ),
                        handler = expectQueryOutputType(it)
                    )
                )
            }
            return incompatibleTypeForB + bHasAnyType + validTypeForB
        }
    }
}
