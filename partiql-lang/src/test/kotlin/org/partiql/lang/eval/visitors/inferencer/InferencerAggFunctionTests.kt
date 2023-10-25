package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.TestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createInvalidArgumentTypeForFunctionError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectQueryOutputType
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectSemanticProblems
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.runTest
import org.partiql.types.BagType
import org.partiql.types.StaticType

class InferencerAggFunctionTests {
    @ParameterizedTest
    @MethodSource("parametersForAggFunctionTests")
    fun aggFunctionTests(tc: TestCase) = runTest(tc)

    companion object {
        private fun createAggFunctionValidTests(
            functionName: String,
            inputTypes: StaticType,
            expectedType: StaticType
        ): TestCase =
            // testing toplevel aggregated function
            // testing sql function(t)
            // global environment here is a bag type
            TestCase(
                name = "top level $functionName($inputTypes) -> $expectedType",
                originalSql = "$functionName(t)",
                globals = mapOf("t" to BagType(inputTypes)),
                handler = expectQueryOutputType(expectedType)
            )

        @JvmStatic
        private fun parametersForAggFunctionTests() =
            // valid tests
            listOf(
                // count
                createAggFunctionValidTests(
                    "COUNT",
                    StaticType.NULL,
                    StaticType.INT
                ),
                createAggFunctionValidTests(
                    "COUNT",
                    StaticType.MISSING,
                    StaticType.INT
                ),
                createAggFunctionValidTests(
                    "COUNT",
                    StaticType.ANY,
                    StaticType.INT
                ),
                createAggFunctionValidTests(
                    "COUNT",
                    StaticType.unionOf(StaticType.NULL, StaticType.MISSING, StaticType.INT),
                    StaticType.INT
                ),
                createAggFunctionValidTests(
                    "COUNT",
                    StaticType.unionOf(StaticType.NULL, StaticType.MISSING, StaticType.INT),
                    StaticType.INT
                ),

                // min
                createAggFunctionValidTests(
                    "MIN",
                    StaticType.MISSING,
                    StaticType.NULL
                ),
                createAggFunctionValidTests(
                    "MIN",
                    StaticType.NULL,
                    StaticType.NULL
                ),
                createAggFunctionValidTests(
                    "MIN",
                    StaticType.unionOf(StaticType.INT, StaticType.DECIMAL, StaticType.FLOAT, StaticType.LIST),
                    StaticType.unionOf(StaticType.INT, StaticType.DECIMAL, StaticType.FLOAT, StaticType.LIST)
                ),
                createAggFunctionValidTests(
                    "MIN",
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.DECIMAL,
                        StaticType.FLOAT,
                        StaticType.LIST,
                        StaticType.NULL,
                        StaticType.MISSING
                    ),
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.DECIMAL,
                        StaticType.FLOAT,
                        StaticType.LIST,
                        StaticType.NULL
                    )
                ),

                // max
                createAggFunctionValidTests(
                    "MAX",
                    StaticType.MISSING,
                    StaticType.NULL
                ),
                createAggFunctionValidTests(
                    "MAX",
                    StaticType.NULL,
                    StaticType.NULL
                ),
                createAggFunctionValidTests(
                    "MAX",
                    StaticType.unionOf(StaticType.INT, StaticType.DECIMAL, StaticType.FLOAT, StaticType.STRING),
                    StaticType.unionOf(StaticType.INT, StaticType.DECIMAL, StaticType.FLOAT, StaticType.STRING)
                ),
                createAggFunctionValidTests(
                    "MAX",
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.DECIMAL,
                        StaticType.FLOAT,
                        StaticType.STRING,
                        StaticType.NULL,
                        StaticType.MISSING
                    ),
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.DECIMAL,
                        StaticType.FLOAT,
                        StaticType.STRING,
                        StaticType.NULL
                    )
                ),

                // avg
                createAggFunctionValidTests(
                    "AVG",
                    StaticType.MISSING,
                    StaticType.NULL
                ),
                createAggFunctionValidTests(
                    "AVG",
                    StaticType.unionOf(StaticType.MISSING, StaticType.NULL),
                    StaticType.NULL
                ),
                createAggFunctionValidTests(
                    "AVG",
                    StaticType.unionOf(StaticType.MISSING, StaticType.NULL, StaticType.INT),
                    StaticType.unionOf(StaticType.NULL, StaticType.DECIMAL)
                ),
                createAggFunctionValidTests(
                    "AVG",
                    StaticType.unionOf(StaticType.INT, StaticType.DECIMAL, StaticType.FLOAT),
                    StaticType.DECIMAL
                ),
                createAggFunctionValidTests(
                    "AVG",
                    StaticType.unionOf(StaticType.INT, StaticType.DECIMAL, StaticType.FLOAT, StaticType.STRING),
                    StaticType.unionOf(StaticType.MISSING, StaticType.DECIMAL)
                ),

                // SUM
                createAggFunctionValidTests(
                    "SUM",
                    StaticType.MISSING,
                    StaticType.NULL
                ),
                createAggFunctionValidTests(
                    "SUM",
                    StaticType.unionOf(StaticType.MISSING, StaticType.NULL),
                    StaticType.NULL
                ),
                createAggFunctionValidTests(
                    "SUM",
                    StaticType.unionOf(StaticType.MISSING, StaticType.NULL, StaticType.INT2),
                    StaticType.unionOf(StaticType.NULL, StaticType.INT2)
                ),
                createAggFunctionValidTests(
                    "SUM",
                    StaticType.unionOf(StaticType.INT2, StaticType.INT4),
                    StaticType.unionOf(StaticType.INT2, StaticType.INT4)
                ),
                createAggFunctionValidTests(
                    "SUM",
                    StaticType.unionOf(StaticType.INT2, StaticType.INT4, StaticType.INT8),
                    StaticType.unionOf(StaticType.INT2, StaticType.INT4, StaticType.INT8)
                ),
                createAggFunctionValidTests(
                    "SUM",
                    StaticType.unionOf(StaticType.INT2, StaticType.INT4, StaticType.INT8, StaticType.FLOAT),
                    StaticType.unionOf(StaticType.INT2, StaticType.INT4, StaticType.INT8, StaticType.FLOAT)
                ),
                createAggFunctionValidTests(
                    "SUM",
                    StaticType.unionOf(
                        StaticType.INT2,
                        StaticType.INT4,
                        StaticType.INT8,
                        StaticType.FLOAT,
                        StaticType.DECIMAL
                    ),
                    StaticType.unionOf(
                        StaticType.INT2,
                        StaticType.INT4,
                        StaticType.INT8,
                        StaticType.FLOAT,
                        StaticType.DECIMAL
                    )
                ),
                createAggFunctionValidTests(
                    "SUM",
                    StaticType.unionOf(
                        StaticType.INT2,
                        StaticType.INT4,
                        StaticType.INT8,
                        StaticType.FLOAT,
                        StaticType.DECIMAL,
                        StaticType.STRING
                    ),
                    StaticType.unionOf(
                        StaticType.INT2,
                        StaticType.INT4,
                        StaticType.INT8,
                        StaticType.FLOAT,
                        StaticType.DECIMAL,
                        StaticType.MISSING
                    )
                )
            ) +
                // sum input type not compatible
                TestCase(
                    name = "data type mismatch SUM(STRING)",
                    originalSql = "SUM(t)",
                    globals = mapOf("t" to BagType(StaticType.STRING)),
                    handler = expectSemanticProblems(
                        expectedErrors = listOf(
                            createInvalidArgumentTypeForFunctionError(
                                sourceLocation = SourceLocationMeta(1L, 1L, 3L),
                                functionName = "sum",
                                expectedArgType = StaticType.unionOf(
                                    StaticType.MISSING,
                                    StaticType.NULL,
                                    StaticType.NUMERIC
                                ).flatten(),
                                actualType = StaticType.STRING
                            )
                        )
                    )
                ) +
                // avg input type not compatible
                TestCase(
                    name = "data type mismatch AVG(STRING)",
                    originalSql = "AVG(t)",
                    globals = mapOf("t" to BagType(StaticType.STRING)),
                    handler = expectSemanticProblems(
                        expectedErrors = listOf(
                            createInvalidArgumentTypeForFunctionError(
                                sourceLocation = SourceLocationMeta(1L, 1L, 3L),
                                functionName = "avg",
                                expectedArgType = StaticType.unionOf(
                                    StaticType.MISSING,
                                    StaticType.NULL,
                                    StaticType.NUMERIC
                                ).flatten(),
                                actualType = StaticType.STRING
                            )
                        )
                    )
                )
    }
}
