package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.errors.Problem
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.staticType
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_NON_BOOL_NON_UNKNOWN_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_UNKNOWN_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.TestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createIncompatibleTypesForExprError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createReturnsMissingError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createReturnsNullOrMissingWarning
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectProblemsAndAssert
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectQueryOutputType
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.runTest
import org.partiql.types.BagType
import org.partiql.types.MissingType
import org.partiql.types.StaticType
import org.partiql.types.StructType

class InferencerJoinPredicateTests {
    @ParameterizedTest
    @MethodSource("parametersForJoinPredicateTests")
    fun joinPredicateTests(tc: TestCase) = runTest(tc)

    companion object {
        private val JOIN_WITH_PREDICATE = listOf("JOIN", "INNER JOIN", "LEFT JOIN", "RIGHT JOIN")

        /**
         * Creates a simple SFW query with a join predicate for each [JOIN_WITH_PREDICATE] that verifies that no errors
         * come up during inference.
         */
        private fun createJoinPredicateTypeValidTests(predicateType: StaticType): List<TestCase> =
            JOIN_WITH_PREDICATE.map { join ->
                TestCase(
                    name = "$join with predicate type $predicateType",
                    originalSql = "SELECT * FROM a $join b ON c",
                    globals = mapOf(
                        "a" to BagType(StructType(mapOf("foo" to StaticType.INT))),
                        "b" to BagType(StructType(mapOf("bar" to StaticType.STRING))),
                        "c" to predicateType
                    ),
                    handler = expectQueryOutputType(
                        BagType(
                            StructType(
                                mapOf(
                                    "foo" to StaticType.INT,
                                    "bar" to StaticType.STRING
                                )
                            )
                        )
                    )
                )
            }

        /**
         * Creates a simple SFW query with a join predicate for each [JOIN_WITH_PREDICATE] that expects
         * [expectedProblems] during inference and checks that the JOIN predicate has a static type of
         * [StaticType.BOOL].
         */
        private fun createJoinPredicateContinuationTypeTests(
            predicateType: StaticType,
            expectedProblems: List<Problem>
        ): List<TestCase> =
            JOIN_WITH_PREDICATE.map { join ->
                TestCase(
                    name = "$join with predicate $predicateType",
                    originalSql = """
                        SELECT * FROM a
                        $join b
                        ON c""",
                    globals = mapOf(
                        "a" to BagType(StructType(mapOf("foo" to StaticType.INT))),
                        "b" to BagType(StructType(mapOf("bar" to StaticType.STRING))),
                        "c" to predicateType
                    ),
                    handler = expectProblemsAndAssert(
                        expectedProblems = expectedProblems,
                        assertionBlock = { partiqlAst ->
                            val query = (partiqlAst as PartiqlAst.Statement.Query)
                            val selectExpr = query.expr as PartiqlAst.Expr.Select
                            val joinExpr = selectExpr.from as PartiqlAst.FromSource.Join
                            assertEquals(StaticType.BOOL, joinExpr.predicate?.metas?.staticType?.type)
                        }
                    )
                )
            }

        @JvmStatic
        @Suppress("unused")
        fun parametersForJoinPredicateTests() =
            // `JOIN` predicates with valid types containing `BOOL`. These tests are meant to just test the `JOIN`
            // predicate inference behavior.
            createJoinPredicateTypeValidTests(predicateType = StaticType.BOOL) +
                createJoinPredicateTypeValidTests(predicateType = StaticType.BOOL.asNullable()) +
                createJoinPredicateTypeValidTests(predicateType = StaticType.BOOL.asOptional()) +
                createJoinPredicateTypeValidTests(
                    predicateType = StaticType.BOOL.asNullable().asOptional()
                ) +
                createJoinPredicateTypeValidTests(
                    predicateType = StaticType.unionOf(
                        StaticType.BOOL,
                        StaticType.INT,
                        StaticType.STRING
                    )
                ) +
                //
                // `JOIN` predicates with invalid types below
                //
                // incompatible types for predicate expression -> incompatible types for expression error
                ALL_NON_BOOL_NON_UNKNOWN_TYPES.flatMap { nonBoolType ->
                    createJoinPredicateContinuationTypeTests(
                        predicateType = nonBoolType,
                        expectedProblems = listOf(
                            createIncompatibleTypesForExprError(
                                SourceLocationMeta(4L, 28L, 1L),
                                expectedType = StaticType.BOOL,
                                actualType = nonBoolType
                            )
                        )
                    )
                } +
                ALL_UNKNOWN_TYPES.flatMap { unknownType ->
                    // missing types for predicate expression -> always missing error
                    if (unknownType is MissingType) {
                        createJoinPredicateContinuationTypeTests(
                            predicateType = unknownType,
                            expectedProblems = listOf(
                                createReturnsMissingError(
                                    SourceLocationMeta(4L, 28L, 1L)
                                )
                            )
                        )
                    } else {
                        listOf(
                            // TODO: verify the behavior when join predicate is null or unionOf(null, missing)
                            //  Current the inferencer will return Bag(struct(foo: int, bar: string)), which may not be correct.
                            TestCase(
                                name = "join with predicate type $unknownType",
                                originalSql = "SELECT * FROM a join b ON c",
                                globals = mapOf(
                                    "a" to BagType(StructType(mapOf("foo" to StaticType.INT))),
                                    "b" to BagType(StructType(mapOf("bar" to StaticType.STRING))),
                                    "c" to unknownType
                                ),
                                handler = expectQueryOutputType(
                                    BagType(
                                        StructType(
                                            mapOf(
                                                "foo" to StaticType.INT,
                                                "bar" to StaticType.STRING
                                            )
                                        )
                                    ),
                                    expectedWarnings = listOf(
                                        createReturnsNullOrMissingWarning(
                                            SourceLocationMeta(1L, 27L, 1L)
                                        )
                                    )
                                )
                            ),
                            TestCase(
                                name = "left join with predicate type $unknownType",
                                originalSql = "SELECT * FROM a left join b ON c",
                                globals = mapOf(
                                    "a" to BagType(StructType(mapOf("foo" to StaticType.INT))),
                                    "b" to BagType(StructType(mapOf("bar" to StaticType.STRING))),
                                    "c" to unknownType
                                ),
                                handler = expectQueryOutputType(
                                    BagType(
                                        StructType(
                                            mapOf(
                                                "foo" to StaticType.INT,
                                                "bar" to StaticType.STRING
                                            )
                                        )
                                    ),
                                    expectedWarnings = listOf(
                                        createReturnsNullOrMissingWarning(
                                            SourceLocationMeta(1L, 32L, 1L)
                                        )
                                    )
                                )
                            ),
                            TestCase(
                                name = "left join with predicate type $unknownType",
                                originalSql = "SELECT * FROM a left join b ON c",
                                globals = mapOf(
                                    "a" to BagType(StructType(mapOf("foo" to StaticType.INT))),
                                    "b" to BagType(StructType(mapOf("bar" to StaticType.STRING))),
                                    "c" to unknownType
                                ),
                                handler = expectQueryOutputType(
                                    BagType(
                                        StructType(
                                            mapOf(
                                                "foo" to StaticType.INT,
                                                "bar" to StaticType.STRING
                                            )
                                        )
                                    ),
                                    expectedWarnings = listOf(
                                        createReturnsNullOrMissingWarning(
                                            SourceLocationMeta(1L, 32L, 1L)
                                        )
                                    )
                                )
                            )
                        )
                    }
                } +
                // other predicate types resulting in an error
                createJoinPredicateContinuationTypeTests(
                    predicateType = StaticType.INT.asNullable(),
                    expectedProblems = listOf(
                        createIncompatibleTypesForExprError(
                            SourceLocationMeta(4L, 28L, 1L),
                            expectedType = StaticType.BOOL,
                            actualType = StaticType.INT.asNullable()
                        )
                    )
                ) +
                createJoinPredicateContinuationTypeTests(
                    predicateType = StaticType.INT.asOptional(),
                    expectedProblems = listOf(
                        createIncompatibleTypesForExprError(
                            SourceLocationMeta(4L, 28L, 1L),
                            expectedType = StaticType.BOOL,
                            actualType = StaticType.INT.asOptional()
                        )
                    )
                ) +
                createJoinPredicateContinuationTypeTests(
                    predicateType = StaticType.INT.asNullable().asOptional(),
                    expectedProblems = listOf(
                        createIncompatibleTypesForExprError(
                            SourceLocationMeta(4L, 28L, 1L),
                            expectedType = StaticType.BOOL,
                            actualType = StaticType.INT.asNullable().asOptional()
                        )
                    )
                ) +
                createJoinPredicateContinuationTypeTests(
                    predicateType = StaticType.unionOf(StaticType.INT, StaticType.FLOAT, StaticType.STRING),
                    expectedProblems = listOf(
                        createIncompatibleTypesForExprError(
                            SourceLocationMeta(4L, 28L, 1L),
                            expectedType = StaticType.BOOL,
                            actualType = StaticType.unionOf(StaticType.INT, StaticType.FLOAT, StaticType.STRING)
                        )
                    )
                )
    }
}
