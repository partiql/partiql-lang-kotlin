package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.errors.Problem
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_NON_NUMERIC_NON_UNKNOWN_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_NUMERIC_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_UNKNOWN_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createDataTypeMismatchError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createReturnsMissingError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createReturnsNullOrMissingWarning
import org.partiql.types.MissingType
import org.partiql.types.StaticType

class InferencerUnaryArithmeticOpTests {
    @ParameterizedTest
    @MethodSource("parametersForUnaryArithmeticOpTests")
    fun unaryArithmeticInferenceTests(tc: InferencerTestUtil.TestCase) = InferencerTestUtil.runTest(tc)

    companion object {
        /**
         * Creates a test case with the specified unary [op] of the form `[op] x` with [argType] corresponding to
         * `x`'s static type. Each test case is expected to have [expectedErrors] through inference.
         */
        private fun createUnaryOpErrorCase(
            name: String,
            op: String,
            argType: StaticType,
            expectedErrors: List<Problem>
        ) =
            InferencerTestUtil.TestCase(
                name = "$op x : $name",
                originalSql = "$op x",
                globals = mapOf("x" to argType),
                handler = InferencerTestUtil.expectSemanticProblems(expectedErrors = expectedErrors)
            )

        private fun createSingleUnaryOpTestCase(
            name: String,
            op: String,
            argType: StaticType,
            expectedType: StaticType,
            expectedWarnings: List<Problem>
        ) =
            InferencerTestUtil.TestCase(
                name = "$op x : $name",
                originalSql = "$op x",
                globals = mapOf("x" to argType),
                handler = InferencerTestUtil.expectQueryOutputType(expectedType = expectedType, expectedWarnings = expectedWarnings)
            )

        /**
         * Creates a test case for each unary arithmetic operand (+, -) of the form `{unary op} x` with [argType]
         * corresponding to `x`'s static type. Test cases are expected to result in [expectedOutputType] as the output
         * static type and have [expectedWarnings] from inference.
         */
        private fun createUnaryArithmeticOpCases(
            name: String,
            argType: StaticType,
            expectedOutputType: StaticType,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            listOf("+", "-").map { op ->
                val query = "$op x"
                InferencerTestUtil.TestCase(
                    name = "$query : $name",
                    originalSql = query,
                    globals = mapOf("x" to argType),
                    handler = InferencerTestUtil.expectQueryOutputType(
                        expectedOutputType,
                        expectedWarnings
                    )
                )
            }

        @JvmStatic
        @Suppress("UNUSED")
        private fun parametersForUnaryArithmeticOpTests() = listOf(
            // numeric operand type
            ALL_NUMERIC_TYPES.flatMap { numericType ->
                createUnaryArithmeticOpCases(
                    name = "$numericType",
                    argType = numericType,
                    expectedOutputType = numericType
                )
            } +
                createUnaryArithmeticOpCases(
                    name = "unary op - ANY",
                    argType = StaticType.ANY,
                    expectedOutputType = StaticType.unionOf(
                        StaticType.NULL,
                        StaticType.MISSING,
                        StaticType.FLOAT,
                        StaticType.INT2,
                        StaticType.INT4,
                        StaticType.INT8,
                        StaticType.INT,
                        StaticType.DECIMAL,
                        StaticType.FLOAT
                    )
                ),
            createUnaryArithmeticOpCases(
                name = "unary op - union(INT, STRING)",
                argType = StaticType.unionOf(StaticType.INT, StaticType.STRING),
                expectedOutputType = StaticType.unionOf(StaticType.INT, StaticType.MISSING)
            )
        ).flatten() +
            //
            // data type mismatch cases below this line
            //
            listOf("+", "-").flatMap { op ->
                // unknown -> expression always returns missing error or null or missing warning
                ALL_UNKNOWN_TYPES.map { unknownType ->
                    if (unknownType is MissingType) {
                        createUnaryOpErrorCase(
                            name = "unary op with always missing error - $unknownType",
                            op = op,
                            argType = unknownType,
                            expectedErrors = listOf(
                                createReturnsMissingError(col = 1, nAryOp = op)
                            )
                        )
                    } else {
                        createSingleUnaryOpTestCase(
                            name = "unary op with null or missing warning - $unknownType",
                            op = op,
                            argType = unknownType,
                            expectedType = unknownType,
                            expectedWarnings = listOf(
                                createReturnsNullOrMissingWarning(col = 1, nAryOp = op)
                            )
                        )
                    }
                } +
                    // incompatible types for unary arithmetic -> data type mismatch
                    ALL_NON_NUMERIC_NON_UNKNOWN_TYPES.map { nonNumericType ->
                        createUnaryOpErrorCase(
                            name = "unary op with data type mismatch - $nonNumericType",
                            op = op,
                            argType = nonNumericType,
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 1,
                                    argTypes = listOf(nonNumericType),
                                    nAryOp = op
                                )
                            )
                        )
                    } +
                    listOf(
                        // other unary arithmetic tests
                        createUnaryOpErrorCase(
                            name = "unary op with data type mismatch - union(STRING, SYMBOL)",
                            op = op,
                            argType = StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL),
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 1,
                                    argTypes = listOf(StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL)),
                                    nAryOp = op
                                )
                            )
                        ),
                        createUnaryOpErrorCase(
                            name = "unary op with data type mismatch - nullable string",
                            op = op,
                            argType = StaticType.STRING.asNullable(),
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 1,
                                    argTypes = listOf(StaticType.STRING.asNullable()),
                                    nAryOp = op
                                )
                            )
                        ),
                        createUnaryOpErrorCase(
                            name = "unary op with data type mismatch - optional string",
                            op = op,
                            argType = StaticType.STRING.asOptional(),
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 1,
                                    argTypes = listOf(StaticType.STRING.asOptional()),
                                    nAryOp = op
                                )
                            )
                        ),
                        createUnaryOpErrorCase(
                            name = "unary op with data type mismatch - nullable, optional string",
                            op = op,
                            argType = StaticType.STRING.asNullable().asOptional(),
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 1,
                                    argTypes = listOf(StaticType.STRING.asNullable().asOptional()),
                                    nAryOp = op
                                )
                            )
                        )
                    )
            }
    }
}
