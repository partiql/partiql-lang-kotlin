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

class InferencerNaryBetweenTests {
    @ParameterizedTest
    @MethodSource("parametersForNAryBetweenTests")
    fun naryBetweenInferenceTests(tc: TestCase) = runTest(tc)

    companion object {
        /**
         * Creates tests with compatible arguments for `BETWEEN`. Argument [comparableTypes] must have the [StaticType]s
         * be comparable to one another.
         *
         * More details of the tests provided in the included comments.
         */
        private fun createNAryBetweenComparableTypeTests(comparableTypes: List<StaticType>): List<TestCase> =
            generateAllUniquePairs(comparableTypes, comparableTypes)
                .flatMap { comparable ->
                    // test of the form: <compatibleValueType> BETWEEN <comparable1> AND <comparable2>
                    // results in bool as all types are comparable
                    comparableTypes.flatMap { comparableValueType ->
                        createNAryBetweenValidTest(
                            name = "x: $comparableValueType, y: ${comparable.first}, z: ${comparable.second}",
                            valueType = comparableValueType,
                            fromType = comparable.first,
                            toType = comparable.second,
                            outputType = StaticType.BOOL,
                            createSwapped = false
                        )
                    }
                }

        /**
         * Creates a test expecting [outputType] and [expectedWarnings] with the query:
         * [valueType] BETWEEN [fromType] AND [toType].
         *
         * If [createSwapped] is true and [fromType] != [toType], then another test will be included with
         * [fromType] and [toType] swapped in the created [TestCase] query.
         */
        private fun createNAryBetweenValidTest(
            name: String,
            valueType: StaticType,
            fromType: StaticType,
            toType: StaticType,
            outputType: StaticType,
            expectedWarnings: List<Problem> = emptyList(),
            createSwapped: Boolean = true
        ): List<TestCase> {
            val originalTest = TestCase(
                name = "x BETWEEN y AND z : $name",
                originalSql = "x BETWEEN y AND z",
                globals = mapOf(
                    "x" to valueType,
                    "y" to fromType,
                    "z" to toType
                ),
                handler = expectQueryOutputType(outputType, expectedWarnings)
            )
            return when (createSwapped && fromType != toType) {
                true ->
                    listOf(
                        originalTest,
                        TestCase(
                            name = "x BETWEEN z AND y : $name",
                            originalSql = "x BETWEEN z AND y",
                            globals = mapOf(
                                "x" to valueType,
                                "y" to fromType,
                                "z" to toType
                            ),
                            handler = expectQueryOutputType(
                                outputType,
                                expectedWarnings
                            )
                        )
                    )
                else -> listOf(originalTest)
            }
        }

        /**
         * Creates a test expecting a data type mismatch error with the query:
         * [valueType] BETWEEN [fromType] AND [toType].
         *
         * If [fromType] != [toType], then another test will be included in the output with [fromType] and [toType]
         * swapped in the created [TestCase] query.
         */
        private fun createNAryBetweenDataTypeMismatchTest(
            name: String,
            valueType: StaticType,
            fromType: StaticType,
            toType: StaticType
        ): List<TestCase> {
            val originalTest = TestCase(
                name = "x BETWEEN y AND z : $name",
                originalSql = "x BETWEEN y AND z",
                globals = mapOf(
                    "x" to valueType,
                    "y" to fromType,
                    "z" to toType
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createDataTypeMismatchError(
                            col = 3,
                            argTypes = listOf(valueType, fromType, toType),
                            nAryOp = "BETWEEN"
                        )
                    )
                )
            )
            return when (fromType != toType) {
                true -> listOf(
                    originalTest,
                    TestCase(
                        name = "x BETWEEN z AND y : $name",
                        originalSql = "x BETWEEN z AND y",
                        globals = mapOf(
                            "x" to valueType,
                            "y" to fromType,
                            "z" to toType
                        ),
                        handler = expectSemanticProblems(
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 3,
                                    argTypes = listOf(valueType, toType, fromType),
                                    nAryOp = "BETWEEN"
                                )
                            )
                        )
                    )
                )
                else -> listOf(originalTest)
            }
        }

        /**
         * Creates a test expecting [expectedErrors] with the query: [valueType] BETWEEN [fromType] AND [toType].
         */
        private fun createNAryBetweenProblemTest(
            name: String,
            valueType: StaticType,
            fromType: StaticType,
            toType: StaticType,
            expectedErrors: List<Problem> = emptyList(),
            expectedWarnings: List<Problem> = emptyList()
        ) =
            listOf(
                TestCase(
                    name = "x BETWEEN y AND z : $name",
                    originalSql = "x BETWEEN y AND z",
                    globals = mapOf(
                        "x" to valueType,
                        "y" to fromType,
                        "z" to toType
                    ),
                    handler = expectSemanticProblems(expectedWarnings, expectedErrors)
                )
            )

        /**
         * Creates multiple different tests using `BETWEEN` resulting in at least one error. Argument [comparableTypes]
         * are expected to be comparable with each other. [incomparableTypes] are expected to be incomparable with each
         * of the [comparableTypes] and not contain any unknown [StaticType]s.
         *
         * More details on the tests are provided in the included comments.
         */
        private fun createMultipleNAryBetweenProblemTests(
            comparableTypes: List<StaticType>,
            incomparableTypes: List<StaticType>
        ): List<TestCase> =
            // <comparable> BETWEEN <incomparable1> AND <incomparable2> -> data type mismatch
            // where <comparable> comes from [comparableTypes] and <incomparable1> <incomparable2> come from
            // `incomparableTypes`
            generateAllUniquePairs(incomparableTypes, incomparableTypes)
                .flatMap { incomparable ->
                    comparableTypes.flatMap { valueType ->
                        createNAryBetweenProblemTest(
                            name = "data type mismatch - x: $valueType, y: ${incomparable.first}, z: ${incomparable.second}",
                            valueType = valueType,
                            fromType = incomparable.first,
                            toType = incomparable.second,
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 3,
                                    argTypes = listOf(valueType, incomparable.first, incomparable.second),
                                    nAryOp = "BETWEEN"
                                )
                            )
                        )
                    }
                } +
                // tests with two comparable types
                generateAllUniquePairs(comparableTypes, comparableTypes)
                    .flatMap { comparable ->
                        // <comparable1> BETWEEN <incomparable> AND <comparable2> -> data type mismatch
                        // <comparable1>, <comparable2> come from [comparableTypes] and are comparable with each other.
                        // <incomparable> comes from [incomparableTypes] and is incomparable with <comparable1>.
                        incomparableTypes.flatMap { incomparable ->
                            createNAryBetweenProblemTest(
                                name = "data type mismatch - x: ${comparable.first}, y: $incomparable, z: ${comparable.second}",
                                valueType = comparable.first,
                                fromType = incomparable,
                                toType = comparable.second,
                                expectedErrors = listOf(
                                    createDataTypeMismatchError(
                                        col = 3,
                                        argTypes = listOf(comparable.first, incomparable, comparable.second),
                                        nAryOp = "BETWEEN"
                                    )
                                )
                            )
                        } +
                            ALL_UNKNOWN_TYPES.flatMap { unknownType ->
                                if (unknownType is MissingType) {
                                    // missing BETWEEN <comparable1> AND <comparable2> -> always missing error
                                    // <comparable1> and <comparable2> come from `comparableTypes` and are comparable with each
                                    // other
                                    createNAryBetweenProblemTest(
                                        name = "always missing error - x: $unknownType, y: ${comparable.first}, z: ${comparable.second}",
                                        valueType = unknownType,
                                        fromType = comparable.first,
                                        toType = comparable.second,
                                        expectedErrors = listOf(
                                            createReturnsMissingError(
                                                col = 3,
                                                nAryOp = "BETWEEN"
                                            )
                                        )
                                    ) +
                                        // <comparable1> BETWEEN <unknown> AND <comparable2> -> null or missing error
                                        // <comparable1> and <comparable2> come from `comparableTypes` and are comparable with each
                                        // other
                                        createNAryBetweenProblemTest(
                                            name = "always missing error - x: ${comparable.first}, y: $unknownType, z: ${comparable.second}",
                                            valueType = comparable.first,
                                            fromType = unknownType,
                                            toType = comparable.second,
                                            expectedErrors = listOf(
                                                createReturnsMissingError(
                                                    col = 3,
                                                    nAryOp = "BETWEEN"
                                                )
                                            )
                                        )
                                } else {
                                    // NULL / Union(NULL, MISSING) BETWEEN <comparable1> AND <comparable2> -> null or missing warning
                                    // <comparable1> and <comparable2> come from `comparableTypes` and are comparable with each
                                    // other
                                    createNAryBetweenProblemTest(
                                        name = "null or missing warning - x: $unknownType, y: ${comparable.first}, z: ${comparable.second}",
                                        valueType = unknownType,
                                        fromType = comparable.first,
                                        toType = comparable.second,
                                        expectedWarnings = listOf(
                                            createReturnsNullOrMissingWarning(
                                                col = 3,
                                                nAryOp = "BETWEEN"
                                            )
                                        )
                                    ) +
                                        // <comparable1> BETWEEN <unknown> AND <comparable2> -> null or missing error
                                        // <comparable1> and <comparable2> come from `comparableTypes` and are comparable with each
                                        // other
                                        createNAryBetweenProblemTest(
                                            name = "null or missing warning - x: ${comparable.first}, y: $unknownType, z: ${comparable.second}",
                                            valueType = comparable.first,
                                            fromType = unknownType,
                                            toType = comparable.second,
                                            expectedWarnings = listOf(
                                                createReturnsNullOrMissingWarning(
                                                    col = 3,
                                                    nAryOp = "BETWEEN"
                                                )
                                            )
                                        )
                                }
                            }
                    } +
                comparableTypes.flatMap { comparable ->
                    incomparableTypes.flatMap { incomparable ->
                        ALL_UNKNOWN_TYPES.flatMap { unknownType ->
                            if (unknownType is MissingType) {
                                // <comparable> BETWEEN <incomparable> AND MISSING -> data type mismatch and always missing
                                // error
                                // <comparable> comes from [comparableTypes] and <incomparable> comes from [incomparableTypes].
                                // Comparing <comparable> with <unknown> results in a null or missing error
                                createNAryBetweenProblemTest(
                                    name = "data type mismatch, always missing error - x: $comparable, y: $incomparable, z: $unknownType",
                                    valueType = comparable,
                                    fromType = incomparable,
                                    toType = unknownType,
                                    expectedErrors = listOf(
                                        createDataTypeMismatchError(
                                            col = 3,
                                            argTypes = listOf(comparable, incomparable, unknownType),
                                            nAryOp = "BETWEEN"
                                        ),
                                        createReturnsMissingError(
                                            col = 3,
                                            nAryOp = "BETWEEN"
                                        )
                                    ),
                                )
                            } else {
                                // <comparable> BETWEEN <incomparable> AND null/ unionOf(null, missing) -> data type mismatch and null or missing
                                // error
                                // <comparable> comes from [comparableTypes] and <incomparable> comes from [incomparableTypes].
                                // Comparing <comparable> with <unknown> results in a null or missing error
                                createNAryBetweenProblemTest(
                                    name = "data type mismatch, null or missing warning - x: $comparable, y: $incomparable, z: $unknownType",
                                    valueType = comparable,
                                    fromType = incomparable,
                                    toType = unknownType,
                                    expectedErrors = listOf(
                                        createDataTypeMismatchError(
                                            col = 3,
                                            argTypes = listOf(comparable, incomparable, unknownType),
                                            nAryOp = "BETWEEN"
                                        )
                                    ),
                                    expectedWarnings = listOf(
                                        createReturnsNullOrMissingWarning(
                                            col = 3,
                                            nAryOp = "BETWEEN"
                                        )
                                    )
                                )
                            }
                        }
                    }
                }

        @JvmStatic
        @Suppress("unused")
        fun parametersForNAryBetweenTests() =
            createNAryBetweenComparableTypeTests(
                ALL_NUMERIC_TYPES
            ) +
                createNAryBetweenComparableTypeTests(
                    ALL_TEXT_TYPES
                ) +
                createNAryBetweenComparableTypeTests(
                    ALL_LOB_TYPES
                ) +
                ALL_TYPES_ONLY_COMPARABLE_TO_SELF.flatMap { otherType ->
                    createNAryBetweenComparableTypeTests(listOf(otherType))
                } +
                createNAryBetweenValidTest(
                    name = "matching union types; x: union(int, string), y: int, z: decimal",
                    valueType = StaticType.unionOf(StaticType.INT, StaticType.STRING),
                    fromType = StaticType.INT,
                    toType = StaticType.DECIMAL,
                    outputType = StaticType.unionOf(StaticType.BOOL, StaticType.MISSING)
                ) +
                createNAryBetweenValidTest(
                    name = "matching union types containing null; x: union(int, string, null), y: int, z: decimal",
                    valueType = StaticType.unionOf(StaticType.INT, StaticType.STRING, StaticType.NULL),
                    fromType = StaticType.INT,
                    toType = StaticType.DECIMAL,
                    outputType = StaticType.unionOf(StaticType.BOOL, StaticType.MISSING, StaticType.NULL)
                ) +
                createNAryBetweenValidTest(
                    name = "x: ANY, y: INT, z: DECIMAL",
                    valueType = StaticType.ANY,
                    fromType = StaticType.INT,
                    toType = StaticType.DECIMAL,
                    outputType = StaticType.unionOf(StaticType.BOOL, StaticType.MISSING, StaticType.NULL)
                ) +
                //
                // data type mismatch cases for arithmetic ops below
                //

                // numeric with non-numerics
                createMultipleNAryBetweenProblemTests(
                    comparableTypes = ALL_NUMERIC_TYPES,
                    incomparableTypes = ALL_NON_NUMERIC_NON_UNKNOWN_TYPES
                ) +
                // text with non-text
                createMultipleNAryBetweenProblemTests(
                    comparableTypes = ALL_TEXT_TYPES,
                    incomparableTypes = ALL_NON_TEXT_NON_UNKNOWN_TYPES
                ) +
                // lob with non-lobs
                createMultipleNAryBetweenProblemTests(
                    comparableTypes = ALL_LOB_TYPES,
                    incomparableTypes = ALL_NON_LOB_NON_UNKNOWN_TYPES
                ) +
                // types only comparable to self with different types
                ALL_TYPES_ONLY_COMPARABLE_TO_SELF.flatMap { otherType ->
                    createMultipleNAryBetweenProblemTests(
                        comparableTypes = listOf(
                            otherType
                        ),
                        incomparableTypes = ALL_NON_UNKNOWN_TYPES.filter { it != otherType }
                    )
                } +
                // unknowns with non-unknown types
                generateAllUniquePairs(
                    ALL_UNKNOWN_TYPES,
                    ALL_UNKNOWN_TYPES
                ).flatMap { unknownTypes ->
                    ALL_NON_UNKNOWN_TYPES.flatMap { nonUnknownType ->
                        if (unknownTypes.first is MissingType || unknownTypes.second is MissingType) {
                            createNAryBetweenProblemTest(
                                name = "always missing error - x: $nonUnknownType, y: ${unknownTypes.first}, z: ${unknownTypes.second}",
                                valueType = nonUnknownType,
                                fromType = unknownTypes.first,
                                toType = unknownTypes.second,
                                expectedErrors = listOf(
                                    createReturnsMissingError(
                                        col = 3,
                                        nAryOp = "BETWEEN"
                                    )
                                )
                            ) +
                                createNAryBetweenProblemTest(
                                    name = "always missing error - x: ${unknownTypes.first}, y: $nonUnknownType, z: ${unknownTypes.second}",
                                    valueType = unknownTypes.first,
                                    fromType = nonUnknownType,
                                    toType = unknownTypes.second,
                                    expectedErrors = listOf(
                                        createReturnsMissingError(
                                            col = 3,
                                            nAryOp = "BETWEEN"
                                        )
                                    )
                                ) +
                                createNAryBetweenProblemTest(
                                    name = "always missing error - x: ${unknownTypes.first}, y: ${unknownTypes.second}, z: $nonUnknownType",
                                    valueType = unknownTypes.first,
                                    fromType = unknownTypes.second,
                                    toType = nonUnknownType,
                                    expectedErrors = listOf(
                                        createReturnsMissingError(
                                            col = 3,
                                            nAryOp = "BETWEEN"
                                        )
                                    )
                                )
                        } else {
                            this.createNAryBetweenProblemTest(
                                name = "null or missing warning - x: $nonUnknownType, y: ${unknownTypes.first}, z: ${unknownTypes.second}",
                                valueType = nonUnknownType,
                                fromType = unknownTypes.first,
                                toType = unknownTypes.second,
                                expectedWarnings = listOf(
                                    createReturnsNullOrMissingWarning(
                                        col = 3,
                                        nAryOp = "BETWEEN"
                                    )
                                )
                            ) +
                                createNAryBetweenProblemTest(
                                    name = "null or missing warning - x: ${unknownTypes.first}, y: $nonUnknownType, z: ${unknownTypes.second}",
                                    valueType = unknownTypes.first,
                                    fromType = nonUnknownType,
                                    toType = unknownTypes.second,
                                    expectedErrors = emptyList(),
                                    expectedWarnings = listOf(
                                        createReturnsNullOrMissingWarning(
                                            col = 3,
                                            nAryOp = "BETWEEN"
                                        )
                                    )
                                ) +
                                createNAryBetweenProblemTest(
                                    name = "null or missing warning - x: ${unknownTypes.first}, y: ${unknownTypes.second}, z: $nonUnknownType",
                                    valueType = unknownTypes.first,
                                    fromType = unknownTypes.second,
                                    toType = nonUnknownType,
                                    expectedErrors = emptyList(),
                                    expectedWarnings = listOf(
                                        createReturnsNullOrMissingWarning(
                                            col = 3,
                                            nAryOp = "BETWEEN"
                                        )
                                    )
                                )
                        }
                    }
                } +
                createNAryBetweenDataTypeMismatchTest(
                    name = "incomparable nullable valueType; x: nullable int, y: nullable string, z: nullable symbol",
                    valueType = StaticType.INT.asNullable(),
                    fromType = StaticType.STRING.asNullable(),
                    toType = StaticType.SYMBOL.asNullable()
                ) +
                createNAryBetweenDataTypeMismatchTest(
                    name = "incomparable nullable from/toType; x: nullable string, y: nullable int, z: nullable symbol",
                    valueType = StaticType.STRING.asNullable(),
                    fromType = StaticType.INT.asNullable(),
                    toType = StaticType.SYMBOL.asNullable()
                ) +
                createNAryBetweenDataTypeMismatchTest(
                    name = "incomparable optional valueType; x: optional int, y: optional string, z: optional symbol",
                    valueType = StaticType.INT.asOptional(),
                    fromType = StaticType.STRING.asOptional(),
                    toType = StaticType.SYMBOL.asOptional()
                ) +
                createNAryBetweenDataTypeMismatchTest(
                    name = "incomparable optional from/toType; x: optional string, y: optional int, z: optional symbol",
                    valueType = StaticType.STRING.asOptional(),
                    fromType = StaticType.INT.asOptional(),
                    toType = StaticType.SYMBOL.asOptional()
                ) +
                createNAryBetweenDataTypeMismatchTest(
                    name = "union comparable to one union, not to other union; x: union(int, decimal), y: union(int, null), z: union(string, symbol)",
                    valueType = StaticType.unionOf(StaticType.INT, StaticType.DECIMAL),
                    fromType = StaticType.unionOf(StaticType.INT, StaticType.NULL),
                    toType = StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL)
                ) +
                createNAryBetweenDataTypeMismatchTest(
                    name = "union incomparable to other unions; x: union(bool, string), y: union(int, null), z: union(string, symbol)",
                    valueType = StaticType.unionOf(StaticType.BOOL, StaticType.STRING),
                    fromType = StaticType.unionOf(StaticType.INT, StaticType.FLOAT),
                    toType = StaticType.unionOf(StaticType.INT, StaticType.DECIMAL)
                ) +
                // valueType is comparable to fromType and toType. but fromType is incomparable to toType
                createNAryBetweenDataTypeMismatchTest(
                    name = "fromType incomparable to toType; x: union(int, string), y: int, z: string",
                    valueType = StaticType.unionOf(StaticType.INT, StaticType.STRING),
                    fromType = StaticType.INT,
                    toType = StaticType.STRING
                )
    }
}
