package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.ALL_NUMERIC_TYPES
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.TestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createDataTypeMismatchError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createIncompatibleTypesForExprError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createInvalidArgumentTypeForFunctionError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createNullOrMissingFunctionArgumentError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createReturnsMissingError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createReturnsNullOrMissingWarning
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectQueryOutputType
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectSemanticProblems
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.runTest
import org.partiql.types.ListType
import org.partiql.types.StaticType

class InferencerContinuationTypeTests {

    @ParameterizedTest
    @MethodSource("parametersForContinuationTypeTests")
    fun continuationTypeTests(tc: TestCase) = runTest(tc)

    companion object {
        /**
         * Creates four tests expecting an error and expecting [expectedContinuationType] as the output type. This is
         * useful for checking the continuation type after an operation errors.
         *
         * Created tests are of the form:
         *   - good [op] bad <- data type mismatch error
         *   - bad [op] good <- data type mismatch error
         *   - good [op] null <- null or missing warning
         *   - null [op] good <- null or missing warning
         *   - good [op] missing <- always missing error
         *   - missing [op] good <- always missing error
         */
        private fun createBinaryNonLogicalOpContinuationTypeTest(
            goodType: StaticType,
            badType: StaticType,
            expectedContinuationType: StaticType,
            op: String
        ) = listOf(
            TestCase(
                name = "data type mismatch error: $goodType $op $badType -> $expectedContinuationType",
                originalSql = "goodT $op badT",
                globals = mapOf(
                    "goodT" to goodType,
                    "badT" to badType
                ),
                handler = expectQueryOutputType(
                    expectedType = expectedContinuationType,
                    expectedErrors = listOf(
                        createDataTypeMismatchError(
                            col = 7,
                            argTypes = listOf(goodType, badType),
                            nAryOp = op
                        )
                    )
                )
            ),
            TestCase(
                name = "data type mismatch error: $badType $op $goodType -> $expectedContinuationType",
                originalSql = "badT $op goodT",
                globals = mapOf(
                    "goodT" to goodType,
                    "badT" to badType
                ),
                handler = expectQueryOutputType(
                    expectedType = expectedContinuationType,
                    expectedErrors = listOf(
                        createDataTypeMismatchError(
                            col = 6,
                            argTypes = listOf(badType, goodType),
                            nAryOp = op
                        )
                    )
                )
            ),
            TestCase(
                name = "null or missing warning: $goodType $op null -> null",
                originalSql = "goodT $op badT",
                globals = mapOf(
                    "goodT" to goodType,
                    "badT" to StaticType.NULL
                ),
                handler = expectQueryOutputType(
                    expectedType = StaticType.NULL,
                    expectedWarnings = listOf(createReturnsNullOrMissingWarning(col = 7, nAryOp = op)),
                    expectedErrors = emptyList()
                )
            ),
            TestCase(
                name = "null or missing warning: null $op $goodType -> null",
                originalSql = "badT $op goodT",
                globals = mapOf(
                    "goodT" to goodType,
                    "badT" to StaticType.NULL
                ),
                handler = expectQueryOutputType(
                    expectedType = StaticType.NULL,
                    expectedWarnings = listOf(createReturnsNullOrMissingWarning(col = 6, nAryOp = op)),
                    expectedErrors = emptyList()
                )
            ),
            TestCase(
                name = "always missing error: $goodType $op missing -> $expectedContinuationType",
                originalSql = "goodT $op badT",
                globals = mapOf(
                    "goodT" to goodType,
                    "badT" to StaticType.MISSING
                ),
                handler = expectQueryOutputType(
                    expectedType = expectedContinuationType,
                    expectedErrors = listOf(createReturnsMissingError(col = 7, nAryOp = op))
                )
            ),
            TestCase(
                name = "always missing error: missing $op $goodType -> $expectedContinuationType",
                originalSql = "badT $op goodT",
                globals = mapOf(
                    "goodT" to goodType,
                    "badT" to StaticType.MISSING
                ),
                handler = expectQueryOutputType(
                    expectedType = expectedContinuationType,
                    expectedErrors = listOf(createReturnsMissingError(col = 6, nAryOp = op)),
                )
            )
        )

        /**
         * For logical op, the continuation type will be unionOf(null, bool) in case of null and bool type.
         */
        private fun createBinaryLogicalOpContinuationTypeTest(
            goodType: StaticType,
            badType: StaticType,
            expectedContinuationType: StaticType,
            op: String
        ) = listOf(
            TestCase(
                name = "data type mismatch error: $goodType $op $badType -> $expectedContinuationType",
                originalSql = "goodT $op badT",
                globals = mapOf(
                    "goodT" to goodType,
                    "badT" to badType
                ),
                handler = expectQueryOutputType(
                    expectedType = expectedContinuationType,
                    expectedErrors = listOf(
                        createDataTypeMismatchError(
                            col = 7,
                            argTypes = listOf(goodType, badType),
                            nAryOp = op
                        )
                    )
                )
            ),
            TestCase(
                name = "data type mismatch error: $badType $op $goodType -> $expectedContinuationType",
                originalSql = "badT $op goodT",
                globals = mapOf(
                    "goodT" to goodType,
                    "badT" to badType
                ),
                handler = expectQueryOutputType(
                    expectedType = expectedContinuationType,
                    expectedErrors = listOf(
                        createDataTypeMismatchError(
                            col = 6,
                            argTypes = listOf(badType, goodType),
                            nAryOp = op
                        )
                    )
                )
            ),
            TestCase(
                name = "null or missing warning: $goodType $op null -> null",
                originalSql = "goodT $op badT",
                globals = mapOf(
                    "goodT" to goodType,
                    "badT" to StaticType.NULL
                ),
                handler = expectQueryOutputType(
                    expectedType = StaticType.unionOf(StaticType.NULL, StaticType.BOOL),
                    expectedWarnings = listOf(createReturnsNullOrMissingWarning(col = 7, nAryOp = op)),
                    expectedErrors = emptyList()
                )
            ),
            TestCase(
                name = "null or missing warning: null $op $goodType -> null",
                originalSql = "badT $op goodT",
                globals = mapOf(
                    "goodT" to goodType,
                    "badT" to StaticType.NULL
                ),
                handler = expectQueryOutputType(
                    expectedType = StaticType.unionOf(StaticType.NULL, StaticType.BOOL),
                    expectedWarnings = listOf(createReturnsNullOrMissingWarning(col = 6, nAryOp = op)),
                    expectedErrors = emptyList()
                )
            ),
            TestCase(
                name = "always missing error: $goodType $op missing -> $expectedContinuationType",
                originalSql = "goodT $op badT",
                globals = mapOf(
                    "goodT" to goodType,
                    "badT" to StaticType.MISSING
                ),
                handler = expectQueryOutputType(
                    expectedType = expectedContinuationType,
                    expectedErrors = listOf(createReturnsMissingError(col = 7, nAryOp = op))
                )
            ),
            TestCase(
                name = "always missing error: missing $op $goodType -> $expectedContinuationType",
                originalSql = "badT $op goodT",
                globals = mapOf(
                    "goodT" to goodType,
                    "badT" to StaticType.MISSING
                ),
                handler = expectQueryOutputType(
                    expectedType = expectedContinuationType,
                    expectedErrors = listOf(createReturnsMissingError(col = 6, nAryOp = op)),
                )
            )
        )

        /**
         * Creates four tests with a single data type mismatch error to check that the single error doesn't cause other
         * errors in the expression. [op] is required to be a left-associative, binary operation. Created expressions
         * are of the form:
         *   - bad [op] good [op] good [op] good <- error at 1st [op]
         *   - good [op] bad [op] good [op] good <- error at 1st [op]
         *   - good [op] good [op] bad [op] good <- error at 2nd [op]
         *   - good [op] good [op] good [op] bad <- error at 3rd [op]
         */
        private fun createChainedOpSingleErrorTests(
            goodType: StaticType,
            badType: StaticType,
            op: String
        ): List<TestCase> {
            val globals = mapOf(
                "goodT" to goodType,
                "badT" to badType
            )
            return listOf(
                TestCase(
                    name = "single data type mismatch error: $badType $op $goodType $op $goodType $op $goodType",
                    originalSql = "badT \n$op goodT \n$op goodT \n$op goodT",
                    globals = globals,
                    handler = expectSemanticProblems(
                        expectedErrors = listOf(
                            createDataTypeMismatchError(
                                line = 2,
                                col = 1,
                                argTypes = listOf(badType, goodType),
                                nAryOp = op
                            )
                        )
                    )
                ),
                TestCase(
                    name = "single data type mismatch error: $goodType $op $badType $goodType $op $goodType $op",
                    originalSql = "goodT \n$op badT \n$op goodT \n$op goodT",
                    globals = globals,
                    handler = expectSemanticProblems(
                        expectedErrors = listOf(
                            createDataTypeMismatchError(
                                line = 2,
                                col = 1,
                                argTypes = listOf(goodType, badType),
                                nAryOp = op
                            )
                        )
                    )
                ),
                TestCase(
                    name = "single data type mismatch error: $goodType $op $goodType $op $badType $op $goodType",
                    originalSql = "goodT \n$op goodT \n$op badT \n$op goodT",
                    globals = globals,
                    handler = expectSemanticProblems(
                        expectedErrors = listOf(
                            createDataTypeMismatchError(
                                line = 3,
                                col = 1,
                                argTypes = listOf(goodType, badType),
                                nAryOp = op
                            )
                        )
                    )
                ),
                TestCase(
                    name = "single data type mismatch error: $goodType $op $goodType $op $goodType $op $badType",
                    originalSql = "goodT \n$op goodT \n$op goodT \n$op badT",
                    globals = globals,
                    handler = expectSemanticProblems(
                        expectedErrors = listOf(
                            createDataTypeMismatchError(
                                line = 4,
                                col = 1,
                                argTypes = listOf(goodType, badType),
                                nAryOp = op
                            )
                        )
                    )
                )
            )
        }

        @JvmStatic
        @Suppress("unused")
        fun parametersForContinuationTypeTests() =
            // arithmetic ops will return a union of all numeric types in the event of an error
            InferencerTestUtil.OpType.ARITHMETIC.operators.flatMap { arithmeticOp ->
                createBinaryNonLogicalOpContinuationTypeTest(
                    goodType = StaticType.INT,
                    badType = StaticType.STRING,
                    expectedContinuationType = StaticType.unionOf(ALL_NUMERIC_TYPES.toSet()),
                    op = arithmeticOp
                )
            } +
                // concat will return string in the event of an error
                createBinaryNonLogicalOpContinuationTypeTest(
                    goodType = StaticType.STRING,
                    badType = StaticType.INT,
                    expectedContinuationType = StaticType.STRING,
                    op = "||"
                ) +
                // LIKE will return bool in the event of an error
                createBinaryNonLogicalOpContinuationTypeTest(
                    goodType = StaticType.STRING,
                    badType = StaticType.INT,
                    expectedContinuationType = StaticType.BOOL,
                    op = "LIKE"
                ) +
                // logical ops will return bool in the event of an error
                InferencerTestUtil.OpType.LOGICAL.operators.flatMap { logicalOp ->
                    createBinaryLogicalOpContinuationTypeTest(
                        goodType = StaticType.BOOL,
                        badType = StaticType.STRING,
                        expectedContinuationType = StaticType.BOOL,
                        op = logicalOp
                    )
                } +
                // comparison ops will return bool in the event of an error
                InferencerTestUtil.OpType.COMPARISON.operators.flatMap { logicalOp ->
                    createBinaryNonLogicalOpContinuationTypeTest(
                        goodType = StaticType.INT,
                        badType = StaticType.STRING,
                        expectedContinuationType = StaticType.BOOL,
                        op = logicalOp
                    )
                } +
                // equality ops will return bool in the event of an error
                InferencerTestUtil.OpType.EQUALITY.operators.flatMap { logicalOp ->
                    createBinaryNonLogicalOpContinuationTypeTest(
                        goodType = StaticType.INT,
                        badType = StaticType.STRING,
                        expectedContinuationType = StaticType.BOOL,
                        op = logicalOp
                    )
                } +
                // unary arithmetic op tests - continuation type of numeric
                listOf("+", "-").flatMap { op ->
                    listOf(
                        TestCase(
                            name = "data type mismatch error: $op string -> union of numerics",
                            originalSql = "$op badT",
                            globals = mapOf("badT" to StaticType.STRING),
                            handler = expectQueryOutputType(
                                expectedType = StaticType.unionOf(ALL_NUMERIC_TYPES.toSet()),
                                expectedErrors = listOf(
                                    createDataTypeMismatchError(
                                        col = 1,
                                        argTypes = listOf(StaticType.STRING),
                                        nAryOp = op
                                    )
                                )
                            )
                        ),
                        TestCase(
                            name = "null or missing warning: $op string -> null",
                            originalSql = "$op nullT",
                            globals = mapOf("nullT" to StaticType.NULL),
                            handler = expectQueryOutputType(
                                expectedType = StaticType.NULL,
                                expectedWarnings = listOf(
                                    createReturnsNullOrMissingWarning(
                                        col = 1,
                                        nAryOp = op
                                    )
                                ),
                                expectedErrors = emptyList()
                            )
                        )
                    )
                } +
                // LIKE tests with bad ESCAPE type - continuation type of bool
                listOf(
                    TestCase(
                        name = "data type mismatch error: string LIKE string ESCAPE int -> bool",
                        originalSql = "goodT LIKE goodT ESCAPE badT",
                        globals = mapOf(
                            "goodT" to StaticType.STRING,
                            "badT" to StaticType.INT
                        ),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.BOOL,
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 7,
                                    argTypes = listOf(StaticType.STRING, StaticType.STRING, StaticType.INT),
                                    nAryOp = "LIKE"
                                )
                            )
                        )
                    ),
                    TestCase(
                        name = "null or missing error: string LIKE string ESCAPE null -> bool",
                        originalSql = "goodT LIKE goodT ESCAPE badT",
                        globals = mapOf(
                            "goodT" to StaticType.STRING,
                            "badT" to StaticType.NULL
                        ),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.NULL,
                            expectedWarnings = listOf(
                                createReturnsNullOrMissingWarning(
                                    col = 7,
                                    nAryOp = "LIKE"
                                )
                            ),
                            expectedErrors = emptyList()
                        )
                    ),
                ) +
                // logical `NOT` with non-bool - continuation type of bool
                listOf(
                    TestCase(
                        name = "data type mismatch error: NOT string -> bool",
                        originalSql = "NOT badT",
                        globals = mapOf("badT" to StaticType.STRING),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.BOOL,
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 1,
                                    argTypes = listOf(StaticType.STRING),
                                    nAryOp = "NOT"
                                )
                            )
                        )
                    ),
                    TestCase(
                        name = "null or missing warning: NOT null -> bool",
                        originalSql = "NOT nullT",
                        globals = mapOf("nullT" to StaticType.NULL),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.NULL,
                            expectedWarnings = listOf(
                                createReturnsNullOrMissingWarning(
                                    col = 1,
                                    nAryOp = "NOT"
                                )
                            ),
                            expectedErrors = emptyList()
                        )
                    )
                ) +
                // `BETWEEN` op tests - continuation type of bool
                listOf(
                    TestCase(
                        name = "data type mismatch error: int BETWEEN string AND string",
                        originalSql = "goodT BETWEEN badT AND badT",
                        globals = mapOf(
                            "goodT" to StaticType.INT,
                            "badT" to StaticType.STRING
                        ),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.BOOL,
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 7,
                                    argTypes = listOf(StaticType.INT, StaticType.STRING, StaticType.STRING),
                                    nAryOp = "BETWEEN"
                                )
                            )
                        )
                    ),
                    TestCase(
                        name = "null or missing warning: null BETWEEN int AND int",
                        originalSql = "nullT BETWEEN goodT AND goodT",
                        globals = mapOf(
                            "nullT" to StaticType.NULL,
                            "goodT" to StaticType.INT
                        ),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.NULL,
                            expectedWarnings = listOf(
                                createReturnsNullOrMissingWarning(
                                    col = 7,
                                    nAryOp = "BETWEEN"
                                )
                            ),
                            expectedErrors = emptyList()
                        )
                    )
                ) +
                // `IN` op tests - continuation type of bool
                listOf(
                    TestCase(
                        name = "data type mismatch error: int IN int",
                        originalSql = "lhs IN rhs",
                        globals = mapOf(
                            "lhs" to StaticType.INT,
                            "rhs" to StaticType.INT
                        ),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.BOOL,
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 5,
                                    argTypes = listOf(StaticType.INT, StaticType.INT),
                                    nAryOp = "IN"
                                )
                            )
                        )
                    ),
                    TestCase(
                        name = "data type mismatch error (incomparable rhs element type): int IN list(string)",
                        originalSql = "lhs IN rhs",
                        globals = mapOf(
                            "lhs" to StaticType.INT,
                            "rhs" to ListType(elementType = StaticType.STRING)
                        ),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.BOOL,
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 5,
                                    argTypes = listOf(StaticType.INT, ListType(StaticType.STRING)),
                                    nAryOp = "IN"
                                )
                            )
                        )
                    ),
                    TestCase(
                        name = "null or missing warning: null IN list(string)",
                        originalSql = "nullT IN rhs",
                        globals = mapOf(
                            "nullT" to StaticType.NULL,
                            "rhs" to ListType(elementType = StaticType.STRING)
                        ),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.NULL,
                            expectedWarnings = listOf(
                                createReturnsNullOrMissingWarning(
                                    col = 7,
                                    nAryOp = "IN"
                                )
                            ),
                            expectedErrors = emptyList()
                        )
                    ),
                    TestCase(
                        name = "null or missing warning: int IN null",
                        originalSql = "lhs IN nullT",
                        globals = mapOf(
                            "lhs" to StaticType.INT,
                            "nullT" to StaticType.NULL
                        ),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.NULL,
                            expectedWarnings = listOf(
                                createReturnsNullOrMissingWarning(
                                    col = 5,
                                    nAryOp = "IN"
                                )
                            ),
                            expectedErrors = emptyList()
                        )
                    )
                ) +
                // `NULLIF` op tests - continuation type of left argument types and null
                listOf(
                    TestCase(
                        name = "data type mismatch error: NULLIF(union(INT, FLOAT), STRING)",
                        originalSql = "NULLIF(lhs, rhs)",
                        globals = mapOf(
                            "lhs" to StaticType.unionOf(StaticType.INT, StaticType.FLOAT),
                            "rhs" to StaticType.STRING
                        ),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.unionOf(StaticType.INT, StaticType.FLOAT, StaticType.NULL),
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    col = 1,
                                    argTypes = listOf(
                                        StaticType.unionOf(StaticType.INT, StaticType.FLOAT),
                                        StaticType.STRING
                                    ),
                                    nAryOp = "NULLIF"
                                )
                            )
                        )
                    ),
                    TestCase(
                        name = "null or missing warning: NULLIF(union(INT, FLOAT), MISSING)",
                        originalSql = "NULLIF(lhs, rhs)",
                        globals = mapOf(
                            "lhs" to StaticType.unionOf(StaticType.INT, StaticType.FLOAT),
                            "rhs" to StaticType.MISSING
                        ),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.unionOf(StaticType.INT, StaticType.FLOAT, StaticType.NULL),
                            expectedErrors = listOf(
                                createReturnsMissingError(
                                    col = 1,
                                    nAryOp = "NULLIF"
                                )
                            )
                        )
                    ),
                    TestCase(
                        name = "null or missing warning: NULLIF(MISSING, union(INT, FLOAT))",
                        originalSql = "NULLIF(lhs, rhs)",
                        globals = mapOf(
                            "lhs" to StaticType.MISSING,
                            "rhs" to StaticType.unionOf(StaticType.INT, StaticType.FLOAT)
                        ),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.unionOf(StaticType.MISSING, StaticType.NULL),
                            expectedErrors = listOf(
                                createReturnsMissingError(
                                    col = 1,
                                    nAryOp = "NULLIF"
                                )
                            )
                        )
                    )
                ) +
                // SimpleCaseWhen should include all `THEN` expression types in the case of error. If no `ELSE` branch is
                // included, then will also include `NULL` in the output types
                listOf(
                    TestCase(
                        name = "data type mismatch error: CASE <int> WHEN <string> THEN <string> WHEN <symbol> THEN <symbol> END",
                        originalSql = "CASE t_int WHEN t_string THEN t_string WHEN t_symbol THEN t_symbol END",
                        globals = mapOf(
                            "t_int" to StaticType.INT,
                            "t_string" to StaticType.STRING,
                            "t_symbol" to StaticType.SYMBOL
                        ),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.unionOf(
                                StaticType.STRING,
                                StaticType.SYMBOL,
                                StaticType.NULL
                            ),
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    SourceLocationMeta(1L, 17L, 8L),
                                    argTypes = listOf(StaticType.INT, StaticType.STRING),
                                    nAryOp = "CASE"
                                ),
                                createDataTypeMismatchError(
                                    SourceLocationMeta(1L, 45L, 8L),
                                    argTypes = listOf(StaticType.INT, StaticType.SYMBOL),
                                    nAryOp = "CASE"
                                )
                            )
                        )
                    ),
                    TestCase(
                        name = "data type mismatch error with elseExpr: CASE <int> WHEN <string> THEN <string> WHEN <symbol> THEN <symbol> ELSE t_float END",
                        originalSql = "CASE t_int WHEN t_string THEN t_string WHEN t_symbol THEN t_symbol ELSE t_float END",
                        globals = mapOf(
                            "t_int" to StaticType.INT,
                            "t_string" to StaticType.STRING,
                            "t_symbol" to StaticType.SYMBOL,
                            "t_float" to StaticType.FLOAT
                        ),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.unionOf(
                                StaticType.STRING,
                                StaticType.SYMBOL,
                                StaticType.FLOAT
                            ),
                            expectedErrors = listOf(
                                createDataTypeMismatchError(
                                    SourceLocationMeta(1L, 17L, 8L),
                                    argTypes = listOf(StaticType.INT, StaticType.STRING),
                                    nAryOp = "CASE"
                                ),
                                createDataTypeMismatchError(
                                    SourceLocationMeta(1L, 45L, 8L),
                                    argTypes = listOf(StaticType.INT, StaticType.SYMBOL),
                                    nAryOp = "CASE"
                                )
                            )
                        )
                    ),
                    TestCase(
                        name = "always returns missing error (from caseValue): CASE <missing> WHEN <string> THEN <string> WHEN <symbol> THEN <symbol> END",
                        originalSql = "CASE t_missing WHEN t_string THEN t_string WHEN t_symbol THEN t_symbol END",
                        globals = mapOf(
                            "t_missing" to StaticType.MISSING,
                            "t_string" to StaticType.STRING,
                            "t_symbol" to StaticType.SYMBOL
                        ),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.unionOf(
                                StaticType.STRING,
                                StaticType.SYMBOL,
                                StaticType.NULL
                            ),
                            expectedErrors = listOf(
                                createReturnsMissingError(
                                    SourceLocationMeta(1L, 6L, 9L)
                                )
                            )
                        )
                    ),
                    TestCase(
                        name = "data type mismatch and always returns missing errors: CASE <int> WHEN <missingT> THEN <string> WHEN <symbol> THEN <symbol> END",
                        originalSql = "CASE t_int WHEN t_missing THEN t_string WHEN t_symbol THEN t_symbol END",
                        globals = mapOf(
                            "t_int" to StaticType.INT,
                            "t_missing" to StaticType.MISSING,
                            "t_string" to StaticType.STRING,
                            "t_symbol" to StaticType.SYMBOL
                        ),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.unionOf(
                                StaticType.STRING,
                                StaticType.SYMBOL,
                                StaticType.NULL
                            ),
                            expectedErrors = listOf(
                                createReturnsMissingError(
                                    SourceLocationMeta(1L, 17L, 9L)
                                ),
                                createDataTypeMismatchError(
                                    SourceLocationMeta(1L, 46L, 8L),
                                    argTypes = listOf(StaticType.INT, StaticType.SYMBOL),
                                    nAryOp = "CASE"
                                )
                            )
                        )
                    )
                ) +
                // SearchedCaseWhen should include all `THEN` expression types in the case of error. If no `ELSE` branch is
                // included, then will also include `NULL` in the output types
                listOf(
                    TestCase(
                        name = "data type mismatch error: CASE WHEN <int> THEN <int> WHEN <string> THEN <string> END",
                        originalSql = "CASE WHEN t_int THEN t_int WHEN t_string THEN t_string END",
                        globals = mapOf(
                            "t_int" to StaticType.INT,
                            "t_string" to StaticType.STRING
                        ),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.unionOf(StaticType.INT, StaticType.STRING, StaticType.NULL),
                            expectedErrors = listOf(
                                createIncompatibleTypesForExprError(
                                    SourceLocationMeta(1L, 11L, 5L),
                                    expectedType = StaticType.BOOL,
                                    actualType = StaticType.INT
                                ),
                                createIncompatibleTypesForExprError(
                                    SourceLocationMeta(1L, 33L, 8L),
                                    expectedType = StaticType.BOOL,
                                    actualType = StaticType.STRING
                                )
                            )
                        )
                    ),
                    TestCase(
                        name = "data type mismatch error with elseExpr: CASE WHEN <int> THEN <int> WHEN <string> THEN <string> ELSE <symbol> END",
                        originalSql = "CASE WHEN t_int THEN t_int WHEN t_string THEN t_string ELSE t_symbol END",
                        globals = mapOf(
                            "t_int" to StaticType.INT,
                            "t_string" to StaticType.STRING,
                            "t_symbol" to StaticType.SYMBOL
                        ),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.unionOf(StaticType.INT, StaticType.STRING, StaticType.SYMBOL),
                            expectedErrors = listOf(
                                createIncompatibleTypesForExprError(
                                    SourceLocationMeta(1L, 11L, 5L),
                                    expectedType = StaticType.BOOL,
                                    actualType = StaticType.INT
                                ),
                                createIncompatibleTypesForExprError(
                                    SourceLocationMeta(1L, 33L, 8L),
                                    expectedType = StaticType.BOOL,
                                    actualType = StaticType.STRING
                                )
                            )
                        )
                    ),
                    TestCase(
                        name = "null or missing warnings/errors: CASE WHEN <null> THEN <null> WHEN <missing> THEN <missing> END",
                        originalSql = "CASE WHEN t_null THEN t_null WHEN t_missing THEN t_missing END",
                        globals = mapOf(
                            "t_null" to StaticType.NULL,
                            "t_missing" to StaticType.MISSING
                        ),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.unionOf(StaticType.NULL, StaticType.MISSING),
                            expectedWarnings = listOf(
                                createReturnsNullOrMissingWarning(
                                    SourceLocationMeta(1L, 11L, 6L)
                                )
                            ),
                            expectedErrors = listOf(
                                createReturnsMissingError(
                                    SourceLocationMeta(1L, 35L, 9L)
                                )
                            )
                        )
                    ),
                    TestCase(
                        name = "data type mismatch and always returns missing errors: whenExprs of non-bools and unknown",
                        originalSql = "CASE WHEN t_int THEN t_int WHEN t_string THEN t_string WHEN t_missing THEN t_missing END",
                        globals = mapOf(
                            "t_int" to StaticType.INT,
                            "t_string" to StaticType.STRING,
                            "t_missing" to StaticType.MISSING
                        ),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.unionOf(
                                StaticType.INT,
                                StaticType.MISSING,
                                StaticType.STRING,
                                StaticType.NULL
                            ),
                            expectedErrors = listOf(
                                createIncompatibleTypesForExprError(
                                    SourceLocationMeta(1L, 11L, 5L),
                                    expectedType = StaticType.BOOL,
                                    actualType = StaticType.INT
                                ),
                                createIncompatibleTypesForExprError(
                                    SourceLocationMeta(1L, 33L, 8L),
                                    expectedType = StaticType.BOOL,
                                    actualType = StaticType.STRING
                                ),
                                createReturnsMissingError(
                                    SourceLocationMeta(1L, 61L, 9L)
                                )
                            )
                        )
                    )
                ) +
                // function calls with invalid arguments leading to errors have a continuation type of the function
                // signature's return type
                listOf(
                    TestCase(
                        name = "invalid function call arg: UPPER(INT) -> STRING",
                        originalSql = "UPPER(x)",
                        globals = mapOf("x" to StaticType.INT),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.STRING,
                            expectedErrors = listOf(
                                createInvalidArgumentTypeForFunctionError(
                                    sourceLocation = SourceLocationMeta(1L, 7L, 1L),
                                    functionName = "upper",
                                    expectedArgType = StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL),
                                    actualType = StaticType.INT
                                )
                            )
                        )
                    ),
                    TestCase(
                        name = "null function call arg: UPPER(NULL) -> STRING",
                        originalSql = "UPPER(x)",
                        globals = mapOf("x" to StaticType.NULL),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.STRING,
                            expectedErrors = listOf(
                                createNullOrMissingFunctionArgumentError(
                                    sourceLocation = SourceLocationMeta(1L, 7L, 1L),
                                    functionName = "upper"
                                )
                            )
                        )
                    ),
                    TestCase(
                        name = "invalid function call arg and null in optional: SUBSTRING(STRING, NULL, BOOL) -> STRING",
                        originalSql = "SUBSTRING('123456789', x, y)",
                        globals = mapOf("x" to StaticType.BOOL, "y" to StaticType.NULL),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.STRING,
                            expectedErrors = listOf(
                                createInvalidArgumentTypeForFunctionError(
                                    sourceLocation = SourceLocationMeta(1L, 24L, 1L),
                                    functionName = "substring",
                                    expectedArgType = StaticType.INT,
                                    actualType = StaticType.BOOL
                                ),
                                createNullOrMissingFunctionArgumentError(
                                    sourceLocation = SourceLocationMeta(1L, 27L, 1L),
                                    functionName = "substring"
                                )
                            )
                        )
                    ),
                    TestCase(
                        name = "invalid function call arg in variadic arg and missing: TRIM(BOTH INT FROM MISSING)",
                        originalSql = "TRIM(BOTH x FROM y)",
                        globals = mapOf("x" to StaticType.INT, "y" to StaticType.MISSING),
                        handler = expectQueryOutputType(
                            expectedType = StaticType.STRING,
                            expectedErrors = listOf(
                                createInvalidArgumentTypeForFunctionError(
                                    sourceLocation = SourceLocationMeta(1L, 11L, 1L),
                                    functionName = "trim",
                                    expectedArgType = StaticType.STRING,
                                    actualType = StaticType.INT
                                ),
                                createNullOrMissingFunctionArgumentError(
                                    sourceLocation = SourceLocationMeta(1L, 18L, 1L),
                                    functionName = "trim"
                                )
                            )
                        )
                    )
                ) +
                // operations that can be chained (i.e. left-associative, binary operation) with a data type mismatch
                // should not lead to multiple errors
                InferencerTestUtil.OpType.ARITHMETIC.operators.flatMap { arithmeticOp ->
                    createChainedOpSingleErrorTests(
                        goodType = StaticType.INT,
                        badType = StaticType.STRING,
                        op = arithmeticOp
                    )
                } +
                createChainedOpSingleErrorTests(
                    goodType = StaticType.STRING,
                    badType = StaticType.INT,
                    op = "||"
                ) +
                InferencerTestUtil.OpType.LOGICAL.operators.flatMap { logicalOp ->
                    createChainedOpSingleErrorTests(
                        goodType = StaticType.BOOL,
                        badType = StaticType.STRING,
                        op = logicalOp
                    )
                }
    }
}
