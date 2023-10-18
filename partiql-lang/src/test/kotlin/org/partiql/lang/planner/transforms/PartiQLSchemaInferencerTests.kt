package org.partiql.lang.planner.transforms

import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.annotations.ExperimentalPartiQLSchemaInferencer
import org.partiql.errors.Problem
import org.partiql.errors.UNKNOWN_PROBLEM_LOCATION
import org.partiql.lang.errors.ProblemCollector
import org.partiql.lang.planner.transforms.PartiQLSchemaInferencerTests.ProblemHandler
import org.partiql.lang.planner.transforms.PartiQLSchemaInferencerTests.TestCase.ErrorTestCase
import org.partiql.lang.planner.transforms.PartiQLSchemaInferencerTests.TestCase.SuccessTestCase
import org.partiql.lang.planner.transforms.PartiQLSchemaInferencerTests.TestCase.ThrowingExceptionTestCase
import org.partiql.plan.debug.PlanPrinter
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.PlanningProblemDetails
import org.partiql.plugins.local.LocalPlugin
import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StaticType.Companion.DECIMAL
import org.partiql.types.StaticType.Companion.INT
import org.partiql.types.StaticType.Companion.MISSING
import org.partiql.types.StaticType.Companion.NULL
import org.partiql.types.StaticType.Companion.STRING
import org.partiql.types.StaticType.Companion.unionOf
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import java.time.Instant
import java.util.stream.Stream
import kotlin.io.path.pathString
import kotlin.io.path.toPath
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PartiQLSchemaInferencerTests {

    @ParameterizedTest
    @ArgumentsSource(TestProvider::class)
    fun test(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("collections")
    @Execution(ExecutionMode.CONCURRENT)
    fun testCollections(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("selectStar")
    @Execution(ExecutionMode.CONCURRENT)
    fun testSelectStar(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("sessionVariables")
    @Execution(ExecutionMode.CONCURRENT)
    fun testSessionVariables(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("bitwiseAnd")
    @Execution(ExecutionMode.CONCURRENT)
    fun testBitwiseAnd(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("unpivotCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testUnpivot(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("joinCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testJoins(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("excludeCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testExclude(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("orderByCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testOrderBy(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("tupleUnionCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testTupleUnion(tc: TestCase) = runTest(tc)

    companion object {

        private val root = this::class.java.getResource("/catalogs")!!.toURI().toPath().pathString

        private val PLUGINS = listOf(LocalPlugin())

        private const val USER_ID = "TEST_USER"

        private val catalogConfig = mapOf(
            "aws" to ionStructOf(
                field("connector_name", ionString("local")),
                field("root", ionString("$root/aws")),
            ),
            "b" to ionStructOf(
                field("connector_name", ionString("local")),
                field("root", ionString("$root/b")),
            ),
            "db" to ionStructOf(
                field("connector_name", ionString("local")),
                field("root", ionString("$root/db")),
            ),
        )

        const val CATALOG_AWS = "aws"
        const val CATALOG_B = "b"
        const val CATALOG_DB = "db"
        val DB_SCHEMA_MARKETS = listOf("markets")

        val TYPE_BOOL = StaticType.BOOL
        private val TYPE_AWS_DDB_PETS_ID = INT
        private val TYPE_AWS_DDB_PETS_BREED = STRING
        val TABLE_AWS_DDB_PETS = BagType(
            elementType = StructType(
                fields = mapOf(
                    "id" to TYPE_AWS_DDB_PETS_ID,
                    "breed" to TYPE_AWS_DDB_PETS_BREED
                ),
                contentClosed = true,
                constraints = setOf(
                    TupleConstraint.Open(false),
                    TupleConstraint.UniqueAttrs(true),
                    TupleConstraint.Ordered
                )
            )
        )
        val TABLE_AWS_DDB_PETS_LIST = ListType(
            elementType = StructType(
                fields = mapOf(
                    "id" to TYPE_AWS_DDB_PETS_ID,
                    "breed" to TYPE_AWS_DDB_PETS_BREED
                ),
                contentClosed = true,
                constraints = setOf(
                    TupleConstraint.Open(false),
                    TupleConstraint.UniqueAttrs(true),
                    TupleConstraint.Ordered
                )
            )
        )
        val TABLE_AWS_DDB_B = BagType(
            StructType(
                fields = mapOf("identifier" to STRING),
                contentClosed = true,
                constraints = setOf(
                    TupleConstraint.Open(false),
                    TupleConstraint.UniqueAttrs(true),
                    TupleConstraint.Ordered
                )
            )
        )
        val TABLE_AWS_B_B = BagType(
            StructType(
                fields = mapOf("identifier" to INT),
                contentClosed = true,
                constraints = setOf(
                    TupleConstraint.Open(false),
                    TupleConstraint.UniqueAttrs(true),
                    TupleConstraint.Ordered
                )
            )
        )
        val TYPE_B_B_B_B_B = INT
        private val TYPE_B_B_B_B = StructType(
            mapOf("b" to TYPE_B_B_B_B_B),
            contentClosed = true,
            constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
        )
        val TYPE_B_B_B_C = INT
        val TYPE_B_B_C = INT
        val TYPE_B_B_B =
            StructType(
                fields = mapOf(
                    "b" to TYPE_B_B_B_B,
                    "c" to TYPE_B_B_B_C
                ),
                contentClosed = true,
                constraints = setOf(
                    TupleConstraint.Open(false),
                    TupleConstraint.UniqueAttrs(true),
                    TupleConstraint.Ordered
                )
            )

        private fun assertProblemExists(problem: () -> Problem) = ProblemHandler { problems, ignoreSourceLocation ->
            when (ignoreSourceLocation) {
                true -> assertTrue("Expected to find ${problem.invoke()} in $problems") { problems.any { it.details == problem.invoke().details } }
                false -> assertTrue("Expected to find ${problem.invoke()} in $problems") { problems.any { it == problem.invoke() } }
            }
        }

        // Tests

        @JvmStatic
        fun collections() = listOf<TestCase>(
            SuccessTestCase(
                name = "Collection BAG<INT>",
                query = "<< 1, 2, 3 >>",
                expected = BagType(INT),
            ),
            SuccessTestCase(
                name = "Collection LIST<INT>",
                query = "[ 1, 2, 3 ]",
                expected = ListType(INT),
            ),
            SuccessTestCase(
                name = "Collection LIST<INT>",
                query = "( 1, 2, 3 )",
                expected = ListType(INT),
            ),
            SuccessTestCase(
                name = "Collection SEXP<INT>",
                query = "SEXP ( 1, 2, 3 )",
                expected = SexpType(INT),
            ),
            SuccessTestCase(
                name = "SELECT from array",
                query = "SELECT VALUE x FROM [ 1, 2, 3 ] as x",
                expected = BagType(INT),
            ),
            SuccessTestCase(
                name = "SELECT from array",
                query = "SELECT x FROM [ 1, 2, 3 ] as x",
                expected = BagType(
                    StructType(
                        fields = listOf(StructType.Field("x", INT)),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
        )

        @JvmStatic
        fun structs() = listOf<TestCase>()

        @JvmStatic
        fun selectStar() = listOf<TestCase>(
            SuccessTestCase(
                name = "Test #8",
                catalog = CATALOG_AWS,
                query = "SELECT * FROM ddb.pets",
                expected = TABLE_AWS_DDB_PETS
            ),
            SuccessTestCase(
                name = "Test #9",
                catalog = CATALOG_AWS,
                query = "SELECT * FROM b.b",
                expected = TABLE_AWS_B_B
            ),
        )

        @JvmStatic
        fun sessionVariables() = listOf(
            SuccessTestCase(
                name = "Current User",
                query = "CURRENT_USER",
                expected = unionOf(STRING, StaticType.NULL)
            ),
            SuccessTestCase(
                name = "Current User Concat",
                query = "CURRENT_USER || 'hello'",
                expected = unionOf(STRING, StaticType.NULL)
            ),
            SuccessTestCase(
                name = "Current User in WHERE",
                query = "SELECT VALUE a FROM [ 0 ] AS a WHERE CURRENT_USER = 'hello'",
                expected = BagType(INT)
            ),
            SuccessTestCase(
                name = "Current User in WHERE",
                query = "SELECT VALUE a FROM [ 0 ] AS a WHERE CURRENT_USER = 5",
                expected = BagType(INT),
            ),
            ErrorTestCase(
                name = "Current User (String) PLUS String",
                query = "CURRENT_USER + 'hello'",
                expected = StaticType.MISSING,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.UnknownFunction(
                            "plus",
                            listOf(
                                unionOf(STRING, StaticType.NULL),
                                STRING,
                            ),
                        )
                    )
                }
            ),
        )

        @JvmStatic
        fun bitwiseAnd() = listOf(
            SuccessTestCase(
                name = "BITWISE_AND_1",
                query = "1 & 2",
                expected = INT
            ),
            SuccessTestCase(
                name = "BITWISE_AND_2",
                query = "CAST(1 AS INT2) & CAST(2 AS INT2)",
                expected = StaticType.unionOf(StaticType.INT2, MISSING)
            ),
            SuccessTestCase(
                name = "BITWISE_AND_3",
                query = "CAST(1 AS INT4) & CAST(2 AS INT4)",
                expected = StaticType.unionOf(StaticType.INT4, MISSING)
            ),
            SuccessTestCase(
                name = "BITWISE_AND_4",
                query = "CAST(1 AS INT8) & CAST(2 AS INT8)",
                expected = StaticType.unionOf(StaticType.INT8, MISSING)
            ),
            SuccessTestCase(
                name = "BITWISE_AND_5",
                query = "CAST(1 AS INT2) & CAST(2 AS INT4)",
                expected = StaticType.unionOf(StaticType.INT4, MISSING)
            ),
            SuccessTestCase(
                name = "BITWISE_AND_6",
                query = "CAST(1 AS INT2) & CAST(2 AS INT8)",
                expected = StaticType.unionOf(StaticType.INT8, MISSING)
            ),
            SuccessTestCase(
                name = "BITWISE_AND_7",
                query = "CAST(1 AS INT2) & 2",
                expected = StaticType.unionOf(INT, MISSING)
            ),
            SuccessTestCase(
                name = "BITWISE_AND_8",
                query = "CAST(1 AS INT4) & CAST(2 AS INT8)",
                expected = StaticType.unionOf(StaticType.INT8, MISSING)
            ),
            SuccessTestCase(
                name = "BITWISE_AND_9",
                query = "CAST(1 AS INT4) & 2",
                expected = StaticType.unionOf(INT, MISSING)
            ),
            SuccessTestCase(
                name = "BITWISE_AND_10",
                query = "CAST(1 AS INT8) & 2",
                expected = StaticType.unionOf(INT, MISSING)
            ),
            SuccessTestCase(
                name = "BITWISE_AND_NULL_OPERAND",
                query = "1 & NULL",
                expected = StaticType.NULL,
            ),
            ErrorTestCase(
                name = "BITWISE_AND_MISSING_OPERAND",
                query = "1 & MISSING",
                expected = StaticType.MISSING,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.ExpressionAlwaysReturnsNullOrMissing
                    )
                }
            ),
            ErrorTestCase(
                name = "BITWISE_AND_NON_INT_OPERAND",
                query = "1 & 'NOT AN INT'",
                expected = StaticType.MISSING,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.UnknownFunction("bitwise_and", listOf(INT, STRING))
                    )
                }
            ),
        )

        @JvmStatic
        fun unpivotCases() = listOf(
            SuccessTestCase(
                name = "UNPIVOT",
                query = "SELECT VALUE v FROM UNPIVOT { 'a': 2 } AS v AT attr WHERE attr = 'a'",
                expected = BagType(INT)
            ),
        )

        @JvmStatic
        fun joinCases() = listOf(
            SuccessTestCase(
                name = "CROSS JOIN",
                query = "SELECT * FROM <<{ 'a': 1 }>> AS t1, <<{ 'b': 2.0 }>> AS t2",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to INT,
                            "b" to StaticType.DECIMAL,
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "LEFT JOIN",
                query = "SELECT * FROM <<{ 'a': 1 }>> AS t1 LEFT JOIN <<{ 'b': 2.0 }>> AS t2 ON TRUE",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to INT,
                            "b" to unionOf(NULL, DECIMAL),
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "LEFT JOIN",
                query = "SELECT b, a FROM <<{ 'a': 1 }>> AS t1 LEFT JOIN <<{ 'b': 2.0 }>> AS t2 ON TRUE",
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("b", unionOf(NULL, DECIMAL)),
                            StructType.Field("a", INT),
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "LEFT JOIN",
                query = "SELECT t1.a, t2.a FROM <<{ 'a': 1 }>> AS t1 LEFT JOIN <<{ 'a': 2.0 }>> AS t2 ON t1.a = t2.a",
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("a", INT),
                            StructType.Field("a", unionOf(NULL, DECIMAL)),
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(false),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "LEFT JOIN ALL",
                query = "SELECT * FROM <<{ 'a': 1 }>> AS t1 LEFT JOIN <<{ 'a': 2.0 }>> AS t2 ON t1.a = t2.a",
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("a", INT),
                            StructType.Field("a", unionOf(NULL, DECIMAL)),
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(false),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "LEFT JOIN ALL",
                query = """
                    SELECT *
                    FROM
                            <<{ 'a': 1 }>> AS t1
                        LEFT JOIN
                            <<{ 'a': 2.0 }>> AS t2
                        ON t1.a = t2.a
                        LEFT JOIN
                            <<{ 'a': 'hello, world' }>> AS t3
                        ON t3.a = 'hello'
                """,
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("a", INT),
                            StructType.Field("a", unionOf(DECIMAL, NULL)),
                            StructType.Field("a", unionOf(STRING, NULL)),
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(false),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            ErrorTestCase(
                name = "LEFT JOIN Ambiguous Reference in ON",
                query = "SELECT * FROM <<{ 'a': 1 }>> AS t1 LEFT JOIN <<{ 'a': 2.0 }>> AS t2 ON a = 3",
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("a", INT),
                            StructType.Field("a", unionOf(DECIMAL, NULL)),
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(false),
                            TupleConstraint.Ordered
                        )
                    )
                ),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.UndefinedVariable("a", false)
                    )
                }
            ),
        )

        @JvmStatic
        fun excludeCases() = listOf(
            SuccessTestCase(
                name = "EXCLUDE SELECT star",
                query = """SELECT * EXCLUDE c.ssn FROM [
                    {
                        'name': 'Alan',
                        'custId': 1,
                        'address': {
                            'city': 'Seattle',
                            'zipcode': 98109,
                            'street': '123 Seaplane Dr.'
                        },
                        'ssn': 123456789
                    }
                ] AS c""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "name" to StaticType.STRING,
                            "custId" to StaticType.INT,
                            "address" to StructType(
                                fields = mapOf(
                                    "city" to StaticType.STRING,
                                    "zipcode" to StaticType.INT,
                                    "street" to StaticType.STRING,
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "EXCLUDE SELECT star multiple paths",
                query = """SELECT * EXCLUDE c.ssn, c.address.street FROM [
                    {
                        'name': 'Alan',
                        'custId': 1,
                        'address': {
                            'city': 'Seattle',
                            'zipcode': 98109,
                            'street': '123 Seaplane Dr.'
                        },
                        'ssn': 123456789
                    }
                ] AS c""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "name" to StaticType.STRING,
                            "custId" to StaticType.INT,
                            "address" to StructType(
                                fields = mapOf(
                                    "city" to StaticType.STRING,
                                    "zipcode" to StaticType.INT
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "EXCLUDE SELECT star list index and list index field",
                query = """SELECT *
                    EXCLUDE
                        t.a.b.c[0],
                        t.a.b.c[1].field
                    FROM [{
                        'a': {
                            'b': {
                                'c': [
                                    {
                                        'field': 0    -- c[0]
                                    },
                                    {
                                        'field': 1    -- c[1]
                                    },
                                    {
                                        'field': 2    -- c[2]
                                    }
                                ]
                            }
                        },
                        'foo': 'bar'
                    }] AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StructType(
                                fields = mapOf(
                                    "b" to StructType(
                                        fields = mapOf(
                                            "c" to ListType(
                                                elementType = StructType(
                                                    fields = mapOf(
                                                        "field" to AnyOfType(
                                                            setOf(
                                                                INT,
                                                                MISSING // c[1]'s `field` was excluded
                                                            )
                                                        )
                                                    ),
                                                    contentClosed = true,
                                                    constraints = setOf(
                                                        TupleConstraint.Open(false),
                                                        TupleConstraint.UniqueAttrs(true)
                                                    )
                                                )
                                            )
                                        ),
                                        contentClosed = true,
                                        constraints = setOf(
                                            TupleConstraint.Open(false),
                                            TupleConstraint.UniqueAttrs(true)
                                        )
                                    )
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            ),
                            "foo" to StaticType.STRING
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "EXCLUDE SELECT star collection index as last step",
                query = """SELECT *
                    EXCLUDE
                        t.a.b.c[0]
                    FROM [{
                        'a': {
                            'b': {
                                'c': [0, 1, 2]
                            }
                        },
                        'foo': 'bar'
                    }] AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StructType(
                                fields = mapOf(
                                    "b" to StructType(
                                        fields = mapOf(
                                            "c" to ListType(
                                                elementType = StaticType.INT
                                            )
                                        ),
                                        contentClosed = true,
                                        constraints = setOf(
                                            TupleConstraint.Open(false),
                                            TupleConstraint.UniqueAttrs(true)
                                        )
                                    )
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            ),
                            "foo" to StaticType.STRING
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            // EXCLUDE regression test (behavior subject to change pending RFC)
            SuccessTestCase(
                name = "EXCLUDE SELECT star collection wildcard as last step",
                query = """SELECT *
                    EXCLUDE
                        t.a[*]
                    FROM [{
                        'a': [0, 1, 2]
                    }] AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to ListType(
                                elementType = StaticType.INT // empty list but still preserve typing information
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "EXCLUDE SELECT star list wildcard",
                query = """SELECT *
                    EXCLUDE
                        t.a.b.c[*].field_x
                    FROM [{
                        'a': {
                            'b': {
                                'c': [
                                    {                    -- c[0]
                                        'field_x': 0, 
                                        'field_y': 0
                                    },
                                    {                    -- c[1]
                                        'field_x': 1,
                                        'field_y': 1
                                    },
                                    {                    -- c[2]
                                        'field_x': 2,
                                        'field_y': 2
                                    }
                                ]
                            }
                        },
                        'foo': 'bar'
                    }] AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StructType(
                                fields = mapOf(
                                    "b" to StructType(
                                        fields = mapOf(
                                            "c" to ListType(
                                                elementType = StructType(
                                                    fields = mapOf(
                                                        "field_y" to StaticType.INT
                                                    ),
                                                    contentClosed = true,
                                                    constraints = setOf(
                                                        TupleConstraint.Open(false),
                                                        TupleConstraint.UniqueAttrs(true)
                                                    )
                                                )
                                            )
                                        ),
                                        contentClosed = true,
                                        constraints = setOf(
                                            TupleConstraint.Open(false),
                                            TupleConstraint.UniqueAttrs(true)
                                        )
                                    )
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            ),
                            "foo" to StaticType.STRING
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "EXCLUDE SELECT star list tuple wildcard",
                query = """SELECT *
                    EXCLUDE
                        t.a.b.c[*].*
                    FROM [{
                        'a': {
                            'b': {
                                'c': [
                                    {                    -- c[0]
                                        'field_x': 0, 
                                        'field_y': 0
                                    },
                                    {                    -- c[1]
                                        'field_x': 1,
                                        'field_y': 1
                                    },
                                    {                    -- c[2]
                                        'field_x': 2,
                                        'field_y': 2
                                    }
                                ]
                            }
                        },
                        'foo': 'bar'
                    }] AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StructType(
                                fields = mapOf(
                                    "b" to StructType(
                                        fields = mapOf(
                                            "c" to ListType(
                                                elementType = StructType(
                                                    fields = mapOf(
                                                        // all fields gone
                                                    ),
                                                    contentClosed = true,
                                                    constraints = setOf(
                                                        TupleConstraint.Open(false),
                                                        TupleConstraint.UniqueAttrs(true)
                                                    )
                                                )
                                            )
                                        ),
                                        contentClosed = true,
                                        constraints = setOf(
                                            TupleConstraint.Open(false),
                                            TupleConstraint.UniqueAttrs(true)
                                        )
                                    )
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            ),
                            "foo" to StaticType.STRING
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "EXCLUDE SELECT star order by",
                query = """SELECT *
                    EXCLUDE
                        t.a
                    FROM [
                        {
                            'a': 2,
                            'foo': 'bar2'
                        },
                        {
                            'a': 1,
                            'foo': 'bar1'
                        },
                        {
                            'a': 3,
                            'foo': 'bar3'
                        }
                    ] AS t
                    ORDER BY t.a""",
                expected = ListType(
                    StructType(
                        fields = mapOf(
                            "foo" to StaticType.STRING
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "EXCLUDE SELECT star with JOINs",
                query = """SELECT *
                    EXCLUDE bar.d
                    FROM 
                    <<
                        {'a': 1, 'b': 11}, 
                        {'a': 2, 'b': 22}
                    >> AS foo,
                    <<
                        {'c': 3, 'd': 33},
                        {'c': 4, 'd': 44}
                    >> AS bar""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StaticType.INT,
                            "b" to StaticType.INT,
                            "c" to StaticType.INT
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "SELECT t.b EXCLUDE ex 1",
                query = """SELECT t.b EXCLUDE t.b[*].b_1
                    FROM <<
                    {
                        'a': {'a_1':1,'a_2':2},
                        'b': [ {'b_1':3,'b_2':4}, {'b_1':5,'b_2':6} ],
                        'c': 7,
                        'd': 8
                    } >> AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "b" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b_2" to StaticType.INT
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                )
                            ),
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "SELECT * EXCLUDE ex 2",
                query = """SELECT * EXCLUDE t.b[*].b_1
                    FROM <<
                    {
                        'a': {'a_1':1,'a_2':2},
                        'b': [ {'b_1':3,'b_2':4}, {'b_1':5,'b_2':6} ],
                        'c': 7,
                        'd': 8
                    } >> AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StructType(
                                fields = mapOf(
                                    "a_1" to StaticType.INT,
                                    "a_2" to StaticType.INT
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            ),
                            "b" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b_2" to StaticType.INT
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                )
                            ),
                            "c" to StaticType.INT,
                            "d" to StaticType.INT
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "SELECT VALUE t.b EXCLUDE",
                query = """SELECT VALUE t.b EXCLUDE t.b[*].b_1
                    FROM <<
                    {
                        'a': {'a_1':1,'a_2':2},
                        'b': [ {'b_1':3,'b_2':4}, {'b_1':5,'b_2':6} ],
                        'c': 7,
                        'd': 8
                    } >> AS t""",
                expected = BagType(
                    ListType(
                        elementType = StructType(
                            fields = mapOf(
                                "b_2" to StaticType.INT
                            ),
                            contentClosed = true,
                            constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                        )
                    ),
                )
            ),
            SuccessTestCase(
                name = "SELECT * EXCLUDE collection wildcard and nested tuple attr",
                query = """SELECT * EXCLUDE t.a[*].b.c
                    FROM <<
                        {
                            'a': [
                                { 'b': { 'c': 0, 'd': 'zero' } },
                                { 'b': { 'c': 1, 'd': 'one' } },
                                { 'b': { 'c': 2, 'd': 'two' } }
                            ]
                        }
                    >> AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b" to StructType(
                                            fields = mapOf(
                                                "d" to StaticType.STRING
                                            ),
                                            contentClosed = true,
                                            constraints = setOf(
                                                TupleConstraint.Open(false),
                                                TupleConstraint.UniqueAttrs(true)
                                            )
                                        ),
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                ),
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "SELECT * EXCLUDE collection index and nested tuple attr",
                query = """SELECT * EXCLUDE t.a[1].b.c
                    FROM <<
                        {
                            'a': [
                                { 'b': { 'c': 0, 'd': 'zero' } },
                                { 'b': { 'c': 1, 'd': 'one' } },
                                { 'b': { 'c': 2, 'd': 'two' } }
                            ]
                        }
                    >> AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b" to StructType(
                                            fields = mapOf(
                                                "c" to StaticType.INT.asOptional(),
                                                "d" to StaticType.STRING
                                            ),
                                            contentClosed = true,
                                            constraints = setOf(
                                                TupleConstraint.Open(false),
                                                TupleConstraint.UniqueAttrs(true)
                                            )
                                        ),
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                ),
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "SELECT * EXCLUDE collection wildcard and nested tuple wildcard",
                query = """SELECT * EXCLUDE t.a[*].b.*
                    FROM <<
                        {
                            'a': [
                                { 'b': { 'c': 0, 'd': 'zero' } },
                                { 'b': { 'c': 1, 'd': 'one' } },
                                { 'b': { 'c': 2, 'd': 'two' } }
                            ]
                        }
                    >> AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b" to StructType(
                                            fields = mapOf(), // empty map; all fields of b excluded
                                            contentClosed = true,
                                            constraints = setOf(
                                                TupleConstraint.Open(false),
                                                TupleConstraint.UniqueAttrs(true)
                                            )
                                        ),
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                ),
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "SELECT * EXCLUDE collection index and nested tuple wildcard",
                query = """SELECT * EXCLUDE t.a[1].b.*
                    FROM <<
                        {
                            'a': [
                                { 'b': { 'c': 0, 'd': 'zero' } },
                                { 'b': { 'c': 1, 'd': 'one' } },
                                { 'b': { 'c': 2, 'd': 'two' } }
                            ]
                        }
                    >> AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b" to StructType(
                                            fields = mapOf( // all fields of b optional
                                                "c" to StaticType.INT.asOptional(),
                                                "d" to StaticType.STRING.asOptional()
                                            ),
                                            contentClosed = true,
                                            constraints = setOf(
                                                TupleConstraint.Open(false),
                                                TupleConstraint.UniqueAttrs(true)
                                            )
                                        ),
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                ),
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "SELECT * EXCLUDE collection wildcard and nested collection wildcard",
                query = """SELECT * EXCLUDE t.a[*].b.d[*].e
                    FROM <<
                        {
                            'a': [
                                { 'b': { 'c': 0, 'd': [{'e': 'zero', 'f': true}] } },
                                { 'b': { 'c': 1, 'd': [{'e': 'one', 'f': true}] } },
                                { 'b': { 'c': 2, 'd': [{'e': 'two', 'f': true}] } }
                            ]
                        }
                    >> AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b" to StructType(
                                            fields = mapOf(
                                                "c" to StaticType.INT,
                                                "d" to ListType(
                                                    elementType = StructType(
                                                        fields = mapOf(
                                                            "f" to StaticType.BOOL
                                                        ),
                                                        contentClosed = true,
                                                        constraints = setOf(
                                                            TupleConstraint.Open(false),
                                                            TupleConstraint.UniqueAttrs(true)
                                                        )
                                                    )
                                                )
                                            ),
                                            contentClosed = true,
                                            constraints = setOf(
                                                TupleConstraint.Open(false),
                                                TupleConstraint.UniqueAttrs(true)
                                            )
                                        ),
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                ),
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "SELECT * EXCLUDE collection index and nested collection wildcard",
                query = """SELECT * EXCLUDE t.a[1].b.d[*].e
                    FROM <<
                        {
                            'a': [
                                { 'b': { 'c': 0, 'd': [{'e': 'zero', 'f': true}] } },
                                { 'b': { 'c': 1, 'd': [{'e': 'one', 'f': true}] } },
                                { 'b': { 'c': 2, 'd': [{'e': 'two', 'f': true}] } }
                            ]
                        }
                    >> AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b" to StructType(
                                            fields = mapOf(
                                                "c" to StaticType.INT,
                                                "d" to ListType(
                                                    elementType = StructType(
                                                        fields = mapOf(
                                                            "e" to StaticType.STRING.asOptional(), // last step is optional since only a[1]... is excluded
                                                            "f" to StaticType.BOOL
                                                        ),
                                                        contentClosed = true,
                                                        constraints = setOf(
                                                            TupleConstraint.Open(false),
                                                            TupleConstraint.UniqueAttrs(true)
                                                        )
                                                    )
                                                )
                                            ),
                                            contentClosed = true,
                                            constraints = setOf(
                                                TupleConstraint.Open(false),
                                                TupleConstraint.UniqueAttrs(true)
                                            )
                                        ),
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                ),
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "SELECT * EXCLUDE collection index and nested collection index",
                query = """SELECT * EXCLUDE t.a[1].b.d[0].e
                    FROM <<
                        {
                            'a': [
                                { 'b': { 'c': 0, 'd': [{'e': 'zero', 'f': true}] } },
                                { 'b': { 'c': 1, 'd': [{'e': 'one', 'f': true}] } },
                                { 'b': { 'c': 2, 'd': [{'e': 'two', 'f': true}] } }
                            ]
                        }
                    >> AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b" to StructType(
                                            fields = mapOf(
                                                "c" to StaticType.INT,
                                                "d" to ListType(
                                                    elementType = StructType(
                                                        fields = mapOf( // same as above
                                                            "e" to StaticType.STRING.asOptional(),
                                                            "f" to StaticType.BOOL
                                                        ),
                                                        contentClosed = true,
                                                        constraints = setOf(
                                                            TupleConstraint.Open(false),
                                                            TupleConstraint.UniqueAttrs(true)
                                                        )
                                                    )
                                                )
                                            ),
                                            contentClosed = true,
                                            constraints = setOf(
                                                TupleConstraint.Open(false),
                                                TupleConstraint.UniqueAttrs(true)
                                            )
                                        ),
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                ),
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "EXCLUDE case sensitive lookup",
                query = """SELECT * EXCLUDE t."a".b['c']
                    FROM <<
                        {
                            'a': {
                                'B': {
                                    'c': 0,
                                    'd': 'foo'
                                }
                            }
                        }
                    >> AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StructType(
                                fields = mapOf(
                                    "B" to StructType(
                                        fields = mapOf(
                                            "d" to StaticType.STRING
                                        ),
                                        contentClosed = true,
                                        constraints = setOf(
                                            TupleConstraint.Open(false),
                                            TupleConstraint.UniqueAttrs(true)
                                        )
                                    ),
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            ),
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "EXCLUDE case sensitive lookup with capitalized and uncapitalized attr",
                query = """SELECT * EXCLUDE t."a".b['c']
                    FROM <<
                        {
                            'a': {
                                'B': {
                                    'c': 0,
                                    'C': true,
                                    'd': 'foo'
                                }
                            }
                        }
                    >> AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StructType(
                                fields = mapOf(
                                    "B" to StructType(
                                        fields = mapOf(
                                            "C" to StaticType.BOOL, // keep 'C'
                                            "d" to StaticType.STRING
                                        ),
                                        contentClosed = true,
                                        constraints = setOf(
                                            TupleConstraint.Open(false),
                                            TupleConstraint.UniqueAttrs(true)
                                        )
                                    ),
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            ),
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "EXCLUDE case sensitive lookup with both capitalized and uncapitalized removed",
                query = """SELECT * EXCLUDE t."a".b.c
                    FROM <<
                        {
                            'a': {
                                'B': {          -- both 'c' and 'C' to be removed
                                    'c': 0,
                                    'C': true,
                                    'd': 'foo'
                                }
                            }
                        }
                    >> AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StructType(
                                fields = mapOf(
                                    "B" to StructType(
                                        fields = mapOf(
                                            "d" to StaticType.STRING
                                        ),
                                        contentClosed = true,
                                        constraints = setOf(
                                            TupleConstraint.Open(false),
                                            TupleConstraint.UniqueAttrs(true)
                                        )
                                    ),
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            ),
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "EXCLUDE with both duplicates",
                query = """SELECT * EXCLUDE t."a".b.c
                    FROM <<
                        {
                            'a': {
                                'B': {
                                    'c': 0,
                                    'c': true,
                                    'd': 'foo'
                                }
                            }
                        }
                    >> AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StructType(
                                fields = mapOf(
                                    "B" to StructType(
                                        fields = mapOf(
                                            // both "c" removed
                                            "d" to StaticType.STRING
                                        ),
                                        contentClosed = true,
                                        constraints = setOf(
                                            TupleConstraint.Open(false),
                                            TupleConstraint.UniqueAttrs(false)
                                        ) // UniqueAttrs set to false
                                    ),
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            ),
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            // EXCLUDE regression test (behavior subject to change pending RFC)
            SuccessTestCase(
                name = "EXCLUDE with removed attribute later referenced",
                query = "SELECT * EXCLUDE t.a, t.a.b FROM << { 'a': { 'b': 1 }, 'c': 2 } >> AS t",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "c" to StaticType.INT
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            // EXCLUDE regression test (behavior subject to change pending RFC)
            SuccessTestCase(
                name = "EXCLUDE with non-existent attribute reference",
                query = "SELECT * EXCLUDE t.attr_does_not_exist FROM << { 'a': 1 } >> AS t",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StaticType.INT
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            // EXCLUDE regression test (behavior subject to change pending RFC); could give error/warning
            SuccessTestCase(
                name = "exclude union of types",
                query = """SELECT t EXCLUDE t.a.b
                    FROM <<
                        {
                            'a': {
                                'b': 1,    -- `b` to be excluded
                                'c': 'foo'
                            }
                        },
                        {
                            'a': NULL
                        }
                    >> AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "t" to StaticType.unionOf(
                                StructType(
                                    fields = mapOf(
                                        "a" to StructType(
                                            fields = mapOf(
                                                "c" to StaticType.STRING
                                            ),
                                            contentClosed = true,
                                            constraints = setOf(
                                                TupleConstraint.Open(false),
                                                TupleConstraint.UniqueAttrs(true)
                                            )
                                        )
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                ),
                                StructType(
                                    fields = mapOf(
                                        "a" to StaticType.NULL
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                ),
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "exclude union of types exclude same type",
                query = """SELECT t EXCLUDE t.a.b
                    FROM <<
                        {
                            'a': {
                                'b': 1,    -- `b` to be excluded
                                'c': 'foo'
                            }
                        },
                        {
                            'a': {
                                'b': 1,    -- `b` to be excluded
                                'c': NULL
                            }
                        }
                    >> AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "t" to StaticType.unionOf(
                                StructType(
                                    fields = mapOf(
                                        "a" to StructType(
                                            fields = mapOf(
                                                "c" to StaticType.STRING
                                            ),
                                            contentClosed = true,
                                            constraints = setOf(
                                                TupleConstraint.Open(false),
                                                TupleConstraint.UniqueAttrs(true)
                                            )
                                        )
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                ),
                                StructType(
                                    fields = mapOf(
                                        "a" to StructType(
                                            fields = mapOf(
                                                "c" to StaticType.NULL
                                            ),
                                            contentClosed = true,
                                            constraints = setOf(
                                                TupleConstraint.Open(false),
                                                TupleConstraint.UniqueAttrs(true)
                                            )
                                        )
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                ),
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "exclude union of types exclude different type",
                query = """SELECT t EXCLUDE t.a.c
                    FROM <<
                        {
                            'a': {
                                'b': 1,
                                'c': 'foo'  -- `c` to be excluded
                            }
                        },
                        {
                            'a': {
                                'b': 1,
                                'c': NULL   -- `c` to be excluded
                            }
                        }
                    >> AS t""",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "t" to StructType( // union gone
                                fields = mapOf(
                                    "a" to StructType(
                                        fields = mapOf(
                                            "b" to StaticType.INT
                                        ),
                                        contentClosed = true,
                                        constraints = setOf(
                                            TupleConstraint.Open(false),
                                            TupleConstraint.UniqueAttrs(true)
                                        )
                                    )
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            // EXCLUDE regression test (behavior subject to change pending RFC); could give error/warning
            SuccessTestCase(
                name = "invalid exclude collection wildcard",
                query = """SELECT * EXCLUDE t.a[*]
                    FROM <<
                        {
                            'a': {
                                'b': {
                                    'c': 0,
                                    'd': 'foo'
                                }
                            }
                        }
                    >> AS t""",
                expected = BagType(
                    elementType = StructType(
                        fields = mapOf(
                            "a" to StructType(
                                fields = mapOf(
                                    "b" to StructType(
                                        fields = mapOf(
                                            "c" to StaticType.INT,
                                            "d" to StaticType.STRING
                                        ),
                                        contentClosed = true,
                                        constraints = setOf(
                                            TupleConstraint.Open(false),
                                            TupleConstraint.UniqueAttrs(true)
                                        )
                                    )
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            // EXCLUDE regression test (behavior subject to change pending RFC); could give error/warning
            SuccessTestCase(
                name = "invalid exclude collection index",
                query = """SELECT * EXCLUDE t.a[1]
                    FROM <<
                        {
                            'a': {
                                'b': {
                                    'c': 0,
                                    'd': 'foo'
                                }
                            }
                        }
                    >> AS t""",
                expected = BagType(
                    elementType = StructType(
                        fields = mapOf(
                            "a" to StructType(
                                fields = mapOf(
                                    "b" to StructType(
                                        fields = mapOf(
                                            "c" to StaticType.INT,
                                            "d" to StaticType.STRING
                                        ),
                                        contentClosed = true,
                                        constraints = setOf(
                                            TupleConstraint.Open(false),
                                            TupleConstraint.UniqueAttrs(true)
                                        )
                                    )
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            // EXCLUDE regression test (behavior subject to change pending RFC); could give error/warning
            SuccessTestCase(
                name = "invalid exclude tuple attr",
                query = """SELECT * EXCLUDE t.a.b
                    FROM <<
                        {
                            'a': [
                                { 'b': 0 },
                                { 'b': 1 },
                                { 'b': 2 }
                            ]
                        }
                    >> AS t""",
                expected = BagType(
                    elementType = StructType(
                        fields = mapOf(
                            "a" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b" to StaticType.INT
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                )
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            // EXCLUDE regression test (behavior subject to change pending RFC); could give error/warning
            SuccessTestCase(
                name = "invalid exclude tuple wildcard",
                query = """SELECT * EXCLUDE t.a.*
                    FROM <<
                        {
                            'a': [
                                { 'b': 0 },
                                { 'b': 1 },
                                { 'b': 2 }
                            ]
                        }
                    >> AS t""",
                expected = BagType(
                    elementType = StructType(
                        fields = mapOf(
                            "a" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b" to StaticType.INT
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                )
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            // EXCLUDE regression test (behavior subject to change pending RFC); could give error/warning
            SuccessTestCase(
                name = "invalid exclude tuple attr step",
                query = """SELECT * EXCLUDE t.b   -- `t.b` does not exist
                    FROM <<
                        {
                            'a': <<
                                { 'b': 0 },
                                { 'b': 1 },
                                { 'b': 2 }
                            >>
                        }
                    >> AS t""",
                expected = BagType(
                    elementType = StructType(
                        fields = mapOf(
                            "a" to BagType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b" to StaticType.INT
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                )
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            // EXCLUDE regression test (behavior subject to change pending RFC); could give error/warning
            // ErrorTestCase(
            //     name = "invalid exclude root",
            //     query = """SELECT * EXCLUDE nonsense.b   -- `nonsense` does not exist in binding tuples
            //         FROM <<
            //             {
            //                 'a': <<
            //                     { 'b': 0 },
            //                     { 'b': 1 },
            //                     { 'b': 2 }
            //                 >>
            //             }
            //         >> AS t""",
            //     expected = BagType(
            //         elementType = StructType(
            //             fields = mapOf(
            //                 "a" to BagType(
            //                     elementType = StructType(
            //                         fields = mapOf(
            //                             "b" to StaticType.INT
            //                         ),
            //                         contentClosed = true,
            //                         constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
            //                     )
            //                 )
            //             ),
            //             contentClosed = true,
            //             constraints = setOf(
            //                 TupleConstraint.Open(false),
            //                 TupleConstraint.UniqueAttrs(true),
            //                 TupleConstraint.Ordered
            //             )
            //         )
            //     ),
            //     problemHandler = assertProblemExists {
            //         Problem(
            //             UNKNOWN_PROBLEM_LOCATION,
            //             PlanningProblemDetails.UnresolvedExcludeExprRoot("nonsense")
            //         )
            //     }
            // ),
            // EXCLUDE regression test (behavior subject to change pending RFC); could give error/warning
            SuccessTestCase(
                name = "exclude with unions and last step collection index",
                query = """SELECT * EXCLUDE t.a[0].c    -- `c`'s type to be unioned with `MISSING`
                    FROM <<
                        {
                            'a': [
                                {
                                    'b': 0,
                                    'c': 0
                                },
                                {
                                    'b': 1,
                                    'c': NULL
                                },
                                {
                                    'b': 2,
                                    'c': 0.1
                                }
                            ]
                        }
                    >> AS t""",
                expected = BagType(
                    elementType = StructType(
                        fields = mapOf(
                            "a" to ListType(
                                elementType = StaticType.unionOf(
                                    StructType(
                                        fields = mapOf(
                                            "b" to StaticType.INT,
                                            "c" to StaticType.INT.asOptional()
                                        ),
                                        contentClosed = true,
                                        constraints = setOf(
                                            TupleConstraint.Open(false),
                                            TupleConstraint.UniqueAttrs(true)
                                        )
                                    ),
                                    StructType(
                                        fields = mapOf(
                                            "b" to StaticType.INT,
                                            "c" to StaticType.NULL.asOptional()
                                        ),
                                        contentClosed = true,
                                        constraints = setOf(
                                            TupleConstraint.Open(false),
                                            TupleConstraint.UniqueAttrs(true)
                                        )
                                    ),
                                    StructType(
                                        fields = mapOf(
                                            "b" to StaticType.INT,
                                            "c" to StaticType.DECIMAL.asOptional()
                                        ),
                                        contentClosed = true,
                                        constraints = setOf(
                                            TupleConstraint.Open(false),
                                            TupleConstraint.UniqueAttrs(true)
                                        )
                                    )
                                )
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "EXCLUDE using a catalog",
                catalog = CATALOG_B,
                query = "SELECT * EXCLUDE t.c FROM b.b.b AS t",
                expected = BagType(
                    elementType = StructType(
                        fields = mapOf(
                            "b" to StructType(
                                fields = mapOf(
                                    "b" to StaticType.INT
                                ),
                                contentClosed = true,
                                constraints = setOf(
                                    TupleConstraint.Open(false),
                                    TupleConstraint.UniqueAttrs(true),
                                    TupleConstraint.Ordered
                                )
                            ),
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
        )

        @JvmStatic
        fun orderByCases() = listOf(
            SuccessTestCase(
                name = "ORDER BY int",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM pets ORDER BY id",
                expected = TABLE_AWS_DDB_PETS_LIST
            ),
            SuccessTestCase(
                name = "ORDER BY str",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM pets ORDER BY breed",
                expected = TABLE_AWS_DDB_PETS_LIST
            ),
            ErrorTestCase(
                name = "ORDER BY str",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM pets ORDER BY unknown_col",
                expected = TABLE_AWS_DDB_PETS_LIST,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.UndefinedVariable("unknown_col", false)
                    )
                }
            ),
        )

        @JvmStatic
        fun tupleUnionCases() = listOf(
            SuccessTestCase(
                name = "Empty Tuple Union",
                query = "TUPLEUNION()",
                expected = StructType(
                    fields = emptyMap(),
                    contentClosed = true,
                    constraints = setOf(
                        TupleConstraint.Open(false),
                        TupleConstraint.UniqueAttrs(true),
                        TupleConstraint.Ordered
                    )
                )
            ),
            SuccessTestCase(
                name = "Tuple Union with Literal Struct",
                query = "TUPLEUNION({ 'a': 1, 'b': 'hello' })",
                expected = StructType(
                    fields = mapOf(
                        "a" to IntType(),
                        "b" to StringType()
                    ),
                    contentClosed = true,
                    constraints = setOf(
                        TupleConstraint.Open(false),
                        TupleConstraint.UniqueAttrs(true),
                    )
                ),
            ),
            SuccessTestCase(
                name = "Tuple Union with Literal Struct AND Duplicates",
                query = "TUPLEUNION({ 'a': 1, 'a': 'hello' })",
                expected = StructType(
                    fields = listOf(
                        StructType.Field("a", INT),
                        StructType.Field("a", STRING),
                    ),
                    contentClosed = true,
                    constraints = setOf(
                        TupleConstraint.Open(false),
                        TupleConstraint.UniqueAttrs(false),
                    )
                ),
            ),
            SuccessTestCase(
                name = "Tuple Union with Nested Struct",
                query = """
                    SELECT VALUE TUPLEUNION(
                      t.a
                    ) FROM <<
                        { 'a': { 'b': 1 } }
                    >> AS t
                """,
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("b", INT),
                        ),
                        contentClosed = true,
                        // TODO: This shouldn't be ordered. However, this doesn't come from the TUPLEUNION. It is
                        //  coming from the RexOpSelect.
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                ),
            ),
            SuccessTestCase(
                name = "Tuple Union with Heterogeneous Data",
                query = """
                    SELECT VALUE TUPLEUNION(
                      t.a
                    ) FROM <<
                        { 'a': { 'b': 1 } },
                        { 'a': 1 }
                    >> AS t
                """,
                expected = BagType(
                    unionOf(
                        MISSING,
                        StructType(
                            fields = listOf(
                                StructType.Field("b", INT),
                            ),
                            contentClosed = true,
                            constraints = setOf(
                                TupleConstraint.Open(false),
                                TupleConstraint.UniqueAttrs(true),
                            )
                        )
                    )
                ),
            ),
            SuccessTestCase(
                name = "Tuple Union with Heterogeneous Data (2)",
                query = """
                    SELECT VALUE TUPLEUNION(
                      t.a
                    ) FROM <<
                        { 'a': { 'b': 1 } },
                        { 'a': { 'b': 'hello' } },
                        { 'a': NULL },
                        { 'a': 4.5 },
                        { }
                    >> AS t
                """,
                expected = BagType(
                    unionOf(
                        NULL,
                        MISSING,
                        StructType(
                            fields = listOf(
                                StructType.Field("b", INT),
                            ),
                            contentClosed = true,
                            constraints = setOf(
                                TupleConstraint.Open(false),
                                TupleConstraint.UniqueAttrs(true),
                            )
                        ),
                        StructType(
                            fields = listOf(
                                StructType.Field("b", STRING),
                            ),
                            contentClosed = true,
                            constraints = setOf(
                                TupleConstraint.Open(false),
                                TupleConstraint.UniqueAttrs(true),
                            )
                        )
                    )
                ),
            ),
            SuccessTestCase(
                name = "Tuple Union with Heterogeneous Data (3)",
                query = """
                    SELECT VALUE TUPLEUNION(
                      p.name
                    ) FROM aws.ddb.persons AS p
                """,
                expected = BagType(
                    unionOf(
                        MISSING,
                        StructType(
                            fields = listOf(
                                StructType.Field("first", STRING),
                                StructType.Field("last", STRING),
                            ),
                            contentClosed = false,
                            constraints = setOf(
                                TupleConstraint.Open(true),
                                TupleConstraint.UniqueAttrs(false),
                            )
                        ),
                        StructType(
                            fields = listOf(
                                StructType.Field("full_name", STRING),
                            ),
                            contentClosed = true,
                            constraints = setOf(
                                TupleConstraint.Open(false),
                                TupleConstraint.UniqueAttrs(true),
                                TupleConstraint.Ordered
                            )
                        ),
                    )
                ),
            ),
            SuccessTestCase(
                name = "Complex Tuple Union with Heterogeneous Data",
                query = """
                    SELECT VALUE TUPLEUNION(
                      p.name,
                      p.name
                    ) FROM aws.ddb.persons AS p
                """,
                expected = BagType(
                    unionOf(
                        MISSING,
                        StructType(
                            fields = listOf(
                                StructType.Field("first", STRING),
                                StructType.Field("last", STRING),
                                StructType.Field("first", STRING),
                                StructType.Field("last", STRING),
                            ),
                            contentClosed = false,
                            constraints = setOf(
                                TupleConstraint.Open(true),
                                TupleConstraint.UniqueAttrs(false),
                            )
                        ),
                        StructType(
                            fields = listOf(
                                StructType.Field("first", STRING),
                                StructType.Field("last", STRING),
                                StructType.Field("full_name", STRING),
                            ),
                            contentClosed = false,
                            constraints = setOf(
                                TupleConstraint.Open(true),
                                TupleConstraint.UniqueAttrs(false),
                            )
                        ),
                        StructType(
                            fields = listOf(
                                StructType.Field("full_name", STRING),
                                StructType.Field("first", STRING),
                                StructType.Field("last", STRING),
                            ),
                            contentClosed = false,
                            constraints = setOf(
                                TupleConstraint.Open(true),
                                TupleConstraint.UniqueAttrs(false),
                            )
                        ),
                        StructType(
                            fields = listOf(
                                StructType.Field("full_name", STRING),
                                StructType.Field("full_name", STRING),
                            ),
                            contentClosed = true,
                            constraints = setOf(
                                TupleConstraint.Open(false),
                                TupleConstraint.UniqueAttrs(false),
                                TupleConstraint.Ordered
                            )
                        ),
                    )
                ),
            ),
        )
    }

    sealed class TestCase {
        class SuccessTestCase(
            val name: String,
            val query: String,
            val catalog: String? = null,
            val catalogPath: List<String> = emptyList(),
            val expected: StaticType,
        ) : TestCase() {
            override fun toString(): String = "$name : $query"
        }

        class ErrorTestCase(
            val name: String,
            val query: String,
            val catalog: String? = null,
            val catalogPath: List<String> = emptyList(),
            val note: String? = null,
            val expected: StaticType? = null,
            val problemHandler: ProblemHandler? = null,
        ) : TestCase() {
            override fun toString(): String = "$name : $query"
        }

        class ThrowingExceptionTestCase(
            val name: String,
            val query: String,
            val catalog: String? = null,
            val catalogPath: List<String> = emptyList(),
            val note: String? = null,
            val expectedThrowable: KClass<out Throwable>,
        ) : TestCase() {
            override fun toString(): String {
                return "$name : $query"
            }
        }
    }

    class TestProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return parameters.map { Arguments.of(it) }.stream()
        }

        private val parameters = listOf(
            ErrorTestCase(
                name = "Pets should not be accessible #1",
                query = "SELECT * FROM pets",
                expected = BagType(
                    unionOf(
                        StructType(
                            fields = emptyMap(),
                            contentClosed = false,
                            constraints = setOf(
                                TupleConstraint.Open(true),
                                TupleConstraint.UniqueAttrs(false),
                            )
                        ),
                        StructType(
                            fields = mapOf(
                                "_1" to StaticType.ANY
                            ),
                            contentClosed = true,
                            constraints = setOf(
                                TupleConstraint.Open(false),
                                TupleConstraint.UniqueAttrs(true),
                            )
                        ),
                    )
                ),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.UndefinedVariable("pets", false)
                    )
                }
            ),
            ErrorTestCase(
                name = "Pets should not be accessible #2",
                catalog = CATALOG_AWS,
                query = "SELECT * FROM pets",
                expected = BagType(
                    unionOf(
                        StructType(
                            fields = emptyMap(),
                            contentClosed = false,
                            constraints = setOf(
                                TupleConstraint.Open(true),
                                TupleConstraint.UniqueAttrs(false),
                            )
                        ),
                        StructType(
                            fields = mapOf(
                                "_1" to StaticType.ANY
                            ),
                            contentClosed = true,
                            constraints = setOf(
                                TupleConstraint.Open(false),
                                TupleConstraint.UniqueAttrs(true),
                            )
                        ),
                    )
                ),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.UndefinedVariable("pets", false)
                    )
                }
            ),
            SuccessTestCase(
                name = "Project all explicitly",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM pets",
                expected = TABLE_AWS_DDB_PETS
            ),
            SuccessTestCase(
                name = "Project all implicitly",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT id, breed FROM pets",
                expected = TABLE_AWS_DDB_PETS
            ),
            SuccessTestCase(
                name = "Test #4",
                catalog = CATALOG_B,
                catalogPath = listOf("b"),
                query = "b",
                expected = TYPE_B_B_B
            ),
            SuccessTestCase(
                name = "Test #5",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM b",
                expected = TABLE_AWS_DDB_B
            ),
            SuccessTestCase(
                name = "Test #6",
                catalog = CATALOG_AWS,
                catalogPath = listOf("b"),
                query = "SELECT * FROM b",
                expected = TABLE_AWS_B_B
            ),
            ErrorTestCase(
                name = "Test #7",
                query = "SELECT * FROM ddb.pets",
                expected = BagType(
                    unionOf(
                        StructType(
                            fields = emptyMap(),
                            contentClosed = false,
                            constraints = setOf(
                                TupleConstraint.Open(true),
                                TupleConstraint.UniqueAttrs(false),
                            )
                        ),
                        StructType(
                            fields = mapOf(
                                "_1" to StaticType.ANY
                            ),
                            contentClosed = true,
                            constraints = setOf(
                                TupleConstraint.Open(false),
                                TupleConstraint.UniqueAttrs(true),
                            )
                        ),
                    )
                ),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.UndefinedVariable("pets", false)
                    )
                }
            ),
            SuccessTestCase(
                name = "Test #10",
                catalog = CATALOG_B,
                query = "b.b",
                expected = TYPE_B_B_B
            ),
            SuccessTestCase(
                name = "Test #11",
                catalog = CATALOG_B,
                catalogPath = listOf("b"),
                query = "b.b",
                expected = TYPE_B_B_B
            ),
            SuccessTestCase(
                name = "Test #12",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM b.b",
                expected = TABLE_AWS_B_B
            ),
            SuccessTestCase(
                name = "Test #13",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM ddb.b",
                expected = TABLE_AWS_DDB_B
            ),
            SuccessTestCase(
                name = "Test #14",
                query = "SELECT * FROM aws.ddb.pets",
                expected = TABLE_AWS_DDB_PETS
            ),
            SuccessTestCase(
                name = "Test #15",
                catalog = CATALOG_AWS,
                query = "SELECT * FROM aws.b.b",
                expected = TABLE_AWS_B_B
            ),
            SuccessTestCase(
                name = "Test #16",
                catalog = CATALOG_B,
                query = "b.b.b",
                expected = TYPE_B_B_B
            ),
            SuccessTestCase(
                name = "Test #17",
                catalog = CATALOG_B,
                query = "b.b.c",
                expected = TYPE_B_B_C
            ),
            SuccessTestCase(
                name = "Test #18",
                catalog = CATALOG_B,
                catalogPath = listOf("b"),
                query = "b.b.b",
                expected = TYPE_B_B_B
            ),
            SuccessTestCase(
                name = "Test #19",
                query = "b.b.b.c",
                expected = TYPE_B_B_B_C
            ),
            SuccessTestCase(
                name = "Test #20",
                query = "b.b.b.b",
                expected = TYPE_B_B_B_B
            ),
            SuccessTestCase(
                name = "Test #21",
                catalog = CATALOG_B,
                query = "b.b.b.b",
                expected = TYPE_B_B_B_B
            ),
            SuccessTestCase(
                name = "Test #22",
                catalog = CATALOG_B,
                query = "b.b.b.c",
                expected = TYPE_B_B_C
            ),
            SuccessTestCase(
                name = "Test #23",
                catalog = CATALOG_B,
                catalogPath = listOf("b"),
                query = "b.b.b.b",
                expected = TYPE_B_B_B_B
            ),
            SuccessTestCase(
                name = "Test #24",
                query = "b.b.b.b.b",
                expected = TYPE_B_B_B_B_B
            ),
            SuccessTestCase(
                name = "Test #24",
                catalog = CATALOG_B,
                query = "b.b.b.b.b",
                expected = TYPE_B_B_B_B_B
            ),
            SuccessTestCase(
                name = "EQ",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.customer_id = 1",
                expected = TYPE_BOOL
            ),
            SuccessTestCase(
                name = "NEQ",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.customer_id <> 1",
                expected = TYPE_BOOL
            ),
            SuccessTestCase(
                name = "GEQ",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.customer_id >= 1",
                expected = TYPE_BOOL
            ),
            SuccessTestCase(
                name = "GT",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.customer_id > 1",
                expected = TYPE_BOOL
            ),
            SuccessTestCase(
                name = "LEQ",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.customer_id <= 1",
                expected = TYPE_BOOL
            ),
            SuccessTestCase(
                name = "LT",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.customer_id < 1",
                expected = TYPE_BOOL
            ),
            SuccessTestCase(
                name = "IN",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.customer_id IN (1, 2, 3)",
                expected = TYPE_BOOL
            ),
            ErrorTestCase(
                name = "IN Failure",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.customer_id IN 'hello'",
                expected = MISSING,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.UnknownFunction(
                            "in_collection",
                            listOf(INT, STRING),
                        )
                    )
                }
            ),
            SuccessTestCase(
                name = "BETWEEN",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.customer_id BETWEEN 1 AND 2",
                expected = TYPE_BOOL
            ),
            ErrorTestCase(
                name = "BETWEEN Failure",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.customer_id BETWEEN 1 AND 'a'",
                expected = MISSING,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.UnknownFunction(
                            "between",
                            listOf(
                                INT,
                                INT,
                                STRING
                            ),
                        )
                    )
                }
            ),
            SuccessTestCase(
                name = "LIKE",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.ship_option LIKE '%ABC%'",
                expected = TYPE_BOOL
            ),
            ErrorTestCase(
                name = "LIKE Failure",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.ship_option LIKE 3",
                expected = MISSING,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.UnknownFunction(
                            "like",
                            listOf(STRING, INT),
                        )
                    )
                }
            ),
            SuccessTestCase(
                name = "Case Insensitive success",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.CUSTOMER_ID = 1",
                expected = TYPE_BOOL
            ),
            ErrorTestCase(
                name = "Case Sensitive failure",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.\"CUSTOMER_ID\" = 1",
                expected = TYPE_BOOL
            ),
            SuccessTestCase(
                name = "Case Sensitive success",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.\"customer_id\" = 1",
                expected = TYPE_BOOL
            ),
            SuccessTestCase(
                name = "1-Level Junction",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "(order_info.customer_id = 1) AND (order_info.marketplace_id = 2)",
                expected = TYPE_BOOL
            ),
            SuccessTestCase(
                name = "2-Level Junction",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "(order_info.customer_id = 1) AND (order_info.marketplace_id = 2) OR (order_info.customer_id = 3) AND (order_info.marketplace_id = 4)",
                expected = TYPE_BOOL
            ),
            SuccessTestCase(
                name = "INT and STR Comparison",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.customer_id = 'something'",
                expected = TYPE_BOOL,
            ),
            ErrorTestCase(
                name = "Nonexisting Comparison",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "non_existing_column = 1",
                expected = StaticType.BOOL,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.UndefinedVariable("non_existing_column", false)
                    )
                }
            ),
            ErrorTestCase(
                name = "Bad comparison",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.customer_id = 1 AND 1",
                expected = MISSING,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.UnknownFunction(
                            "and",
                            listOf(StaticType.BOOL, INT),
                        )
                    )
                }
            ),
            ErrorTestCase(
                name = "Bad comparison",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "1 AND order_info.customer_id = 1",
                expected = MISSING,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.UnknownFunction(
                            "and",
                            listOf(INT, StaticType.BOOL),
                        )
                    )
                }
            ),
            ErrorTestCase(
                name = "Unknown column",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "SELECT unknown_col FROM orders WHERE customer_id = 1",
                expected = BagType(
                    StructType(
                        fields = mapOf("unknown_col" to AnyType()),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                ),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.UndefinedVariable("unknown_col", false)
                    )
                }
            ),
            SuccessTestCase(
                name = "LIMIT INT",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM pets LIMIT 5",
                expected = TABLE_AWS_DDB_PETS
            ),
            ErrorTestCase(
                name = "LIMIT STR",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM pets LIMIT '5'",
                expected = TABLE_AWS_DDB_PETS,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.UnexpectedType(STRING, setOf(INT))
                    )
                }
            ),
            SuccessTestCase(
                name = "OFFSET INT",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM pets LIMIT 1 OFFSET 5",
                expected = TABLE_AWS_DDB_PETS
            ),
            ErrorTestCase(
                name = "OFFSET STR",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM pets LIMIT 1 OFFSET '5'",
                expected = TABLE_AWS_DDB_PETS,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.UnexpectedType(STRING, setOf(INT))
                    )
                }
            ),
            SuccessTestCase(
                name = "CAST",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT CAST(breed AS INT) AS cast_breed FROM pets",
                expected = BagType(
                    StructType(
                        fields = mapOf("cast_breed" to unionOf(INT, MISSING)),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "UPPER",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT UPPER(breed) AS upper_breed FROM pets",
                expected = BagType(
                    StructType(
                        fields = mapOf("upper_breed" to STRING),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "Non-tuples",
                query = "SELECT a FROM << [ 1, 1.0 ] >> AS a",
                expected = BagType(
                    StructType(
                        fields = mapOf("a" to ListType(unionOf(INT, StaticType.DECIMAL))),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "Non-tuples in SELECT VALUE",
                query = "SELECT VALUE a FROM << [ 1, 1.0 ] >> AS a",
                expected =
                BagType(ListType(unionOf(INT, StaticType.DECIMAL)))
            ),
            SuccessTestCase(
                name = "SELECT VALUE",
                query = "SELECT VALUE [1, 1.0] FROM <<>>",
                expected =
                BagType(ListType(unionOf(INT, StaticType.DECIMAL)))
            ),
            SuccessTestCase(
                name = "Duplicate fields in struct",
                query = """
                    SELECT t.a AS a
                    FROM <<
                        { 'a': 1, 'a': 'hello' }
                    >> AS t
                """,
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("a", unionOf(INT, STRING))
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "Duplicate fields in ordered STRUCT. NOTE: b.b.d is an ordered struct with two attributes (e). First is INT.",
                query = """
                    SELECT d.e AS e
                    FROM << b.b.d >> AS d
                """,
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("e", INT)
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "Duplicate fields in struct",
                query = """
                    SELECT a AS a
                    FROM <<
                        { 'a': 1, 'a': 'hello' }
                    >> AS t
                """,
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("a", unionOf(INT, STRING))
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "AGGREGATE over INTS",
                query = "SELECT a, COUNT(*) AS c, SUM(a) AS s, MIN(b) AS m FROM << {'a': 1, 'b': 2} >> GROUP BY a",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to INT,
                            "c" to INT,
                            "s" to INT,
                            "m" to INT,
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "AGGREGATE over DECIMALS",
                query = "SELECT a, COUNT(*) AS c, SUM(a) AS s, MIN(b) AS m FROM << {'a': 1.0, 'b': 2.0}, {'a': 1.0, 'b': 2.0} >> GROUP BY a",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StaticType.DECIMAL,
                            "c" to INT,
                            "s" to StaticType.DECIMAL,
                            "m" to StaticType.DECIMAL,
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "Current User",
                query = "CURRENT_USER",
                expected = unionOf(STRING, NULL)
            ),
            SuccessTestCase(
                name = "Trim",
                query = "trim(' ')",
                expected = STRING
            ),
            SuccessTestCase(
                name = "Current User Concat",
                query = "CURRENT_USER || 'hello'",
                expected = unionOf(STRING, NULL)
            ),
            SuccessTestCase(
                name = "Current User Concat in WHERE",
                query = "SELECT VALUE a FROM [ 0 ] AS a WHERE CURRENT_USER = 'hello'",
                expected = BagType(INT)
            ),
            SuccessTestCase(
                name = "TRIM_2",
                query = "trim(' ' FROM ' Hello, World! ')",
                expected = STRING
            ),
            SuccessTestCase(
                name = "TRIM_1",
                query = "trim(' Hello, World! ')",
                expected = STRING
            ),
            SuccessTestCase(
                name = "TRIM_3",
                query = "trim(LEADING ' ' FROM ' Hello, World! ')",
                expected = STRING
            ),
            ErrorTestCase(
                name = "TRIM_2_error",
                query = "trim(2 FROM ' Hello, World! ')",
                expected = MISSING,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.UnknownFunction(
                            "trim_chars",
                            args = listOf(STRING, INT)
                        )
                    )
                }
            ),
        )
    }

    private fun runTest(tc: TestCase) = when (tc) {
        is SuccessTestCase -> runTest(tc)
        is ErrorTestCase -> runTest(tc)
        is ThrowingExceptionTestCase -> runTest(tc)
    }

    @OptIn(ExperimentalPartiQLSchemaInferencer::class)
    private fun runTest(tc: ThrowingExceptionTestCase) {
        val session = PartiQLPlanner.Session(
            tc.query.hashCode().toString(),
            USER_ID,
            tc.catalog,
            tc.catalogPath,
            catalogConfig,
            Instant.now()
        )
        val collector = ProblemCollector()
        val ctx = PartiQLSchemaInferencer.Context(session, PLUGINS, collector)
        val exception = assertThrows<Throwable> {
            PartiQLSchemaInferencer.infer(tc.query, ctx)
            Unit
        }
        val cause = exception.cause
        assertNotNull(cause)
        assertEquals(tc.expectedThrowable, cause::class)
    }

    @OptIn(ExperimentalPartiQLSchemaInferencer::class)
    private fun runTest(tc: SuccessTestCase) {
        val session = PartiQLPlanner.Session(
            tc.query.hashCode().toString(),
            USER_ID,
            tc.catalog,
            tc.catalogPath,
            catalogConfig,
            Instant.now()
        )
        val collector = ProblemCollector()
        val ctx = PartiQLSchemaInferencer.Context(session, PLUGINS, collector)
        val result = PartiQLSchemaInferencer.inferInternal(tc.query, ctx)

        assert(collector.problems.isEmpty()) {
            collector.problems.toString()
        }
        val actual = result.second
        assert(tc.expected == actual) {
            buildString {
                appendLine()
                appendLine("Expect: ${tc.expected}")
                appendLine("Actual: $actual")
                appendLine()
                PlanPrinter.append(this, result.first)
            }
        }
    }

    @OptIn(ExperimentalPartiQLSchemaInferencer::class)
    private fun runTest(tc: ErrorTestCase) {
        val session = PartiQLPlanner.Session(
            tc.query.hashCode().toString(),
            USER_ID,
            tc.catalog,
            tc.catalogPath,
            catalogConfig,
            Instant.now()
        )
        val collector = ProblemCollector()
        val ctx = PartiQLSchemaInferencer.Context(session, PLUGINS, collector)
        val result = PartiQLSchemaInferencer.inferInternal(tc.query, ctx)
        assert(collector.problems.isNotEmpty()) {
            buildString {
                appendLine("Expected to find problems, but none were found.")
                appendLine()
                PlanPrinter.append(this, result.first)
            }
        }
        if (tc.expected != null) {
            assert(tc.expected == result.second) {
                buildString {
                    appendLine()
                    appendLine("Expect: ${tc.expected}")
                    appendLine("Actual: ${result.second}")
                    appendLine()
                    PlanPrinter.append(this, result.first)
                }
            }
        }
        assert(collector.problems.isNotEmpty()) {
            "Expected to find problems, but none were found."
        }
        tc.problemHandler?.handle(collector.problems, true)
    }

    fun interface ProblemHandler {
        fun handle(problems: List<Problem>, ignoreSourceLocation: Boolean)
    }
}
