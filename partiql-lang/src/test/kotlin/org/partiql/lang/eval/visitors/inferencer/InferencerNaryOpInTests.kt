package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.errors.Problem
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_LOB_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_NON_COLLECTION_NON_UNKNOWN_TYPES
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
import org.partiql.types.BagType
import org.partiql.types.CollectionType
import org.partiql.types.ListType
import org.partiql.types.MissingType
import org.partiql.types.SexpType
import org.partiql.types.StaticType

class InferencerNaryOpInTests {
    @ParameterizedTest
    @MethodSource("parametersForNAryOpInTests")
    fun nAryOpInTests(tc: TestCase) = runTest(tc)

    companion object {
        /**
         * Creates a test for each [CollectionType] of the form: [leftType] IN collection([rightElementType]). Each test
         * expects [StaticType.BOOL] as the output query type.
         *
         * Also creates a test with the row-value constructor of the form: [leftType] IN ([rightElementType])
         */
        private fun createNAryOpInAllCollectionsTest(
            leftType: StaticType,
            rightElementType: StaticType
        ): List<TestCase> = listOf(
            TestCase(
                name = "NAry op IN - $leftType IN list($rightElementType)",
                originalSql = "lhs IN rhs",
                globals = mapOf(
                    "lhs" to leftType,
                    "rhs" to ListType(elementType = rightElementType)
                ),
                handler = expectQueryOutputType(StaticType.BOOL)
            ),
            TestCase(
                name = "NAry op IN - $leftType IN bag($rightElementType)",
                originalSql = "lhs IN rhs",
                globals = mapOf(
                    "lhs" to leftType,
                    "rhs" to BagType(elementType = rightElementType)
                ),
                handler = expectQueryOutputType(StaticType.BOOL)
            ),
            TestCase(
                name = "NAry op IN - $leftType IN sexp($rightElementType)",
                originalSql = "lhs IN rhs",
                globals = mapOf(
                    "lhs" to leftType,
                    "rhs" to SexpType(elementType = rightElementType)
                ),
                handler = expectQueryOutputType(StaticType.BOOL)
            ),
            // row-value constructor test
            TestCase(
                name = "NAry op IN - $leftType IN ($rightElementType)",
                originalSql = "lhs IN (rhs)",
                globals = mapOf(
                    "lhs" to leftType,
                    "rhs" to rightElementType
                ),
                handler = expectQueryOutputType(StaticType.BOOL)
            )
        )

        /**
         * Creates a test for each [CollectionType] expecting a data type mismatch error due to an incomparable
         * element type. The created queries will take one type from [leftTypes] and one type from [incomparableTypes]
         * and be of the form: leftType IN collection(incomparableType)
         */
        private fun createNAryOpInErrorIncomparableElementTests(
            leftTypes: List<StaticType>,
            incomparableTypes: List<StaticType>
        ): List<TestCase> =
            leftTypes.flatMap { leftType ->
                incomparableTypes.flatMap { incomparableType ->
                    listOf(
                        createNAryOpInErrorTest(
                            name = "$leftType IN list($incomparableType) - data type mismatch",
                            leftType = leftType,
                            rightType = ListType(elementType = incomparableType),
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 5,
                                    argTypes = listOf(leftType, ListType(incomparableType)),
                                    nAryOp = "IN"
                                ),
                            )
                        ),
                        createNAryOpInErrorTest(
                            name = "$leftType IN bag($incomparableType) - data type mismatch",
                            leftType = leftType,
                            rightType = BagType(elementType = incomparableType),
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 5,
                                    argTypes = listOf(leftType, BagType(incomparableType)),
                                    nAryOp = "IN"
                                ),
                            )
                        ),
                        createNAryOpInErrorTest(
                            name = "$leftType IN sexp($incomparableType) - data type mismatch",
                            leftType = leftType,
                            rightType = SexpType(elementType = incomparableType),
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 5,
                                    argTypes = listOf(leftType, SexpType(incomparableType)),
                                    nAryOp = "IN"
                                ),
                            )
                        )
                    )
                }
            }

        /**
         * Creates a test expecting [outputType] with the query: [leftType] IN [rightType]
         */
        private fun createNAryOpInTest(
            name: String,
            leftType: StaticType,
            rightType: StaticType,
            outputType: StaticType,
            expectedWarnings: List<Problem> = emptyList()
        ): TestCase =
            TestCase(
                name = "NAry op IN - ($name)",
                originalSql = "lhs IN rhs",
                globals = mapOf(
                    "lhs" to leftType,
                    "rhs" to rightType
                ),
                handler = expectQueryOutputType(outputType, expectedWarnings)
            )

        /**
         * Creates a test that expects [expectedErrors] when inferring the static type of the query:
         * [leftType] IN [rightType]
         */
        private fun createNAryOpInErrorTest(
            name: String,
            leftType: StaticType,
            rightType: StaticType,
            expectedErrors: List<Problem>,
            expectedWarnings: List<Problem> = emptyList()
        ): TestCase =
            TestCase(
                name = "NAry op IN - $name",
                originalSql = "lhs IN rhs",
                globals = mapOf(
                    "lhs" to leftType,
                    "rhs" to rightType
                ),
                handler = expectSemanticProblems(expectedErrors = expectedErrors, expectedWarnings = expectedWarnings)
            )

        @JvmStatic
        @Suppress("unused")
        fun parametersForNAryOpInTests() =
            generateAllUniquePairs(
                ALL_NUMERIC_TYPES,
                ALL_NUMERIC_TYPES
            ).flatMap {
                createNAryOpInAllCollectionsTest(
                    leftType = it.first,
                    rightElementType = it.second
                )
            } +
                generateAllUniquePairs(
                    ALL_TEXT_TYPES,
                    ALL_TEXT_TYPES
                ).flatMap {
                    createNAryOpInAllCollectionsTest(
                        leftType = it.first,
                        rightElementType = it.second
                    )
                } +
                generateAllUniquePairs(
                    ALL_LOB_TYPES,
                    ALL_LOB_TYPES
                ).flatMap {
                    createNAryOpInAllCollectionsTest(
                        leftType = it.first,
                        rightElementType = it.second
                    )
                } +
                ALL_TYPES_ONLY_COMPARABLE_TO_SELF.flatMap {
                    createNAryOpInAllCollectionsTest(
                        leftType = it,
                        rightElementType = it
                    )
                } +
                listOf(
                    createNAryOpInTest(
                        name = "STRING IN unionOf(STRING, INT) LIST",
                        leftType = StaticType.STRING,
                        rightType = ListType(elementType = StaticType.unionOf(StaticType.STRING, StaticType.INT)),
                        outputType = StaticType.BOOL
                    ),
                    createNAryOpInTest(
                        name = "STRING IN unionOf(STRING, NULL) LIST",
                        leftType = StaticType.STRING,
                        rightType = ListType(elementType = StaticType.unionOf(StaticType.STRING, StaticType.NULL)),
                        outputType = StaticType.unionOf(StaticType.BOOL, StaticType.NULL)
                    ),
                    createNAryOpInTest(
                        name = "STRING IN unionOf(STRING, MISSING) LIST",
                        leftType = StaticType.STRING,
                        rightType = ListType(
                            elementType = StaticType.unionOf(
                                StaticType.STRING,
                                StaticType.MISSING
                            )
                        ),
                        outputType = StaticType.unionOf(StaticType.BOOL, StaticType.MISSING)
                    ),
                    createNAryOpInTest(
                        name = "STRING IN unionOf(STRING, MISSING, NULL) LIST",
                        leftType = StaticType.STRING,
                        rightType = ListType(
                            elementType = StaticType.unionOf(
                                StaticType.STRING,
                                StaticType.MISSING,
                                StaticType.NULL
                            )
                        ),
                        outputType = StaticType.unionOf(StaticType.BOOL, StaticType.MISSING, StaticType.NULL)
                    ),
                    createNAryOpInTest(
                        name = "STRING IN ANY LIST",
                        leftType = StaticType.STRING,
                        rightType = StaticType.LIST,
                        outputType = StaticType.unionOf(StaticType.BOOL, StaticType.MISSING, StaticType.NULL)
                    ),
                    createNAryOpInTest(
                        name = "STRING IN ANY SEXP",
                        leftType = StaticType.STRING,
                        rightType = StaticType.SEXP,
                        outputType = StaticType.unionOf(StaticType.BOOL, StaticType.MISSING, StaticType.NULL)
                    ),
                    createNAryOpInTest(
                        name = "STRING IN ANY BAG",
                        leftType = StaticType.STRING,
                        rightType = StaticType.BAG,
                        outputType = StaticType.unionOf(StaticType.BOOL, StaticType.MISSING, StaticType.NULL)
                    ),
                    createNAryOpInTest(
                        name = "ANY IN ANY BAG",
                        leftType = StaticType.ANY,
                        rightType = StaticType.BAG,
                        outputType = StaticType.unionOf(StaticType.BOOL, StaticType.MISSING, StaticType.NULL)
                    ),
                    createNAryOpInTest(
                        name = "ANY IN ANY",
                        leftType = StaticType.ANY,
                        rightType = StaticType.ANY,
                        outputType = StaticType.unionOf(StaticType.BOOL, StaticType.MISSING, StaticType.NULL)
                    ),
                    createNAryOpInTest(
                        name = "ANY IN unionOf(ANY BAG, empty STRUCT)",
                        leftType = StaticType.ANY,
                        rightType = StaticType.unionOf(StaticType.BAG, StaticType.STRUCT),
                        outputType = StaticType.unionOf(StaticType.BOOL, StaticType.NULL, StaticType.MISSING)
                    ),
                    createNAryOpInTest(
                        name = "ANY IN unionOf(ANY BAG, ANY LIST)",
                        leftType = StaticType.ANY,
                        rightType = StaticType.unionOf(StaticType.BAG, StaticType.LIST),
                        outputType = StaticType.unionOf(StaticType.BOOL, StaticType.NULL, StaticType.MISSING)
                    ),
                    createNAryOpInTest(
                        name = "STRING LIST IN STRING LIST LIST",
                        leftType = ListType(elementType = StaticType.STRING),
                        rightType = ListType(elementType = ListType(elementType = StaticType.STRING)),
                        outputType = StaticType.BOOL
                    ),
                    createNAryOpInTest(
                        name = "STRING LIST IN unionOf(STRING LIST LIST, STRING BAG BAG)",
                        leftType = ListType(elementType = StaticType.STRING),
                        rightType = StaticType.unionOf(
                            ListType(elementType = ListType(elementType = StaticType.STRING)),
                            BagType(elementType = BagType(elementType = StaticType.STRING))
                        ),
                        outputType = StaticType.BOOL
                    ),
                    createNAryOpInTest(
                        name = "STRING LIST IN unionOf(STRING LIST LIST, MISSING)",
                        leftType = ListType(elementType = StaticType.STRING),
                        rightType = StaticType.unionOf(
                            ListType(elementType = ListType(elementType = StaticType.STRING)),
                            StaticType.MISSING
                        ),
                        outputType = StaticType.unionOf(StaticType.BOOL, StaticType.MISSING)
                    ),
                    createNAryOpInTest(
                        name = "STRING LIST IN unionOf(STRING LIST LIST, NULL)",
                        leftType = ListType(elementType = StaticType.STRING),
                        rightType = StaticType.unionOf(
                            ListType(elementType = ListType(elementType = StaticType.STRING)),
                            StaticType.NULL
                        ),
                        outputType = StaticType.unionOf(StaticType.BOOL, StaticType.NULL)
                    ),
                    createNAryOpInTest(
                        name = "STRING LIST IN unionOf(STRING LIST LIST, MISSING, NULL)",
                        leftType = ListType(elementType = StaticType.STRING),
                        rightType = StaticType.unionOf(
                            ListType(elementType = ListType(elementType = StaticType.STRING)),
                            StaticType.MISSING,
                            StaticType.NULL
                        ),
                        outputType = StaticType.unionOf(StaticType.BOOL, StaticType.MISSING, StaticType.NULL)
                    ),
                    // row-value constructor tests
                    TestCase(
                        name = "NAry op IN - <int> IN (<int>, <null>, <int>)",
                        originalSql = "intT IN (intT, nullT, intT)",
                        globals = mapOf(
                            "intT" to StaticType.INT,
                            "nullT" to StaticType.NULL
                        ),
                        handler = expectQueryOutputType(
                            StaticType.unionOf(
                                StaticType.BOOL,
                                StaticType.NULL
                            )
                        )
                    ),
                    TestCase(
                        name = "NAry op IN - <int> IN (<int>, <missing>, <int>)",
                        originalSql = "intT IN (intT, missingT, intT)",
                        globals = mapOf(
                            "intT" to StaticType.INT,
                            "missingT" to StaticType.MISSING
                        ),
                        handler = expectQueryOutputType(
                            StaticType.unionOf(
                                StaticType.BOOL,
                                StaticType.MISSING
                            )
                        )
                    ),
                    TestCase(
                        name = "NAry op IN - <int> IN (<int>, <missing>, <null>)",
                        originalSql = "intT IN (intT, missingT, nullT)",
                        globals = mapOf(
                            "intT" to StaticType.INT,
                            "missingT" to StaticType.MISSING,
                            "nullT" to StaticType.NULL
                        ),
                        handler = expectQueryOutputType(
                            StaticType.unionOf(
                                StaticType.BOOL,
                                StaticType.MISSING,
                                StaticType.NULL
                            )
                        )
                    ),
                    TestCase(
                        name = "NAry op IN - <int> IN (<int>, <nullOrMissing>, <int>)",
                        originalSql = "intT IN (intT, nullOrMissingT, intT)",
                        globals = mapOf(
                            "intT" to StaticType.INT,
                            "nullOrMissingT" to StaticType.NULL_OR_MISSING
                        ),
                        handler = expectQueryOutputType(
                            StaticType.unionOf(
                                StaticType.BOOL,
                                StaticType.MISSING,
                                StaticType.NULL
                            )
                        )
                    )
                ) +
                //
                // `IN` cases with an error
                //
                // non-unknown IN non-collection (non-unknown) -> data type mismatch
                ALL_NON_UNKNOWN_TYPES.flatMap { nonUnknown ->
                    ALL_NON_COLLECTION_NON_UNKNOWN_TYPES.map { nonCollection ->
                        createNAryOpInErrorTest(
                            name = "$nonUnknown IN $nonCollection - data type mismatch",
                            leftType = nonUnknown,
                            rightType = nonCollection,
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 5,
                                    argTypes = listOf(nonUnknown, nonCollection),
                                    nAryOp = "IN"
                                )
                            )
                        )
                    }
                } +
                // unknown IN non-collection (non-unknown) -> data type mismatch and always returns missing error
                ALL_UNKNOWN_TYPES.flatMap { unknown ->
                    if (unknown is MissingType) {
                        ALL_NON_COLLECTION_NON_UNKNOWN_TYPES.map { nonCollection ->
                            createNAryOpInErrorTest(
                                name = "$unknown IN $nonCollection - data type mismatch, always returns missing error",
                                leftType = unknown,
                                rightType = nonCollection,
                                expectedErrors = listOf(
                                    createDataTypeMismatchError(
                                        col = 5,
                                        argTypes = listOf(unknown, nonCollection),
                                        nAryOp = "IN"
                                    ),
                                    createReturnsMissingError(
                                        col = 5,
                                        nAryOp = "IN"
                                    )
                                )
                            )
                        }
                    } else {
                        ALL_NON_COLLECTION_NON_UNKNOWN_TYPES.map { nonCollection ->
                            createNAryOpInErrorTest(
                                name = "$unknown IN $nonCollection - data type mismatch, null or missing error",
                                leftType = unknown,
                                rightType = nonCollection,
                                expectedErrors = listOf(
                                    createDataTypeMismatchError(
                                        col = 5,
                                        argTypes = listOf(unknown, nonCollection),
                                        nAryOp = "IN"
                                    ),
                                ),
                                expectedWarnings = listOf(
                                    createReturnsNullOrMissingWarning(
                                        col = 5,
                                        nAryOp = "IN"
                                    )
                                )
                            )
                        }
                    }
                } +
                // numeric IN collection(non-numeric) -> data type mismatch
                createNAryOpInErrorIncomparableElementTests(
                    ALL_NUMERIC_TYPES,
                    ALL_NON_NUMERIC_NON_UNKNOWN_TYPES
                ) +
                // text IN collection(non-text) -> data type mismatch
                createNAryOpInErrorIncomparableElementTests(
                    ALL_TEXT_TYPES,
                    ALL_NON_TEXT_NON_UNKNOWN_TYPES
                ) +
                // lob IN collection(non-lob) -> data type mismatch
                createNAryOpInErrorIncomparableElementTests(
                    ALL_LOB_TYPES,
                    ALL_NON_LOB_NON_UNKNOWN_TYPES
                ) +
                // type-only-comparable-to-self IN collection(other type) -> data type mismatch
                ALL_TYPES_ONLY_COMPARABLE_TO_SELF.flatMap { type ->
                    createNAryOpInErrorIncomparableElementTests(
                        listOf(type),
                        ALL_NON_UNKNOWN_TYPES.filter { it != type }
                    )
                } +
                // unknown IN collection(type) -> unknown operand error
                ALL_UNKNOWN_TYPES.flatMap { unknownType ->
                    StaticType.ALL_TYPES.flatMap { type ->
                        if (unknownType is MissingType) {
                            listOf(
                                createNAryOpInErrorTest(
                                    name = "$unknownType IN list($type) - unknown error",
                                    leftType = unknownType,
                                    rightType = ListType(elementType = type),
                                    expectedErrors = listOf(
                                        createReturnsMissingError(
                                            col = 5,
                                            nAryOp = "IN"
                                        )
                                    )
                                ),
                                createNAryOpInErrorTest(
                                    name = "$unknownType IN bag($type) - unknown error",
                                    leftType = unknownType,
                                    rightType = BagType(elementType = type),
                                    expectedErrors = listOf(
                                        createReturnsMissingError(
                                            col = 5,
                                            nAryOp = "IN"
                                        )
                                    )
                                ),
                                createNAryOpInErrorTest(
                                    name = "$unknownType IN sexp($type) - unknown error",
                                    leftType = unknownType,
                                    rightType = SexpType(elementType = type),
                                    expectedErrors = listOf(
                                        createReturnsMissingError(
                                            col = 5,
                                            nAryOp = "IN"
                                        )
                                    )
                                )
                            )
                        } else {
                            listOf(
                                createNAryOpInTest(
                                    name = "$unknownType IN list($type) - unknown error",
                                    leftType = unknownType,
                                    rightType = ListType(elementType = type),
                                    outputType = unknownType,
                                    expectedWarnings = listOf(
                                        createReturnsNullOrMissingWarning(
                                            col = 5,
                                            nAryOp = "IN"
                                        )
                                    )
                                ),
                                createNAryOpInTest(
                                    name = "$unknownType IN bag($type) - unknown error",
                                    leftType = unknownType,
                                    rightType = BagType(elementType = type),
                                    outputType = unknownType,
                                    expectedWarnings = listOf(
                                        createReturnsNullOrMissingWarning(
                                            col = 5,
                                            nAryOp = "IN"
                                        )
                                    )
                                ),
                                createNAryOpInTest(
                                    name = "$unknownType IN sexp($type) - unknown error",
                                    leftType = unknownType,
                                    rightType = SexpType(elementType = type),
                                    outputType = unknownType,
                                    expectedWarnings = listOf(
                                        createReturnsNullOrMissingWarning(
                                            col = 5,
                                            nAryOp = "IN"
                                        )
                                    )
                                )
                            )
                        }
                    }
                } +
                // type IN unknown -> unknown operand error
                StaticType.ALL_TYPES.flatMap { type ->
                    ALL_UNKNOWN_TYPES.map { unknownType ->
                        if (unknownType is MissingType) {
                            createNAryOpInErrorTest(
                                name = "$type IN $unknownType - unknown error",
                                leftType = type,
                                rightType = unknownType,
                                expectedErrors = listOf(
                                    createReturnsMissingError(
                                        col = 5,
                                        nAryOp = "IN"
                                    )
                                )
                            )
                        } else {
                            if (type is MissingType) {
                                createNAryOpInErrorTest(
                                    name = "$type IN $unknownType - unknown error",
                                    leftType = type,
                                    rightType = unknownType,
                                    expectedErrors = listOf(
                                        createReturnsMissingError(
                                            col = 5,
                                            nAryOp = "IN"
                                        )
                                    )
                                )
                            } else {
                                createNAryOpInTest(
                                    name = "$type IN $unknownType - unknown error",
                                    leftType = type,
                                    rightType = unknownType,
                                    outputType = unknownType,
                                    expectedWarnings = listOf(
                                        createReturnsNullOrMissingWarning(
                                            col = 5,
                                            nAryOp = "IN"
                                        )
                                    )
                                )
                            }
                        }
                    }
                } +
                // other tests resulting in an error
                listOf(
                    createNAryOpInErrorTest(
                        name = "ANY IN INT - data type mismatch",
                        leftType = StaticType.ANY,
                        rightType = StaticType.INT,
                        expectedErrors = listOf(
                            createDataTypeMismatchError(
                                col = 5,
                                argTypes = listOf(StaticType.ANY, StaticType.INT),
                                nAryOp = "IN"
                            )
                        )
                    ),
                    createNAryOpInErrorTest(
                        name = "ANY IN unionOf(INT, empty struct) - data type mismatch",
                        leftType = StaticType.ANY,
                        rightType = StaticType.unionOf(StaticType.INT, StaticType.STRUCT),
                        expectedErrors = listOf(
                            createDataTypeMismatchError(
                                col = 5,
                                argTypes = listOf(
                                    StaticType.ANY,
                                    StaticType.unionOf(StaticType.INT, StaticType.STRUCT)
                                ),
                                nAryOp = "IN"
                            )
                        )
                    ),
                    createNAryOpInTest(
                        name = "ANY IN NULL - unknown error",
                        leftType = StaticType.ANY,
                        rightType = StaticType.NULL,
                        outputType = StaticType.NULL_OR_MISSING,
                        expectedWarnings = listOf(
                            createReturnsNullOrMissingWarning(
                                col = 5,
                                nAryOp = "IN"
                            )
                        )
                    )
                )
    }
}
