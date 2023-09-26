package org.partiql.lang.planner.transforms

import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.annotations.ExperimentalPartiQLSchemaInferencer
import org.partiql.errors.Problem
import org.partiql.errors.UNKNOWN_PROBLEM_LOCATION
import org.partiql.lang.ast.passes.SemanticProblemDetails
import org.partiql.lang.errors.ProblemCollector
import org.partiql.lang.planner.PlanningProblemDetails
import org.partiql.lang.planner.transforms.PartiQLSchemaInferencerTests.ProblemHandler
import org.partiql.lang.planner.transforms.PartiQLSchemaInferencerTests.TestCase.ErrorTestCase
import org.partiql.lang.planner.transforms.PartiQLSchemaInferencerTests.TestCase.SuccessTestCase
import org.partiql.plan.Rex
import org.partiql.plugins.mockdb.LocalPlugin
import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.ListType
import org.partiql.types.StaticType
import org.partiql.types.StaticType.Companion.INT
import org.partiql.types.StaticType.Companion.STRING
import org.partiql.types.StaticType.Companion.unionOf
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import java.net.URL
import java.time.Instant
import java.util.stream.Stream
import kotlin.test.assertTrue

class PartiQLSchemaInferencerTests {

    companion object {
        private val PLUGINS = listOf(LocalPlugin())

        private const val USER_ID = "TEST_USER"
        private val CATALOG_MAP = listOf("aws", "b", "db").associateWith { catalogName ->
            val catalogUrl: URL =
                PartiQLSchemaInferencerTests::class.java.classLoader.getResource("catalogs/$catalogName")
                    ?: error("Couldn't be found")
            ionStructOf(
                field("connector_name", ionString("localdb")),
                field("localdb_root", ionString(catalogUrl.path))
            )
        }
        const val CATALOG_AWS = "aws"
        const val CATALOG_B = "b"
        const val CATALOG_DB = "db"
        val DB_SCHEMA_MARKETS = listOf("markets")

        val TYPE_BOOL = StaticType.BOOL
        private val TYPE_AWS_DDB_PETS_ID = StaticType.INT
        private val TYPE_AWS_DDB_PETS_BREED = StaticType.STRING
        val TABLE_AWS_DDB_PETS_ELEMENT_TYPE = StructType(
            fields = mapOf(
                "id" to TYPE_AWS_DDB_PETS_ID,
                "breed" to TYPE_AWS_DDB_PETS_BREED
            ),
            contentClosed = true,
            constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
        )
        val TABLE_AWS_DDB_PETS_BAG = BagType(
            elementType = TABLE_AWS_DDB_PETS_ELEMENT_TYPE
        )
        val TABLE_AWS_DDB_PETS_LIST = ListType(
            elementType = TABLE_AWS_DDB_PETS_ELEMENT_TYPE
        )
        val TABLE_AWS_DDB_B = BagType(
            StructType(
                fields = mapOf("identifier" to StaticType.STRING),
                contentClosed = true,
                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
            )
        )
        val TABLE_AWS_B_B = BagType(
            StructType(
                fields = mapOf("identifier" to StaticType.INT),
                contentClosed = true,
                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
            )
        )
        val TYPE_B_B_B_B_B = StaticType.INT
        private val TYPE_B_B_B_B = StructType(
            mapOf("b" to TYPE_B_B_B_B_B),
            contentClosed = true,
            constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
        )
        val TYPE_B_B_B_C = StaticType.INT
        val TYPE_B_B_C = StaticType.INT
        val TYPE_B_B_B =
            StructType(
                fields = mapOf(
                    "b" to TYPE_B_B_B_B,
                    "c" to TYPE_B_B_B_C
                ),
                contentClosed = true,
                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
            )
    }

    @ParameterizedTest
    @ArgumentsSource(TestProvider::class)
    fun test(tc: TestCase) = runTest(tc)

    sealed class TestCase {
        class SuccessTestCase(
            val name: String,
            val query: String,
            val catalog: String? = null,
            val catalogPath: List<String> = emptyList(),
            val expected: StaticType
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
            val problemHandler: ProblemHandler? = null
        ) : TestCase() {
            override fun toString(): String = "$name : $query"
        }
    }

    @ParameterizedTest
    @ArgumentsSource(TestProviderExclude::class)
    fun testExclude(tc: TestCase) = runTest(tc)
    class TestProviderExclude : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return parameters.map { Arguments.of(it) }.stream()
        }

        private val parameters = listOf(
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
                                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                    ),
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            ),
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
            ),
            SuccessTestCase(
                name = "EXCLUDE case sensitive lookup with both capitalized and uncapitalized removed",
                query = """SELECT * EXCLUDE t."a".b.c
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
                                            "d" to StaticType.STRING
                                        ),
                                        contentClosed = true,
                                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                    ),
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            ),
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
            ),
        )
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
                    StructType(
                        fields = mapOf("pets" to StaticType.ANY),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
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
                    StructType(
                        fields = mapOf("pets" to StaticType.ANY),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
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
                expected = TABLE_AWS_DDB_PETS_BAG
            ),
            SuccessTestCase(
                name = "Project all implicitly",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT id, breed FROM pets",
                expected = TABLE_AWS_DDB_PETS_BAG
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
                    StructType(
                        fields = mapOf("pets" to StaticType.ANY),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
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
                name = "Test #8",
                catalog = CATALOG_AWS,
                query = "SELECT * FROM ddb.pets",
                expected = TABLE_AWS_DDB_PETS_BAG
            ),
            SuccessTestCase(
                name = "Test #9",
                catalog = CATALOG_AWS,
                query = "SELECT * FROM b.b",
                expected = TABLE_AWS_B_B
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
                expected = TABLE_AWS_DDB_PETS_BAG
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
                expected = TYPE_BOOL,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(INT, STRING),
                            "IN"
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
                expected = TYPE_BOOL,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(
                                INT,
                                INT,
                                STRING
                            ),
                            "between"
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
                expected = StaticType.MISSING,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(STRING, INT),
                            "LIKE"
                        )
                    )
                }
            ),
            SuccessTestCase(
                name = "Case insensitive",
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
            ErrorTestCase(
                name = "INT and STR Comparison",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.customer_id = 'something'",
                expected = TYPE_BOOL,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(INT, STRING),
                            "EQ"
                        )
                    )
                }
            ),
            ErrorTestCase(
                name = "Nonexisting Comparison",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "non_existing_column = 1",
                expected = AnyOfType(
                    setOf(
                        StaticType.MISSING,
                        StaticType.NULL,
                        StaticType.BOOL
                    )
                ),
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
                expected = StaticType.MISSING,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(StaticType.BOOL, INT),
                            "AND"
                        )
                    )
                }
            ),
            ErrorTestCase(
                name = "Bad comparison",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "1 AND order_info.customer_id = 1",
                expected = StaticType.MISSING,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(INT, StaticType.BOOL),
                            "AND"
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
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
            SuccessTestCase(
                name = "ORDER BY str",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM pets ORDER BY unknown_col",
                expected = TABLE_AWS_DDB_PETS_LIST
            ),
            SuccessTestCase(
                name = "LIMIT INT",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM pets LIMIT 5",
                expected = TABLE_AWS_DDB_PETS_BAG
            ),
            ErrorTestCase(
                name = "LIMIT STR",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM pets LIMIT '5'",
                expected = TABLE_AWS_DDB_PETS_BAG,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.IncompatibleDataTypeForExpr(INT, STRING)
                    )
                }
            ),
            SuccessTestCase(
                name = "OFFSET INT",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM pets LIMIT 1 OFFSET 5",
                expected = TABLE_AWS_DDB_PETS_BAG
            ),
            ErrorTestCase(
                name = "OFFSET STR",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM pets LIMIT 1 OFFSET '5'",
                expected = TABLE_AWS_DDB_PETS_BAG,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.IncompatibleDataTypeForExpr(INT, STRING)
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
                        fields = mapOf("cast_breed" to unionOf(StaticType.INT, StaticType.MISSING)),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
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
                        fields = mapOf("upper_breed" to StaticType.STRING),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
            ),
            SuccessTestCase(
                name = "Non-tuples",
                query = "SELECT a FROM << [ 1, 1.0 ] >> AS a",
                expected = BagType(
                    StructType(
                        fields = mapOf("a" to ListType(unionOf(StaticType.INT, StaticType.DECIMAL))),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
            ),
            SuccessTestCase(
                name = "Non-tuples in SELECT VALUE",
                query = "SELECT VALUE a FROM << [ 1, 1.0 ] >> AS a",
                expected =
                BagType(ListType(unionOf(StaticType.INT, StaticType.DECIMAL)))
            ),
            SuccessTestCase(
                name = "SELECT VALUE",
                query = "SELECT VALUE [1, 1.0] FROM <<>>",
                expected =
                BagType(ListType(unionOf(StaticType.INT, StaticType.DECIMAL)))
            ),
            SuccessTestCase(
                name = "UNPIVOT",
                query = "SELECT VALUE v FROM UNPIVOT { 'a': 2 } AS v AT attr WHERE attr = 'a'",
                expected =
                BagType(StaticType.INT)

            ),
            SuccessTestCase(
                name = "CROSS JOIN",
                query = "SELECT * FROM <<{ 'a': 1 }>> AS t1, <<{ 'b': 2.0 }>> AS t2",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StaticType.INT,
                            "b" to StaticType.DECIMAL,
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
            ),
            SuccessTestCase(
                name = "LEFT JOIN",
                query = "SELECT * FROM <<{ 'a': 1 }>> AS t1 LEFT JOIN <<{ 'b': 2.0 }>> AS t2 ON TRUE",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StaticType.INT,
                            "b" to StaticType.DECIMAL,
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
            ),
            SuccessTestCase(
                name = "LEFT JOIN",
                query = "SELECT b, a FROM <<{ 'a': 1 }>> AS t1 LEFT JOIN <<{ 'b': 2.0 }>> AS t2 ON TRUE",
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("b", StaticType.DECIMAL),
                            StructType.Field("a", StaticType.INT),
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
            ),
            SuccessTestCase(
                name = "LEFT JOIN",
                query = "SELECT t1.a, t2.a FROM <<{ 'a': 1 }>> AS t1 LEFT JOIN <<{ 'a': 2.0 }>> AS t2 ON t1.a = t2.a",
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("a", StaticType.INT),
                            StructType.Field("a", StaticType.DECIMAL),
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
            ),
            SuccessTestCase(
                name = "LEFT JOIN ALL",
                query = "SELECT * FROM <<{ 'a': 1 }>> AS t1 LEFT JOIN <<{ 'a': 2.0 }>> AS t2 ON t1.a = t2.a",
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("a", StaticType.INT),
                            StructType.Field("a", StaticType.DECIMAL),
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
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
                            StructType.Field("a", StaticType.INT),
                            StructType.Field("a", StaticType.DECIMAL),
                            StructType.Field("a", StaticType.STRING),
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
            ),
            ErrorTestCase(
                name = "LEFT JOIN Ambiguous Reference in ON",
                query = "SELECT * FROM <<{ 'a': 1 }>> AS t1 LEFT JOIN <<{ 'a': 2.0 }>> AS t2 ON a = 3",
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("a", StaticType.INT),
                            StructType.Field("a", StaticType.DECIMAL),
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                ),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.UndefinedVariable("a", false)
                    )
                }
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
            ),
            SuccessTestCase(
                name = "AGGREGATE over INTS",
                query = "SELECT a, COUNT(*) AS c, SUM(a) AS s, MIN(b) AS m FROM << {'a': 1, 'b': 2} >> GROUP BY a",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StaticType.INT,
                            "c" to StaticType.INT,
                            "s" to StaticType.INT,
                            "m" to StaticType.INT,
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
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
                            "c" to StaticType.INT,
                            "s" to StaticType.DECIMAL,
                            "m" to StaticType.DECIMAL,
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
            ),
            SuccessTestCase(
                name = "Current User",
                query = "CURRENT_USER",
                expected = unionOf(STRING, StaticType.NULL)
            ),
            SuccessTestCase(
                name = "Trim",
                query = "trim(' ')",
                expected = StaticType.STRING
            ),
            SuccessTestCase(
                name = "Current User Concat",
                query = "CURRENT_USER || 'hello'",
                expected = unionOf(STRING, StaticType.NULL)
            ),
            SuccessTestCase(
                name = "Current User Concat in WHERE",
                query = "SELECT VALUE a FROM [ 0 ] AS a WHERE CURRENT_USER = 'hello'",
                expected = BagType(StaticType.INT)
            ),
            SuccessTestCase(
                name = "TRIM_2",
                query = "trim(' ' FROM ' Hello, World! ')",
                expected = StaticType.STRING
            ),
            SuccessTestCase(
                name = "TRIM_1",
                query = "trim(' Hello, World! ')",
                expected = StaticType.STRING
            ),
            SuccessTestCase(
                name = "TRIM_3",
                query = "trim(LEADING ' ' FROM ' Hello, World! ')",
                expected = StaticType.STRING
            ),
            ErrorTestCase(
                name = "TRIM_2_error",
                query = "trim(2 FROM ' Hello, World! ')",
                expected = StaticType.STRING,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.InvalidArgumentTypeForFunction(
                            "trim",
                            unionOf(StaticType.STRING, StaticType.SYMBOL),
                            StaticType.INT,
                        )
                    )
                }
            ),
            ErrorTestCase(
                name = "Current User Concat in WHERE",
                query = "SELECT VALUE a FROM [ 0 ] AS a WHERE CURRENT_USER = 5",
                expected = BagType(StaticType.INT),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(
                                unionOf(STRING, StaticType.NULL),
                                INT,
                            ),
                            Rex.Binary.Op.EQ.name
                        )
                    )
                }
            ),
            ErrorTestCase(
                name = "Current User (String) PLUS String",
                query = "CURRENT_USER + 'hello'",
                expected = unionOf(StaticType.MISSING, StaticType.NULL),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(
                                unionOf(STRING, StaticType.NULL),
                                STRING,
                            ),
                            Rex.Binary.Op.PLUS.name
                        )
                    )
                }
            ),
            // EXCLUDE test cases
            SuccessTestCase(
                name = "EXCLUDE SELECT list",
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
            ),
            SuccessTestCase(
                name = "EXCLUDE SELECT list multiple paths",
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
            ),
            SuccessTestCase(
                name = "EXCLUDE SELECT list index and list index field",
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
                                                                StaticType.INT,
                                                                StaticType.MISSING // c[1]'s `field` was excluded
                                                            )
                                                        )
                                                    ),
                                                    contentClosed = true,
                                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                                )
                                            )
                                        ),
                                        contentClosed = true,
                                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                    )
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            ),
                            "foo" to StaticType.STRING
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
            ),
            SuccessTestCase(
                name = "EXCLUDE SELECT list wildcard",
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
                                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                                )
                                            )
                                        ),
                                        contentClosed = true,
                                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                    )
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            ),
                            "foo" to StaticType.STRING
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
            ),
            SuccessTestCase(
                name = "EXCLUDE SELECT list tuple wildcard",
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
                                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                                )
                                            )
                                        ),
                                        contentClosed = true,
                                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                    )
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            ),
                            "foo" to StaticType.STRING
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
            ),
            SuccessTestCase(
                name = "EXCLUDE SELECT list order by",
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
            ),
            SuccessTestCase(
                name = "EXCLUDE SELECT list with JOINs",
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
            ),
            SuccessTestCase(
                name = "SELECT * EXCLUDE ex 1",
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
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
                                            constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                        ),
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                ),
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
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
                                            constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                        ),
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                ),
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
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
                                            constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                        ),
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                ),
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
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
                                            constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                        ),
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                ),
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
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
                                                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                                    )
                                                )
                                            ),
                                            contentClosed = true,
                                            constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                        ),
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                ),
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
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
                                                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                                    )
                                                )
                                            ),
                                            contentClosed = true,
                                            constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                        ),
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                ),
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
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
                                                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                                    )
                                                )
                                            ),
                                            contentClosed = true,
                                            constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                        ),
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                ),
                            )
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
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
                                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                    ),
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            ),
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
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
                                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                    ),
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            ),
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
            ),
            SuccessTestCase(
                name = "EXCLUDE case sensitive lookup with both capitalized and uncapitalized removed",
                query = """SELECT * EXCLUDE t."a".b.c
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
                                            "d" to StaticType.STRING
                                        ),
                                        contentClosed = true,
                                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                    ),
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            ),
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
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
                                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(false)) // UniqueAttrs set to false
                                    ),
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            ),
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                )
            ),
            ErrorTestCase(
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
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.InvalidExcludeExpr
                    )
                }
            ),
            ErrorTestCase(
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
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.InvalidExcludeExpr
                    )
                }
            ),
            ErrorTestCase(
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
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.InvalidExcludeExpr
                    )
                }
            ),
            ErrorTestCase(
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
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        PlanningProblemDetails.InvalidExcludeExpr
                    )
                }
            ),
        )

        private fun assertProblemExists(problem: () -> Problem) = ProblemHandler { problems, ignoreSourceLocation ->
            when (ignoreSourceLocation) {
                true -> assertTrue("Expected to find ${problem.invoke()} in $problems") { problems.any { it.details == problem.invoke().details } }
                false -> assertTrue("Expected to find ${problem.invoke()} in $problems") { problems.any { it == problem.invoke() } }
            }
        }
    }

    private fun runTest(tc: TestCase) = when (tc) {
        is SuccessTestCase -> runTest(tc)
        is ErrorTestCase -> runTest(tc)
    }

    @OptIn(ExperimentalPartiQLSchemaInferencer::class)
    private fun runTest(tc: SuccessTestCase) {
        val session = PlannerSession(
            tc.query.hashCode().toString(),
            USER_ID,
            tc.catalog,
            tc.catalogPath,
            CATALOG_MAP,
            Instant.now()
        )
        val collector = ProblemCollector()
        val ctx = PartiQLSchemaInferencer.Context(session, PLUGINS, collector)
        val result = PartiQLSchemaInferencer.infer(tc.query, ctx)
        assert(collector.problems.isEmpty()) {
            collector.problems.toString()
        }

        assert(tc.expected == result) {
            buildString {
                appendLine("Expected: ${tc.expected}")
                appendLine("Actual: $result")
            }
        }
    }

    @OptIn(ExperimentalPartiQLSchemaInferencer::class)
    private fun runTest(tc: ErrorTestCase) {
        val session = PlannerSession(
            tc.query.hashCode().toString(),
            USER_ID,
            tc.catalog,
            tc.catalogPath,
            CATALOG_MAP,
            Instant.now()
        )
        val collector = ProblemCollector()
        val ctx = PartiQLSchemaInferencer.Context(session, PLUGINS, collector)
        val result = PartiQLSchemaInferencer.infer(tc.query, ctx)
        assert(collector.problems.isNotEmpty()) {
            "Expected to find problems, but none were found."
        }
        if (tc.expected != null) {
            assert(tc.expected == result) {
                buildString {
                    appendLine("Expected: ${tc.expected}")
                    appendLine("Actual: $result")
                }
            }
        }
        tc.problemHandler?.handle(collector.problems, true)
    }

    fun interface ProblemHandler {
        fun handle(problems: List<Problem>, ignoreSourceLocation: Boolean)
    }
}
