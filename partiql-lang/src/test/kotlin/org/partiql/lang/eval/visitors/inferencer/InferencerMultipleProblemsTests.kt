package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.errors.Problem
import org.partiql.errors.ProblemLocation
import org.partiql.lang.ast.passes.SemanticProblemDetails
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.TestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.customerType
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectQueryOutputType
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectSemanticProblems
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.formatFunc
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.runTest
import org.partiql.types.AnyOfType
import org.partiql.types.BagType
import org.partiql.types.ListType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StructType

class InferencerMultipleProblemsTests {
    @ParameterizedTest
    @MethodSource("parametersForMultipleInferenceProblemsTests")
    fun multipleInferenceProblemsTests(tc: TestCase) = runTest(tc)

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun parametersForMultipleInferenceProblemsTests() = listOf(
            TestCase(
                "projections with paths",
                "SELECT c.firstName, c.age FROM customers AS c",
                mapOf(
                    "customers" to customerType
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "firstName" to StaticType.STRING,
                                "age" to StaticType.INT2
                            ),
                            true
                        )
                    )
                )
            ),
            TestCase(
                "projections with paths with missing attributes and closed content",
                "SELECT c.missing_attr FROM customers AS c",
                mapOf(
                    "customers" to customerType
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "missing_attr" to StaticType.MISSING
                            ),
                            true
                        )
                    )
                )
            ),
            TestCase(
                "projections with paths with missing attributes and open content",
                "SELECT c.missing_attr FROM customers AS c",
                mapOf(
                    "customers" to BagType(
                        StructType(
                            mapOf(
                                "firstName" to StaticType.STRING
                            ),
                            contentClosed = false
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "missing_attr" to StaticType.ANY
                            ),
                            true
                        )
                    )
                )
            ),
            TestCase(
                "projections with multi level paths",
                "SELECT c.address.city FROM customers AS c",
                mapOf(
                    "customers" to customerType
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "city" to StaticType.STRING
                            ),
                            true
                        )
                    )
                )
            ),
            TestCase(
                "projections with select *",
                "SELECT * FROM customers AS c",
                mapOf(
                    "customers" to customerType
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            (customerType.elementType as StructType).fields,
                            (customerType.elementType as StructType).contentClosed
                        )
                    )
                )
            ),
            TestCase(
                "projections with duplicate items in select list query",
                "SELECT firstName as foo, age as foo FROM customers AS c",
                mapOf(
                    "customers" to customerType
                ),
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        Problem(
                            ProblemLocation(1L, 26L, 3L),
                            SemanticProblemDetails.DuplicateAliasesInSelectListItem
                        )
                    ),
                    expectedWarnings = emptyList()
                )
            ),
            TestCase(
                "projections with AT value for list",
                "SELECT elem, atVal FROM x AS elem AT atVal",
                mapOf(
                    "x" to ListType(StaticType.STRING)
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "elem" to StaticType.STRING,
                                "atVal" to StaticType.INT
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "projections with AT value for bag",
                "SELECT elem, atVal FROM x AS elem AT atVal",
                mapOf(
                    "x" to BagType(StaticType.STRING)
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "elem" to StaticType.STRING,
                                "atVal" to StaticType.MISSING
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "projections with from source as ANY",
                "SELECT elem, atVal FROM x AS elem AT atVal",
                mapOf(
                    "x" to StaticType.ANY
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "elem" to StaticType.ANY,
                                // INT possible because `any` includes list. MISSING for everything else.
                                "atVal" to StaticType.unionOf(StaticType.INT, StaticType.MISSING)
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "projections with from source as a union of list and bag",
                "SELECT elem, atVal FROM x AS elem AT atVal",
                mapOf(
                    "x" to StaticType.unionOf(ListType(StaticType.INT), BagType(StaticType.STRING))
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "elem" to StaticType.unionOf(StaticType.INT, StaticType.STRING),
                                "atVal" to StaticType.unionOf(StaticType.INT, StaticType.MISSING)
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "projections with from source as a union of list and a non-collection type",
                "SELECT elem, atVal FROM x AS elem AT atVal",
                mapOf(
                    "x" to StaticType.unionOf(ListType(StaticType.INT), StaticType.STRING)
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                // StaticType.STRING coerces to BagType(StaticType.STRING) in FROM clause
                                "elem" to StaticType.unionOf(StaticType.INT, StaticType.STRING),
                                "atVal" to StaticType.unionOf(StaticType.INT, StaticType.MISSING)
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "projections with from source as collection of collections",
                "SELECT elem, atVal FROM x AS elem AT atVal",
                mapOf(
                    "x" to BagType(ListType(StaticType.STRING))
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "elem" to ListType(StaticType.STRING),
                                "atVal" to StaticType.MISSING
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "projections with from source as a union of null and missing",
                "SELECT elem, atVal FROM x AS elem AT atVal",
                mapOf(
                    "x" to StaticType.unionOf(StaticType.NULL, StaticType.MISSING)
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "elem" to StaticType.unionOf(StaticType.NULL, StaticType.MISSING),
                                "atVal" to StaticType.MISSING
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "projections with from source as a sexp",
                "SELECT elem, atVal FROM x AS elem AT atVal",
                mapOf(
                    "x" to SexpType(StaticType.INT)
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                // Evaluator behavior when this test case was written -
                                // PartiQL> select f, x from sexp(1,2,3) as f at x
                                //   |
                                // ==='
                                // <<
                                //  {
                                //    'f': `(1 2 3)`
                                //  }
                                // >>
                                // ---
                                "elem" to SexpType(StaticType.INT),
                                "atVal" to StaticType.MISSING
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "UNPIVOT on closed content struct",
                "SELECT sym, price FROM UNPIVOT closingPrice AS price AT sym",
                mapOf(
                    "closingPrice" to StructType(
                        mapOf(
                            "msft" to StaticType.DECIMAL,
                            "amzn" to StaticType.DECIMAL
                        ),
                        contentClosed = true
                    )
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "sym" to StaticType.STRING,
                                "price" to StaticType.DECIMAL
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "UNPIVOT with open content",
                "SELECT sym, price FROM UNPIVOT closingPrice AS price AT sym",
                mapOf(
                    "closingPrice" to StructType(
                        mapOf(
                            "msft" to StaticType.STRING,
                            "amzn" to StaticType.DECIMAL
                        ),
                        contentClosed = false
                    )
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "sym" to StaticType.STRING.asOptional(),
                                "price" to StaticType.ANY
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "UNPIVOT on AnyType",
                "SELECT sym, price FROM UNPIVOT closingPrice AS price AT sym",
                mapOf(
                    "closingPrice" to StaticType.ANY
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "sym" to StaticType.STRING.asOptional(),
                                "price" to StaticType.ANY
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "UNPIVOT on MISSING",
                "SELECT sym, price FROM UNPIVOT closingPrice AS price AT sym",
                mapOf(
                    "closingPrice" to StaticType.MISSING
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "sym" to StaticType.MISSING,
                                "price" to StaticType.MISSING
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "UNPIVOT on IntType",
                "SELECT sym, price FROM UNPIVOT closingPrice AS price AT sym",
                mapOf(
                    "closingPrice" to StaticType.INT
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "sym" to StaticType.STRING,
                                "price" to StaticType.INT
                            ),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "join with closed-content structs",
                "SELECT * FROM a, b",
                mapOf(
                    "a" to BagType(
                        StructType(
                            mapOf("x" to StaticType.INT4),
                            true
                        )
                    ),
                    "b" to BagType(
                        StructType(
                            mapOf("y" to StaticType.INT4),
                            true
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "x" to StaticType.INT4,
                                "y" to StaticType.INT4
                            ),
                            true
                        )
                    )
                )
            ),
            TestCase(
                "join with mixed open content-ness",
                "SELECT * FROM a, b",
                mapOf(
                    "a" to BagType(
                        StructType(
                            mapOf("x" to StaticType.INT4),
                            true
                        )
                    ),
                    "b" to BagType(
                        StructType(
                            mapOf("y" to StaticType.INT4),
                            false
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "x" to StaticType.INT4,
                                "y" to StaticType.INT4
                            ),
                            false
                        )
                    )
                )
            ),
            TestCase(
                "SELECT VALUE",
                "SELECT VALUE elem FROM a_list AS elem",
                mapOf("a_list" to ListType(StaticType.STRING)),
                handler = expectQueryOutputType(BagType(StaticType.STRING))
            ),
            TestCase(
                "un-nesting a list",
                "SELECT o.orderId, o.orderDate FROM customers AS c, @c.orders AS o",
                mapOf(
                    "customers" to customerType
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf(
                                "orderId" to StaticType.STRING,
                                "orderDate" to StaticType.TIMESTAMP
                            ),
                            true
                        )
                    )
                )
            ),
            TestCase(
                "coalesce op with variadic arguments - int, null",
                "COALESCE(1, null)",
                handler = expectQueryOutputType(StaticType.INT)
            ),
            TestCase(
                "coalesce op with variadic arguments - int, missing",
                "COALESCE(1, missing)",
                handler = expectQueryOutputType(StaticType.INT)
            ),
            TestCase(
                "coalesce op with variadic arguments - null, missing",
                "COALESCE(null, missing)",
                handler = expectQueryOutputType(StaticType.NULL)
            ),
            TestCase(
                "coalesce op with variadic arguments - null, string, missing, int",
                "COALESCE(null, 'a', missing, 1)",
                handler = expectQueryOutputType(StaticType.STRING)
            ),
            TestCase(
                "coalesce op with variadic arguments - missing, missing",
                "COALESCE(missing, missing)",
                handler = expectQueryOutputType(StaticType.MISSING)
            ),
            TestCase(
                "coalesce op with variadic arguments - null, null",
                "COALESCE(null, null)",
                handler = expectQueryOutputType(StaticType.NULL)
            ),
            TestCase(
                "coalesce op with variadic arguments - union types",
                "COALESCE(x, y)",
                mapOf(
                    "x" to StaticType.unionOf(StaticType.NULL, StaticType.STRING),
                    "y" to StaticType.STRING
                ),
                handler = expectQueryOutputType(StaticType.STRING)
            ),
            TestCase(
                "coalesce op with variadic arguments - union types",
                "COALESCE(x, y)",
                mapOf(
                    "x" to StaticType.unionOf(StaticType.NULL, StaticType.STRING),
                    "y" to StaticType.INT
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.STRING,
                        StaticType.INT
                    )
                )
            ),
            TestCase(
                "coalesce op with variadic arguments - union types",
                "COALESCE(x, y)",
                mapOf(
                    "x" to StaticType.unionOf(StaticType.NULL, StaticType.STRING),
                    "y" to StaticType.unionOf(StaticType.NULL, StaticType.INT)
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.NULL,
                        StaticType.STRING,
                        StaticType.INT
                    )
                )
            ),
            TestCase(
                "coalesce op with variadic arguments - union types",
                "COALESCE(x, y)",
                mapOf(
                    "x" to StaticType.unionOf(StaticType.NULL, StaticType.STRING),
                    "y" to StaticType.unionOf(StaticType.NULL, StaticType.INT, StaticType.MISSING)
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.NULL,
                        StaticType.STRING,
                        StaticType.INT
                    )
                )
            ),
            TestCase(
                "coalesce op with variadic arguments - union types",
                "COALESCE(x, y)",
                mapOf(
                    "x" to StaticType.unionOf(StaticType.MISSING, StaticType.STRING),
                    "y" to StaticType.unionOf(StaticType.MISSING, StaticType.INT)
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.MISSING,
                        StaticType.STRING,
                        StaticType.INT
                    )
                )
            ),
            TestCase(
                "coalesce op with variadic arguments - union types",
                "COALESCE(x, y)",
                mapOf(
                    "x" to StaticType.unionOf(StaticType.MISSING, StaticType.NULL, StaticType.STRING),
                    "y" to StaticType.unionOf(StaticType.MISSING, StaticType.INT)
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.MISSING,
                        StaticType.NULL,
                        StaticType.STRING,
                        StaticType.INT
                    )
                )
            ),
            TestCase(
                "coalesce op with variadic arguments - union types",
                "COALESCE(x, y, z, w)",
                mapOf(
                    "x" to StaticType.unionOf(StaticType.MISSING, StaticType.STRING),
                    "y" to StaticType.unionOf(StaticType.MISSING, StaticType.INT),
                    "z" to StaticType.unionOf(StaticType.BOOL, StaticType.BAG),
                    "w" to StaticType.unionOf(StaticType.NULL, StaticType.LIST)
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.BOOL,
                        StaticType.BAG,
                        StaticType.STRING,
                        StaticType.INT
                    )
                )
            ),
            TestCase(
                "function signature without optional/variadic arguments, null propagating given matching type",
                "UPPER('test')",
                handler = expectQueryOutputType(StaticType.STRING)
            ),
            TestCase(
                "function signature without optional/variadic arguments, null propagating given matching type with null",
                "UPPER(x)",
                mapOf(
                    "x" to AnyOfType(setOf(StaticType.NULL, StaticType.STRING))
                ),
                handler = expectQueryOutputType(
                    AnyOfType(
                        setOf(
                            StaticType.NULL,
                            StaticType.STRING
                        )
                    )
                )
            ),
            TestCase(
                "function signature without optional/variadic arguments, null propagating given matching type with missing",
                "UPPER(x)",
                mapOf(
                    "x" to AnyOfType(setOf(StaticType.MISSING, StaticType.STRING))
                ),
                handler = expectQueryOutputType(
                    AnyOfType(
                        setOf(
                            StaticType.MISSING,
                            StaticType.STRING
                        )
                    )
                )
            ),
            TestCase(
                "function signature without optional/variadic arguments, null propagating given non matching type but has a valid overlapping type",
                "UPPER(x)",
                mapOf(
                    "x" to AnyOfType(setOf(StaticType.INT, StaticType.STRING))
                ),
                handler = expectQueryOutputType(
                    AnyOfType(
                        setOf(
                            StaticType.MISSING,
                            StaticType.STRING
                        )
                    )
                )
            ),
            TestCase(
                "function signature without optional/variadic arguments, null propagating given non matching type but has a valid overlapping type including null",
                "UPPER(x)",
                mapOf(
                    "x" to AnyOfType(setOf(StaticType.INT, StaticType.BOOL, StaticType.STRING, StaticType.NULL))
                ),
                handler = expectQueryOutputType(
                    AnyOfType(
                        setOf(
                            StaticType.MISSING,
                            StaticType.NULL,
                            StaticType.STRING
                        )
                    )
                )
            ),
            TestCase(
                "function signature without optional/variadic arguments, with ANY in arguments",
                "UPPER(x)",
                mapOf(
                    "x" to StaticType.ANY
                ),
                handler = expectQueryOutputType(
                    AnyOfType(
                        setOf(
                            StaticType.MISSING,
                            StaticType.NULL,
                            StaticType.STRING
                        )
                    )
                )
            ),
            TestCase(
                "function signature without optional/variadic arguments, null propagating given lesser than expected arguments",
                "UPPER()",
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        Problem(
                            ProblemLocation(1L, 1L, 5L),
                            SemanticProblemDetails.IncorrectNumberOfArgumentsToFunctionCall(
                                functionName = "upper",
                                expectedArity = 1..1,
                                actualArity = 0
                            )
                        )
                    )
                )
            ),
            TestCase(
                "function signature without optional/variadic arguments, null propagating given more than expected arguments",
                "UPPER('test', 'test')",
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        Problem(
                            ProblemLocation(1L, 1L, 5L),
                            SemanticProblemDetails.IncorrectNumberOfArgumentsToFunctionCall(
                                functionName = "upper",
                                expectedArity = 1..1,
                                actualArity = 2
                            )
                        )
                    )
                )
            ),
            TestCase(
                "null propagating function with union types with null",
                "UPPER(x)",
                mapOf(
                    "x" to AnyOfType(setOf(StaticType.STRING, StaticType.NULL))

                ),
                handler = expectQueryOutputType(
                    AnyOfType(
                        setOf(
                            StaticType.STRING,
                            StaticType.NULL
                        )
                    )
                )
            ),
            TestCase(
                "function that expects no arguments",
                "utcnow()",
                handler = expectQueryOutputType(StaticType.TIMESTAMP)
            ),
            TestCase(
                "function that expects no arguments given arguments",
                "utcnow(null)",
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        Problem(
                            ProblemLocation(1L, 1L, 6L),
                            SemanticProblemDetails.IncorrectNumberOfArgumentsToFunctionCall(
                                functionName = "utcnow",
                                expectedArity = 0..0,
                                actualArity = 1
                            )
                        )
                    )
                )
            ),
            TestCase(
                "function with more than one argument",
                "date_add(year, 5, `2010-01-01T`)",
                handler = expectQueryOutputType(StaticType.TIMESTAMP)
            ),
            TestCase(
                "function with more than one argument with union types having nulls",
                "date_add(year, x, y)",
                mapOf(
                    "x" to AnyOfType(setOf(StaticType.NULL, StaticType.INT)),
                    "y" to AnyOfType(setOf(StaticType.NULL, StaticType.TIMESTAMP))
                ),
                handler = expectQueryOutputType(
                    AnyOfType(
                        setOf(
                            StaticType.TIMESTAMP,
                            StaticType.NULL
                        )
                    )
                )
            ),
            TestCase(
                "function with more than one argument with union types having missing",
                "date_add(year, x, y)",
                mapOf(
                    "x" to AnyOfType(setOf(StaticType.MISSING, StaticType.INT)),
                    "y" to AnyOfType(setOf(StaticType.MISSING, StaticType.TIMESTAMP))
                ),
                handler = expectQueryOutputType(
                    AnyOfType(
                        setOf(
                            StaticType.TIMESTAMP,
                            StaticType.MISSING
                        )
                    )
                )
            ),
            TestCase(
                "function with more than one argument with union types having null and missing",
                "date_add(year, x, y)",
                mapOf(
                    "x" to AnyOfType(setOf(StaticType.MISSING, StaticType.INT)),
                    "y" to AnyOfType(setOf(StaticType.NULL, StaticType.TIMESTAMP))
                ),
                handler = expectQueryOutputType(
                    AnyOfType(
                        setOf(
                            StaticType.TIMESTAMP,
                            StaticType.MISSING,
                            StaticType.NULL
                        )
                    )
                )
            ),
            TestCase(
                "function with more than one argument with union types and at least one valid case",
                "date_add(year, x, y)",
                mapOf(
                    "x" to AnyOfType(setOf(StaticType.INT, StaticType.STRING)),
                    "y" to AnyOfType(setOf(StaticType.INT, StaticType.TIMESTAMP))
                ),
                handler = expectQueryOutputType(
                    AnyOfType(
                        setOf(
                            StaticType.TIMESTAMP,
                            StaticType.MISSING
                        )
                    )
                )
            ),
            TestCase(
                "function with more than one argument with union types, nulls and at least one valid case",
                "date_add(year, x, y)",
                mapOf(
                    "x" to AnyOfType(setOf(StaticType.INT, StaticType.STRING)),
                    "y" to AnyOfType(setOf(StaticType.INT, StaticType.TIMESTAMP, StaticType.NULL))
                ),
                handler = expectQueryOutputType(
                    AnyOfType(
                        setOf(
                            StaticType.TIMESTAMP,
                            StaticType.MISSING,
                            StaticType.NULL
                        )
                    )
                )
            ),
            TestCase(
                "function that expects union type in signature",
                "size([1, 2])",
                handler = expectQueryOutputType(StaticType.INT)
            ),
            TestCase(
                "function that expects union type in signature",
                "size({'a': 1, 'b': 2})",
                mapOf(),
                handler = expectQueryOutputType(StaticType.INT)
            ),
            TestCase(
                "function that has optional parameter",
                "SUBSTRING('123456789', 0, 999)",
                handler = expectQueryOutputType(StaticType.STRING)
            ),
            TestCase(
                "function that has optional parameter with missing optional parameter",
                "SUBSTRING('123456789', 0)",
                handler = expectQueryOutputType(StaticType.STRING)
            ),
            TestCase(
                "function that has optional parameter with optional parameter as union type",
                "SUBSTRING('123456789', x, y)",
                mapOf(
                    "x" to StaticType.INT,
                    "y" to AnyOfType(setOf(StaticType.INT, StaticType.NULL))
                ),
                handler = expectQueryOutputType(
                    AnyOfType(
                        setOf(
                            StaticType.STRING,
                            StaticType.NULL
                        )
                    )
                )
            ),
            TestCase(
                "function that has optional parameter with optional parameter as union type",
                "SUBSTRING('123456789', x, y)",
                mapOf(
                    "x" to StaticType.INT,
                    "y" to AnyOfType(setOf(StaticType.INT, StaticType.MISSING))
                ),
                handler = expectQueryOutputType(
                    AnyOfType(
                        setOf(
                            StaticType.STRING,
                            StaticType.MISSING
                        )
                    )
                )
            ),
            TestCase(
                "function that has optional parameter with too many arguments",
                "TO_TIMESTAMP('February 2016', 'MMMM yyyy', 'extra arg')",
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        Problem(
                            ProblemLocation(1L, 1L, 12L),
                            SemanticProblemDetails.IncorrectNumberOfArgumentsToFunctionCall(
                                functionName = "to_timestamp",
                                expectedArity = 1..2,
                                actualArity = 3
                            )
                        )
                    )
                )
            ),
            TestCase(
                "custom function",
                "format('test %d %s', [1, 'a'])",
                customFunctionSignatures = listOf(formatFunc.signature),
                handler = expectQueryOutputType(StaticType.STRING)
            ),
            TestCase(
                "non-existent function",
                "non_existent(null)",
                handler = expectSemanticProblems(
                    expectedErrors = listOf(
                        Problem(
                            ProblemLocation(1L, 1L, 12L),
                            SemanticProblemDetails.NoSuchFunction(
                                functionName = "non_existent"
                            )
                        )
                    )
                )
            ),
            TestCase(
                "LET bindings",
                "SELECT nameLength FROM A LET char_length(A.name) AS nameLength",
                mapOf("A" to BagType(StructType(mapOf("name" to StaticType.STRING)))),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf("nameLength" to StaticType.INT),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "Multiple unique bindings with LET",
                "SELECT x, y FROM A LET char_length(A.name) AS x, x + 1 AS y",
                mapOf("A" to BagType(StructType(mapOf("name" to StaticType.STRING)))),
                handler = expectQueryOutputType(
                    BagType(StructType(mapOf("x" to StaticType.INT, "y" to StaticType.INT), contentClosed = true))
                )
            ),
            TestCase(
                "Array index with numeric literal ",
                "SELECT a.l[1] AS x FROM a",
                mapOf(
                    "a" to StructType(
                        mapOf("l" to ListType(elementType = StaticType.BOOL))
                    )
                ),
                handler = expectQueryOutputType(
                    BagType(StructType(mapOf("x" to StaticType.BOOL), contentClosed = true))
                )
            ),
            TestCase(
                "Array index with call to operator ",
                "SELECT a.l[1 + 1] AS x FROM a",
                mapOf(
                    "a" to StructType(
                        mapOf("l" to ListType(elementType = StaticType.BOOL))
                    )
                ),
                handler = expectQueryOutputType(
                    BagType(StructType(mapOf("x" to StaticType.BOOL), contentClosed = true))
                )
            ),
            TestCase(
                "Struct index with call to operator ",
                "SELECT a.l[1 + 1] AS x, a.l.y AS p FROM a",
                mapOf(
                    "a" to StructType(
                        mapOf("l" to StructType(mapOf("y" to StaticType.BOOL)))
                    )
                ),
                handler = expectQueryOutputType(
                    BagType(
                        StructType(
                            mapOf("x" to StaticType.MISSING, "p" to StaticType.BOOL),
                            contentClosed = true
                        )
                    )
                )
            ),
            TestCase(
                "Struct pathing ",
                "SELECT a.b.c.d AS x FROM a",
                mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf(
                                            "d" to StaticType.BOOL,
                                            "e" to StaticType.DECIMAL
                                        )
                                    ),
                                    "xx" to StaticType.BLOB
                                )
                            ),
                            "ww" to StaticType.CLOB
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    BagType(StructType(mapOf("x" to StaticType.BOOL), contentClosed = true))
                )
            )
        )
    }
}
