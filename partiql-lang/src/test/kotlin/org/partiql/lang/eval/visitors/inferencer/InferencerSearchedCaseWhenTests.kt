package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_LOB_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_NON_BOOL_NON_UNKNOWN_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_NUMERIC_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_TEXT_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_UNKNOWN_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.TestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createIncompatibleTypesForExprError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createReturnsMissingError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createReturnsNullOrMissingWarning
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectQueryOutputType
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectSemanticProblems
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.runTest
import org.partiql.types.MissingType
import org.partiql.types.StaticType

class InferencerSearchedCaseWhenTests {
    @ParameterizedTest
    @MethodSource("parametersForSearchedCaseWhen")
    fun searchedCaseWhenTests(tc: TestCase) = runTest(tc)

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun parametersForSearchedCaseWhen() =
            listOf(
                TestCase(
                    "CASE WHEN with ELSE expression resulting single type",
                    "CASE WHEN true THEN true ELSE false END",
                    handler = expectQueryOutputType(StaticType.BOOL)
                ),
                TestCase(
                    "CASE WHEN without ELSE expression",
                    "CASE WHEN true THEN true WHEN false THEN false END",
                    handler = expectQueryOutputType(
                        StaticType.unionOf(
                            StaticType.BOOL,
                            StaticType.NULL
                        )
                    )
                ),
                TestCase(
                    "CASE WHEN without ELSE expression resulting union type",
                    "CASE WHEN true THEN 'true' WHEN false THEN false END",
                    handler = expectQueryOutputType(
                        StaticType.unionOf(
                            StaticType.STRING,
                            StaticType.BOOL,
                            StaticType.NULL
                        )
                    )
                ),
                TestCase(
                    "CASE WHEN with ELSE expression, nullable type in THEN",
                    "CASE WHEN true THEN nullable_bool ELSE false END",
                    mapOf(
                        "nullable_bool" to StaticType.unionOf(StaticType.BOOL, StaticType.NULL)
                    ),
                    handler = expectQueryOutputType(
                        StaticType.unionOf(
                            StaticType.BOOL,
                            StaticType.NULL
                        )
                    )
                ),
                TestCase(
                    "CASE WHEN with ELSE expression, nullable type in ELSE",
                    "CASE WHEN true THEN true ELSE nullable_bool END",
                    mapOf(
                        "nullable_bool" to StaticType.unionOf(StaticType.BOOL, StaticType.NULL)
                    ),
                    handler = expectQueryOutputType(
                        StaticType.unionOf(
                            StaticType.BOOL,
                            StaticType.NULL
                        )
                    )
                ),
                TestCase(
                    "CASE WHEN with ELSE expression, different nullable types",
                    "CASE WHEN true THEN nullable_string ELSE nullable_bool END",
                    mapOf(
                        "nullable_bool" to StaticType.unionOf(StaticType.BOOL, StaticType.NULL),
                        "nullable_string" to StaticType.unionOf(StaticType.STRING, StaticType.NULL)
                    ),
                    handler = expectQueryOutputType(
                        StaticType.unionOf(
                            StaticType.BOOL,
                            StaticType.STRING,
                            StaticType.NULL
                        )
                    )
                ),
                TestCase(
                    "CASE WHEN with ELSE expression, WHEN expr INT compared to nullable INT, THEN of known types",
                    """
                     CASE
                         WHEN t_int = t_nullable_int THEN t_int
                         ELSE t_string
                     END
                     """,
                    mapOf(
                        "t_int" to StaticType.INT,
                        "t_nullable_int" to StaticType.INT.asNullable(),
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
                    "CASE WHEN without ELSE expression, WHEN expr INT compared to nullable INT, THEN of known type",
                    """
                     CASE
                         WHEN t_int = t_nullable_int THEN t_int
                     END
                     """,
                    mapOf(
                        "t_int" to StaticType.INT,
                        "t_nullable_int" to StaticType.INT.asNullable()
                    ),
                    handler = expectQueryOutputType(
                        StaticType.unionOf(
                            StaticType.INT,
                            StaticType.NULL
                        )
                    )
                ),
                TestCase(
                    "CASE WHEN with ELSE expression, WHEN expr INT compared to optional INT, THEN of known types",
                    """
                     CASE
                         WHEN t_int = t_optional_int THEN t_int
                         ELSE t_string
                     END
                     """,
                    mapOf(
                        "t_int" to StaticType.INT,
                        "t_optional_int" to StaticType.INT.asOptional(),
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
                    "CASE WHEN without ELSE expression, WHEN expr INT compared to optional INT, THEN of known types",
                    """
                     CASE
                         WHEN t_int = t_optional_int THEN t_int
                     END
                     """,
                    mapOf(
                        "t_int" to StaticType.INT,
                        "t_optional_int" to StaticType.INT.asOptional()
                    ),
                    handler = expectQueryOutputType(
                        StaticType.unionOf(
                            StaticType.INT,
                            StaticType.NULL
                        )
                    )
                ),
                TestCase(
                    "CASE WHEN with ELSE expression, WHEN expr union with bool, THEN of known types",
                    """
                     CASE
                         WHEN u_bool_and_other_types THEN t_int
                         ELSE t_string
                     END
                     """,
                    mapOf(
                        "u_bool_and_other_types" to StaticType.unionOf(
                            StaticType.BOOL,
                            StaticType.INT,
                            StaticType.NULL
                        ),
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
                    "CASE WHEN without ELSE expression, WHEN expr union with bool, THEN of known type",
                    """
                     CASE
                         WHEN u_bool_and_other_types THEN t_int
                     END
                     """,
                    mapOf(
                        "u_bool_and_other_types" to StaticType.unionOf(
                            StaticType.BOOL,
                            StaticType.INT,
                            StaticType.NULL
                        ),
                        "t_int" to StaticType.INT
                    ),
                    handler = expectQueryOutputType(
                        StaticType.unionOf(
                            StaticType.INT,
                            StaticType.NULL
                        )
                    )
                ),
                TestCase(
                    "CASE WHEN with ELSE expression, WHEN expr ANY, THEN of known type",
                    """
                     CASE
                         WHEN t_any THEN t_int
                         ELSE t_string
                     END
                     """,
                    mapOf(
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
                    "CASE WHEN without ELSE expression, WHEN expr ANY, THEN of known type",
                    """
                     CASE
                         WHEN t_any THEN t_int
                     END
                     """,
                    mapOf(
                        "t_any" to StaticType.ANY,
                        "t_int" to StaticType.INT
                    ),
                    handler = expectQueryOutputType(
                        StaticType.unionOf(
                            StaticType.INT,
                            StaticType.NULL
                        )
                    )
                )
            ) +
                //
                // SearchedCaseWhen error cases below
                //

                // tests with non-bool, non-unknown whenExpr
                ALL_NON_BOOL_NON_UNKNOWN_TYPES.flatMap { nonBool ->
                    listOf(
                        TestCase(
                            name = "data type mismatch error - $nonBool whenExpr",
                            originalSql = """
                            CASE
                                WHEN t_non_bool THEN t_non_bool
                            END
                            """,
                            globals = mapOf("t_non_bool" to nonBool),
                            handler = expectSemanticProblems(
                                expectedErrors = listOf(
                                    createIncompatibleTypesForExprError(
                                        SourceLocationMeta(3L, 38L, 10L),
                                        expectedType = StaticType.BOOL,
                                        actualType = nonBool
                                    )
                                )
                            )
                        ),
                        TestCase(
                            name = "data type mismatch error - $nonBool whenExpr and elseExpr",
                            originalSql = """
                            CASE
                                WHEN t_non_bool THEN t_non_bool
                                ELSE t_non_bool
                            END
                            """,
                            globals = mapOf("t_non_bool" to nonBool),
                            handler = expectSemanticProblems(
                                expectedErrors = listOf(
                                    createIncompatibleTypesForExprError(
                                        SourceLocationMeta(3L, 38L, 10L),
                                        expectedType = StaticType.BOOL,
                                        actualType = nonBool
                                    )
                                )
                            )
                        )
                    )
                } +
                // tests with unknown whenExpr
                ALL_UNKNOWN_TYPES.flatMap { unknownType ->
                    if (unknownType is MissingType) {
                        listOf(
                            TestCase(
                                name = "always missing error - $unknownType whenExpr",
                                originalSql = """
                            CASE
                                WHEN t_unknown THEN t_unknown
                            END
                            """,
                                globals = mapOf("t_unknown" to unknownType),
                                handler = expectSemanticProblems(
                                    expectedErrors = listOf(
                                        createReturnsMissingError(
                                            SourceLocationMeta(3L, 38L, 9L)
                                        )
                                    )
                                )
                            ),
                            TestCase(
                                name = "always missing error - $unknownType whenExpr and elseExpr",
                                originalSql = """
                            CASE
                                WHEN t_unknown THEN t_unknown
                                ELSE t_unknown
                            END
                            """,
                                globals = mapOf("t_unknown" to unknownType),
                                handler = expectSemanticProblems(
                                    expectedErrors = listOf(
                                        createReturnsMissingError(
                                            SourceLocationMeta(3L, 38L, 9L)
                                        )
                                    )
                                )
                            )
                        )
                    } else {
                        listOf(
                            TestCase(
                                name = "null or missing warning - $unknownType whenExpr",
                                originalSql = """
                            CASE
                                WHEN t_unknown THEN t_unknown
                            END
                            """,
                                globals = mapOf("t_unknown" to unknownType),
                                handler = expectQueryOutputType(
                                    expectedType = unknownType,
                                    expectedWarnings = listOf(
                                        createReturnsNullOrMissingWarning(
                                            SourceLocationMeta(3L, 38L, 9L)
                                        )
                                    )
                                )
                            ),
                            TestCase(
                                name = "null or missing warning - $unknownType whenExpr and elseExpr",
                                originalSql = """
                            CASE
                                WHEN t_unknown THEN t_unknown
                                ELSE t_unknown
                            END
                            """,
                                globals = mapOf("t_unknown" to unknownType),
                                handler = expectQueryOutputType(
                                    expectedType = unknownType,
                                    expectedWarnings = listOf(
                                        createReturnsNullOrMissingWarning(
                                            SourceLocationMeta(3L, 38L, 9L)
                                        )
                                    )
                                )
                            )
                        )
                    }
                } +
                listOf(
                    TestCase(
                        name = "multiple errors - non-bool whenExprs and unknown whenExprs",
                        originalSql = """
                        CASE
                            WHEN t_int THEN t_int
                            WHEN t_string THEN t_string
                            WHEN t_any THEN t_any
                            WHEN t_null THEN t_null
                            WHEN t_missing THEN t_missing
                        END
                        """,
                        globals = mapOf(
                            "t_int" to StaticType.INT,
                            "t_string" to StaticType.STRING,
                            "t_any" to StaticType.ANY,
                            "t_null" to StaticType.NULL,
                            "t_missing" to StaticType.MISSING
                        ),
                        handler = expectSemanticProblems(
                            expectedErrors = listOf(
                                createIncompatibleTypesForExprError(
                                    SourceLocationMeta(3L, 34L, 5L),
                                    expectedType = StaticType.BOOL,
                                    actualType = StaticType.INT
                                ),
                                createIncompatibleTypesForExprError(
                                    SourceLocationMeta(4L, 34L, 8L),
                                    expectedType = StaticType.BOOL,
                                    actualType = StaticType.STRING
                                ),
                                createReturnsMissingError(
                                    SourceLocationMeta(7L, 34L, 9L)
                                )
                            ),
                            expectedWarnings = listOf(
                                createReturnsNullOrMissingWarning(
                                    SourceLocationMeta(6L, 34L, 6L)
                                )
                            )
                        )
                    ),
                    TestCase(
                        name = "multiple errors - whenExprs of unions not containing bool",
                        originalSql = """
                        CASE
                            WHEN t_numeric THEN t_numeric
                            WHEN t_text THEN t_text
                            WHEN t_lob THEN t_lob
                            WHEN t_null_or_missing THEN t_null_or_missing
                        END
                        """,
                        globals = mapOf(
                            "t_numeric" to StaticType.unionOf(ALL_NUMERIC_TYPES.toSet()),
                            "t_text" to StaticType.unionOf(ALL_TEXT_TYPES.toSet()),
                            "t_lob" to StaticType.unionOf(ALL_LOB_TYPES.toSet()),
                            "t_null_or_missing" to StaticType.NULL_OR_MISSING
                        ),
                        handler = expectSemanticProblems(
                            expectedErrors = listOf(
                                createIncompatibleTypesForExprError(
                                    SourceLocationMeta(3L, 34L, 9L),
                                    expectedType = StaticType.BOOL,
                                    actualType = StaticType.unionOf(ALL_NUMERIC_TYPES.toSet())
                                ),
                                createIncompatibleTypesForExprError(
                                    SourceLocationMeta(4L, 34L, 6L),
                                    expectedType = StaticType.BOOL,
                                    actualType = StaticType.unionOf(ALL_TEXT_TYPES.toSet())
                                ),
                                createIncompatibleTypesForExprError(
                                    SourceLocationMeta(5L, 34L, 5L),
                                    expectedType = StaticType.BOOL,
                                    actualType = StaticType.unionOf(ALL_LOB_TYPES.toSet())
                                ),
                            ),
                            expectedWarnings = listOf(
                                createReturnsNullOrMissingWarning(
                                    SourceLocationMeta(6L, 34L, 17L)
                                )
                            )
                        )
                    )
                )
    }
}
