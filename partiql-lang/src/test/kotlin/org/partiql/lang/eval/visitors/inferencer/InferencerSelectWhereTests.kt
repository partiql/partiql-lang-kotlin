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

class InferencerSelectWhereTests {
    @ParameterizedTest
    @MethodSource("parametersForSelectWhereTests")
    fun selectWhereTests(tc: TestCase) = runTest(tc)

    companion object {
        /**
         * Creates a simple SFW query with a where expression of type [whereType]. Verifies that no errors are
         * encountered during inference.
         */
        private fun createSelectWhereTypeValidTests(whereType: StaticType, expectedWarnings: List<Problem> = emptyList()): TestCase =
            TestCase(
                name = "SELECT * FROM t WHERE <$whereType>",
                originalSql = "SELECT * FROM t WHERE condition",
                globals = mapOf("t" to BagType(StructType(mapOf("condition" to whereType)))),
                handler = expectQueryOutputType(BagType(StructType(mapOf("condition" to whereType))), expectedWarnings)
            )

        /**
         * Creates a simple SFW query with a where expression of type [whereType]. Expects [expectedProblems] during
         * inference and checks that the where clause has a static type of [StaticType.BOOL].
         */
        private fun createSelectWhereContinuationTypeTests(
            whereType: StaticType,
            expectedProblems: List<Problem>
        ): TestCase =
            TestCase(
                name = "SELECT * FROM t WHERE <$whereType>",
                originalSql = "SELECT * FROM t WHERE condition",
                globals = mapOf("t" to BagType(StructType(mapOf("condition" to whereType)))),
                handler = expectProblemsAndAssert(
                    expectedProblems = expectedProblems,
                    assertionBlock = { partiqlAst ->
                        val query = partiqlAst as PartiqlAst.Statement.Query
                        val selectExpr = query.expr as PartiqlAst.Expr.Select
                        val whereExpr = selectExpr.where
                        assertEquals(StaticType.BOOL, whereExpr?.metas?.staticType?.type)
                    }
                )
            )

        @JvmStatic
        @Suppress("unused")
        fun parametersForSelectWhereTests() =
            // `WHERE` expressions with valid types containing `BOOL`. These tests are meant to just test the
            // `WHERE` expression inference behavior.
            listOf(
                createSelectWhereTypeValidTests(whereType = StaticType.BOOL),
                createSelectWhereTypeValidTests(whereType = StaticType.BOOL.asNullable()),
                createSelectWhereTypeValidTests(whereType = StaticType.BOOL.asOptional()),
                createSelectWhereTypeValidTests(
                    whereType = StaticType.BOOL.asNullable().asOptional()
                ),
                createSelectWhereTypeValidTests(
                    whereType = StaticType.unionOf(
                        StaticType.BOOL,
                        StaticType.INT,
                        StaticType.STRING
                    )
                )
            ) +
                //
                // `WHERE` expressions with invalid types below
                //
                // incompatible types for where expression -> incompatible types for expression error
                ALL_NON_BOOL_NON_UNKNOWN_TYPES.map { nonBoolType ->
                    createSelectWhereContinuationTypeTests(
                        whereType = nonBoolType,
                        expectedProblems = listOf(
                            createIncompatibleTypesForExprError(
                                SourceLocationMeta(1L, 23L, 9L),
                                expectedType = StaticType.BOOL,
                                actualType = nonBoolType
                            )
                        )
                    )
                } +
                // unknown types for where expression -> null or missing error
                ALL_UNKNOWN_TYPES.map { unknownType ->
                    if (unknownType is MissingType) {
                        createSelectWhereContinuationTypeTests(
                            whereType = unknownType,
                            expectedProblems = listOf(
                                createReturnsMissingError(
                                    SourceLocationMeta(1L, 23L, 9L)
                                )
                            )
                        )
                    } else {
                        createSelectWhereTypeValidTests(
                            whereType = unknownType,
                            expectedWarnings = listOf(
                                createReturnsNullOrMissingWarning(
                                    SourceLocationMeta(1L, 23L, 9L)
                                )
                            )
                        )
                    }
                } +
                listOf(
                    // other where expression types resulting in an error
                    createSelectWhereContinuationTypeTests(
                        whereType = StaticType.INT.asNullable(),
                        expectedProblems = listOf(
                            createIncompatibleTypesForExprError(
                                SourceLocationMeta(1L, 23L, 9L),
                                expectedType = StaticType.BOOL,
                                actualType = StaticType.INT.asNullable()
                            )
                        )
                    ),
                    createSelectWhereContinuationTypeTests(
                        whereType = StaticType.INT.asOptional(),
                        expectedProblems = listOf(
                            createIncompatibleTypesForExprError(
                                SourceLocationMeta(1L, 23L, 9L),
                                expectedType = StaticType.BOOL,
                                actualType = StaticType.INT.asOptional()
                            )
                        )
                    ),
                    createSelectWhereContinuationTypeTests(
                        whereType = StaticType.INT.asNullable().asOptional(),
                        expectedProblems = listOf(
                            createIncompatibleTypesForExprError(
                                SourceLocationMeta(1L, 23L, 9L),
                                expectedType = StaticType.BOOL,
                                actualType = StaticType.INT.asNullable().asOptional()
                            )
                        )
                    ),
                    createSelectWhereContinuationTypeTests(
                        whereType = StaticType.unionOf(StaticType.INT, StaticType.FLOAT, StaticType.STRING),
                        expectedProblems = listOf(
                            createIncompatibleTypesForExprError(
                                SourceLocationMeta(1L, 23L, 9L),
                                expectedType = StaticType.BOOL,
                                actualType = StaticType.unionOf(StaticType.INT, StaticType.FLOAT, StaticType.STRING)
                            )
                        )
                    )
                )
    }
}
