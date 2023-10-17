package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.errors.Problem
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_NON_BOOL_NON_UNKNOWN_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_NON_UNKNOWN_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_UNKNOWN_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.TestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createDataTypeMismatchError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createDoubleNAryOpCases
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createReturnsMissingError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createReturnsNullOrMissingWarning
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createSingleNAryOpCasesWithSwappedArgs
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.doubleOpErrorCases
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectQueryOutputType
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectSemanticProblems
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.generateAllUniquePairs
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.runTest
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.singleNAryOpErrorTestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.singleNAryTestCase
import org.partiql.types.MissingType
import org.partiql.types.StaticType

class InferencerNaryLogicalTests {
    @ParameterizedTest
    @MethodSource("parametersForNAryLogicalTests")
    fun naryLogicalInferenceTests(tc: TestCase) = runTest(tc)

    companion object {
        private fun createNotTestCase(
            name: String,
            argType: StaticType,
            expectedType: StaticType,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            TestCase(
                name = name,
                originalSql = "NOT x",
                globals = mapOf("x" to argType),
                handler = expectQueryOutputType(expectedType, expectedWarnings)
            )

        private fun createNotDataTypeMismatchTestCase(
            name: String,
            argType: StaticType,
            expectedErrors: List<Problem>
        ) =
            TestCase(
                name = name,
                originalSql = "NOT x",
                globals = mapOf("x" to argType),
                handler = expectSemanticProblems(expectedErrors = expectedErrors)
            )

        /**
         * Creates two test cases with the specified operand and expected types for every NAry logical
         * operator, about `LOGICAL_OPERATORS.size X 2` test cases in total.
         * If leftType != rightType, then new testCase "y {op} x" is created
         */
        private fun singleNAryLogicalCases(
            name: String,
            leftType: StaticType,
            rightType: StaticType,
            expectedType: StaticType,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            createSingleNAryOpCasesWithSwappedArgs(
                InferencerTestUtil.OpType.LOGICAL,
                name,
                leftType,
                rightType,
                expectQueryOutputType(expectedType, expectedWarnings)
            )

        /**
         * Creates one test case with the specified operand and expected types for every arithmetic
         * operator, combined with every other logical operator, `LOGICAL_OPERATORS.size^2` test
         * cases in total.
         */
        private fun doubleLogicalOpCases(
            name: String,
            leftType: StaticType,
            middleType: StaticType,
            rightType: StaticType,
            expectedType: StaticType,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            createDoubleNAryOpCases(
                InferencerTestUtil.OpType.LOGICAL,
                name,
                leftType,
                middleType,
                rightType,
                expectedType,
                expectedWarnings
            )

        @JvmStatic
        @Suppress("unused")
        fun parametersForNAryLogicalTests() = listOf(
            //
            // `NOT` successful cases below this line
            //
            createNotTestCase(
                name = "NAry op NOT with boolean type",
                argType = StaticType.BOOL,
                expectedType = StaticType.BOOL
            ),
            createNotTestCase(
                name = "NAry op NOT with Union types",
                argType = StaticType.unionOf(StaticType.INT, StaticType.BOOL),
                expectedType = StaticType.unionOf(StaticType.MISSING, StaticType.BOOL)
            ),
            createNotTestCase(
                name = "NAry op NOT with ANY type",
                argType = StaticType.ANY,
                expectedType = StaticType.unionOf(StaticType.MISSING, StaticType.NULL, StaticType.BOOL)
            ),
            //
            // `NOT` data type mismatch cases below this line
            //
            createNotDataTypeMismatchTestCase(
                name = "NAry op NOT data type mismatch - nullable non-bool",
                argType = StaticType.INT.asNullable(),
                expectedErrors = listOf(
                    createDataTypeMismatchError(
                        col = 1,
                        argTypes = listOf(StaticType.INT.asNullable()),
                        nAryOp = "NOT"
                    )
                )
            ),
            createNotDataTypeMismatchTestCase(
                name = "NAry op NOT data type mismatch - optional non-bool",
                argType = StaticType.INT.asOptional(),
                expectedErrors = listOf(
                    createDataTypeMismatchError(
                        col = 1,
                        argTypes = listOf(StaticType.INT.asOptional()),
                        nAryOp = "NOT"
                    )
                )
            ),
            createNotDataTypeMismatchTestCase(
                name = "NAry op NOT data type mismatch - nullable, optional non-bool",
                argType = StaticType.INT.asNullable().asOptional(),
                expectedErrors = listOf(
                    createDataTypeMismatchError(
                        col = 1,
                        argTypes = listOf(StaticType.INT.asNullable().asOptional()),
                        nAryOp = "NOT"
                    )
                )
            ),
            createNotDataTypeMismatchTestCase(
                name = "NAry op NOT data type mismatch - union of non-bool types",
                argType = StaticType.unionOf(StaticType.INT, StaticType.STRING),
                expectedErrors = listOf(
                    createDataTypeMismatchError(
                        col = 1,
                        argTypes = listOf(StaticType.unionOf(StaticType.INT, StaticType.STRING)),
                        nAryOp = "NOT"
                    )
                )
            )
        ) +
            // `NOT` non-bool -> data type mismatch
            ALL_NON_BOOL_NON_UNKNOWN_TYPES.map { nonBoolType ->
                createNotDataTypeMismatchTestCase(
                    name = "NAry op NOT data type mismatch - $nonBoolType",
                    argType = nonBoolType,
                    expectedErrors = listOf(
                        createDataTypeMismatchError(
                            col = 1,
                            argTypes = listOf(nonBoolType),
                            nAryOp = "NOT"
                        )
                    )
                )
            } +
            // `NOT` unknown -> , null or missing error
            ALL_UNKNOWN_TYPES.map { unknownType ->
                if (unknownType is MissingType) {
                    createNotDataTypeMismatchTestCase(
                        name = "NAry op NOT null or missing error - $unknownType",
                        argType = unknownType,
                        expectedErrors = listOf(
                            createReturnsMissingError(
                                col = 1,
                                nAryOp = "NOT"
                            )
                        )
                    )
                } else {
                    createNotTestCase(
                        name = "NAry op NOT null or missing error - $unknownType",
                        argType = unknownType,
                        expectedType = unknownType,
                        expectedWarnings = listOf(
                            createReturnsNullOrMissingWarning(
                                col = 1,
                                nAryOp = "NOT"
                            )
                        )
                    )
                }
            } + listOf(
            //
            // `AND` + `OR` successful cases below this line
            //
            singleNAryLogicalCases(
                "bool, bool",
                leftType = StaticType.BOOL,
                rightType = StaticType.BOOL,
                expectedType = StaticType.BOOL
            ),
            singleNAryLogicalCases(
                "bool, any",
                leftType = StaticType.BOOL,
                rightType = StaticType.ANY,
                expectedType = StaticType.unionOf(StaticType.MISSING, StaticType.NULL, StaticType.BOOL)
            ),
            singleNAryLogicalCases(
                "union(int, bool), bool",
                leftType = StaticType.unionOf(StaticType.INT, StaticType.BOOL),
                rightType = StaticType.BOOL,
                expectedType = StaticType.unionOf(StaticType.MISSING, StaticType.BOOL)
            ),
            doubleLogicalOpCases(
                "bool, bool, bool",
                leftType = StaticType.BOOL,
                middleType = StaticType.BOOL,
                rightType = StaticType.BOOL,
                expectedType = StaticType.BOOL
            ),
            //
            // `AND` + `OR` data type mismatch cases below this line
            //
            InferencerTestUtil.OpType.LOGICAL.operators.flatMap { op ->
                // non-unknown, non-boolean with non-unknown -> data type mismatch
                generateAllUniquePairs(
                    ALL_NON_BOOL_NON_UNKNOWN_TYPES,
                    ALL_NON_UNKNOWN_TYPES
                ).map {
                    singleNAryOpErrorTestCase(
                        name = "data type mismatch - ${it.first}, ${it.second}",
                        op = op,
                        leftType = it.first,
                        rightType = it.second,
                        expectedErrors = listOf(
                            createDataTypeMismatchError(
                                col = 3,
                                argTypes = listOf(it.first, it.second),
                                nAryOp = op
                            )
                        )
                    )
                } +
                    // non-unknown, non-boolean with unknown -> data type mismatch and null or missing error
                    generateAllUniquePairs(
                        ALL_NON_BOOL_NON_UNKNOWN_TYPES,
                        ALL_UNKNOWN_TYPES
                    ).map {
                        if (it.first is MissingType || it.second is MissingType) {
                            singleNAryOpErrorTestCase(
                                name = "data type mismatch, always missing error - ${it.first}, ${it.second}",
                                op = op,
                                leftType = it.first,
                                rightType = it.second,
                                expectedErrors = listOf(
                                    createDataTypeMismatchError(
                                        col = 3,
                                        argTypes = listOf(it.first, it.second),
                                        nAryOp = op
                                    ),
                                    createReturnsMissingError(
                                        col = 3,
                                        nAryOp = op
                                    )
                                )
                            )
                        } else {
                            singleNAryOpErrorTestCase(
                                name = "data type mismatch, null or missing warning - ${it.first}, ${it.second}",
                                op = op,
                                leftType = it.first,
                                rightType = it.second,
                                expectedErrors = listOf(
                                    createDataTypeMismatchError(
                                        col = 3,
                                        argTypes = listOf(it.first, it.second),
                                        nAryOp = op
                                    ),
                                ),
                                expectedWarnings = listOf(
                                    createReturnsNullOrMissingWarning(
                                        col = 3,
                                        nAryOp = op
                                    )
                                )
                            )
                        }
                    } +
                    // bool with an unknown -> null or missing error
                    generateAllUniquePairs(
                        listOf(StaticType.BOOL),
                        ALL_UNKNOWN_TYPES
                    ).map {
                        if (it.first is MissingType || it.second is MissingType) {
                            singleNAryOpErrorTestCase(
                                name = "always missing error - ${it.first}, ${it.second}",
                                op = op,
                                leftType = it.first,
                                rightType = it.second,
                                expectedErrors = listOf(
                                    createReturnsMissingError(
                                        col = 3,
                                        nAryOp = op
                                    )
                                )
                            )
                        } else if (it.first == StaticType.NULL_OR_MISSING || it.second == StaticType.NULL_OR_MISSING) {
                            singleNAryTestCase(
                                name = "null or missing warning - ${it.first}, ${it.second}",
                                op = op,
                                leftType = it.first,
                                rightType = it.second,
                                expectedType = StaticType.unionOf(StaticType.MISSING, StaticType.NULL, StaticType.BOOL),
                                expectedWarnings = listOf(
                                    createReturnsNullOrMissingWarning(
                                        col = 3,
                                        nAryOp = op
                                    )
                                )
                            )
                        } else {
                            singleNAryTestCase(
                                name = "null or missing warning - ${it.first}, ${it.second}",
                                op = op,
                                leftType = it.first,
                                rightType = it.second,
                                expectedType = StaticType.unionOf(StaticType.NULL, StaticType.BOOL),
                                expectedWarnings = listOf(
                                    createReturnsNullOrMissingWarning(
                                        col = 3,
                                        nAryOp = op
                                    )
                                )
                            )
                        }
                    } +
                    // unknown with an unknown -> null or missing error
                    generateAllUniquePairs(
                        ALL_UNKNOWN_TYPES,
                        ALL_UNKNOWN_TYPES
                    ).map {
                        if (it.first is MissingType || it.second is MissingType) {
                            singleNAryOpErrorTestCase(
                                name = "always missing error - ${it.first}, ${it.second}",
                                op = op,
                                leftType = it.first,
                                rightType = it.second,
                                expectedErrors = listOf(
                                    createReturnsMissingError(
                                        col = 3,
                                        nAryOp = op
                                    )
                                )
                            )
                        } else if (it.first == StaticType.NULL_OR_MISSING || it.second == StaticType.NULL_OR_MISSING) {
                            singleNAryTestCase(
                                name = "null or missing warning - ${it.first}, ${it.second}",
                                op = op,
                                leftType = it.first,
                                rightType = it.second,
                                expectedType = StaticType.NULL_OR_MISSING,
                                expectedWarnings = listOf(
                                    createReturnsNullOrMissingWarning(
                                        col = 3,
                                        nAryOp = op
                                    )
                                )
                            )
                        } else {
                            singleNAryTestCase(
                                name = "null or missing warning - ${it.first}, ${it.second}",
                                op = op,
                                leftType = it.first,
                                rightType = it.second,
                                expectedType = StaticType.NULL,
                                expectedWarnings = listOf(
                                    createReturnsNullOrMissingWarning(
                                        col = 3,
                                        nAryOp = op
                                    )
                                )
                            )
                        }
                    } + listOf(
                    singleNAryOpErrorTestCase(
                        "data type mismatch - union(int, string), bool",
                        op = op,
                        leftType = StaticType.unionOf(StaticType.INT, StaticType.STRING),
                        rightType = StaticType.BOOL,
                        expectedErrors = listOf(
                            createDataTypeMismatchError(
                                col = 3,
                                argTypes = listOf(
                                    StaticType.unionOf(StaticType.INT, StaticType.STRING),
                                    StaticType.BOOL
                                ),
                                nAryOp = op
                            )
                        )
                    ),
                    singleNAryOpErrorTestCase(
                        "data type mismatch - null_or_missing, int",
                        op = op,
                        leftType = StaticType.NULL_OR_MISSING,
                        rightType = StaticType.INT,
                        expectedErrors = listOf(
                            createDataTypeMismatchError(
                                col = 3,
                                argTypes = listOf(StaticType.NULL_OR_MISSING, StaticType.INT),
                                nAryOp = op
                            ),
                        ),
                        expectedWarnings = listOf(
                            createReturnsNullOrMissingWarning(
                                col = 3,
                                nAryOp = op
                            )
                        )
                    )
                )
            } +
                // double logical op with at least one non-unknown, non-boolean -> data type mismatch
                listOf("AND", "OR ").flatMap { op ->
                    ALL_NON_BOOL_NON_UNKNOWN_TYPES.flatMap { nonBoolType ->
                        listOf(
                            doubleOpErrorCases(
                                name = "data type mismatch - $nonBoolType, bool, bool",
                                op = op,
                                leftType = nonBoolType,
                                middleType = StaticType.BOOL,
                                rightType = StaticType.BOOL,
                                expectedErrors = listOf(
                                    createDataTypeMismatchError(
                                        col = 3,
                                        argTypes = listOf(nonBoolType, StaticType.BOOL),
                                        nAryOp = op.trim()
                                    )
                                )
                            ),
                            doubleOpErrorCases(
                                name = "data type mismatch - bool, $nonBoolType, bool",
                                op = op,
                                leftType = StaticType.BOOL,
                                middleType = nonBoolType,
                                rightType = StaticType.BOOL,
                                expectedErrors = listOf(
                                    createDataTypeMismatchError(
                                        col = 3,
                                        argTypes = listOf(StaticType.BOOL, nonBoolType),
                                        nAryOp = op.trim()
                                    )
                                )
                            ),
                            doubleOpErrorCases(
                                name = "data type mismatch - bool, bool, $nonBoolType",
                                op = op,
                                leftType = StaticType.BOOL,
                                middleType = StaticType.BOOL,
                                rightType = nonBoolType,
                                expectedErrors = listOf(
                                    createDataTypeMismatchError(
                                        col = 9,
                                        argTypes = listOf(StaticType.BOOL, nonBoolType),
                                        nAryOp = op.trim()
                                    )
                                )
                            ),
                            doubleOpErrorCases(
                                name = "data type mismatch - $nonBoolType, $nonBoolType, bool",
                                op = op,
                                leftType = nonBoolType,
                                middleType = nonBoolType,
                                rightType = StaticType.BOOL,
                                expectedErrors = listOf(
                                    createDataTypeMismatchError(
                                        col = 3,
                                        argTypes = listOf(nonBoolType, nonBoolType),
                                        nAryOp = op.trim()
                                    )
                                )
                            ),
                            doubleOpErrorCases(
                                name = "data type mismatch - bool, $nonBoolType, $nonBoolType",
                                op = op,
                                leftType = StaticType.BOOL,
                                middleType = nonBoolType,
                                rightType = nonBoolType,
                                expectedErrors = listOf(
                                    createDataTypeMismatchError(
                                        col = 3,
                                        argTypes = listOf(StaticType.BOOL, nonBoolType),
                                        nAryOp = op.trim()
                                    ),
                                    createDataTypeMismatchError(
                                        col = 9,
                                        argTypes = listOf(StaticType.BOOL, nonBoolType),
                                        nAryOp = op.trim()
                                    )
                                )
                            ),
                            doubleOpErrorCases(
                                name = "data type mismatch - $nonBoolType, $nonBoolType, $nonBoolType",
                                op = op,
                                leftType = nonBoolType,
                                middleType = nonBoolType,
                                rightType = nonBoolType,
                                expectedErrors = listOf(
                                    createDataTypeMismatchError(
                                        col = 3,
                                        argTypes = listOf(nonBoolType, nonBoolType),
                                        nAryOp = op.trim()
                                    ),
                                    createDataTypeMismatchError(
                                        col = 9,
                                        argTypes = listOf(StaticType.BOOL, nonBoolType),
                                        nAryOp = op.trim()
                                    )
                                )
                            )
                        )
                    }
                }
        ).flatten()
    }
}
