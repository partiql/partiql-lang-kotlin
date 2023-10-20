package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.errors.Problem
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_LOB_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_NON_LOB_NON_UNKNOWN_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_NON_NUMERIC_NON_UNKNOWN_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_NON_TEXT_NON_UNKNOWN_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_NON_UNKNOWN_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_NUMERIC_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_TEXT_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_TYPES_ONLY_COMPARABLE_TO_SELF
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_UNKNOWN_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.TestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createDataTypeMismatchError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createReturnsMissingError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createReturnsNullOrMissingWarning
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectQueryOutputType
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectSemanticProblems
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.generateAllUniquePairs
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.runTest
import org.partiql.types.MissingType
import org.partiql.types.StaticType

class InferencerNullIfTests {
    @ParameterizedTest
    @MethodSource("parametersForNullIfTests")
    fun nullIfTests(tc: TestCase) = runTest(tc)

    companion object {
        /**
         * Creates a test of the form: NULLIF([leftType], [rightType]) and expects an output type of [leftType] with
         * [StaticType.NULL].
         */
        private fun createValidNullIfTest(
            leftType: StaticType,
            rightType: StaticType,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            TestCase(
                name = "NULLIF($leftType, $rightType)",
                originalSql = "NULLIF(lhs, rhs)",
                globals = mapOf(
                    "lhs" to leftType,
                    "rhs" to rightType
                ),
                handler = expectQueryOutputType(leftType.asNullable(), expectedWarnings)
            )

        /**
         * Creates a test of the form: NULLIF([leftType], [rightType]) and expects [expectedErrors] during inference.
         */
        private fun createErrorNullIfTest(
            name: String,
            leftType: StaticType,
            rightType: StaticType,
            expectedErrors: List<Problem>
        ) =
            TestCase(
                name = name,
                originalSql = "NULLIF(lhs, rhs)",
                globals = mapOf(
                    "lhs" to leftType,
                    "rhs" to rightType
                ),
                handler = expectSemanticProblems(expectedErrors = expectedErrors)
            )

        @JvmStatic
        @Suppress("unused")
        fun parametersForNullIfTests() =
            // NULLIF(<numeric>, <numeric>)
            generateAllUniquePairs(
                ALL_NUMERIC_TYPES,
                ALL_NUMERIC_TYPES
            ).map {
                createValidNullIfTest(
                    leftType = it.first,
                    rightType = it.second
                )
            } +
                // NULLIF(<text>, <text>)
                generateAllUniquePairs(
                    ALL_TEXT_TYPES,
                    ALL_TEXT_TYPES
                ).map {
                    createValidNullIfTest(
                        leftType = it.first,
                        rightType = it.second
                    )
                } +
                // NULLIF(<lob>, <lob>)
                generateAllUniquePairs(
                    ALL_LOB_TYPES,
                    ALL_LOB_TYPES
                ).map {
                    createValidNullIfTest(
                        leftType = it.first,
                        rightType = it.second
                    )
                } +
                // `NULLIF` with types only comparable to self
                ALL_TYPES_ONLY_COMPARABLE_TO_SELF.map {
                    createValidNullIfTest(
                        leftType = it,
                        rightType = it
                    )
                } +
                // other valid `NULLIF` tests
                listOf(
                    createValidNullIfTest(
                        leftType = StaticType.ANY,
                        rightType = StaticType.STRING
                    ),
                    createValidNullIfTest(
                        leftType = StaticType.unionOf(StaticType.STRING, StaticType.INT),
                        rightType = StaticType.STRING
                    ),
                    createValidNullIfTest(
                        leftType = StaticType.unionOf(StaticType.STRING, StaticType.INT),
                        rightType = StaticType.unionOf(StaticType.INT8, StaticType.FLOAT, StaticType.SYMBOL)
                    ),
                    createValidNullIfTest(
                        leftType = StaticType.INT.asNullable(),
                        rightType = StaticType.INT.asOptional()
                    ),
                    createValidNullIfTest(
                        leftType = StaticType.INT.asNullable(),
                        rightType = StaticType.FLOAT.asOptional()
                    )
                ) +
                //
                // `NULLIF` error cases below
                //

                // NULLIF with a numeric and non-numeric, non-unknown -> data type mismatch
                ALL_NUMERIC_TYPES.flatMap { numericType ->
                    ALL_NON_NUMERIC_NON_UNKNOWN_TYPES.map { nonNumericType ->
                        createErrorNullIfTest(
                            name = "data type mismatch - NULLIF($numericType, $nonNumericType)",
                            leftType = numericType,
                            rightType = nonNumericType,
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 1,
                                    argTypes = listOf(numericType, nonNumericType),
                                    nAryOp = "NULLIF"
                                )
                            )
                        )
                    }
                } +
                // NULLIF with a text and non-text, non-unknown -> data type mismatch
                ALL_TEXT_TYPES.flatMap { textType ->
                    ALL_NON_TEXT_NON_UNKNOWN_TYPES.map { nonTextType ->
                        createErrorNullIfTest(
                            name = "data type mismatch - NULLIF($textType, $nonTextType)",
                            leftType = textType,
                            rightType = nonTextType,
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 1,
                                    argTypes = listOf(textType, nonTextType),
                                    nAryOp = "NULLIF"
                                )
                            )
                        )
                    }
                } +
                // NULLIF with a lob and non-lob, non-unknown -> data type mismatch
                ALL_LOB_TYPES.flatMap { lobType ->
                    ALL_NON_LOB_NON_UNKNOWN_TYPES.map { nonLobType ->
                        createErrorNullIfTest(
                            name = "data type mismatch - NULLIF($lobType, $nonLobType)",
                            leftType = lobType,
                            rightType = nonLobType,
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 1,
                                    argTypes = listOf(lobType, nonLobType),
                                    nAryOp = "NULLIF"
                                )
                            )
                        )
                    }
                } +
                // NULLIF with a type only comparable to itself and other non-unknown type -> data type mismatch
                ALL_TYPES_ONLY_COMPARABLE_TO_SELF.flatMap { type ->
                    ALL_NON_UNKNOWN_TYPES.filter { it != type }.map { incomparableToType ->
                        createErrorNullIfTest(
                            name = "data type mismatch - NULLIF($type, $incomparableToType)",
                            leftType = type,
                            rightType = incomparableToType,
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 1,
                                    argTypes = listOf(type, incomparableToType),
                                    nAryOp = "NULLIF"
                                )
                            )
                        )
                    }
                } +
                // NULLIF with a type and unknown -> always return missing error or null or missing warning
                generateAllUniquePairs(
                    StaticType.ALL_TYPES,
                    ALL_UNKNOWN_TYPES
                ).map {
                    if (it.first is MissingType || it.second is MissingType) {
                        createErrorNullIfTest(
                            name = "always missing error - ${it.first}, ${it.second}",
                            leftType = it.first,
                            rightType = it.second,
                            expectedErrors = listOf(
                                createReturnsMissingError(
                                    col = 1,
                                    nAryOp = "NULLIF"
                                )
                            )
                        )
                    } else {
                        createValidNullIfTest(
                            leftType = it.first,
                            rightType = it.second,
                            expectedWarnings = listOf(
                                createReturnsNullOrMissingWarning(
                                    col = 1,
                                    nAryOp = "NULLIF"
                                )
                            )
                        )
                    }
                } +
                // other miscellaneous error tests
                listOf(
                    createErrorNullIfTest(
                        name = "data type mismatch - NULLIF(nullable int, string)",
                        leftType = StaticType.INT.asNullable(),
                        rightType = StaticType.STRING,
                        expectedErrors = listOf(
                            createDataTypeMismatchError(
                                col = 1,
                                argTypes = listOf(StaticType.INT.asNullable(), StaticType.STRING),
                                nAryOp = "NULLIF"
                            )
                        )
                    ),
                    createErrorNullIfTest(
                        name = "data type mismatch - NULLIF(optional int, string)",
                        leftType = StaticType.INT.asOptional(),
                        rightType = StaticType.STRING,
                        expectedErrors = listOf(
                            createDataTypeMismatchError(
                                col = 1,
                                argTypes = listOf(StaticType.INT.asOptional(), StaticType.STRING),
                                nAryOp = "NULLIF"
                            )
                        )
                    ),
                    createErrorNullIfTest(
                        name = "data type mismatch - NULLIF(nullable int, nullable string)",
                        leftType = StaticType.INT.asNullable(),
                        rightType = StaticType.STRING.asNullable(),
                        expectedErrors = listOf(
                            createDataTypeMismatchError(
                                col = 1,
                                argTypes = listOf(StaticType.INT.asNullable(), StaticType.STRING.asNullable()),
                                nAryOp = "NULLIF"
                            )
                        )
                    ),
                    createErrorNullIfTest(
                        name = "data type mismatch - NULLIF(union(string, int), bool)",
                        leftType = StaticType.unionOf(StaticType.STRING, StaticType.INT),
                        rightType = StaticType.BOOL,
                        expectedErrors = listOf(
                            createDataTypeMismatchError(
                                col = 1,
                                argTypes = listOf(
                                    StaticType.unionOf(StaticType.STRING, StaticType.INT),
                                    StaticType.BOOL
                                ),
                                nAryOp = "NULLIF"
                            )
                        )
                    ),
                    createErrorNullIfTest(
                        name = "data type mismatch - NULLIF(union(string, int), union(bag, list))",
                        leftType = StaticType.unionOf(StaticType.STRING, StaticType.INT),
                        rightType = StaticType.unionOf(StaticType.BAG, StaticType.LIST),
                        expectedErrors = listOf(
                            createDataTypeMismatchError(
                                col = 1,
                                argTypes = listOf(
                                    StaticType.unionOf(StaticType.STRING, StaticType.INT),
                                    StaticType.unionOf(StaticType.BAG, StaticType.LIST)
                                ),
                                nAryOp = "NULLIF"
                            )
                        )
                    ),
                    createErrorNullIfTest(
                        name = "always returns missing error - NULLIF(missing, optional int)",
                        leftType = StaticType.MISSING,
                        rightType = StaticType.INT.asOptional(),
                        expectedErrors = listOf(
                            createReturnsMissingError(
                                col = 1,
                                nAryOp = "NULLIF"
                            )
                        )
                    ),
                    createValidNullIfTest(
                        leftType = StaticType.ANY,
                        rightType = StaticType.NULL_OR_MISSING,
                        expectedWarnings = listOf(
                            createReturnsNullOrMissingWarning(
                                col = 1,
                                nAryOp = "NULLIF"
                            )
                        )
                    )
                )
    }
}
