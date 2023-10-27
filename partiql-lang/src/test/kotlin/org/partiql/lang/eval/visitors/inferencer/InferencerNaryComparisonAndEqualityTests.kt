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
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createSingleNAryOpCasesWithSwappedArgs
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectQueryOutputType
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectSemanticProblems
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.generateAllUniquePairs
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.runTest
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.singleNAryOpMismatchWithSwappedCases
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.singleNAryTestCase
import org.partiql.types.BagType
import org.partiql.types.ListType
import org.partiql.types.MissingType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StructType

class InferencerNaryComparisonAndEqualityTests {
    @ParameterizedTest
    @MethodSource("parametersForNAryComparisonAndEqualityTests")
    fun naryComparisonAndEqualityInferenceTests(tc: TestCase) = runTest(tc)

    companion object {
        /**
         * Creates two test cases with the specified operand and expected types for every NAry comparison and equality
         * operators, about `(COMPARISON_OPERATORS.size + EQUALITY_OPERATORS.size) X 2` test cases in total.
         * If [leftType] != [rightType], then new [TestCase] "y {op} x" is created.
         *
         * The expected output type will be [expectedComparisonType] for every created comparison op test case. If
         * [expectedEqualityType] is not specified, the created equality op test case will default to use
         * [expectedComparisonType]. Otherwise, the created equality op test case will use [expectedEqualityType] as the
         * expected output type.
         */
        private fun singleNAryComparisonAndEqualityCases(
            name: String,
            leftType: StaticType,
            rightType: StaticType,
            expectedComparisonType: StaticType,
            expectedEqualityType: StaticType = expectedComparisonType,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            createSingleNAryOpCasesWithSwappedArgs(
                InferencerTestUtil.OpType.COMPARISON,
                name,
                leftType,
                rightType,
                expectQueryOutputType(expectedComparisonType, expectedWarnings)
            ) +
                createSingleNAryOpCasesWithSwappedArgs(
                    InferencerTestUtil.OpType.EQUALITY,
                    name,
                    leftType,
                    rightType,
                    expectQueryOutputType(
                        expectedEqualityType,
                        expectedWarnings
                    )
                )

        /**
         * Creates a [TestCase] with the query "x [op] y" with x bound to [leftType] and y to [rightType]. This
         * [TestCase] expects [expectedError] during inference.
         */
        private fun singleNAryOpErrorTestCase(
            name: String,
            op: String,
            leftType: StaticType,
            rightType: StaticType,
            expectedError: List<Problem>
        ) =
            TestCase(
                name = "x $op y : $name",
                originalSql = "x $op y",
                globals = mapOf(
                    "x" to leftType,
                    "y" to rightType
                ),
                handler = expectSemanticProblems(expectedErrors = expectedError)
            )

        @JvmStatic
        @Suppress("unused")
        fun parametersForNAryComparisonAndEqualityTests() =
            // number {comparison/equality op} number -> bool
            generateAllUniquePairs(
                ALL_NUMERIC_TYPES,
                ALL_NUMERIC_TYPES
            ).flatMap {
                singleNAryComparisonAndEqualityCases(
                    name = "${it.first}, ${it.second}",
                    leftType = it.first,
                    rightType = it.second,
                    expectedComparisonType = StaticType.BOOL
                )
            } +
                // text {comparison/equality op} text -> bool
                generateAllUniquePairs(
                    ALL_TEXT_TYPES,
                    ALL_TEXT_TYPES
                ).flatMap {
                    singleNAryComparisonAndEqualityCases(
                        name = "${it.first}, ${it.second}",
                        leftType = it.first,
                        rightType = it.second,
                        expectedComparisonType = StaticType.BOOL
                    )
                } +
                // lob {comparison/equality op} lob -> bool
                generateAllUniquePairs(
                    ALL_LOB_TYPES,
                    ALL_LOB_TYPES
                ).flatMap {
                    singleNAryComparisonAndEqualityCases(
                        name = "${it.first}, ${it.second}",
                        leftType = it.first,
                        rightType = it.second,
                        expectedComparisonType = StaticType.BOOL
                    )
                } +
                listOf(
                    singleNAryComparisonAndEqualityCases(
                        name = "bool, bool",
                        leftType = StaticType.BOOL,
                        rightType = StaticType.BOOL,
                        expectedComparisonType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "timestamp, timestamp",
                        leftType = StaticType.TIMESTAMP,
                        rightType = StaticType.TIMESTAMP,
                        expectedComparisonType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "list, list",
                        leftType = StaticType.LIST,
                        rightType = StaticType.LIST,
                        expectedComparisonType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "sexp, sexp",
                        leftType = StaticType.SEXP,
                        rightType = StaticType.SEXP,
                        expectedComparisonType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "bag, bag",
                        leftType = StaticType.BAG,
                        rightType = StaticType.BAG,
                        expectedComparisonType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "struct, struct",
                        leftType = StaticType.STRUCT,
                        rightType = StaticType.STRUCT,
                        expectedComparisonType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "int4, union(string, float); equality gives bool",
                        leftType = StaticType.INT4,
                        rightType = StaticType.unionOf(StaticType.STRING, StaticType.FLOAT),
                        expectedComparisonType = StaticType.unionOf(StaticType.MISSING, StaticType.BOOL),
                        expectedEqualityType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "int4, union(null, float)",
                        leftType = StaticType.INT4,
                        rightType = StaticType.unionOf(StaticType.NULL, StaticType.FLOAT),
                        expectedComparisonType = StaticType.unionOf(StaticType.NULL, StaticType.BOOL)
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "int4, union(missing, float)",
                        leftType = StaticType.INT4,
                        rightType = StaticType.unionOf(StaticType.MISSING, StaticType.FLOAT),
                        expectedComparisonType = StaticType.unionOf(StaticType.MISSING, StaticType.BOOL)
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "int4, any",
                        leftType = StaticType.INT4,
                        rightType = StaticType.ANY,
                        expectedComparisonType = StaticType.unionOf(
                            StaticType.MISSING,
                            StaticType.NULL,
                            StaticType.BOOL
                        )
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "union(int4, float), union(int4, string); equality gives bool",
                        leftType = StaticType.unionOf(StaticType.INT4, StaticType.FLOAT),
                        rightType = StaticType.unionOf(StaticType.INT4, StaticType.STRING),
                        expectedComparisonType = StaticType.unionOf(StaticType.MISSING, StaticType.BOOL),
                        expectedEqualityType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "union(int4, decimal), union(int4, float)",
                        leftType = StaticType.unionOf(StaticType.INT4, StaticType.DECIMAL),
                        rightType = StaticType.unionOf(StaticType.INT4, StaticType.FLOAT),
                        expectedComparisonType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "union(int4, string), union(int4, string); equality gives bool",
                        leftType = StaticType.unionOf(StaticType.INT4, StaticType.STRING),
                        rightType = StaticType.unionOf(StaticType.INT4, StaticType.STRING),
                        expectedComparisonType = StaticType.unionOf(StaticType.MISSING, StaticType.BOOL),
                        expectedEqualityType = StaticType.BOOL
                    ),
                    // Collections with different, comparable element types
                    singleNAryComparisonAndEqualityCases(
                        name = "list(int), list(decimal)",
                        leftType = ListType(StaticType.INT),
                        rightType = ListType(StaticType.DECIMAL),
                        expectedComparisonType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "list(int), list(null)",
                        leftType = ListType(StaticType.INT),
                        rightType = ListType(StaticType.NULL),
                        expectedComparisonType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "list(int), list(missing)",
                        leftType = ListType(StaticType.INT),
                        rightType = ListType(StaticType.MISSING),
                        expectedComparisonType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "list(int), list(unionOf(int, decimal))",
                        leftType = ListType(StaticType.INT),
                        rightType = ListType(StaticType.unionOf(StaticType.INT, StaticType.DECIMAL)),
                        expectedComparisonType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "list(int), list(unionOf(int, timestamp))",
                        leftType = ListType(StaticType.INT),
                        rightType = ListType(StaticType.unionOf(StaticType.INT, StaticType.TIMESTAMP)),
                        expectedComparisonType = StaticType.BOOL
                    ),
                    // Collections with different, incomparable element types doesn't give any error/warning. Further
                    // container comparability checks deferred to later https://github.com/partiql/partiql-lang-kotlin/issues/505
                    singleNAryComparisonAndEqualityCases(
                        name = "list(int), list(timestamp)",
                        leftType = ListType(StaticType.INT),
                        rightType = ListType(StaticType.TIMESTAMP),
                        expectedComparisonType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "sexp(int), sexp(timestamp)",
                        leftType = SexpType(StaticType.INT),
                        rightType = SexpType(StaticType.TIMESTAMP),
                        expectedComparisonType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "bag(int), bag(timestamp)",
                        leftType = BagType(StaticType.INT),
                        rightType = BagType(StaticType.TIMESTAMP),
                        expectedComparisonType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "list(int), list(unionOf(timestamp, bool))",
                        leftType = ListType(StaticType.INT),
                        rightType = ListType(StaticType.unionOf(StaticType.TIMESTAMP, StaticType.BOOL)),
                        expectedComparisonType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "list(list(int)), list(list(timestamp)) - nested incompatible lists",
                        leftType = ListType(ListType(StaticType.INT)),
                        rightType = ListType(ListType(StaticType.TIMESTAMP)),
                        expectedComparisonType = StaticType.BOOL
                    ),
                    // structs with comparable fields
                    singleNAryComparisonAndEqualityCases(
                        name = "struct(a to int), struct(a to decimal)",
                        leftType = StructType(mapOf("a" to StaticType.INT)),
                        rightType = StructType(mapOf("a" to StaticType.DECIMAL)),
                        expectedComparisonType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "struct(a to int, b to string), struct(a to decimal, b to symbol) - multiple, comparable fields",
                        leftType = StructType(mapOf("a" to StaticType.INT, "b" to StaticType.STRING)),
                        rightType = StructType(mapOf("a" to StaticType.DECIMAL, "b" to StaticType.SYMBOL)),
                        expectedComparisonType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "struct(a to int), struct(a to missing)",
                        leftType = StructType(mapOf("a" to StaticType.INT)),
                        rightType = StructType(mapOf("a" to StaticType.MISSING)),
                        expectedComparisonType = StaticType.BOOL
                    ),
                    // structs with different numbers of fields. Further container comparability checks deferred to later
                    // https://github.com/partiql/partiql-lang-kotlin/issues/505
                    singleNAryComparisonAndEqualityCases(
                        name = "struct(a to int), struct()",
                        leftType = StructType(mapOf("a" to StaticType.INT)),
                        rightType = StaticType.STRUCT,
                        expectedComparisonType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "struct(a to int), struct(a to decimal, b to float)",
                        leftType = StructType(mapOf("a" to StaticType.INT)),
                        rightType = StructType(mapOf("a" to StaticType.DECIMAL, "b" to StaticType.FLOAT)),
                        expectedComparisonType = StaticType.BOOL
                    ),
                    // structs with incomparable fields. Further container comparability checks deferred to later
                    // https://github.com/partiql/partiql-lang-kotlin/issues/505
                    singleNAryComparisonAndEqualityCases(
                        name = "struct(a to int), struct(a to timestamp)",
                        leftType = StructType(mapOf("a" to StaticType.INT)),
                        rightType = StructType(mapOf("a" to StaticType.TIMESTAMP)),
                        expectedComparisonType = StaticType.BOOL
                    ),
                    singleNAryComparisonAndEqualityCases(
                        name = "struct(a to int, b to symbol), struct(a to timestamp, b to timestamp) - multiple incomparable",
                        leftType = StructType(mapOf("a" to StaticType.INT, "b" to StaticType.SYMBOL)),
                        rightType = StructType(mapOf("a" to StaticType.TIMESTAMP, "b" to StaticType.TIMESTAMP)),
                        expectedComparisonType = StaticType.BOOL
                    ),
                    // struct with different number of fields an incomparable field
                    singleNAryComparisonAndEqualityCases(
                        name = "struct(a to int), struct(a to timestamp, b to timestamp)",
                        leftType = StructType(mapOf("a" to StaticType.INT)),
                        rightType = StructType(mapOf("a" to StaticType.TIMESTAMP, "b" to StaticType.TIMESTAMP)),
                        expectedComparisonType = StaticType.BOOL
                    ),
                ).flatten() +
                (InferencerTestUtil.OpType.COMPARISON.operators + InferencerTestUtil.OpType.EQUALITY.operators).flatMap { op ->
                    // comparing numeric type with non-numeric, non-unknown type -> data type mismatch
                    ALL_NUMERIC_TYPES.flatMap { numericType ->
                        ALL_NON_NUMERIC_NON_UNKNOWN_TYPES.map { nonNumericType ->
                            singleNAryOpErrorTestCase(
                                name = "data type mismatch - $numericType, $nonNumericType",
                                op = op,
                                leftType = numericType,
                                rightType = nonNumericType,
                                expectedError = listOf(
                                    createDataTypeMismatchError(
                                        col = 3,
                                        argTypes = listOf(numericType, nonNumericType),
                                        nAryOp = op
                                    )
                                )
                            )
                        }
                    } +
                        // comparing text type with non-text, non-unknown type -> data type mismatch
                        ALL_TEXT_TYPES.flatMap { textType ->
                            ALL_NON_TEXT_NON_UNKNOWN_TYPES.map { nonTextType ->
                                singleNAryOpErrorTestCase(
                                    name = "data type mismatch - $textType, $nonTextType",
                                    op = op,
                                    leftType = textType,
                                    rightType = nonTextType,
                                    expectedError = listOf(
                                        createDataTypeMismatchError(
                                            col = 3,
                                            argTypes = listOf(textType, nonTextType),
                                            nAryOp = op
                                        )
                                    )
                                )
                            }
                        } +
                        // comparing lob type with non-lob, non-unknown type -> data type mismatch
                        ALL_LOB_TYPES.flatMap { lobType ->
                            ALL_NON_LOB_NON_UNKNOWN_TYPES.map { nonLobType ->
                                singleNAryOpErrorTestCase(
                                    name = "data type mismatch - $lobType, $nonLobType",
                                    op = op,
                                    leftType = lobType,
                                    rightType = nonLobType,
                                    expectedError = listOf(
                                        createDataTypeMismatchError(
                                            col = 3,
                                            argTypes = listOf(lobType, nonLobType),
                                            nAryOp = op
                                        )
                                    )
                                )
                            }
                        } +
                        // comparing non-categorized types with non-unknown other type -> data type mismatch
                        ALL_TYPES_ONLY_COMPARABLE_TO_SELF.flatMap { otherType ->
                            ALL_NON_UNKNOWN_TYPES.filter { it != otherType }.map { nonCompatibleType ->
                                singleNAryOpErrorTestCase(
                                    name = "data type mismatch - $otherType, $nonCompatibleType",
                                    op = op,
                                    leftType = otherType,
                                    rightType = nonCompatibleType,
                                    expectedError = listOf(
                                        createDataTypeMismatchError(
                                            col = 3,
                                            argTypes = listOf(otherType, nonCompatibleType),
                                            nAryOp = op
                                        )
                                    )
                                )
                            }
                        } +
                        // any type compared with an unknown -> always returns missing error or null or missing warning
                        generateAllUniquePairs(
                            StaticType.ALL_TYPES,
                            ALL_UNKNOWN_TYPES
                        ).map {
                            if (it.first is MissingType || it.second is MissingType) {
                                singleNAryOpErrorTestCase(
                                    name = "always missing error - ${it.first}, ${it.second}",
                                    op = op,
                                    leftType = it.first,
                                    rightType = it.second,
                                    expectedError = listOf(
                                        createReturnsMissingError(
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
                                    expectedType = if (it.first == StaticType.NULL_OR_MISSING || it.second == StaticType.NULL_OR_MISSING) StaticType.NULL_OR_MISSING else StaticType.NULL,
                                    expectedWarnings = listOf(
                                        createReturnsNullOrMissingWarning(
                                            col = 3,
                                            nAryOp = op
                                        )
                                    )
                                )
                            }
                        } +
                        // other unknown type problem tests
                        singleNAryOpErrorTestCase(
                            name = "missing, union(null, float)",
                            op = op,
                            leftType = StaticType.MISSING,
                            rightType = StaticType.unionOf(StaticType.NULL, StaticType.FLOAT),
                            expectedError = listOf(
                                createReturnsMissingError(
                                    col = 3,
                                    nAryOp = op
                                )
                            )
                        ) +
                        singleNAryTestCase(
                            name = "union(null, missing), any",
                            op = op,
                            leftType = StaticType.NULL_OR_MISSING,
                            rightType = StaticType.ANY,
                            expectedType = StaticType.NULL_OR_MISSING,
                            expectedWarnings = listOf(
                                createReturnsNullOrMissingWarning(
                                    col = 3,
                                    nAryOp = op
                                )
                            )
                        ) +
                        // other miscellaneous tests
                        singleNAryOpMismatchWithSwappedCases(
                            name = "int, union(timestamp, null)",
                            op = op,
                            leftType = StaticType.INT,
                            rightType = StaticType.unionOf(StaticType.TIMESTAMP, StaticType.NULL)
                        ) +
                        singleNAryOpMismatchWithSwappedCases(
                            name = "int, union(timestamp, missing)",
                            op = op,
                            leftType = StaticType.INT,
                            rightType = StaticType.unionOf(StaticType.TIMESTAMP, StaticType.MISSING)
                        ) +
                        singleNAryOpMismatchWithSwappedCases(
                            name = "union(int missing), union(timestamp, missing)",
                            op = op,
                            leftType = StaticType.unionOf(StaticType.INT, StaticType.MISSING),
                            rightType = StaticType.unionOf(StaticType.TIMESTAMP, StaticType.MISSING)
                        ) +
                        singleNAryOpMismatchWithSwappedCases(
                            name = "union(int, decimal, float), union(string, symbol)",
                            op = op,
                            leftType = StaticType.unionOf(StaticType.INT, StaticType.DECIMAL, StaticType.FLOAT),
                            rightType = StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL)
                        )
                }
    }
}
