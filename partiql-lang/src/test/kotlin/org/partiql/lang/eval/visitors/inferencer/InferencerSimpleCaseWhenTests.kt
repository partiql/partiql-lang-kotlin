package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_LOB_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_NUMERIC_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_TEXT_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_TYPES_ONLY_COMPARABLE_TO_SELF
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.TestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createDataTypeMismatchError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createReturnsMissingError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createReturnsNullOrMissingWarning
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectQueryOutputType
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectSemanticProblems
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.runTest
import org.partiql.types.StaticType

class InferencerSimpleCaseWhenTests {
    @ParameterizedTest
    @MethodSource("parametersForSimpleCaseWhen")
    fun simpleCaseWhenTests(tc: TestCase) = runTest(tc)

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun parametersForSimpleCaseWhen() = listOf(
            TestCase(
                name = "CASE <val> WHEN with ELSE expression resulting single type",
                originalSql = "CASE 'a_string' WHEN 'a_string' THEN true ELSE false END",
                handler = expectQueryOutputType(StaticType.BOOL)
            ),
            TestCase(
                name = "CASE <val> WHEN without ELSE expression",
                originalSql = "CASE 'a_string' WHEN 'a_string' THEN true END",
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.BOOL,
                        StaticType.NULL
                    )
                )
            ),
            TestCase(
                name = "CASE <val> WHEN without ELSE expression resulting union type",
                originalSql = "CASE 'a_string' WHEN 'a_string' THEN 'another_string' END",
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.STRING,
                        StaticType.NULL
                    )
                )
            ),
            TestCase(
                name = "CASE <val> WHEN with ELSE expression, INT compared to known types, THEN and ELSE of known types",
                originalSql = """
                    CASE 1 
                        WHEN t_int THEN t_int
                        WHEN t_float THEN t_decimal
                        ELSE t_string
                    END
                    """,
                globals = mapOf(
                    "t_int" to StaticType.INT,
                    "t_float" to StaticType.FLOAT,
                    "t_decimal" to StaticType.DECIMAL,
                    "t_string" to StaticType.STRING
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.DECIMAL,
                        StaticType.STRING
                    )
                )
            ),
            TestCase(
                name = "CASE <val> WHEN with ELSE expression, INT compared to ANY, THEN and ELSE of known types",
                originalSql = """
                    CASE 1 
                        WHEN t_any THEN t_int
                        ELSE t_string
                    END
                    """,
                globals = mapOf(
                    "t_any" to StaticType.ANY,
                    "t_int" to StaticType.INT,
                    "t_string" to StaticType.STRING
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.STRING
                    )
                )
            ),
            TestCase(
                name = "CASE <val> WHEN with ELSE expression, INT compared to nullable type, THEN and ELSE of known types",
                originalSql = """
                    CASE 1 
                        WHEN t_nullable_int THEN t_int
                        ELSE t_string
                    END
                    """,
                globals = mapOf(
                    "t_nullable_int" to StaticType.INT.asNullable(),
                    "t_int" to StaticType.INT,
                    "t_string" to StaticType.STRING
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.STRING
                    )
                )
            ),
            TestCase(
                name = "CASE <val> WHEN with ELSE expression, INT compared to optional type, THEN and ELSE of known types",
                originalSql = """
                    CASE 1 
                        WHEN t_optional_int THEN t_int
                        ELSE t_string
                    END
                    """,
                globals = mapOf(
                    "t_optional_int" to StaticType.INT.asOptional(),
                    "t_int" to StaticType.INT,
                    "t_string" to StaticType.STRING
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.STRING
                    )
                )
            ),
            TestCase(
                name = "CASE <val> WHEN with ELSE expression, INT compared to nullable and optional, THEN and ELSE of known types",
                originalSql = """
                    CASE 1 
                        WHEN t_nullable_int THEN t_int
                        WHEN t_optional_int THEN t_decimal
                        ELSE t_string
                    END
                    """,
                globals = mapOf(
                    "t_nullable_int" to StaticType.INT.asNullable(),
                    "t_optional_int" to StaticType.INT.asOptional(),
                    "t_int" to StaticType.INT,
                    "t_decimal" to StaticType.DECIMAL,
                    "t_string" to StaticType.STRING
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.DECIMAL,
                        StaticType.STRING
                    )
                )
            ),
            TestCase(
                name = "CASE <val> WHEN with ELSE expression, union of numerics compared with numerics, THEN and ELSE of known types",
                originalSql = """
                    CASE t_numerics
                        WHEN t_int THEN t_int
                        WHEN t_decimal THEN t_decimal
                        ELSE t_float
                    END
                    """,
                globals = mapOf(
                    "t_numerics" to StaticType.unionOf(ALL_NUMERIC_TYPES.toSet()),
                    "t_int" to StaticType.INT,
                    "t_decimal" to StaticType.DECIMAL,
                    "t_float" to StaticType.FLOAT
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.DECIMAL,
                        StaticType.FLOAT
                    )
                )
            ),
            TestCase(
                name = "CASE <val> WHEN with ELSE expression, int compared with unions containing int, THEN and ELSE of known types",
                originalSql = """
                    CASE t_int
                        WHEN t_int_string THEN t_int_string
                        WHEN t_int_bool THEN t_int_bool
                        ELSE t_float
                    END
                    """,
                globals = mapOf(
                    "t_int" to StaticType.INT,
                    "t_int_string" to StaticType.unionOf(StaticType.INT, StaticType.STRING),
                    "t_int_bool" to StaticType.unionOf(StaticType.INT, StaticType.BOOL),
                    "t_float" to StaticType.FLOAT
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.STRING,
                        StaticType.BOOL,
                        StaticType.FLOAT
                    )
                )
            ),
            //
            // SimpleCaseWhen error cases below
            //
            TestCase(
                name = "data type mismatch errors: CASE <int> WHEN <string> THEN <string> WHEN <symbol> THEN <symbol> END",
                originalSql = """
                    CASE t_int
                        WHEN t_string THEN t_string
                        WHEN t_symbol THEN t_symbol
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
                            argTypes = listOf(StaticType.INT, StaticType.SYMBOL), nAryOp = "CASE"
                        )
                    )
                )
            ),
            TestCase(
                name = "data type mismatch errors: CASE <nullable_int> WHEN <nullable_string> THEN <nullable_string> WHEN <optional_symbol> THEN <optional_symbol> END",
                originalSql = """
                    CASE t_nullable_int
                        WHEN t_nullable_string THEN t_nullable_string
                        WHEN t_optional_symbol THEN t_optional_symbol
                    END
                    """,
                globals = mapOf(
                    "t_nullable_int" to StaticType.INT.asNullable(),
                    "t_nullable_string" to StaticType.STRING.asNullable(),
                    "t_optional_symbol" to StaticType.SYMBOL.asOptional()
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createDataTypeMismatchError(
                            SourceLocationMeta(
                                3L,
                                30L,
                                17L
                            ),
                            argTypes = listOf(StaticType.INT.asNullable(), StaticType.STRING.asNullable()),
                            nAryOp = "CASE"
                        ),
                        createDataTypeMismatchError(
                            SourceLocationMeta(
                                4L,
                                30L,
                                17L
                            ),
                            argTypes = listOf(StaticType.INT.asNullable(), StaticType.SYMBOL.asOptional()),
                            nAryOp = "CASE"
                        )
                    )
                )
            ),
            // Todo: Inferencer keeps all the `THEN` expr types even if we know the comparison will not succeed, why?
            TestCase(
                name = "null or missing warning (caseValue = null): CASE <null> WHEN <string> THEN <string> WHEN <symbol> THEN <symbol> END",
                originalSql = """
                    CASE t_null
                        WHEN t_string THEN t_string
                        WHEN t_symbol THEN t_symbol
                    END
                    """,
                globals = mapOf(
                    "t_null" to StaticType.NULL,
                    "t_string" to StaticType.STRING,
                    "t_symbol" to StaticType.SYMBOL
                ),
                handler = expectQueryOutputType(
                    expectedType = StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL, StaticType.NULL),
                    expectedWarnings = listOf(
                        createReturnsNullOrMissingWarning(
                            SourceLocationMeta(
                                2L,
                                26L,
                                6L
                            )
                        )
                    )
                )
            ),
            TestCase(
                name = "always missing error (caseValue = missing): CASE <missing> WHEN <string> THEN <string> WHEN <symbol> THEN <symbol> END",
                originalSql = """
                    CASE t_missing
                        WHEN t_string THEN t_string
                        WHEN t_symbol THEN t_symbol
                    END
                    """,
                globals = mapOf(
                    "t_missing" to StaticType.MISSING,
                    "t_string" to StaticType.STRING,
                    "t_symbol" to StaticType.SYMBOL
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createReturnsMissingError(
                            SourceLocationMeta(
                                2L,
                                26L,
                                9L
                            )
                        )
                    )
                )
            ),
            // Todo: Inferencer keeps all the `THEN` expr types even if we know the comparison will not succeed, why?
            //  Missing Type is lost here, why?
            TestCase(
                name = "null or missing warning (caseValue = null_or_missing): CASE <null_or_missing> WHEN <string> THEN <string> WHEN <symbol> THEN <symbol> END",
                originalSql = """
                    CASE t_null_or_missing
                        WHEN t_string THEN t_string
                        WHEN t_symbol THEN t_symbol
                    END
                    """,
                globals = mapOf(
                    "t_null_or_missing" to StaticType.NULL_OR_MISSING,
                    "t_string" to StaticType.STRING,
                    "t_symbol" to StaticType.SYMBOL
                ),
                handler = expectQueryOutputType(
                    expectedType = StaticType.unionOf(StaticType.NULL, StaticType.STRING, StaticType.SYMBOL),
                    expectedWarnings = listOf(
                        createReturnsNullOrMissingWarning(
                            SourceLocationMeta(
                                2L,
                                26L,
                                17L
                            )
                        )
                    ),
                )
            ),
            TestCase(
                name = "data type mismatch and null or missing warnings: caseValue = INT, whenExprs of unknowns and incompatible types",
                originalSql = """
                    CASE t_int
                        WHEN t_null THEN t_null
                        WHEN t_missing THEN t_missing
                        WHEN t_null_or_missing THEN t_null_or_missing
                        WHEN t_string THEN t_string
                        WHEN t_symbol THEN t_symbol
                    END
                    """,
                globals = mapOf(
                    "t_int" to StaticType.INT,
                    "t_null" to StaticType.NULL,
                    "t_missing" to StaticType.MISSING,
                    "t_null_or_missing" to StaticType.NULL_OR_MISSING,
                    "t_string" to StaticType.STRING,
                    "t_symbol" to StaticType.SYMBOL
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createReturnsMissingError(
                            SourceLocationMeta(
                                4L,
                                30L,
                                9L
                            )
                        ),
                        createDataTypeMismatchError(
                            SourceLocationMeta(
                                6L,
                                30L,
                                8L
                            ),
                            argTypes = listOf(StaticType.INT, StaticType.STRING), nAryOp = "CASE"
                        ),
                        createDataTypeMismatchError(
                            SourceLocationMeta(
                                7L,
                                30L,
                                8L
                            ),
                            argTypes = listOf(StaticType.INT, StaticType.SYMBOL), nAryOp = "CASE"
                        )
                    ),
                    expectedWarnings = listOf(
                        createReturnsNullOrMissingWarning(
                            SourceLocationMeta(
                                3L,
                                30L,
                                6L
                            )
                        ),
                        createReturnsNullOrMissingWarning(
                            SourceLocationMeta(
                                5L,
                                30L,
                                17L
                            )
                        ),
                    )
                )
            ),
            TestCase(
                name = "always missing error at caseValue and whenExprs: caseValue = null, whenExprs of unknowns",
                originalSql = """
                    CASE t_null
                        WHEN t_null THEN t_null
                        WHEN t_missing THEN t_missing
                        WHEN t_string THEN t_string
                    END
                    """,
                globals = mapOf(
                    "t_null" to StaticType.NULL,
                    "t_missing" to StaticType.MISSING,
                    "t_string" to StaticType.STRING
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createReturnsMissingError(
                            SourceLocationMeta(
                                4L,
                                30L,
                                9L
                            )
                        )
                    ),
                    expectedWarnings = listOf(
                        createReturnsNullOrMissingWarning(
                            SourceLocationMeta(
                                2L,
                                26L,
                                6L
                            )
                        ),
                        createReturnsNullOrMissingWarning(
                            SourceLocationMeta(
                                3L,
                                30L,
                                6L
                            )
                        ),
                    )
                )
            ),
            TestCase(
                name = "data type mismatch errors: caseValue = union of numeric types, whenExprs of non-numeric union types",
                originalSql = """
                    CASE t_numeric
                        WHEN t_text THEN t_text
                        WHEN t_lob THEN t_lob
                        WHEN t_other THEN t_other
                    END
                    """,
                globals = mapOf(
                    "t_numeric" to StaticType.unionOf(ALL_NUMERIC_TYPES.toSet()),
                    "t_text" to StaticType.unionOf(ALL_TEXT_TYPES.toSet()),
                    "t_lob" to StaticType.unionOf(ALL_LOB_TYPES.toSet()),
                    "t_other" to StaticType.unionOf(ALL_TYPES_ONLY_COMPARABLE_TO_SELF.toSet())
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createDataTypeMismatchError(
                            SourceLocationMeta(3L, 30L, 6L),
                            argTypes = listOf(
                                StaticType.unionOf(ALL_NUMERIC_TYPES.toSet()),
                                StaticType.unionOf(ALL_TEXT_TYPES.toSet())
                            ),
                            nAryOp = "CASE"
                        ),
                        createDataTypeMismatchError(
                            SourceLocationMeta(4L, 30L, 5L),
                            argTypes = listOf(
                                StaticType.unionOf(ALL_NUMERIC_TYPES.toSet()),
                                StaticType.unionOf(ALL_LOB_TYPES.toSet())
                            ),
                            nAryOp = "CASE"
                        ),
                        createDataTypeMismatchError(
                            SourceLocationMeta(5L, 30L, 7L),
                            argTypes = listOf(
                                StaticType.unionOf(ALL_NUMERIC_TYPES.toSet()),
                                StaticType.unionOf(ALL_TYPES_ONLY_COMPARABLE_TO_SELF.toSet())
                            ),
                            nAryOp = "CASE"
                        )
                    )
                )
            )
        )
    }
}
