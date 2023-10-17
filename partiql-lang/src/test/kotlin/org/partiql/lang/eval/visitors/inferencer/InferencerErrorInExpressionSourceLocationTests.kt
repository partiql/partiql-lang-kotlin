package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.TestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createDataTypeMismatchError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createIncompatibleTypesForExprError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectSemanticProblems
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.runTest
import org.partiql.types.BagType
import org.partiql.types.StaticType
import org.partiql.types.StructType

class InferencerErrorInExpressionSourceLocationTests {

    @ParameterizedTest
    @MethodSource("parametersForErrorInExpressionSourceLocationTests")
    fun errorInExpressionSourceLocationTests(tc: TestCase) = runTest(tc)

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun parametersForErrorInExpressionSourceLocationTests() = listOf(
            // tests with a data type mismatch where the error points to the start of the expression
            TestCase(
                name = "SimpleCaseWhen error in WHEN expression",
                originalSql =
                """
                    CASE t_int
                        WHEN t_string || t_string || t_string THEN t_string
                        WHEN t_symbol || t_symbol || t_symbol THEN t_symbol
                    END
                    """,
                globals = mapOf(
                    "t_int" to StaticType.INT,
                    "t_string" to StaticType.STRING,
                    "t_symbol" to StaticType.SYMBOL
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createDataTypeMismatchError(
                            SourceLocationMeta(
                                3L,
                                30L,
                                8L
                            ),
                            argTypes = listOf(StaticType.INT, StaticType.STRING), nAryOp = "CASE"
                        ),
                        createDataTypeMismatchError(
                            SourceLocationMeta(
                                4L,
                                30L,
                                8L
                            ),
                            argTypes = listOf(StaticType.INT, StaticType.STRING), nAryOp = "CASE"
                        )
                    )
                )
            ),
            TestCase(
                name = "SearchedCaseWhen error in WHEN expression",
                originalSql =
                """
                    CASE
                        WHEN t_string || t_string || t_string THEN t_string
                        WHEN t_symbol || t_symbol || t_symbol THEN t_symbol
                    END
                    """,
                globals = mapOf(
                    "t_string" to StaticType.STRING,
                    "t_symbol" to StaticType.SYMBOL
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createIncompatibleTypesForExprError(
                            SourceLocationMeta(
                                3L,
                                30L,
                                8L
                            ),
                            expectedType = StaticType.BOOL, actualType = StaticType.STRING
                        ),
                        createIncompatibleTypesForExprError(
                            SourceLocationMeta(
                                4L,
                                30L,
                                8L
                            ),
                            expectedType = StaticType.BOOL, actualType = StaticType.STRING
                        )
                    )
                )
            ),
            TestCase(
                name = "JOIN condition error in expression",
                originalSql = """
                    SELECT * FROM a
                    JOIN b
                    ON c + c + c""",
                globals = mapOf(
                    "a" to BagType(StructType(mapOf("foo" to StaticType.INT))),
                    "b" to BagType(StructType(mapOf("bar" to StaticType.STRING))),
                    "c" to StaticType.INT
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createIncompatibleTypesForExprError(
                            SourceLocationMeta(
                                4L,
                                24L,
                                1L
                            ),
                            expectedType = StaticType.BOOL, actualType = StaticType.INT
                        )
                    )
                )
            ),
            TestCase(
                name = "WHERE clause error in expression",
                originalSql = "SELECT * FROM t WHERE a + a + a",
                globals = mapOf("t" to BagType(StructType(mapOf("a" to StaticType.INT)))),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createIncompatibleTypesForExprError(
                            SourceLocationMeta(
                                1L,
                                23L,
                                1L
                            ),
                            expectedType = StaticType.BOOL, actualType = StaticType.INT
                        )
                    )
                )
            )
        )
    }
}
