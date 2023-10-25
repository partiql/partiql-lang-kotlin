package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.errors.Problem
import org.partiql.errors.ProblemLocation
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.passes.SemanticProblemDetails
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.TestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createDataTypeMismatchError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createInvalidArgumentTypeForFunctionError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.createNullOrMissingFunctionArgumentError
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectSemanticProblems
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.runTest
import org.partiql.types.ListType
import org.partiql.types.StaticType
import org.partiql.types.StructType

class InferencerErrorInFunctionCallArgumentTests {

    @ParameterizedTest
    @MethodSource("parametersForErrorInFunctionCallArgumentTests")
    fun errorInFunctionCallArgumentTests(tc: TestCase) = runTest(tc)

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun parametersForErrorInFunctionCallArgumentTests() = listOf(
            TestCase(
                name = "function signature without optional/variadic arguments, null propagating given non matching type",
                originalSql = "UPPER(x)",
                globals = mapOf("x" to StaticType.INT),
                handler = expectSemanticProblems(
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
                name = "function signature without optional/variadic arguments, null propagating given non matching type for a path",
                originalSql = "UPPER(x.y)",
                globals = mapOf("x" to StructType(mapOf("y" to StaticType.INT))),
                handler = expectSemanticProblems(
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
                name = "function signature without optional/variadic arguments, null propagating given non matching type for list reference",
                originalSql = "UPPER(x[0])",
                globals = mapOf("x" to ListType(StaticType.INT)),
                handler = expectSemanticProblems(
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
                name = "function signature without optional/variadic arguments, null propagating given non matching type with missing",
                originalSql = "UPPER(x)",
                globals = mapOf("x" to StaticType.INT.asOptional()),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 7L, 1L),
                            functionName = "upper",
                            expectedArgType = StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL),
                            actualType = StaticType.INT.asOptional()
                        )
                    )
                )
            ),
            TestCase(
                name = "function signature without optional/variadic arguments, null propagating given non matching type with no valid overlapping type",
                originalSql = "UPPER(x)",
                globals = mapOf("x" to StaticType.unionOf(StaticType.INT, StaticType.BOOL)),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 7L, 1L),
                            functionName = "upper",
                            expectedArgType = StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL),
                            actualType = StaticType.unionOf(StaticType.INT, StaticType.BOOL)
                        )
                    )
                )
            ),
            TestCase(
                name = "function signature without optional/variadic arguments, null propagating given non matching type including null with no valid overlapping type",
                originalSql = "UPPER(x)",
                globals = mapOf("x" to StaticType.unionOf(StaticType.INT, StaticType.BOOL, StaticType.NULL)),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 7L, 1L),
                            functionName = "upper",
                            expectedArgType = StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL),
                            actualType = StaticType.unionOf(StaticType.INT, StaticType.BOOL, StaticType.NULL)
                        )
                    )
                )
            ),
            TestCase(
                name = "function signature without optional/variadic arguments, null propagating given null",
                originalSql = "UPPER(x)",
                globals = mapOf("x" to StaticType.NULL),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 7L, 1L),
                            functionName = "upper"
                        )
                    )
                )
            ),
            TestCase(
                name = "function signature without optional/variadic arguments, null propagating given missing",
                originalSql = "UPPER(x)",
                globals = mapOf("x" to StaticType.MISSING),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 7L, 1L),
                            functionName = "upper"
                        )
                    )
                )
            ),
            TestCase(
                name = "function signature without optional/variadic arguments, null propagating given non matching type with null and missing",
                originalSql = "UPPER(x)",
                globals = mapOf("x" to StaticType.NULL_OR_MISSING),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 7L, 1L),
                            functionName = "upper"
                        )
                    )
                )
            ),
            TestCase(
                name = "function with more than one argument - invalid arg type at 2nd arg",
                originalSql = "date_add(year, x, `2010-01-01T`)",
                globals = mapOf("x" to StaticType.DECIMAL),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 16L, 1L),
                            functionName = "date_add",
                            expectedArgType = StaticType.INT,
                            actualType = StaticType.DECIMAL
                        )
                    )
                )
            ),
            TestCase(
                name = "function with more than one argument - null 3rd arg",
                originalSql = "date_add(year, 5, x)",
                globals = mapOf("x" to StaticType.NULL),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 19L, 1L),
                            functionName = "date_add"
                        )
                    )
                )
            ),
            TestCase(
                name = "function with more than one argument - missing 3rd arg",
                originalSql = "date_add(year, 5, x)",
                globals = mapOf("x" to StaticType.MISSING),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 19L, 1L),
                            functionName = "date_add"
                        )
                    )
                )
            ),
            TestCase(
                name = "function with more than one argument having missing and one argument null",
                originalSql = "date_add(year, x, y)",
                globals = mapOf(
                    "x" to StaticType.MISSING,
                    "y" to StaticType.NULL
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 16L, 1L),
                            functionName = "date_add"
                        ),
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 19L, 1L),
                            functionName = "date_add"
                        )
                    )
                )
            ),
            TestCase(
                name = "function with more than one argument with nulls and union types with nulls",
                originalSql = "date_add(year, x, y)",
                globals = mapOf(
                    "x" to StaticType.unionOf(StaticType.INT4, StaticType.NULL),
                    "y" to StaticType.NULL
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 19L, 1L),
                            functionName = "date_add"
                        )
                    )
                )
            ),
            TestCase(
                name = "function with more than one argument with union types and no valid case (x should have a INT type)",
                originalSql = "date_add(year, x, y)",
                globals = mapOf(
                    "x" to StaticType.unionOf(StaticType.BOOL, StaticType.STRING),
                    "y" to StaticType.unionOf(StaticType.INT, StaticType.TIMESTAMP)
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 16L, 1L),
                            functionName = "date_add",
                            expectedArgType = StaticType.INT,
                            actualType = StaticType.unionOf(StaticType.BOOL, StaticType.STRING)
                        )
                    )
                )
            ),
            TestCase(
                name = "function with more than one argument with union types, nulls and with no valid case",
                originalSql = "date_add(year, x, y)",
                globals = mapOf(
                    "x" to StaticType.unionOf(StaticType.BOOL, StaticType.STRING),
                    "y" to StaticType.unionOf(StaticType.INT, StaticType.TIMESTAMP, StaticType.NULL)
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 16L, 1L),
                            functionName = "date_add",
                            expectedArgType = StaticType.INT,
                            actualType = StaticType.unionOf(StaticType.BOOL, StaticType.STRING)
                        )
                    )
                )
            ),
            TestCase(
                name = "function that expects union type in signature but NULL provided",
                originalSql = "size(x)",
                globals = mapOf("x" to StaticType.NULL),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 6L, 1L),
                            functionName = "size"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that expects union type in signature but MISSING provided",
                originalSql = "size(x)",
                globals = mapOf("x" to StaticType.MISSING),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 6L, 1L),
                            functionName = "size"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that expects union type in signature but STRING provided",
                originalSql = "size(x)",
                globals = mapOf("x" to StaticType.STRING),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 6L, 1L),
                            functionName = "size",
                            expectedArgType = StaticType.unionOf(
                                StaticType.BAG,
                                StaticType.LIST,
                                StaticType.STRUCT,
                                StaticType.SEXP
                            ),
                            actualType = StaticType.STRING
                        )
                    )
                )
            ),
            TestCase(
                name = "function that that could have optional parameter with null",
                originalSql = "SUBSTRING('123456789', x)",
                globals = mapOf("x" to StaticType.NULL),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 24L, 1L),
                            functionName = "substring"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that has optional parameter with null",
                originalSql = "SUBSTRING('123456789', x, 0)",
                globals = mapOf("x" to StaticType.NULL),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 24L, 1L),
                            functionName = "substring"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that has optional parameter with missing",
                originalSql = "SUBSTRING('123456789', x, 0)",
                globals = mapOf("x" to StaticType.MISSING),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 24L, 1L),
                            functionName = "substring"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that has optional parameter with optional parameter as null",
                originalSql = "SUBSTRING('123456789', 0, x)",
                globals = mapOf("x" to StaticType.NULL),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 27L, 1L),
                            functionName = "substring"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that has optional parameter with optional parameter as missing",
                originalSql = "SUBSTRING('123456789', 0, x)",
                globals = mapOf("x" to StaticType.MISSING),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 27L, 1L),
                            functionName = "substring"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that has optional parameter with optional parameters as null and missing",
                originalSql = "SUBSTRING('123456789', x, y)",
                globals = mapOf(
                    "x" to StaticType.NULL,
                    "y" to StaticType.MISSING
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 24L, 1L),
                            functionName = "substring"
                        ),
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 27L, 1L),
                            functionName = "substring"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that has optional parameter with optional parameters as missing and null",
                originalSql = "SUBSTRING('123456789', x, y)",
                globals = mapOf(
                    "x" to StaticType.MISSING,
                    "y" to StaticType.NULL
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 24L, 1L),
                            functionName = "substring"
                        ),
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 27L, 1L),
                            functionName = "substring"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that has optional parameter with optional parameters as null and null",
                originalSql = "SUBSTRING('123456789', x, y)",
                globals = mapOf(
                    "x" to StaticType.NULL,
                    "y" to StaticType.NULL
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 24L, 1L),
                            functionName = "substring"
                        ),
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 27L, 1L),
                            functionName = "substring"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that has optional parameter with optional parameters as missing and missing",
                originalSql = "SUBSTRING('123456789', x, y)",
                globals = mapOf(
                    "x" to StaticType.MISSING,
                    "y" to StaticType.MISSING
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 24L, 1L),
                            functionName = "substring"
                        ),
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 27L, 1L),
                            functionName = "substring"
                        )
                    )
                )
            ),
            TestCase(
                name = "function that has optional parameter with optional parameter with incorrect type",
                originalSql = "SUBSTRING('123456789', 0, x)",
                globals = mapOf("x" to StaticType.STRING),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 27L, 1L),
                            functionName = "substring",
                            expectedArgType = StaticType.INT,
                            actualType = StaticType.STRING
                        )
                    )
                )
            ),
            // tests with a variadic ExprFunction
            TestCase(
                name = "variadic function, missing in required with variadic params",
                originalSql = "TRIM(BOTH FROM x)",
                globals = mapOf("x" to StaticType.MISSING),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 16L, 1L),
                            functionName = "trim"
                        )
                    )
                )
            ),
            TestCase(
                name = "variadic function, missing in required with no variadic params",
                originalSql = "TRIM(x)",
                globals = mapOf("x" to StaticType.MISSING),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 6L, 1L),
                            functionName = "trim"
                        )
                    )
                )
            ),
            TestCase(
                name = "variadic function, string in required, missing in variadic with other variadic param",
                originalSql = "TRIM(BOTH x FROM y)",
                globals = mapOf("x" to StaticType.MISSING, "y" to StaticType.STRING),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 11L, 1L),
                            functionName = "trim"
                        )
                    )
                )
            ),
            TestCase(
                name = "variadic function, string in required, missing in variadic with no other variadic param",
                originalSql = "TRIM(x FROM y)",
                globals = mapOf("x" to StaticType.MISSING, "y" to StaticType.STRING),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 6L, 1L),
                            functionName = "trim"
                        )
                    )
                )
            ),
            TestCase(
                name = "variadic function, missing in required, string in variadic with other variadic param",
                originalSql = "TRIM(BOTH x FROM y)",
                globals = mapOf("x" to StaticType.STRING, "y" to StaticType.MISSING),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 18L, 1L),
                            functionName = "trim"
                        )
                    )
                )
            ),
            TestCase(
                name = "variadic function, missing in required, string in variadic with no other variadic param",
                originalSql = "TRIM(x FROM y)",
                globals = mapOf("x" to StaticType.STRING, "y" to StaticType.MISSING),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 13L, 1L),
                            functionName = "trim"
                        )
                    )
                )
            ),
            TestCase(
                name = "variadic function, bad type in required with other variadic param",
                originalSql = "TRIM(BOTH FROM x)",
                globals = mapOf(
                    "x" to StaticType.unionOf(
                        StaticType.BOOL,
                        StaticType.INT,
                        StaticType.TIMESTAMP,
                        StaticType.LIST
                    )
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 16L, 1L),
                            functionName = "trim",
                            expectedArgType = StaticType.STRING,
                            actualType = StaticType.unionOf(
                                StaticType.BOOL,
                                StaticType.INT,
                                StaticType.TIMESTAMP,
                                StaticType.LIST
                            )
                        )
                    )
                )
            ),
            TestCase(
                name = "variadic function, missing in required, bad type in variadic with other variadic param",
                originalSql = "TRIM(BOTH x FROM y)",
                globals = mapOf(
                    "x" to StaticType.unionOf(
                        StaticType.BOOL,
                        StaticType.INT,
                        StaticType.TIMESTAMP,
                        StaticType.LIST
                    ),
                    "y" to StaticType.MISSING
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        createInvalidArgumentTypeForFunctionError(
                            sourceLocation = SourceLocationMeta(1L, 11L, 1L),
                            functionName = "trim",
                            expectedArgType = StaticType.STRING,
                            actualType = StaticType.unionOf(
                                StaticType.BOOL,
                                StaticType.INT,
                                StaticType.TIMESTAMP,
                                StaticType.LIST
                            )
                        ),
                        createNullOrMissingFunctionArgumentError(
                            sourceLocation = SourceLocationMeta(1L, 18L, 1L),
                            functionName = "trim"
                        )
                    )
                )
            ),
            TestCase(
                name = "multiple errors - too few args in function call and arithmetic datatype mismatch",
                originalSql = "size() + a",
                globals = mapOf("a" to StaticType.STRING),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        Problem(
                            ProblemLocation(1L, 1L, 4L),
                            SemanticProblemDetails.IncorrectNumberOfArgumentsToFunctionCall(
                                functionName = "size",
                                expectedArity = 1..1,
                                actualArity = 0
                            )
                        ),
                        createDataTypeMismatchError(
                            col = 8,
                            argTypes = listOf(StaticType.INT, StaticType.STRING),
                            nAryOp = "+"
                        )
                    )
                )
            ),
            TestCase(
                name = "multiple errors - too many args in function call and arithmetic datatype mismatch",
                originalSql = "size(l, extra_arg) + a",
                globals = mapOf(
                    "a" to StaticType.STRING,
                    "l" to StaticType.LIST,
                    "extra_arg" to StaticType.LIST
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        Problem(
                            ProblemLocation(1L, 1L, 4L),
                            SemanticProblemDetails.IncorrectNumberOfArgumentsToFunctionCall(
                                functionName = "size",
                                expectedArity = 1..1,
                                actualArity = 2
                            )
                        ),
                        createDataTypeMismatchError(
                            col = 20,
                            argTypes = listOf(StaticType.INT, StaticType.STRING),
                            nAryOp = "+"
                        )
                    )
                )
            )
        )
    }
}
