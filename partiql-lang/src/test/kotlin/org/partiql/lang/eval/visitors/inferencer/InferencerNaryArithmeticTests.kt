package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.errors.Problem
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_NON_NUMERIC_NON_UNKNOWN_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_NON_UNKNOWN_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_NUMERIC_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_UNKNOWN_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createDataTypeMismatchError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createDoubleNAryOpCases
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createReturnsMissingError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createReturnsNullOrMissingWarning
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createSingleNAryOpCasesWithSwappedArgs
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.doubleOpErrorCases
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.doubleOpTestCases
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectQueryOutputType
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectSemanticProblems
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.generateAllUniquePairs
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.runTest
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.singleNAryOpErrorTestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.singleNAryOpMismatchWithSwappedCases
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.singleNAryTestCase
import org.partiql.types.AnyOfType
import org.partiql.types.MissingType
import org.partiql.types.StaticType

class InferencerNaryArithmeticTests {

    @ParameterizedTest
    @MethodSource("parametersForNAryArithmeticTests")
    fun naryArithmeticInferenceTests(tc: InferencerTestUtil.TestCase) = runTest(tc)

    companion object {
        /**
         * Creates two test cases with the specified operand and expected types and warnings for every arithmetic
         * operator (creates x {op} y and y {op} x), about `ARITHMETIC_OPERATORS.size * 2` test cases in total.
         * If leftType != rightType, then new testCase "y {op} x" is created.
         */
        private fun singleArithmeticOpCases(
            name: String,
            leftType: StaticType,
            rightType: StaticType,
            expectedType: StaticType,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            createSingleNAryOpCasesWithSwappedArgs(
                InferencerTestUtil.OpType.ARITHMETIC,
                name,
                leftType,
                rightType,
                expectQueryOutputType(expectedType, expectedWarnings)
            )

        /**
         * Creates one test case with the specified operand and expected types for every arithmetic
         * operator, combined with every other arithmetic operator, `ARITHMETIC_OPERATORS.size^2` test
         * cases in total.
         */
        private fun doubleArithmeticOpCases(
            name: String,
            leftType: StaticType,
            middleType: StaticType,
            rightType: StaticType,
            expectedType: StaticType,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            createDoubleNAryOpCases(
                InferencerTestUtil.OpType.ARITHMETIC,
                name,
                leftType,
                middleType,
                rightType,
                expectedType,
                expectedWarnings
            )

        @JvmStatic
        @Suppress("UNUSED")
        fun parametersForNAryArithmeticTests() =
            // Same numeric operand types, single binary operator
            ALL_NUMERIC_TYPES.flatMap { numericType ->
                singleArithmeticOpCases(
                    name = "$numericType",
                    leftType = numericType,
                    rightType = numericType,
                    expectedType = numericType
                )
            } +
                // Same numeric operand types, double binary operators
                ALL_NUMERIC_TYPES.flatMap { numericType ->
                    doubleArithmeticOpCases(
                        name = "$numericType",
                        leftType = numericType,
                        middleType = numericType,
                        rightType = numericType,
                        expectedType = numericType
                    )
                } +
                listOf(
                    // mixed operand types, single binary operators
                    singleArithmeticOpCases(
                        name = "int2 and int4 operands",
                        leftType = StaticType.INT2,
                        rightType = StaticType.INT4,
                        expectedType = StaticType.INT4
                    ),
                    singleArithmeticOpCases(
                        name = "int2 and int8 operands",
                        leftType = StaticType.INT2,
                        rightType = StaticType.INT8,
                        expectedType = StaticType.INT8
                    ),
                    singleArithmeticOpCases(
                        name = "int2 and int operands",
                        leftType = StaticType.INT2,
                        rightType = StaticType.INT,
                        expectedType = StaticType.INT
                    ),
                    singleArithmeticOpCases(
                        name = "int4 and int8 operands",
                        leftType = StaticType.INT4,
                        rightType = StaticType.INT8,
                        expectedType = StaticType.INT8
                    ),
                    singleArithmeticOpCases(
                        name = "int4 and int operands",
                        leftType = StaticType.INT4,
                        rightType = StaticType.INT,
                        expectedType = StaticType.INT
                    ),
                    singleArithmeticOpCases(
                        name = "int4 and any_of(int2, int4) operands",
                        leftType = StaticType.INT4,
                        rightType = StaticType.unionOf(StaticType.INT2, StaticType.INT4),
                        expectedType = StaticType.INT4
                    ),
                    singleArithmeticOpCases(
                        name = "int8 and any_of(int2, int4)",
                        leftType = StaticType.INT8,
                        rightType = StaticType.unionOf(StaticType.INT2, StaticType.INT4),
                        expectedType = StaticType.INT8
                    ),
                    singleArithmeticOpCases(
                        name = "int8 and any_of(int2, int4, float)",
                        leftType = StaticType.INT8,
                        rightType = StaticType.unionOf(StaticType.INT2, StaticType.INT4, StaticType.FLOAT),
                        expectedType = StaticType.unionOf(StaticType.INT8, StaticType.FLOAT)
                    ),
                    singleArithmeticOpCases(
                        name = "int8 and any_of(int8, int2, int4, float, decimal)",
                        leftType = StaticType.INT8,
                        rightType = StaticType.unionOf(
                            StaticType.INT2,
                            StaticType.INT4,
                            StaticType.FLOAT,
                            StaticType.DECIMAL
                        ),
                        expectedType = StaticType.unionOf(StaticType.INT8, StaticType.FLOAT, StaticType.DECIMAL)
                    ),
                    singleArithmeticOpCases(
                        name = "any_of(int8, decimal) and any_of(int2, int4, float)",
                        leftType = StaticType.unionOf(StaticType.INT8, StaticType.DECIMAL),
                        rightType = StaticType.unionOf(StaticType.INT2, StaticType.INT4, StaticType.FLOAT),
                        expectedType = StaticType.unionOf(StaticType.INT8, StaticType.FLOAT, StaticType.DECIMAL)
                    ),
                    doubleArithmeticOpCases(
                        name = "int2, int4 and int8",
                        leftType = StaticType.INT2,
                        middleType = StaticType.INT4,
                        rightType = StaticType.INT8,
                        expectedType = StaticType.INT8
                    ),

                    // mixed operand types, double binary operators
                    doubleArithmeticOpCases(
                        name = "int8, int4 and int2",
                        leftType = StaticType.INT8,
                        middleType = StaticType.INT4,
                        rightType = StaticType.INT2,
                        expectedType = StaticType.INT8
                    ),
                    doubleArithmeticOpCases(
                        name = "any_of(int8, decimal) and int4",
                        leftType = StaticType.unionOf(StaticType.INT8, StaticType.DECIMAL),
                        middleType = StaticType.INT4,
                        rightType = StaticType.INT2,
                        expectedType = StaticType.unionOf(StaticType.INT8, StaticType.DECIMAL)
                    ),
                    doubleArithmeticOpCases(
                        name = "any_of(int8, decimal), any_of(int4, float, missing) and any_of(int2, decimal)",
                        leftType = StaticType.unionOf(StaticType.INT8, StaticType.DECIMAL),
                        middleType = StaticType.unionOf(StaticType.INT4, StaticType.FLOAT, StaticType.MISSING),
                        rightType = StaticType.unionOf(StaticType.INT2, StaticType.DECIMAL),
                        expectedType = StaticType.unionOf(
                            StaticType.MISSING,
                            StaticType.INT8,
                            StaticType.FLOAT,
                            StaticType.DECIMAL
                        )
                    ),

                    // NULL propagation, single binary operators
                    singleArithmeticOpCases(
                        name = "one nullable operand",
                        leftType = StaticType.INT4.asNullable(),
                        rightType = StaticType.INT4,
                        expectedType = StaticType.INT4.asNullable()
                    ),
                    singleArithmeticOpCases(
                        name = "two nullable operands",
                        leftType = StaticType.INT4.asNullable(),
                        rightType = StaticType.INT4.asNullable(),
                        expectedType = StaticType.INT4.asNullable()
                    ),
                    singleArithmeticOpCases(
                        name = "int4, union(int4, float)",
                        leftType = StaticType.INT4,
                        rightType = AnyOfType(setOf(StaticType.INT4, StaticType.FLOAT)),
                        expectedType = AnyOfType(setOf(StaticType.INT4, StaticType.FLOAT))
                    ),
                    singleArithmeticOpCases(
                        name = "int4, union(int4, float)",
                        leftType = StaticType.DECIMAL,
                        rightType = AnyOfType(setOf(StaticType.INT4, StaticType.FLOAT)),
                        expectedType = StaticType.DECIMAL
                    ),
                    singleArithmeticOpCases(
                        name = "any, int",
                        leftType = StaticType.ANY,
                        rightType = StaticType.INT,
                        expectedType = StaticType.unionOf(
                            StaticType.MISSING,
                            StaticType.NULL,
                            StaticType.INT,
                            StaticType.FLOAT,
                            StaticType.DECIMAL
                        )
                    ),
                    singleArithmeticOpCases(
                        name = "any, float",
                        leftType = StaticType.ANY,
                        rightType = StaticType.FLOAT,
                        expectedType = StaticType.unionOf(
                            StaticType.MISSING,
                            StaticType.NULL,
                            StaticType.FLOAT,
                            StaticType.DECIMAL
                        )
                    ),
                    singleArithmeticOpCases(
                        name = "any, decimal",
                        leftType = StaticType.ANY,
                        rightType = StaticType.DECIMAL,
                        expectedType = StaticType.unionOf(
                            StaticType.MISSING,
                            StaticType.NULL,
                            StaticType.DECIMAL
                        )
                    ),
                    singleArithmeticOpCases(
                        name = "any, any",
                        leftType = StaticType.ANY,
                        rightType = StaticType.ANY,
                        expectedType = StaticType.unionOf(
                            StaticType.MISSING,
                            StaticType.NULL,
                            StaticType.INT,
                            StaticType.INT2,
                            StaticType.INT4,
                            StaticType.INT8,
                            StaticType.FLOAT,
                            StaticType.DECIMAL
                        )
                    ),

                    // NULL propagation, single binary operator
                    singleArithmeticOpCases(
                        name = "int4, union(null, float)",
                        leftType = StaticType.INT4,
                        rightType = StaticType.FLOAT.asNullable(),
                        expectedType = StaticType.FLOAT.asNullable()
                    ),

                    // NULL propagation, double binary operators
                    doubleArithmeticOpCases(
                        name = "one nullable operand, 1 of 3",
                        leftType = StaticType.INT4.asNullable(),
                        middleType = StaticType.INT4,
                        rightType = StaticType.INT4,
                        expectedType = StaticType.INT4.asNullable()
                    ),
                    doubleArithmeticOpCases(
                        name = "one nullable operand, 2 of 3",
                        leftType = StaticType.INT4,
                        middleType = StaticType.INT4.asNullable(),
                        rightType = StaticType.INT4,
                        expectedType = StaticType.INT4.asNullable()
                    ),
                    doubleArithmeticOpCases(
                        name = "one nullable operand, 3 of 3",
                        leftType = StaticType.INT4,
                        middleType = StaticType.INT4,
                        rightType = StaticType.INT4.asNullable(),
                        expectedType = StaticType.INT4.asNullable()
                    ),
                    doubleArithmeticOpCases(
                        name = "three nullable operands",
                        leftType = StaticType.INT4.asNullable(),
                        middleType = StaticType.INT4.asNullable(),
                        rightType = StaticType.INT4.asNullable(),
                        expectedType = StaticType.INT4.asNullable()
                    ),

                    // MISSING propagation, single binary operators
                    singleArithmeticOpCases(
                        name = "one optional operand, 1 of 2",
                        leftType = StaticType.INT4.asOptional(),
                        rightType = StaticType.INT4,
                        expectedType = StaticType.INT4.asOptional()
                    ),
                    singleArithmeticOpCases(
                        name = "one optional operand, 2 of 2",
                        leftType = StaticType.INT4,
                        rightType = StaticType.INT4.asOptional(),
                        expectedType = StaticType.INT4.asOptional()
                    ),
                    singleArithmeticOpCases(
                        name = "two optional operands",
                        leftType = StaticType.INT4.asOptional(),
                        rightType = StaticType.INT4.asOptional(),
                        expectedType = StaticType.INT4.asOptional()
                    ),

                    // NULL propagation, double binary operators
                    doubleArithmeticOpCases(
                        name = "one optional operand, 1 of 3",
                        leftType = StaticType.INT4.asOptional(),
                        middleType = StaticType.INT4,
                        rightType = StaticType.INT4,
                        expectedType = StaticType.INT4.asOptional()
                    ),
                    doubleArithmeticOpCases(
                        name = "one optional operand, 2 of 3",
                        leftType = StaticType.INT4,
                        middleType = StaticType.INT4.asOptional(),
                        rightType = StaticType.INT4,
                        expectedType = StaticType.INT4.asOptional()
                    ),
                    doubleArithmeticOpCases(
                        name = "one optional operand, 3 of 3",
                        leftType = StaticType.INT4,
                        middleType = StaticType.INT4,
                        rightType = StaticType.INT4.asOptional(),
                        expectedType = StaticType.INT4.asOptional()
                    ),
                    doubleArithmeticOpCases(
                        name = "three optional operands",
                        leftType = StaticType.INT4.asOptional(),
                        middleType = StaticType.INT4.asOptional(),
                        rightType = StaticType.INT4.asOptional(),
                        expectedType = StaticType.INT4.asOptional()
                    ),
                    doubleArithmeticOpCases(
                        name = "int4, float, int4",
                        leftType = StaticType.INT4,
                        middleType = StaticType.FLOAT,
                        rightType = StaticType.INT4,
                        expectedType = StaticType.FLOAT
                    ),
                    doubleArithmeticOpCases(
                        name = "float, decimal, int4",
                        leftType = StaticType.FLOAT,
                        middleType = StaticType.DECIMAL,
                        rightType = StaticType.INT4,
                        expectedType = StaticType.DECIMAL
                    ),
                    doubleArithmeticOpCases(
                        name = "nullable and optional",
                        leftType = StaticType.INT4,
                        middleType = StaticType.INT4.asNullable(),
                        rightType = StaticType.INT4.asOptional(),
                        expectedType = StaticType.INT4.asOptional().asNullable()
                    ),

                    //
                    // data type mismatch cases for arithmetic ops below
                    //
                    InferencerTestUtil.OpType.ARITHMETIC.operators.flatMap { op ->
                        // non-numeric, non-unknown with non-unknown -> data type mismatch error
                        generateAllUniquePairs(
                            ALL_NON_NUMERIC_NON_UNKNOWN_TYPES,
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
                            // non-numeric, non-unknown with an unknown -> data type mismatch and null or missing error
                            generateAllUniquePairs(
                                ALL_NON_NUMERIC_NON_UNKNOWN_TYPES,
                                ALL_UNKNOWN_TYPES
                            ).map {
                                if (it.second is MissingType || it.first is MissingType) {
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
                            // numeric with an unknown -> null or missing error
                            generateAllUniquePairs(
                                ALL_NUMERIC_TYPES,
                                ALL_UNKNOWN_TYPES
                            ).map {
                                if (it.second is MissingType || it.first is MissingType) {
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
                                } else if (it.first.withMetas(emptyMap()) == StaticType.NULL_OR_MISSING || it.second.withMetas(emptyMap()) == StaticType.NULL_OR_MISSING) {
                                    singleNAryTestCase(
                                        name = "with null or missing warning - ${it.first}, ${it.second}",
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
                                        name = "with null or missing warning - ${it.first}, ${it.second}",
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
                            } +
                            // unknown with an unknown -> null or missing error
                            generateAllUniquePairs(
                                ALL_UNKNOWN_TYPES,
                                ALL_UNKNOWN_TYPES
                            ).map {
                                if (it.second is MissingType || it.first is MissingType) {
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
                                } else if (it.first.withMetas(emptyMap()) == StaticType.NULL_OR_MISSING || it.second.withMetas(emptyMap()) == StaticType.NULL_OR_MISSING) {
                                    singleNAryTestCase(
                                        name = "with null or missing warning - ${it.first}, ${it.second}",
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
                                        name = "with null or missing warning - ${it.first}, ${it.second}",
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
                            } +
                            listOf(
                                // double arithmetic ops with unknowns -> null or missing errors
                                doubleOpTestCases(
                                    name = "null, null, null",
                                    op = op,
                                    leftType = StaticType.NULL,
                                    middleType = StaticType.NULL,
                                    rightType = StaticType.NULL,
                                    expectedType = StaticType.NULL,
                                    expectedWarnings = listOf(
                                        createReturnsNullOrMissingWarning(
                                            col = 3,
                                            nAryOp = op
                                        ),
                                        createReturnsNullOrMissingWarning(
                                            col = 7,
                                            nAryOp = op
                                        )
                                    )
                                ),
                                doubleOpErrorCases(
                                    name = "null, null, missing",
                                    op = op,
                                    leftType = StaticType.NULL,
                                    middleType = StaticType.NULL,
                                    rightType = StaticType.MISSING,
                                    expectedErrors = listOf(
                                        createReturnsMissingError(
                                            col = 7,
                                            nAryOp = op
                                        )
                                    ),
                                    expectedWarnings = listOf(
                                        createReturnsNullOrMissingWarning(
                                            col = 3,
                                            nAryOp = op
                                        )
                                    )
                                ),
                                doubleOpErrorCases(
                                    name = "missing, null, null",
                                    op = op,
                                    leftType = StaticType.MISSING,
                                    middleType = StaticType.NULL,
                                    rightType = StaticType.NULL,
                                    expectedErrors = listOf(
                                        createReturnsMissingError(
                                            col = 3,
                                            nAryOp = op
                                        ),
                                    ),
                                    expectedWarnings = listOf(
                                        createReturnsNullOrMissingWarning(
                                            col = 7,
                                            nAryOp = op
                                        )
                                    )
                                ),
                                doubleOpErrorCases(
                                    name = "null, missing, null",
                                    op = op,
                                    leftType = StaticType.NULL,
                                    middleType = StaticType.MISSING,
                                    rightType = StaticType.NULL,
                                    expectedErrors = listOf(
                                        createReturnsMissingError(
                                            col = 3,
                                            nAryOp = op
                                        ),
                                    ),
                                    expectedWarnings = listOf(
                                        createReturnsNullOrMissingWarning(
                                            col = 7,
                                            nAryOp = op
                                        )
                                    )
                                ),
                                doubleOpErrorCases(
                                    name = "missing, missing, null",
                                    op = op,
                                    leftType = StaticType.MISSING,
                                    middleType = StaticType.MISSING,
                                    rightType = StaticType.NULL,
                                    expectedErrors = listOf(
                                        createReturnsMissingError(
                                            col = 3,
                                            nAryOp = op
                                        ),
                                    ),
                                    expectedWarnings = listOf(
                                        createReturnsNullOrMissingWarning(
                                            col = 7,
                                            nAryOp = op
                                        )
                                    )
                                ),
                                doubleOpErrorCases(
                                    name = "null, missing, missing",
                                    op = op,
                                    leftType = StaticType.NULL,
                                    middleType = StaticType.MISSING,
                                    rightType = StaticType.MISSING,
                                    expectedErrors = listOf(
                                        createReturnsMissingError(
                                            col = 3,
                                            nAryOp = op
                                        ),
                                        createReturnsMissingError(
                                            col = 7,
                                            nAryOp = op
                                        )
                                    )
                                ),
                                doubleOpErrorCases(
                                    name = "missing, null, missing",
                                    op = op,
                                    leftType = StaticType.MISSING,
                                    middleType = StaticType.NULL,
                                    rightType = StaticType.MISSING,
                                    expectedErrors = listOf(
                                        createReturnsMissingError(
                                            col = 3,
                                            nAryOp = op
                                        ),
                                        createReturnsMissingError(
                                            col = 7,
                                            nAryOp = op
                                        )
                                    )
                                ),
                                doubleOpErrorCases(
                                    name = "missing, missing, missing",
                                    op = op,
                                    leftType = StaticType.MISSING,
                                    middleType = StaticType.MISSING,
                                    rightType = StaticType.MISSING,
                                    expectedErrors = listOf(
                                        createReturnsMissingError(
                                            col = 3,
                                            nAryOp = op
                                        ),
                                        createReturnsMissingError(
                                            col = 7,
                                            nAryOp = op
                                        )
                                    )
                                )
                            ) +
                            // other test cases resulting in a data type mismatch
                            listOf(
                                Pair(
                                    StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL),
                                    StaticType.SYMBOL
                                ),
                                Pair(
                                    StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL),
                                    StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL)
                                ),
                                Pair(StaticType.STRING.asNullable(), StaticType.INT4),
                                Pair(StaticType.STRING.asOptional(), StaticType.INT4),
                                Pair(StaticType.STRING.asNullable().asOptional(), StaticType.INT4),
                                Pair(StaticType.ANY, StaticType.STRING)
                            ).flatMap {
                                singleNAryOpMismatchWithSwappedCases(
                                    name = "data type mismatch - ${it.first}, ${it.second}",
                                    op = op,
                                    leftType = it.first,
                                    rightType = it.second
                                )
                            }
                    }
                ).flatten() +
                listOf(
                    InferencerTestUtil.TestCase(
                        name = "multiple errors - arithmetic datatype mismatches",
                        originalSql = "(a + b) + (c - d)",
                        globals = mapOf(
                            "a" to StaticType.STRING,
                            "b" to StaticType.INT,
                            "c" to StaticType.SYMBOL,
                            "d" to StaticType.FLOAT
                        ),
                        handler = expectSemanticProblems(
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 4,
                                    argTypes = listOf(StaticType.STRING, StaticType.INT),
                                    nAryOp = "+"
                                ),
                                createDataTypeMismatchError(
                                    col = 14,
                                    argTypes = listOf(StaticType.SYMBOL, StaticType.FLOAT),
                                    nAryOp = "-"
                                )
                            )
                        )
                    ),
                    InferencerTestUtil.TestCase(
                        name = "inference error - multiple arithmetic ops always resulting in unknown",
                        originalSql = "(a + b) + (c - d)",
                        globals = mapOf(
                            "a" to StaticType.NULL,
                            "b" to StaticType.INT,
                            "c" to StaticType.MISSING,
                            "d" to StaticType.FLOAT
                        ),
                        handler = expectSemanticProblems(
                            expectedErrors = listOf(
                                createReturnsMissingError(col = 14, nAryOp = "-")
                            ),
                            expectedWarnings = listOf(
                                createReturnsNullOrMissingWarning(col = 4, nAryOp = "+"),
                                createReturnsNullOrMissingWarning(col = 9, nAryOp = "+"),
                            )
                        )
                    ),
                )
    }
}
