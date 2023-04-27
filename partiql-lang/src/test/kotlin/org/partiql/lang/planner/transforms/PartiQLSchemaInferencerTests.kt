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
import org.partiql.lang.ast.UNKNOWN_SOURCE_LOCATION
import org.partiql.lang.ast.passes.SemanticProblemDetails
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemCollector
import org.partiql.lang.planner.PlanningProblemDetails
import org.partiql.lang.planner.transforms.PartiQLSchemaInferencerTests.TestCase.ErrorTestCase
import org.partiql.lang.planner.transforms.PartiQLSchemaInferencerTests.TestCase.SuccessTestCase
import org.partiql.plan.Rex
import org.partiql.plugins.mockdb.LocalPlugin
import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.ListType
import org.partiql.types.StaticType
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
        val TABLE_AWS_DDB_PETS = BagType(
            elementType = StructType(
                fields = mapOf(
                    "id" to TYPE_AWS_DDB_PETS_ID,
                    "breed" to TYPE_AWS_DDB_PETS_BREED
                ),
                contentClosed = true,
                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
            )
        )
        val TABLE_AWS_DDB_B = BagType(
            StructType(
                fields = mapOf("identifier" to StaticType.STRING),
                contentClosed = true,
                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
            )
        )
        val TABLE_AWS_B_B = BagType(
            StructType(
                fields = mapOf("identifier" to StaticType.INT),
                contentClosed = true,
                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
            )
        )
        val TYPE_B_B_B_B_B = StaticType.INT
        private val TYPE_B_B_B_B = StructType(
            mapOf("b" to TYPE_B_B_B_B_B),
            contentClosed = true,
            constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
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
                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                    )
                ),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_SOURCE_LOCATION,
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                    )
                ),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_SOURCE_LOCATION,
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                    )
                ),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_SOURCE_LOCATION,
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
                        UNKNOWN_SOURCE_LOCATION,
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(StaticType.INT, StaticType.STRING),
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
                        UNKNOWN_SOURCE_LOCATION,
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(
                                StaticType.INT,
                                StaticType.INT,
                                StaticType.STRING
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
                        UNKNOWN_SOURCE_LOCATION,
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(StaticType.STRING, StaticType.INT),
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
                        UNKNOWN_SOURCE_LOCATION,
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(StaticType.INT, StaticType.STRING),
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
                        UNKNOWN_SOURCE_LOCATION,
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
                        UNKNOWN_SOURCE_LOCATION,
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(StaticType.BOOL, StaticType.INT),
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
                        UNKNOWN_SOURCE_LOCATION,
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(StaticType.INT, StaticType.BOOL),
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                    )
                ),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_SOURCE_LOCATION,
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
                        UNKNOWN_SOURCE_LOCATION,
                        SemanticProblemDetails.IncompatibleDataTypeForExpr(StaticType.INT, StaticType.STRING)
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
                        UNKNOWN_SOURCE_LOCATION,
                        SemanticProblemDetails.IncompatibleDataTypeForExpr(StaticType.INT, StaticType.STRING)
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                    )
                )
            ),
            SuccessTestCase(
                name = "LEFT JOIN",
                query = "SELECT b, a FROM <<{ 'a': 1 }>> AS t1 LEFT JOIN <<{ 'b': 2.0 }>> AS t2 ON TRUE",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "b" to StaticType.DECIMAL,
                            "a" to StaticType.INT,
                        ),
                        contentClosed = true,
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                    )
                )
            ),
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
                name = "Current User Concat in WHERE",
                query = "SELECT VALUE a FROM [ 0 ] AS a WHERE CURRENT_USER = 'hello'",
                expected = BagType(StaticType.INT)
            ),
            ErrorTestCase(
                name = "Current User Concat in WHERE",
                query = "SELECT VALUE a FROM [ 0 ] AS a WHERE CURRENT_USER = 5",
                expected = BagType(StaticType.INT),
                problemHandler = assertProblemExists {
                    Problem(
                        UNKNOWN_SOURCE_LOCATION,
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(
                                unionOf(StaticType.STRING, StaticType.NULL),
                                StaticType.INT,
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
                        UNKNOWN_SOURCE_LOCATION,
                        SemanticProblemDetails.IncompatibleDatatypesForOp(
                            listOf(
                                unionOf(StaticType.STRING, StaticType.NULL),
                                StaticType.STRING,
                            ),
                            Rex.Binary.Op.PLUS.name
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
            collector.problems.toString()
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
