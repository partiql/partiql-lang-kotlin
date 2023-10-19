package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.errors.Problem
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_NON_TEXT_NON_UNKNOWN_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_NON_UNKNOWN_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_TEXT_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_UNKNOWN_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.TestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createDataTypeMismatchError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createReturnsMissingError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createReturnsNullOrMissingWarning
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createSingleNAryOpCasesWithSwappedArgs
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectQueryOutputType
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.generateAllUniquePairs
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.runTest
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.singleNAryOpErrorTestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.singleNAryOpMismatchWithSwappedCases
import org.partiql.types.MissingType
import org.partiql.types.NumberConstraint
import org.partiql.types.StaticType
import org.partiql.types.StringType

class InferencerNaryConcatTests {
    @ParameterizedTest
    @MethodSource("parametersForNAryConcatTests")
    fun naryConcatInferenceTests(tc: TestCase) = runTest(tc)

    companion object {
        /**
         * Creates two test cases with the specified operand and expected types and warnings for the concat op
         * (creates x || y and y || x).
         *
         * If [leftType] != [rightType], then new [TestCase] "y {op} x" is created.
         */
        private fun createNAryConcatTest(
            name: String,
            leftType: StaticType,
            rightType: StaticType,
            expectedType: StaticType,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            createSingleNAryOpCasesWithSwappedArgs(
                InferencerTestUtil.OpType.CONCAT,
                name,
                leftType,
                rightType,
                expectQueryOutputType(expectedType, expectedWarnings)
            )

        /**
         * Creates a [TestCase] for the concat with the query "x || y" with `x` corresponding to [leftType] and `y`
         * corresponding to [rightType]. The created [TestCase] will expect [expectedErrors] through inference.
         */
        private fun createNAryConcatErrorTest(
            name: String,
            leftType: StaticType,
            rightType: StaticType,
            expectedErrors: List<Problem>,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            singleNAryOpErrorTestCase(
                name,
                "||",
                leftType,
                rightType,
                expectedErrors,
                expectedWarnings
            )

        @JvmStatic
        @Suppress("unused")
        fun parametersForNAryConcatTests() = listOf(
            createNAryConcatTest(
                name = "unconstrained string, unconstrained string",
                leftType = StaticType.STRING,
                rightType = StaticType.STRING,
                expectedType = StaticType.STRING
            ),
            createNAryConcatTest(
                name = "unconstrained string, symbol",
                leftType = StaticType.STRING,
                rightType = StaticType.SYMBOL,
                expectedType = StaticType.STRING
            ),
            createNAryConcatTest(
                name = "unconstrained symbol, unconstrained symbol",
                leftType = StaticType.SYMBOL,
                rightType = StaticType.SYMBOL,
                expectedType = StaticType.STRING
            ),
            createNAryConcatTest(
                name = "constrained string equals, unconstrained string",
                leftType = StringType(NumberConstraint.Equals(4)),
                rightType = StaticType.STRING,
                expectedType = StaticType.STRING
            ),
            createNAryConcatTest(
                name = "constrained string up to, unconstrained string",
                leftType = StringType(NumberConstraint.UpTo(4)),
                rightType = StaticType.STRING,
                expectedType = StaticType.STRING
            ),
            createNAryConcatTest(
                name = "constrained string equals 4, constrained string equals 6",
                leftType = StringType(NumberConstraint.Equals(4)),
                rightType = StringType(NumberConstraint.Equals(6)),
                expectedType = StringType(NumberConstraint.Equals(10))
            ),
            createNAryConcatTest(
                name = "constrained string equals 4, constrained string up to 6",
                leftType = StringType(NumberConstraint.Equals(4)),
                rightType = StringType(NumberConstraint.UpTo(6)),
                expectedType = StringType(NumberConstraint.UpTo(10))
            ),
            createNAryConcatTest(
                name = "constrained string up to 4, constrained string equals 6",
                leftType = StringType(NumberConstraint.UpTo(4)),
                rightType = StringType(NumberConstraint.Equals(6)),
                expectedType = StringType(NumberConstraint.UpTo(10))
            ),
            createNAryConcatTest(
                name = "constrained string up to 4, constrained string up to 6",
                leftType = StringType(NumberConstraint.UpTo(4)),
                rightType = StringType(NumberConstraint.UpTo(6)),
                expectedType = StringType(NumberConstraint.UpTo(10))
            ),
            createNAryConcatTest(
                name = "ANY, ANY",
                leftType = StaticType.ANY,
                rightType = StaticType.ANY,
                expectedType = StaticType.unionOf(StaticType.MISSING, StaticType.STRING, StaticType.NULL)
            ),
            createNAryConcatTest(
                name = "compatible union type, symbol",
                leftType = StaticType.unionOf(StaticType.INT, StaticType.STRING),
                rightType = StaticType.SYMBOL,
                expectedType = StaticType.unionOf(StaticType.MISSING, StaticType.STRING)
            ),
            createNAryConcatTest(
                name = "compatible union type, null",
                leftType = StaticType.unionOf(StaticType.INT, StaticType.STRING, StaticType.NULL),
                rightType = StaticType.SYMBOL,
                expectedType = StaticType.unionOf(StaticType.MISSING, StaticType.STRING, StaticType.NULL)
            )
        ).flatten() +
            //
            // data type mismatch cases below this line
            //

            // non-text, non-unknown with non-unknown -> data type mismatch
            generateAllUniquePairs(
                ALL_NON_TEXT_NON_UNKNOWN_TYPES,
                ALL_NON_UNKNOWN_TYPES
            ).map {
                createNAryConcatErrorTest(
                    name = "data type mismatch - ${it.first}, ${it.second}",
                    leftType = it.first,
                    rightType = it.second,
                    expectedErrors = listOf(
                        createDataTypeMismatchError(
                            col = 3,
                            argTypes = listOf(it.first, it.second),
                            nAryOp = "||"
                        )
                    )
                )
            } +
            // non-text, non-unknown with an unknown -> data type mismatch & always returns missing error or null or missing warning
            generateAllUniquePairs(
                ALL_NON_TEXT_NON_UNKNOWN_TYPES,
                ALL_UNKNOWN_TYPES
            ).map {
                if (it.first is MissingType || it.second is MissingType) {
                    createNAryConcatErrorTest(
                        name = "data type mismatch, always missing error - ${it.first}, ${it.second}",
                        leftType = it.first,
                        rightType = it.second,
                        expectedErrors = listOf(
                            createDataTypeMismatchError(
                                col = 3,
                                argTypes = listOf(it.first, it.second),
                                nAryOp = "||"
                            ),
                            createReturnsMissingError(
                                col = 3,
                                nAryOp = "||"
                            )
                        )
                    )
                } else {
                    createNAryConcatErrorTest(
                        name = "data type mismatch, null or missing warning - ${it.first}, ${it.second}",
                        leftType = it.first,
                        rightType = it.second,
                        expectedErrors = listOf(
                            createDataTypeMismatchError(
                                col = 3,
                                argTypes = listOf(it.first, it.second),
                                nAryOp = "||"
                            ),
                        ),
                        expectedWarnings = listOf(
                            createReturnsNullOrMissingWarning(
                                col = 3,
                                nAryOp = "||"
                            )
                        )
                    )
                }
            } +
            // text with an unknown -> always returns missing error or null or missing warning
            generateAllUniquePairs(
                ALL_TEXT_TYPES,
                ALL_UNKNOWN_TYPES
            ).map {
                if (it.first is MissingType || it.second is MissingType) {
                    createNAryConcatErrorTest(
                        name = "always missing error - ${it.first}, ${it.second}",
                        leftType = it.first,
                        rightType = it.second,
                        expectedErrors = listOf(
                            createReturnsMissingError(
                                col = 3,
                                nAryOp = "||"
                            )
                        )
                    )
                } else {
                    InferencerTestUtil.singleNAryTestCase(
                        name = "null or missing warning - ${it.first}, ${it.second}",
                        op = "||",
                        leftType = it.first,
                        rightType = it.second,
                        expectedType = if (it.first == StaticType.NULL_OR_MISSING || it.second == StaticType.NULL_OR_MISSING) StaticType.NULL_OR_MISSING else StaticType.NULL,
                        expectedWarnings = listOf(
                            createReturnsNullOrMissingWarning(
                                col = 3,
                                nAryOp = "||"
                            )
                        )
                    )
                }
            } +
            // unknown with an unknown -> always returns missing error or null or missing warning
            generateAllUniquePairs(
                ALL_UNKNOWN_TYPES,
                ALL_UNKNOWN_TYPES
            ).map {
                if (it.first is MissingType || it.second is MissingType) {
                    createNAryConcatErrorTest(
                        name = "always missing error - ${it.first}, ${it.second}",
                        leftType = it.first,
                        rightType = it.second,
                        expectedErrors = listOf(
                            createReturnsMissingError(
                                col = 3,
                                nAryOp = "||"
                            )
                        )
                    )
                } else {
                    InferencerTestUtil.singleNAryTestCase(
                        name = "null or missing warning - ${it.first}, ${it.second}",
                        op = "||",
                        leftType = it.first,
                        rightType = it.second,
                        expectedType = if (it.first == StaticType.NULL_OR_MISSING || it.second == StaticType.NULL_OR_MISSING) StaticType.NULL_OR_MISSING else StaticType.NULL,
                        expectedWarnings = listOf(
                            createReturnsNullOrMissingWarning(
                                col = 3,
                                nAryOp = "||"
                            )
                        )
                    )
                }
            } + listOf(
            InferencerTestUtil.singleNAryTestCase(
                name = "null or missing warning - constrained string, null",
                op = "||",
                leftType = StringType(NumberConstraint.Equals(2)),
                rightType = StaticType.NULL,
                expectedType = StaticType.NULL,
                expectedWarnings = listOf(
                    createReturnsNullOrMissingWarning(
                        col = 3,
                        nAryOp = "||"
                    )
                )
            ),
            createNAryConcatErrorTest(
                name = "always missing error - constrained string, missing",
                leftType = StringType(NumberConstraint.Equals(2)),
                rightType = StaticType.MISSING,
                expectedErrors = listOf(
                    createReturnsMissingError(
                        col = 3,
                        nAryOp = "||"
                    )
                )
            ),
            createNAryConcatErrorTest(
                name = "always missing error - compatible union type, missing",
                leftType = StaticType.unionOf(StaticType.INT, StaticType.STRING, StaticType.NULL),
                rightType = StaticType.MISSING,
                expectedErrors = listOf(
                    createReturnsMissingError(
                        col = 3,
                        nAryOp = "||"
                    )
                )
            )
        ) +
            singleNAryOpMismatchWithSwappedCases(
                name = "data type mismatch - constrained string, int",
                op = "||",
                leftType = StringType(NumberConstraint.Equals(2)),
                rightType = StaticType.INT
            ) +
            singleNAryOpMismatchWithSwappedCases(
                name = "data type mismatch - union(int, string), bool",
                op = "||",
                leftType = StaticType.unionOf(StaticType.INT, StaticType.STRING),
                rightType = StaticType.BOOL
            ) +
            singleNAryOpMismatchWithSwappedCases(
                name = "data type mismatch - union(int, string, null), bool",
                op = "||",
                leftType = StaticType.unionOf(StaticType.INT, StaticType.STRING, StaticType.NULL),
                rightType = StaticType.BOOL
            ) +
            singleNAryOpMismatchWithSwappedCases(
                name = "data type mismatch - nullable int, string",
                op = "||",
                leftType = StaticType.INT.asNullable(),
                rightType = StaticType.STRING
            ) +
            singleNAryOpMismatchWithSwappedCases(
                name = "data type mismatch - optional int, string",
                op = "||",
                leftType = StaticType.INT.asOptional(),
                rightType = StaticType.STRING
            ) +
            singleNAryOpMismatchWithSwappedCases(
                name = "data type mismatch - nullable + optional int, string",
                op = "||",
                leftType = StaticType.INT.asNullable().asOptional(),
                rightType = StaticType.STRING
            ) +
            singleNAryOpMismatchWithSwappedCases(
                name = "data type mismatch - any, int",
                op = "||",
                leftType = StaticType.ANY,
                rightType = StaticType.INT
            )
    }
}
