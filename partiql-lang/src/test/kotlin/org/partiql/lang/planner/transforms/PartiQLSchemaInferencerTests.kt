package org.partiql.lang.planner.transforms

import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.junit.jupiter.api.Test
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
import org.partiql.plugins.local.LocalPlugin
import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.ListType
import org.partiql.types.StaticType
import org.partiql.types.StaticType.Companion.BOOL
import org.partiql.types.StaticType.Companion.INT
import org.partiql.types.StaticType.Companion.MISSING
import org.partiql.types.StaticType.Companion.NULL
import org.partiql.types.StaticType.Companion.STRING
import org.partiql.types.StaticType.Companion.unionOf
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import java.time.Instant
import java.util.stream.Stream
import kotlin.io.path.pathString
import kotlin.io.path.toPath
import kotlin.test.assertTrue

class PartiQLSchemaInferencerTests {

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

        val TYPE_BOOL = BOOL
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
                    StructType(
                        fields = mapOf("pets" to StaticType.ANY),
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
                        PlanningProblemDetails.UndefinedVariable("pets", false)
                    )
                }
            ),
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
                expected = MISSING,
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
                        MISSING,
                        NULL,
                        BOOL
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
                expected = MISSING,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(BOOL, INT),
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
                expected = MISSING,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(INT, BOOL),
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
                name = "ORDER BY int",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM pets ORDER BY id",
                expected = TABLE_AWS_DDB_PETS
            ),
            SuccessTestCase(
                name = "ORDER BY str",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM pets ORDER BY breed",
                expected = TABLE_AWS_DDB_PETS
            ),
            SuccessTestCase(
                name = "ORDER BY str",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM pets ORDER BY unknown_col",
                expected = TABLE_AWS_DDB_PETS
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
                        SemanticProblemDetails.IncompatibleDataTypeForExpr(INT, STRING)
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
                name = "UNPIVOT",
                query = "SELECT VALUE v FROM UNPIVOT { 'a': 2 } AS v AT attr WHERE attr = 'a'",
                expected =
                BagType(INT)

            ),
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
                query = "SELECT b, a FROM <<{ 'a': 1 }>> AS t1 LEFT JOIN <<{ 'b': 2.0 }>> AS t2 ON TRUE",
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("b", StaticType.DECIMAL),
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
                            StructType.Field("a", StaticType.DECIMAL),
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
                name = "LEFT JOIN ALL",
                query = "SELECT * FROM <<{ 'a': 1 }>> AS t1 LEFT JOIN <<{ 'a': 2.0 }>> AS t2 ON t1.a = t2.a",
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("a", INT),
                            StructType.Field("a", StaticType.DECIMAL),
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
                            StructType.Field("a", StaticType.DECIMAL),
                            StructType.Field("a", STRING),
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
            ErrorTestCase(
                name = "LEFT JOIN Ambiguous Reference in ON",
                query = "SELECT * FROM <<{ 'a': 1 }>> AS t1 LEFT JOIN <<{ 'a': 2.0 }>> AS t2 ON a = 3",
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("a", INT),
                            StructType.Field("a", StaticType.DECIMAL),
                        ),
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
            SuccessTestCase(
                name = "Subquery coercion in top-level expression",
                query = "COALESCE(SELECT identifier FROM aws.ddb.b)",
                expected = STRING
            ),
            SuccessTestCase(
                name = "Subquery coercion in WHERE. Also showcases conflicting bindings. INT vs STRING.",
                query = """
                    SELECT VALUE identifier
                    FROM aws.b.b AS b -- aws.b.b.identifier is an INT
                    WHERE
                        COALESCE(SELECT identifier AS identifier FROM aws.ddb.b) IS NOT NULL -- aws.ddb.b.identifier is a STRING
                """,
                expected = BagType(INT)
            ),
            SuccessTestCase(
                name = "Subquery coercion in SFW",
                query = """
                    SELECT
                        (SELECT identifier FROM aws.ddb.b) AS some_str,                         -- identifier is STRING
                        ('hello' || (SELECT identifier FROM aws.ddb.b)) AS concat_str,          -- identifier is STRING
                        (1 < (SELECT id FROM aws.ddb.pets)) AS one_lt_id,                       -- id is INT
                        ((SELECT id FROM aws.ddb.pets) > 1) AS id_gt_one,                       -- id is INT
                        (1 IN (SELECT id FROM aws.ddb.pets)) AS one_in_ids,                     -- id is INT
                        ([1, 2] IN (SELECT id, id + 1 FROM aws.ddb.pets)) AS array_in_ids,      -- id is INT
                        ([1, 2] <= (SELECT id, id + 1 FROM aws.ddb.pets)) AS lit_array_lte_ids  -- id is INT
                    FROM
                        << 0, 1, 2 >> AS t
                """.trimIndent(),
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("some_str", STRING),
                            StructType.Field("concat_str", STRING),
                            StructType.Field("one_lt_id", BOOL),
                            StructType.Field("id_gt_one", BOOL),
                            StructType.Field("one_in_ids", BOOL),
                            StructType.Field("array_in_ids", BOOL),
                            StructType.Field("lit_array_lte_ids", BOOL),
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered
                        )
                    )
                )
            ),
            ErrorTestCase(
                name = "List of Lists on RHS for IN",
                query = "1 IN (SELECT id, id + 1 FROM aws.ddb.pets)",
                expected = BOOL,
                problemHandler = assertProblemExists {
                    Problem(
                        sourceLocation = UNKNOWN_PROBLEM_LOCATION,
                        details = SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(INT, BagType(ListType(INT))),
                            "IN"
                        )
                    )
                }
            ),
            ErrorTestCase(
                name = "Coercion of select with multiple projections in COALESCE",
                query = "COALESCE(SELECT id AS id, id + 1 AS increment FROM aws.ddb.pets)",
                problemHandler = assertProblemExists {
                    Problem(
                        sourceLocation = UNKNOWN_PROBLEM_LOCATION,
                        details = SemanticProblemDetails.CoercionError(
                            StructType(
                                fields = listOf(
                                    StructType.Field("id", INT),
                                    StructType.Field("increment", INT),
                                ),
                                contentClosed = true,
                                constraints = setOf(
                                    TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered
                                )
                            ),
                        )
                    )
                }
            ),
            ErrorTestCase(
                name = "Lists on LHS for IN",
                query = "[1, 2] IN (SELECT id FROM aws.ddb.pets)",
                expected = BOOL,
                problemHandler = assertProblemExists {
                    Problem(
                        sourceLocation = UNKNOWN_PROBLEM_LOCATION,
                        details = SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(ListType(INT), BagType(INT)),
                            "IN"
                        )
                    )
                }
            ),
            ErrorTestCase(
                name = "List of Lists on RHS for LTE",
                query = "1 <= (SELECT id AS id, id + 1 AS increment FROM aws.ddb.pets)",
                expected = unionOf(MISSING, NULL, BOOL),
                problemHandler = assertProblemExists {
                    Problem(
                        sourceLocation = UNKNOWN_PROBLEM_LOCATION,
                        details = SemanticProblemDetails.CoercionError(
                            StructType(
                                fields = listOf(
                                    StructType.Field("id", INT),
                                    StructType.Field("increment", INT),
                                ),
                                contentClosed = true,
                                constraints = setOf(
                                    TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered
                                )
                            ),
                        )
                    )
                }
            ),
            ErrorTestCase(
                name = "SELECT * in Subquery", // TODO: This needs to be fixed.
                query = "1 IN (SELECT * FROM aws.ddb.pets)",
                expected = unionOf(MISSING, NULL, BOOL),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.UnimplementedError("* in SELECT VALUE")
                    )
                }
            ),
            ErrorTestCase(
                name = "SELECT * in Subquery with IN coercion rules", // TODO: This needs to be fixed.
                query = "[1, 2] IN (SELECT * FROM aws.ddb.pets)",
                expected = unionOf(MISSING, NULL, BOOL),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.UnimplementedError("* in SELECT VALUE")
                    )
                }
            ),
            SuccessTestCase(
                name = "SELECT * in Subquery with plus -- aws.b.b has one column (INT)",
                query = "1 + (SELECT * FROM aws.b.b)",
                expected = INT,
            ),
            ErrorTestCase(
                name = "Cannot coerce subquery of multiple columns into single value",
                query = "[1, 2] + (SELECT * FROM aws.ddb.pets)",
                expected = unionOf(MISSING, NULL),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.CoercionError(
                            StructType(
                                fields = listOf(
                                    StructType.Field("id", INT),
                                    StructType.Field("breed", STRING),
                                ),
                                contentClosed = true,
                                constraints = setOf(
                                    TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered
                                )
                            ),
                        )
                    )
                }
            ),
            ErrorTestCase(
                name = "SELECT * in Subquery with comparison -- aws.ddb.pets has two columns",
                query = "1 < (SELECT * FROM aws.ddb.pets)",
                expected = unionOf(MISSING, NULL, BOOL),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.CoercionError(
                            StructType(
                                fields = listOf(
                                    StructType.Field("id", INT),
                                    StructType.Field("breed", STRING),
                                ),
                                contentClosed = true,
                                constraints = setOf(
                                    TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered
                                )
                            ),
                        )
                    )
                }
            ),
            SuccessTestCase(
                name = "SELECT * in Subquery with comparison -- aws.b.b has one column (INT)",
                query = "1 < (SELECT * FROM aws.b.b)",
                expected = BOOL,
            ),
            ErrorTestCase(
                name = "SELECT * in Subquery with comparison -- aws.ddb.pets has two columns (INT)",
                query = "1 < (SELECT * FROM aws.ddb.pets)",
                expected = unionOf(NULL, MISSING, BOOL),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.CoercionError(
                            StructType(
                                fields = listOf(
                                    StructType.Field("id", INT),
                                    StructType.Field("breed", STRING),
                                ),
                                contentClosed = true,
                                constraints = setOf(
                                    TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered
                                )
                            ),
                        )
                    )
                }
            ),
            ErrorTestCase(
                name = "SELECT * in multi-column subquery with comparison coercion.", // TODO: This needs to be fixed.
                query = "[1, 2] < (SELECT * FROM aws.ddb.pets)",
                expected = unionOf(NULL, MISSING, BOOL),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.UnimplementedError("* in SELECT VALUE")
                    )
                }
            ),
            ErrorTestCase(
                name = "SELECT * in single-column subquery with comparison coercion.", // TODO: This needs to be fixed.
                query = "[1, 2] < (SELECT * FROM aws.b.b)",
                expected = unionOf(NULL, MISSING, BOOL),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.UnimplementedError("* in SELECT VALUE")
                    )
                }
            ),
            ErrorTestCase(
                name = "SELECT * in Subquery with comparison of array", // TODO: This needs to be fixed.
                query = "[1, 2] < (SELECT * FROM aws.ddb.pets)",
                expected = unionOf(MISSING, NULL, BOOL),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.UnimplementedError("* in SELECT VALUE")
                    )
                }
            ),
            ErrorTestCase(
                name = "List of Lists on LHS for LTE",
                query = "[1,2] <= (SELECT id FROM aws.ddb.pets)",
                expected = BOOL,
                problemHandler = assertProblemExists {
                    Problem(
                        sourceLocation = UNKNOWN_PROBLEM_LOCATION,
                        details = SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(ListType(INT), INT),
                            "LTE"
                        )
                    )
                }
            ),
            ErrorTestCase(
                name = "TRIM_2_error",
                query = "trim(2 FROM ' Hello, World! ')",
                expected = STRING,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.InvalidArgumentTypeForFunction(
                            "trim",
                            unionOf(STRING, StaticType.SYMBOL),
                            INT,
                        )
                    )
                }
            ),
            ErrorTestCase(
                name = "Current User Concat in WHERE",
                query = "SELECT VALUE a FROM [ 0 ] AS a WHERE CURRENT_USER = 5",
                expected = BagType(INT),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(
                                unionOf(STRING, NULL),
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
                expected = unionOf(MISSING, NULL),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(
                                unionOf(STRING, NULL),
                                STRING,
                            ),
                            Rex.Binary.Op.PLUS.name
                        )
                    )
                }
            ),
            SuccessTestCase(
                name = "BITWISE_AND_1",
                query = "1 & 2",
                expected = StaticType.INT
            ),
            // casting to a parameterized type produced Missing.
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
                expected = StaticType.unionOf(StaticType.INT, MISSING)
            ),
            SuccessTestCase(
                name = "BITWISE_AND_8",
                query = "CAST(1 AS INT4) & CAST(2 AS INT8)",
                expected = StaticType.unionOf(StaticType.INT8, MISSING)
            ),
            SuccessTestCase(
                name = "BITWISE_AND_9",
                query = "CAST(1 AS INT4) & 2",
                expected = StaticType.unionOf(StaticType.INT, MISSING)
            ),
            SuccessTestCase(
                name = "BITWISE_AND_10",
                query = "CAST(1 AS INT8) & 2",
                expected = StaticType.unionOf(StaticType.INT, MISSING)
            ),
            ErrorTestCase(
                name = "BITWISE_AND_NULL_OPERAND",
                query = "1 & NULL",
                expected = StaticType.NULL,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.ExpressionAlwaysReturnsNullOrMissing
                    )
                }
            ),
            ErrorTestCase(
                name = "BITWISE_AND_MISSING_OPERAND",
                query = "1 & MISSING",
                expected = StaticType.MISSING,
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_PROBLEM_LOCATION,
                        SemanticProblemDetails.ExpressionAlwaysReturnsNullOrMissing
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
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(
                                INT, STRING
                            ),
                            Rex.Binary.Op.BITWISE_AND.name
                        )
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
            catalogConfig,
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
            catalogConfig,
            Instant.now()
        )
        val collector = ProblemCollector()
        val ctx = PartiQLSchemaInferencer.Context(session, PLUGINS, collector)
        val result = PartiQLSchemaInferencer.infer(tc.query, ctx)
        if (tc.expected != null) {
            assert(tc.expected == result) {
                buildString {
                    appendLine("Expected: ${tc.expected}")
                    appendLine("Actual: $result")
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

    @Test
    fun test() {
        runTest(
            ErrorTestCase(
                name = "Case Sensitive failure",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.\"CUSTOMER_ID\" = 1",
                expected = TYPE_BOOL
            )
        )
    }
}
