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
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectQueryOutputType
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectSemanticProblems
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.generateAllUniquePairs
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.runTest
import org.partiql.types.MissingType
import org.partiql.types.StaticType

class InferencerNaryLikeTests {

    @ParameterizedTest
    @MethodSource("parametersForNAryLikeTests")
    fun naryLikeInferenceTests(tc: TestCase) = runTest(tc)

    companion object {
        private fun createNAryLikeTest(
            name: String,
            valueType: StaticType,
            patternType: StaticType,
            escapeType: StaticType?,
            handler: (InferencerTestUtil.ResolveTestResult) -> Unit
        ) =
            when (escapeType) {
                null -> TestCase(
                    name = name,
                    originalSql = "x LIKE y",
                    globals = mapOf(
                        "x" to valueType,
                        "y" to patternType
                    ),
                    handler = handler
                )
                else -> TestCase(
                    name = name,
                    originalSql = "x LIKE y ESCAPE z",
                    globals = mapOf(
                        "x" to valueType,
                        "y" to patternType,
                        "z" to escapeType
                    ),
                    handler = handler
                )
            }

        private fun createNAryLikeValidTest(
            name: String,
            valueType: StaticType,
            patternType: StaticType,
            escapeType: StaticType? = null,
            outputType: StaticType,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            createNAryLikeTest(
                name,
                valueType,
                patternType,
                escapeType,
                handler = expectQueryOutputType(
                    expectedType = outputType,
                    expectedWarnings = expectedWarnings
                )
            )

        private fun createNAryLikeErrorTest(
            name: String,
            valueType: StaticType,
            patternType: StaticType,
            escapeType: StaticType? = null,
            expectedErrors: List<Problem>,
            expectedWarnings: List<Problem> = emptyList()
        ) =
            createNAryLikeTest(
                name,
                valueType,
                patternType,
                escapeType,
                handler = expectSemanticProblems(expectedErrors = expectedErrors, expectedWarnings = expectedWarnings)
            )

        @JvmStatic
        @Suppress("unused")
        fun parametersForNAryLikeTests() = listOf(
            createNAryLikeValidTest(
                name = "NAry op LIKE - string LIKE string",
                valueType = StaticType.STRING,
                patternType = StaticType.STRING,
                outputType = StaticType.BOOL
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - string LIKE symbol",
                valueType = StaticType.STRING,
                patternType = StaticType.SYMBOL,
                outputType = StaticType.BOOL
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - any LIKE any",
                valueType = StaticType.ANY,
                patternType = StaticType.ANY,
                outputType = StaticType.unionOf(StaticType.BOOL, StaticType.MISSING, StaticType.NULL)
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - valid union LIKE string",
                valueType = StaticType.unionOf(StaticType.STRING, StaticType.INT),
                patternType = StaticType.STRING,
                outputType = StaticType.unionOf(StaticType.BOOL, StaticType.MISSING)
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - valid union with null LIKE symbol",
                valueType = StaticType.unionOf(StaticType.INT, StaticType.STRING, StaticType.NULL),
                patternType = StaticType.SYMBOL,
                outputType = StaticType.unionOf(StaticType.MISSING, StaticType.BOOL, StaticType.NULL)
            ),
            // If the optional escape character is provided, it can result in failure even if the type is text (string,
            // in this case)
            // This is because the escape character needs to be a single character (string with length 1),
            // Even if the escape character is of length 1, escape sequence can be incorrect.
            // Check EvaluatingCompiler.checkPattern method for more details.
            createNAryLikeValidTest(
                name = "NAry op LIKE - string LIKE string ESCAPE string",
                valueType = StaticType.STRING,
                patternType = StaticType.STRING,
                escapeType = StaticType.STRING,
                outputType = StaticType.unionOf(StaticType.MISSING, StaticType.BOOL)
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - symbol LIKE string ESCAPE symbol",
                valueType = StaticType.SYMBOL,
                patternType = StaticType.STRING,
                escapeType = StaticType.SYMBOL,
                outputType = StaticType.unionOf(StaticType.MISSING, StaticType.BOOL)
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - symbol LIKE symbol ESCAPE string",
                valueType = StaticType.SYMBOL,
                patternType = StaticType.SYMBOL,
                escapeType = StaticType.STRING,
                outputType = StaticType.unionOf(StaticType.MISSING, StaticType.BOOL)
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - symbol LIKE symbol ESCAPE symbol",
                valueType = StaticType.SYMBOL,
                patternType = StaticType.SYMBOL,
                escapeType = StaticType.SYMBOL,
                outputType = StaticType.unionOf(StaticType.MISSING, StaticType.BOOL)
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - escape type of union(int, string)",
                valueType = StaticType.STRING,
                patternType = StaticType.STRING,
                escapeType = StaticType.unionOf(StaticType.INT, StaticType.STRING),
                outputType = StaticType.unionOf(StaticType.BOOL, StaticType.MISSING)
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - value type of union(int, string)",
                valueType = StaticType.unionOf(StaticType.INT, StaticType.STRING),
                patternType = StaticType.STRING,
                escapeType = StaticType.STRING,
                outputType = StaticType.unionOf(StaticType.BOOL, StaticType.MISSING)
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - value type of union(int, string, null)",
                valueType = StaticType.unionOf(StaticType.INT, StaticType.STRING, StaticType.NULL),
                patternType = StaticType.STRING,
                escapeType = StaticType.STRING,
                outputType = StaticType.unionOf(StaticType.BOOL, StaticType.MISSING, StaticType.NULL)
            ),
            createNAryLikeValidTest(
                name = "NAry op LIKE - pattern type of union(int, string)",
                valueType = StaticType.STRING,
                patternType = StaticType.unionOf(StaticType.INT, StaticType.STRING, StaticType.NULL),
                escapeType = StaticType.STRING,
                outputType = StaticType.unionOf(StaticType.BOOL, StaticType.MISSING, StaticType.NULL)
            )
        ) +
            //
            // data type mismatch cases below this line
            //

            // 2 args (value and pattern args only) - non-text, non-unknown with non-unknown -> data type mismatch
            generateAllUniquePairs(
                ALL_NON_TEXT_NON_UNKNOWN_TYPES,
                ALL_NON_UNKNOWN_TYPES
            ).map {
                createNAryLikeErrorTest(
                    name = "data type mismatch - ${it.first}, ${it.second}",
                    valueType = it.first,
                    patternType = it.second,
                    expectedErrors = listOf(
                        createDataTypeMismatchError(
                            col = 3,
                            argTypes = listOf(it.first, it.second),
                            nAryOp = "LIKE"
                        )
                    )
                )
            } +
            // non-text, non-unknown with unknown -> data type mismatch and null or missing error
            generateAllUniquePairs(
                ALL_NON_TEXT_NON_UNKNOWN_TYPES,
                ALL_UNKNOWN_TYPES
            ).map {
                if (it.first is MissingType || it.second is MissingType) {
                    createNAryLikeErrorTest(
                        name = "data type mismatch, always missing error - ${it.first}, ${it.second}",
                        valueType = it.first,
                        patternType = it.second,
                        expectedErrors = listOf(
                            createDataTypeMismatchError(
                                col = 3,
                                argTypes = listOf(it.first, it.second),
                                nAryOp = "LIKE"
                            ),
                            createReturnsMissingError(
                                col = 3,
                                nAryOp = "LIKE"
                            )
                        )
                    )
                } else {
                    createNAryLikeErrorTest(
                        name = "data type mismatch, null or missing warning - ${it.first}, ${it.second}",
                        valueType = it.first,
                        patternType = it.second,
                        expectedErrors = listOf(
                            createDataTypeMismatchError(
                                col = 3,
                                argTypes = listOf(it.first, it.second),
                                nAryOp = "LIKE"
                            ),
                        ),
                        expectedWarnings = listOf(
                            createReturnsNullOrMissingWarning(
                                col = 3,
                                nAryOp = "LIKE"
                            )
                        )
                    )
                }
            } +
            // text with an unknown -> null or missing error
            generateAllUniquePairs(
                ALL_TEXT_TYPES,
                ALL_UNKNOWN_TYPES
            ).map {
                if (it.first is MissingType || it.second is MissingType) {
                    createNAryLikeErrorTest(
                        name = "always missing error - ${it.first}, ${it.second}",
                        valueType = it.first,
                        patternType = it.second,
                        expectedErrors = listOf(
                            createReturnsMissingError(
                                col = 3,
                                nAryOp = "LIKE"
                            )
                        )
                    )
                } else {
                    createNAryLikeValidTest(
                        name = "null or missing warning - ${it.first}, ${it.second}",
                        valueType = it.first,
                        patternType = it.second,
                        outputType = if (it.first == StaticType.NULL_OR_MISSING || it.second == StaticType.NULL_OR_MISSING) StaticType.NULL_OR_MISSING else StaticType.NULL,
                        expectedWarnings = listOf(
                            createReturnsNullOrMissingWarning(
                                col = 3,
                                nAryOp = "LIKE"
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
                    createNAryLikeErrorTest(
                        name = "always missing error - ${it.first}, ${it.second}",
                        valueType = it.first,
                        patternType = it.second,
                        expectedErrors = listOf(
                            createReturnsMissingError(
                                col = 3,
                                nAryOp = "LIKE"
                            )
                        )
                    )
                } else {
                    createNAryLikeValidTest(
                        name = "null or missing warning - ${it.first}, ${it.second}",
                        valueType = it.first,
                        patternType = it.second,
                        outputType = if (it.first == StaticType.NULL_OR_MISSING || it.second == StaticType.NULL_OR_MISSING) StaticType.NULL_OR_MISSING else StaticType.NULL,
                        expectedWarnings = listOf(
                            createReturnsNullOrMissingWarning(
                                col = 3,
                                nAryOp = "LIKE"
                            )
                        )
                    )
                }
            } +
            // 3 args - 1 invalid argument (non-text, non-unknown) -> data type mismatch
            generateAllUniquePairs(
                ALL_TEXT_TYPES,
                ALL_TEXT_TYPES
            ).flatMap { textTypes ->
                val (textType1, textType2) = textTypes
                ALL_NON_TEXT_NON_UNKNOWN_TYPES.flatMap { nonTextType ->
                    listOf(
                        createNAryLikeErrorTest(
                            name = "NAry op LIKE data type mismatch - $nonTextType LIKE $textType1 ESCAPE $textType2",
                            valueType = nonTextType,
                            patternType = textType1,
                            escapeType = textType2,
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 3,
                                    argTypes = listOf(nonTextType, textType1, textType2),
                                    nAryOp = "LIKE"
                                )
                            )
                        ),
                        createNAryLikeErrorTest(
                            name = "NAry op LIKE data type mismatch - $textType1 LIKE $nonTextType ESCAPE $textType2",
                            valueType = textType1,
                            patternType = nonTextType,
                            escapeType = textType2,
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 3,
                                    argTypes = listOf(textType1, nonTextType, textType2),
                                    nAryOp = "LIKE"
                                )
                            )
                        ),
                        createNAryLikeErrorTest(
                            name = "NAry op LIKE data type mismatch - $textType1 LIKE $textType2 ESCAPE $nonTextType",
                            valueType = textType1,
                            patternType = textType2,
                            escapeType = nonTextType,
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 3,
                                    argTypes = listOf(textType1, textType2, nonTextType),
                                    nAryOp = "LIKE"
                                )
                            )
                        )
                    )
                }
            } +
            listOf(
                createNAryLikeErrorTest(
                    name = "NAry op LIKE data type mismatch - union(string, int, null) LIKE bool",
                    valueType = StaticType.unionOf(StaticType.STRING, StaticType.INT, StaticType.NULL),
                    patternType = StaticType.BOOL,
                    expectedErrors = listOf(
                        createDataTypeMismatchError(
                            col = 3,
                            argTypes = listOf(
                                StaticType.unionOf(
                                    StaticType.STRING,
                                    StaticType.INT,
                                    StaticType.NULL
                                ),
                                StaticType.BOOL
                            ),
                            nAryOp = "LIKE"
                        )
                    )
                ),
                createNAryLikeErrorTest(
                    name = "NAry op LIKE data type mismatch - 3 args, escape type of union of incompatible types",
                    valueType = StaticType.STRING,
                    patternType = StaticType.STRING,
                    escapeType = StaticType.unionOf(StaticType.INT, StaticType.DECIMAL, StaticType.BOOL),
                    expectedErrors = listOf(
                        createDataTypeMismatchError(
                            col = 3,
                            argTypes = listOf(
                                StaticType.STRING,
                                StaticType.STRING,
                                StaticType.unionOf(StaticType.INT, StaticType.DECIMAL, StaticType.BOOL)
                            ),
                            nAryOp = "LIKE"
                        )
                    )
                ),
                createNAryLikeErrorTest(
                    name = "NAry op LIKE data type mismatch - 3 args, escape type of union of incompatible types",
                    valueType = StaticType.STRING,
                    patternType = StaticType.unionOf(StaticType.INT, StaticType.DECIMAL, StaticType.BOOL),
                    escapeType = StaticType.SYMBOL,
                    expectedErrors = listOf(
                        createDataTypeMismatchError(
                            col = 3,
                            argTypes = listOf(
                                StaticType.STRING,
                                StaticType.unionOf(StaticType.INT, StaticType.DECIMAL, StaticType.BOOL),
                                StaticType.SYMBOL
                            ),
                            nAryOp = "LIKE"
                        )
                    )
                ),
                createNAryLikeErrorTest(
                    name = "NAry op LIKE data type mismatch - 3 args, escape type of union of incompatible types",
                    valueType = StaticType.unionOf(StaticType.INT, StaticType.DECIMAL, StaticType.BOOL),
                    patternType = StaticType.STRING,
                    escapeType = StaticType.STRING,
                    expectedErrors = listOf(
                        createDataTypeMismatchError(
                            col = 3,
                            argTypes = listOf(
                                StaticType.unionOf(
                                    StaticType.INT,
                                    StaticType.DECIMAL,
                                    StaticType.BOOL
                                ),
                                StaticType.STRING, StaticType.STRING
                            ),
                            nAryOp = "LIKE"
                        )
                    )
                ),
                createNAryLikeValidTest(
                    name = "NAry op LIKE with null or missing warning - string LIKE string ESCAPE null",
                    valueType = StaticType.STRING,
                    patternType = StaticType.STRING,
                    escapeType = StaticType.NULL,
                    outputType = StaticType.NULL,
                    expectedWarnings = listOf(
                        createReturnsNullOrMissingWarning(
                            col = 3,
                            nAryOp = "LIKE"
                        )
                    )
                ),
                createNAryLikeValidTest(
                    name = "NAry op LIKE with null or missing warning - string LIKE null ESCAPE string",
                    valueType = StaticType.STRING,
                    patternType = StaticType.NULL,
                    escapeType = StaticType.STRING,
                    outputType = StaticType.NULL,
                    expectedWarnings = listOf(
                        createReturnsNullOrMissingWarning(
                            col = 3,
                            nAryOp = "LIKE"
                        )
                    )
                ),
                createNAryLikeValidTest(
                    name = "NAry op LIKE with null or missing warning - null LIKE string ESCAPE string",
                    valueType = StaticType.NULL,
                    patternType = StaticType.STRING,
                    escapeType = StaticType.STRING,
                    outputType = StaticType.NULL,
                    expectedWarnings = listOf(
                        createReturnsNullOrMissingWarning(
                            col = 3,
                            nAryOp = "LIKE"
                        )
                    )
                ),
                createNAryLikeValidTest(
                    name = "NAry op LIKE with null or missing warning - null LIKE null ESCAPE null",
                    valueType = StaticType.NULL,
                    patternType = StaticType.NULL,
                    escapeType = StaticType.NULL,
                    outputType = StaticType.NULL,
                    expectedWarnings = listOf(
                        createReturnsNullOrMissingWarning(
                            col = 3,
                            nAryOp = "LIKE"
                        )
                    )
                ),
                createNAryLikeErrorTest(
                    name = "NAry op LIKE with always missing error - string LIKE missing ESCAPE string",
                    valueType = StaticType.STRING,
                    patternType = StaticType.MISSING,
                    escapeType = StaticType.STRING,
                    expectedErrors = listOf(
                        createReturnsMissingError(
                            col = 3,
                            nAryOp = "LIKE"
                        )
                    )
                ),
                createNAryLikeErrorTest(
                    name = "NAry op LIKE with always missing error - missing LIKE string ESCAPE string",
                    valueType = StaticType.MISSING,
                    patternType = StaticType.STRING,
                    escapeType = StaticType.STRING,
                    expectedErrors = listOf(
                        createReturnsMissingError(
                            col = 3,
                            nAryOp = "LIKE"
                        )
                    )
                ),
                createNAryLikeErrorTest(
                    name = "NAry op LIKE with always missing error - missing LIKE missing ESCAPE missing",
                    valueType = StaticType.MISSING,
                    patternType = StaticType.MISSING,
                    escapeType = StaticType.MISSING,
                    expectedErrors = listOf(
                        createReturnsMissingError(
                            col = 3,
                            nAryOp = "LIKE"
                        )
                    )
                ),
                createNAryLikeErrorTest(
                    name = "NAry op LIKE with always missing error - missing LIKE null ESCAPE null",
                    valueType = StaticType.MISSING,
                    patternType = StaticType.NULL,
                    escapeType = StaticType.NULL,
                    expectedErrors = listOf(
                        createReturnsMissingError(
                            col = 3,
                            nAryOp = "LIKE"
                        )
                    )
                ),
                createNAryLikeErrorTest(
                    name = "NAry op LIKE with always missing error - null LIKE missing ESCAPE null",
                    valueType = StaticType.NULL,
                    patternType = StaticType.MISSING,
                    escapeType = StaticType.NULL,
                    expectedErrors = listOf(
                        createReturnsMissingError(
                            col = 3,
                            nAryOp = "LIKE"
                        )
                    )
                ),
                createNAryLikeErrorTest(
                    name = "NAry op LIKE with always missing error - null LIKE null ESCAPE missing",
                    valueType = StaticType.NULL,
                    patternType = StaticType.NULL,
                    escapeType = StaticType.MISSING,
                    expectedErrors = listOf(
                        createReturnsMissingError(
                            col = 3,
                            nAryOp = "LIKE"
                        )
                    )
                ),
                createNAryLikeErrorTest(
                    name = "NAry op LIKE data type mismatch, always missing error - 3 args, incompatible escape type with unknown types",
                    valueType = StaticType.NULL,
                    patternType = StaticType.MISSING,
                    escapeType = StaticType.INT,
                    expectedErrors = listOf(
                        createDataTypeMismatchError(
                            col = 3,
                            argTypes = listOf(StaticType.NULL, StaticType.MISSING, StaticType.INT),
                            nAryOp = "LIKE"
                        ),
                        createReturnsMissingError(
                            col = 3,
                            nAryOp = "LIKE"
                        )
                    )
                ),
                createNAryLikeErrorTest(
                    name = "NAry op LIKE data type mismatch, always missing error - 3 args, incompatible pattern type with unknown types",
                    valueType = StaticType.NULL,
                    patternType = StaticType.INT,
                    escapeType = StaticType.MISSING,
                    expectedErrors = listOf(
                        createDataTypeMismatchError(
                            col = 3,
                            argTypes = listOf(StaticType.NULL, StaticType.INT, StaticType.MISSING),
                            nAryOp = "LIKE"
                        ),
                        createReturnsMissingError(
                            col = 3,
                            nAryOp = "LIKE"
                        )
                    )
                ),
                createNAryLikeErrorTest(
                    name = "NAry op LIKE data type mismatch, always missing error - 3 args, incompatible value type with unknown types",
                    valueType = StaticType.STRUCT,
                    patternType = StaticType.NULL,
                    escapeType = StaticType.MISSING,
                    expectedErrors = listOf(
                        createDataTypeMismatchError(
                            col = 3,
                            argTypes = listOf(StaticType.STRUCT, StaticType.NULL, StaticType.MISSING),
                            nAryOp = "LIKE"
                        ),
                        createReturnsMissingError(
                            col = 3,
                            nAryOp = "LIKE"
                        )
                    )
                ),
            )
    }
}
