package org.partiql.planner.internal.typer

import com.amazon.ionelement.api.loadSingleElement
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.errors.Problem
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Identifier
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.Statement
import org.partiql.plan.debug.PlanPrinter
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.catalog.Session
import org.partiql.planner.internal.ProblemGenerator
import org.partiql.planner.internal.typer.PlanTyper.Companion.toCType
import org.partiql.planner.internal.typer.PlanTyperTestsPorted.TestCase.ErrorTestCase
import org.partiql.planner.internal.typer.PlanTyperTestsPorted.TestCase.SuccessTestCase
import org.partiql.planner.internal.typer.PlanTyperTestsPorted.TestCase.ThrowingExceptionTestCase
import org.partiql.planner.test.PartiQLTest
import org.partiql.planner.test.PartiQLTestProvider
import org.partiql.planner.util.ProblemCollector
import org.partiql.plugins.local.toStaticType
import org.partiql.plugins.memory.MemoryCatalog
import org.partiql.plugins.memory.MemoryConnector
import org.partiql.plugins.memory.MemoryObject
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.BagType
import org.partiql.types.ListType
import org.partiql.types.PType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StaticType.Companion.ANY
import org.partiql.types.StaticType.Companion.INT4
import org.partiql.types.StaticType.Companion.INT8
import org.partiql.types.StaticType.Companion.STRUCT
import org.partiql.types.StaticType.Companion.unionOf
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.value.PartiQLValueExperimental
import java.util.stream.Stream
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class PlanTyperTestsPorted {

    sealed class TestCase {
        class SuccessTestCase(
            val name: String? = null,
            val key: PartiQLTest.Key? = null,
            val query: String? = null,
            val catalog: String = "pql",
            val catalogPath: List<String> = emptyList(),
            val expected: CompilerType,
            val warnings: ProblemHandler? = null,
        ) : TestCase() {
            constructor(
                name: String? = null,
                key: PartiQLTest.Key? = null,
                query: String? = null,
                catalog: String = "pql",
                catalogPath: List<String> = emptyList(),
                expected: StaticType,
                warnings: ProblemHandler? = null,
            ) : this(name, key, query, catalog, catalogPath, PType.fromStaticType(expected).toCType(), warnings)

            override fun toString(): String {
                if (key != null) {
                    return "${key.group} : ${key.name}"
                }
                return "${name!!} : $query"
            }
        }

        class ErrorTestCase(
            val name: String,
            val key: PartiQLTest.Key?,
            val query: String?,
            val catalog: String = "pql",
            val catalogPath: List<String> = emptyList(),
            val note: String?,
            val expected: CompilerType?,
            val problemHandler: ProblemHandler?,
        ) : TestCase() {

            constructor(
                name: String,
                key: PartiQLTest.Key? = null,
                query: String? = null,
                catalog: String = "pql",
                catalogPath: List<String> = emptyList(),
                note: String? = null,
                expected: StaticType? = null,
                problemHandler: ProblemHandler? = null,
            ) : this(name, key, query, catalog, catalogPath, note, expected?.let { PType.fromStaticType(it).toCType() }, problemHandler)

            override fun toString(): String = "$name : ${query ?: key}"
        }

        class ThrowingExceptionTestCase(
            val name: String,
            val query: String,
            val catalog: String = "pql",
            val catalogPath: List<String> = emptyList(),
            val note: String? = null,
            val expectedThrowable: KClass<out Throwable>,
        ) : TestCase() {
            override fun toString(): String {
                return "$name : $query"
            }
        }
    }

    companion object {

        private val parser = PartiQLParser.default()
        private val planner = PartiQLPlanner.builder().signal().build()

        private fun assertProblemExists(problem: Problem) = ProblemHandler { problems, ignoreSourceLocation ->
            val message = buildString {
                appendLine("Expected problems to include: $problem")
                appendLine("Received: [")
                problems.forEach {
                    append("\t")
                    appendLine(it)
                }
                appendLine("]")
            }
            when (ignoreSourceLocation) {
                true -> assertTrue(message) { problems.any { it.details == problem.details } }
                false -> assertTrue(message) { problems.any { it == problem } }
            }
        }

        val session = object : ConnectorSession {
            override fun getQueryId(): String = "query-id"
            override fun getUserId(): String = "user-id"
        }

        private fun id(vararg parts: Identifier.Symbol): Identifier {
            return when (parts.size) {
                0 -> error("Identifier requires more than one part.")
                1 -> parts.first()
                else -> Identifier.Qualified(parts.first(), parts.drop(1))
            }
        }

        private fun sensitive(part: String): Identifier.Symbol =
            Identifier.Symbol(part, Identifier.CaseSensitivity.SENSITIVE)

        private fun insensitive(part: String): Identifier.Symbol =
            Identifier.Symbol(part, Identifier.CaseSensitivity.INSENSITIVE)

        /**
         * MemoryConnector.Factory from reading the resources in /resource_path.txt for Github CI/CD.
         */
        @OptIn(PartiQLValueExperimental::class)
        val catalogs: List<Pair<String, ConnectorMetadata>> by lazy {
            val inputStream = this::class.java.getResourceAsStream("/resource_path.txt")!!
            val map = mutableMapOf<String, MutableList<Pair<BindingPath, StaticType>>>()
            inputStream.reader().readLines().forEach { path ->
                if (path.startsWith("catalogs/default")) {
                    val schema = this::class.java.getResourceAsStream("/$path")!!
                    val ion = loadSingleElement(schema.reader().readText())
                    val staticType = ion.toStaticType()
                    val steps = path.substring(0, path.length - 4).split('/').drop(2) // drop the catalogs/default
                    val catalogName = steps.first()
                    val bindingSteps = steps
                        .drop(1)
                        .map { BindingName(it, BindingCase.INSENSITIVE) }
                    val bindingPath = BindingPath(bindingSteps)
                    if (map.containsKey(catalogName)) {
                        map[catalogName]!!.add(bindingPath to staticType)
                    } else {
                        map[catalogName] = mutableListOf(bindingPath to staticType)
                    }
                }
            }
            map.entries.map { (catalogName, bindings) ->
                val catalog = MemoryCatalog.builder().name(catalogName).build()
                val connector = MemoryConnector(catalog)
                for (binding in bindings) {
                    val path = binding.first
                    val obj = MemoryObject(binding.second)
                    catalog.insert(path, obj)
                }
                catalogName to connector.getMetadata(session)
            }
        }

        private fun key(name: String) = PartiQLTest.Key("schema_inferencer", name)

        //
        // testing result utility
        //
        const val CATALOG_AWS = "aws"
        const val CATALOG_B = "b"
        const val CATALOG_DB = "db"
        val DB_SCHEMA_MARKETS = listOf("markets")

        val TYPE_BOOL = StaticType.BOOL
        private val TYPE_AWS_DDB_PETS_ID = StaticType.INT4
        private val TYPE_AWS_DDB_PETS_BREED = StaticType.STRING
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
                fields = mapOf("identifier" to StaticType.STRING),
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
                fields = mapOf("identifier" to StaticType.INT4),
                contentClosed = true,
                constraints = setOf(
                    TupleConstraint.Open(false),
                    TupleConstraint.UniqueAttrs(true),
                    TupleConstraint.Ordered
                )
            )
        )
        val TYPE_B_B_B_B_B = StaticType.INT4
        private val TYPE_B_B_B_B = StructType(
            mapOf("b" to TYPE_B_B_B_B_B),
            contentClosed = true,
            constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
        )
        val TYPE_B_B_B_C = StaticType.INT4
        val TYPE_B_B_C = StaticType.INT4
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

        private fun insensitiveId(symbol: String) = BindingPath(listOf(BindingName(symbol, BindingCase.INSENSITIVE)))

        private fun sensitiveId(symbol: String) = BindingPath(listOf(BindingName(symbol, BindingCase.SENSITIVE)))

        private fun idQualified(vararg symbol: Pair<String, BindingCase>) = symbol.map {
            BindingName(it.first, it.second)
        }.let { BindingPath(it) }

        //
        // Parameterized Test Source
        //
        @JvmStatic
        fun collections() = listOf<TestCase>(
            SuccessTestCase(
                name = "Collection BAG<INT4>",
                key = key("collections-01"),
                expected = BagType(StaticType.INT4),
            ),
            SuccessTestCase(
                name = "Collection LIST<INT4>",
                key = key("collections-02"),
                expected = ListType(StaticType.INT4),
            ),
            SuccessTestCase(
                name = "Collection LIST<INT4>",
                key = key("collections-03"),
                expected = ListType(StaticType.INT4),
            ),
            SuccessTestCase(
                name = "Collection SEXP<INT4>",
                key = key("collections-04"),
                expected = SexpType(StaticType.INT4),
            ),
            SuccessTestCase(
                name = "SELECT from array",
                key = key("collections-05"),
                expected = BagType(StaticType.INT4),
            ),
            SuccessTestCase(
                name = "SELECT from array",
                key = key("collections-06"),
                expected = BagType(
                    StructType(
                        fields = listOf(StructType.Field("x", StaticType.INT4)),
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
            SuccessTestCase(
                name = "Select star with join",
                key = key("sanity-05"),
                catalog = "pql",
                expected = BagType(
                    StructType(
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(false),
                            TupleConstraint.Ordered
                        ),
                        fields = listOf(
                            StructType.Field(
                                "name",
                                StructType(
                                    fields = listOf(
                                        StructType.Field("first", StaticType.STRING),
                                        StructType.Field("last", StaticType.STRING),
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(
                                        TupleConstraint.Open(false),
                                        TupleConstraint.UniqueAttrs(true),
                                        TupleConstraint.Ordered
                                    ),
                                )
                            ),
                            StructType.Field("ssn", StaticType.STRING),
                            StructType.Field("employer", StaticType.STRING),
                            StructType.Field("name", StaticType.STRING),
                            StructType.Field("tax_id", StaticType.INT8),
                            StructType.Field(
                                "address",
                                StructType(
                                    fields = listOf(
                                        StructType.Field("street", StaticType.STRING),
                                        StructType.Field("zip", StaticType.INT4),
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(
                                        TupleConstraint.Open(false),
                                        TupleConstraint.UniqueAttrs(true),
                                        TupleConstraint.Ordered
                                    )
                                )
                            ),
                        )
                    )
                )
            ),
            SuccessTestCase(
                name = "Select star",
                key = key("sanity-06"),
                catalog = "pql",
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("first", StaticType.STRING),
                            StructType.Field("last", StaticType.STRING),
                            StructType.Field("full_name", StaticType.STRING),
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
        fun scanCases() = listOf(
            SuccessTestCase(
                name = "Basic Scan Indexed",
                key = key("sanity-07"),
                catalog = "pql",
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("first", StaticType.STRING),
                            StructType.Field("i", StaticType.INT8),
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
        fun distinctClauseCases() = listOf<TestCase>(
            SuccessTestCase(
                name = "Distinct SQL Select",
                catalog = CATALOG_AWS,
                query = "SELECT DISTINCT a, b FROM << { 'a': 1, 'b': 'Hello, world!' } >>;",
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("a", StaticType.INT4),
                            StructType.Field("b", StaticType.STRING),
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
                name = "Distinct SQL Select with Ordering",
                catalog = CATALOG_AWS,
                query = "SELECT DISTINCT a, b FROM << { 'a': 1, 'b': 'Hello, world!' } >> ORDER BY a;",
                expected = ListType(
                    StructType(
                        fields = listOf(
                            StructType.Field("a", StaticType.INT4),
                            StructType.Field("b", StaticType.STRING),
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
                name = "Distinct SQL Select *",
                catalog = CATALOG_AWS,
                query = "SELECT DISTINCT * FROM << { 'a': 1, 'b': 'Hello, world!' } >>;",
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("a", StaticType.INT4),
                            StructType.Field("b", StaticType.STRING),
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
                name = "Distinct SQL Select * with Ordering",
                catalog = CATALOG_AWS,
                query = "SELECT DISTINCT * FROM << { 'a': 1, 'b': 'Hello, world!' } >> ORDER BY a;",
                expected = ListType(
                    StructType(
                        fields = listOf(
                            StructType.Field("a", StaticType.INT4),
                            StructType.Field("b", StaticType.STRING),
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
                name = "Distinct PartiQL Select Value *",
                catalog = CATALOG_AWS,
                query = "SELECT DISTINCT VALUE a FROM << { 'a': 1, 'b': 'Hello, world!' } >>;",
                expected = BagType(StaticType.INT4)
            ),
            SuccessTestCase(
                name = "Distinct PartiQL Select Value * with Ordering",
                catalog = CATALOG_AWS,
                query = "SELECT DISTINCT VALUE a FROM << { 'a': 1, 'b': 'Hello, world!' } >> ORDER BY a;",
                expected = ListType(StaticType.INT4)
            ),
        )

        @JvmStatic
        fun pivotCases() = listOf(
            SuccessTestCase(
                name = "Basic PIVOT",
                key = key("pivot-00"),
                catalog = "pql",
                expected = StructType(
                    contentClosed = false,
                    constraints = setOf(
                        TupleConstraint.Open(true),
                    )
                )
            ),
        )

        @JvmStatic
        fun isTypeCases() = listOf(
            SuccessTestCase(
                name = "IS BOOL",
                key = key("is-type-00"),
                catalog = "pql",
                catalogPath = listOf("main"),
                expected = StaticType.BOOL
            ),
            SuccessTestCase(
                name = "IS INT",
                key = key("is-type-01"),
                catalog = "pql",
                catalogPath = listOf("main"),
                expected = StaticType.BOOL
            ),
            SuccessTestCase(
                name = "IS STRING",
                key = key("is-type-02"),
                catalog = "pql",
                catalogPath = listOf("main"),
                expected = StaticType.BOOL
            ),
            SuccessTestCase(
                name = "IS NULL",
                key = key("is-type-03"),
                catalog = "pql",
                expected = StaticType.BOOL,
            ),
            // TODO: For some reason, the conformance tests say that this results in TRUE. Regardless, we know it returns
            //  a boolean. We should re-look at what the conformance tests should return.
            SuccessTestCase(
                name = "MISSING IS NULL",
                key = key("is-type-04"),
                catalog = "pql",
                expected = StaticType.BOOL,
            ),
            SuccessTestCase(
                name = "NULL IS NULL",
                key = key("is-type-05"),
                catalog = "pql",
                expected = StaticType.BOOL,
            ),
            SuccessTestCase(
                name = "MISSING IS MISSING",
                key = key("is-type-06"),
                catalog = "pql",
                expected = StaticType.BOOL,
            ),
            SuccessTestCase(
                name = "NULL IS MISSING",
                key = key("is-type-07"),
                catalog = "pql",
                expected = StaticType.BOOL,
            ),
            ErrorTestCase(
                name = "ERROR always MISSING",
                key = key("is-type-08"),
                catalog = "pql",
            ),
        )

        @JvmStatic
        fun castCases() = listOf(
            SuccessTestCase(
                name = "DECIMAL AS INT2",
                key = key("cast-00"),
                catalog = "pql",
                expected = StaticType.INT2,
            ),
            SuccessTestCase(
                name = "DECIMAL AS INT4",
                key = key("cast-01"),
                catalog = "pql",
                expected = StaticType.INT4,
            ),
            SuccessTestCase(
                name = "DECIMAL AS INT8",
                key = key("cast-02"),
                catalog = "pql",
                expected = StaticType.INT8,
            ),
            SuccessTestCase(
                name = "DECIMAL AS INT",
                key = key("cast-03"),
                catalog = "pql",
                expected = StaticType.INT4,
            ),
            SuccessTestCase(
                name = "DECIMAL AS BIGINT",
                key = key("cast-04"),
                catalog = "pql",
                expected = StaticType.INT8,
            ),
            SuccessTestCase(
                name = "DECIMAL_ARBITRARY AS DECIMAL",
                key = key("cast-05"),
                catalog = "pql",
                expected = StaticType.DECIMAL,
            ),
        )

        @JvmStatic
        fun sessionVariables() = listOf(
            SuccessTestCase(
                name = "Current User",
                query = "CURRENT_USER",
                expected = StaticType.STRING
            ),
            SuccessTestCase(
                name = "Current User Concat",
                query = "CURRENT_USER || 'hello'",
                expected = StaticType.STRING
            ),
            SuccessTestCase(
                name = "Current User in WHERE",
                query = "SELECT VALUE a FROM [ 0 ] AS a WHERE CURRENT_USER = 'hello'",
                expected = BagType(StaticType.INT4)
            ),
            SuccessTestCase(
                name = "Current User in WHERE",
                query = "SELECT VALUE a FROM [ 0 ] AS a WHERE CURRENT_USER = 5",
                expected = BagType(StaticType.INT4),
            ),
            SuccessTestCase(
                name = "Testing CURRENT_USER and CURRENT_DATE Binders",
                query = """
                    SELECT
                        CURRENT_USER,
                        CURRENT_DATE,
                        CURRENT_USER AS "curr_user",
                        CURRENT_DATE AS "curr_date",
                        CURRENT_USER || ' is my name.' AS name_desc
                    FROM << 0, 1 >>;
                """.trimIndent(),
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("CURRENT_USER", StaticType.STRING),
                            StructType.Field("CURRENT_DATE", StaticType.DATE),
                            StructType.Field("curr_user", StaticType.STRING),
                            StructType.Field("curr_date", StaticType.DATE),
                            StructType.Field("name_desc", StaticType.STRING),
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
                name = "Current User (String) PLUS String",
                query = "CURRENT_USER + 'hello'",
                expected = ANY,
                problemHandler = assertProblemExists(
                    ProblemGenerator.incompatibleTypesForOp(
                        listOf(
                            StaticType.STRING,
                            StaticType.STRING,
                        ),
                        "PLUS",
                    )
                )
            ),
        )

        @JvmStatic
        fun bitwiseAnd() = listOf(
            SuccessTestCase(
                name = "BITWISE_AND_1",
                query = "1 & 2",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                name = "BITWISE_AND_2",
                query = "CAST(1 AS INT2) & CAST(2 AS INT2)",
                expected = StaticType.INT2
            ),
            SuccessTestCase(
                name = "BITWISE_AND_3",
                query = "1 & 2",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                name = "BITWISE_AND_4",
                query = "CAST(1 AS INT8) & CAST(2 AS INT8)",
                expected = StaticType.INT8
            ),
            SuccessTestCase(
                name = "BITWISE_AND_5",
                query = "CAST(1 AS INT2) & 2",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                name = "BITWISE_AND_6",
                query = "CAST(1 AS INT2) & CAST(2 AS INT8)",
                expected = StaticType.INT8
            ),
            SuccessTestCase(
                name = "BITWISE_AND_7",
                query = "CAST(1 AS INT2) & 2",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                name = "BITWISE_AND_8",
                query = "1 & CAST(2 AS INT8)",
                expected = StaticType.INT8
            ),
            SuccessTestCase(
                name = "BITWISE_AND_9",
                query = "1 & 2",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                name = "BITWISE_AND_10",
                query = "CAST(1 AS INT8) & 2",
                expected = StaticType.INT8
            ),
            SuccessTestCase(
                name = "BITWISE_AND_NULL_OPERAND",
                query = "1 & NULL",
                expected = INT4,
            ),
            ErrorTestCase(
                name = "BITWISE_AND_MISSING_OPERAND",
                query = "1 & MISSING",
                expected = INT4,
                problemHandler = assertProblemExists(
                    ProblemGenerator.expressionAlwaysReturnsMissing("Static function always receives MISSING arguments.")
                )
            ),
            ErrorTestCase(
                name = "BITWISE_AND_NON_INT_OPERAND",
                query = "1 & 'NOT AN INT'",
                expected = StaticType.ANY,
                problemHandler = assertProblemExists(
                    ProblemGenerator.incompatibleTypesForOp(
                        listOf(StaticType.INT4, StaticType.STRING),
                        "BITWISE_AND",
                    )
                )
            ),
        )

        @JvmStatic
        fun unpivotCases() = listOf(
            SuccessTestCase(
                name = "UNPIVOT",
                query = "SELECT VALUE v FROM UNPIVOT { 'a': 2 } AS v AT attr WHERE attr = 'a'",
                expected = BagType(StaticType.INT4)
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
                            "a" to StaticType.INT4,
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
                            "a" to StaticType.INT4,
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
                            StructType.Field("a", StaticType.INT4),
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
                            StructType.Field("a", StaticType.INT4),
                            StructType.Field("a", StaticType.DECIMAL),
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
                            StructType.Field("a", StaticType.INT4),
                            StructType.Field("a", StaticType.DECIMAL),
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
                            StructType.Field("a", StaticType.INT4),
                            StructType.Field("a", StaticType.DECIMAL),
                            StructType.Field("a", StaticType.STRING),
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
                            StructType.Field("a", StaticType.INT4),
                            StructType.Field("a", StaticType.DECIMAL),
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(false),
                            TupleConstraint.Ordered
                        )
                    )
                ),
                problemHandler = assertProblemExists(
                    ProblemGenerator.undefinedVariable(insensitive("a"), setOf("t1", "t2"))
                )
            ),
            SuccessTestCase(
                name = "LEFT JOIN (Lateral references)",
                query = """
                    SELECT VALUE rhs
                    FROM << [0, 1, 2], [10, 11, 12], [20, 21, 22] >> AS lhs
                    LEFT OUTER JOIN lhs AS rhs
                    ON lhs[2] = rhs
                """.trimIndent(),
                expected = BagType(INT4)
            ),
            SuccessTestCase(
                name = "INNER JOIN (Lateral references)",
                query = """
                    SELECT VALUE rhs
                    FROM << [0, 1, 2], [10, 11, 12], [20, 21, 22] >> AS lhs
                    INNER JOIN lhs AS rhs
                    ON lhs[2] = rhs
                """.trimIndent(),
                expected = BagType(INT4)
            ),
            ErrorTestCase(
                name = "RIGHT JOIN (Doesn't support lateral references)",
                query = """
                    SELECT VALUE rhs
                    FROM << [0, 1, 2], [10, 11, 12], [20, 21, 22] >> AS lhs
                    RIGHT OUTER JOIN lhs AS rhs
                    ON lhs[2] = rhs
                """.trimIndent(),
                problemHandler = assertProblemExists(
                    ProblemGenerator.undefinedVariable(insensitive("lhs"), setOf())
                )
            ),
            ErrorTestCase(
                name = "FULL JOIN (Doesn't support lateral references)",
                query = """
                    SELECT VALUE rhs
                    FROM << [0, 1, 2], [10, 11, 12], [20, 21, 22] >> AS lhs
                    FULL OUTER JOIN lhs AS rhs
                    ON lhs[2] = rhs
                """.trimIndent(),
                problemHandler = assertProblemExists(
                    ProblemGenerator.undefinedVariable(insensitive("lhs"), setOf())
                )
            ),
        )

        @JvmStatic
        fun excludeCases() = listOf(
            SuccessTestCase(
                name = "EXCLUDE SELECT star",
                key = key("exclude-01"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "name" to StaticType.STRING,
                            "custId" to StaticType.INT4,
                            "address" to StructType(
                                fields = mapOf(
                                    "city" to StaticType.STRING,
                                    "zipcode" to StaticType.INT4,
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
                key = key("exclude-02"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "name" to StaticType.STRING,
                            "custId" to StaticType.INT4,
                            "address" to StructType(
                                fields = mapOf(
                                    "city" to StaticType.STRING,
                                    "zipcode" to StaticType.INT4
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
                key = key("exclude-03"),
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
                                                        "field" to INT4
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
                key = key("exclude-04"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StructType(
                                fields = mapOf(
                                    "b" to StructType(
                                        fields = mapOf(
                                            "c" to ListType(
                                                elementType = StaticType.INT4
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
                key = key("exclude-05"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to ListType(
                                elementType = StaticType.INT4 // empty list but still preserve typing information
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
                key = key("exclude-06"),
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
                                                        "field_y" to StaticType.INT4
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
                name = "EXCLUDE SELECT star tuple wildcard as last step",
                key = key("exclude-07"),
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
                key = key("exclude-08"),
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
                name = "EXCLUDE SELECT star with JOIN",
                key = key("exclude-09"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StaticType.INT4,
                            "b" to StaticType.INT4,
                            "c" to StaticType.INT4
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
                key = key("exclude-10"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "b" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b_2" to StaticType.INT4
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
                key = key("exclude-11"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StructType(
                                fields = mapOf(
                                    "a_1" to StaticType.INT4,
                                    "a_2" to StaticType.INT4
                                ),
                                contentClosed = true,
                                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                            ),
                            "b" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b_2" to StaticType.INT4
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                                )
                            ),
                            "c" to StaticType.INT4,
                            "d" to StaticType.INT4
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
                key = key("exclude-12"),
                expected = BagType(
                    ListType(
                        elementType = StructType(
                            fields = mapOf(
                                "b_2" to StaticType.INT4
                            ),
                            contentClosed = true,
                            constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true))
                        )
                    ),
                )
            ),
            SuccessTestCase(
                name = "SELECT * EXCLUDE collection wildcard and nested tuple attr",
                key = key("exclude-13"),
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
                key = key("exclude-14"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b" to StructType(
                                            fields = mapOf(
                                                "c" to StaticType.INT4,
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
                key = key("exclude-15"),
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
                key = key("exclude-16"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b" to StructType(
                                            fields = mapOf( // all fields of b optional
                                                "c" to StaticType.INT4,
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
                name = "SELECT * EXCLUDE collection wildcard and nested collection wildcard",
                key = key("exclude-17"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b" to StructType(
                                            fields = mapOf(
                                                "c" to StaticType.INT4,
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
                key = key("exclude-18"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b" to StructType(
                                            fields = mapOf(
                                                "c" to StaticType.INT4,
                                                "d" to ListType(
                                                    elementType = StructType(
                                                        fields = mapOf(
                                                            "e" to StaticType.STRING, // last step is optional since only a[1]... is excluded
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
                key = key("exclude-19"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b" to StructType(
                                            fields = mapOf(
                                                "c" to StaticType.INT4,
                                                "d" to ListType(
                                                    elementType = StructType(
                                                        fields = mapOf( // same as above
                                                            "e" to StaticType.STRING,
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
                key = key("exclude-20"),
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
                key = key("exclude-21"),
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
                key = key("exclude-22"),
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
                key = key("exclude-23"),
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
                key = key("exclude-24"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "c" to StaticType.INT4
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
                key = key("exclude-25"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StaticType.INT4
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
                key = key("exclude-26"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "t" to StructType(
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
                key = key("exclude-27"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "t" to StructType(
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
                key = key("exclude-28"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "t" to StructType( // union gone
                                fields = mapOf(
                                    "a" to StructType(
                                        fields = mapOf(
                                            "b" to StaticType.INT4
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
                key = key("exclude-29"),
                expected = BagType(
                    elementType = StructType(
                        fields = mapOf(
                            "a" to StructType(
                                fields = mapOf(
                                    "b" to StructType(
                                        fields = mapOf(
                                            "c" to StaticType.INT4,
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
                key = key("exclude-30"),
                expected = BagType(
                    elementType = StructType(
                        fields = mapOf(
                            "a" to StructType(
                                fields = mapOf(
                                    "b" to StructType(
                                        fields = mapOf(
                                            "c" to StaticType.INT4,
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
                key = key("exclude-31"),
                expected = BagType(
                    elementType = StructType(
                        fields = mapOf(
                            "a" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b" to StaticType.INT4
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
                key = key("exclude-32"),
                expected = BagType(
                    elementType = StructType(
                        fields = mapOf(
                            "a" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b" to StaticType.INT4
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
                key = key("exclude-33"),
                expected = BagType(
                    elementType = StructType(
                        fields = mapOf(
                            "a" to BagType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b" to StaticType.INT4
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
            ErrorTestCase(
                name = "invalid exclude root",
                key = key("exclude-34"),
                expected = BagType(
                    elementType = StructType(
                        fields = mapOf(
                            "a" to BagType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b" to StaticType.INT4
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
                ),
                problemHandler = assertProblemExists(
                    ProblemGenerator.unresolvedExcludedExprRoot("nonsense")
                )
            ),
            // EXCLUDE regression test (behavior subject to change pending RFC); could give error/warning
            SuccessTestCase(
                name = "exclude with unions and last step collection index",
                key = key("exclude-35"),
                expected = BagType(
                    elementType = StructType(
                        fields = mapOf(
                            "a" to ListType(
                                elementType = StructType(
                                    fields = mapOf(
                                        "b" to StaticType.INT4,
                                        "c" to ANY
                                    ),
                                    contentClosed = true,
                                    constraints = setOf(
                                        TupleConstraint.Open(false),
                                        TupleConstraint.UniqueAttrs(true)
                                    )
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
            // TODO: Actual is bag(struct(b: int4, [Open(value=false), UniqueAttrs(value=true), Ordered]))
//            SuccessTestCase(
//                name = "EXCLUDE using a catalog",
//                catalog = CATALOG_B,
//                key = key("exclude-36"), // SELECT * EXCLUDE t.c FROM b.b.b AS t;
//                expected = BagType(
//                    elementType = StructType(
//                        fields = mapOf(
//                            "b" to StructType(
//                                fields = mapOf(
//                                    "b" to StaticType.INT4
//                                ),
//                                contentClosed = true,
//                                constraints = setOf(
//                                    TupleConstraint.Open(false),
//                                    TupleConstraint.UniqueAttrs(true),
//                                    TupleConstraint.Ordered
//                                )
//                            ),
//                        ),
//                        contentClosed = true,
//                        constraints = setOf(
//                            TupleConstraint.Open(false),
//                            TupleConstraint.UniqueAttrs(true),
//                            TupleConstraint.Ordered
//                        )
//                    )
//                )
//            ),
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
                problemHandler = assertProblemExists(
                    ProblemGenerator.undefinedVariable(insensitive("unknown_col"), setOf("pets"))
                )
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
                        "a" to StaticType.INT4,
                        "b" to StaticType.STRING,
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
                        StructType.Field("a", StaticType.INT4),
                        StructType.Field("a", StaticType.STRING),
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
                            StructType.Field("b", StaticType.INT4),
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
                expected = BagType(ANY),
            ),
            SuccessTestCase(
                name = "Tuple Union with Heterogeneous Data (2)",
                query = """
                    SELECT VALUE TUPLEUNION(
                      t.a
                    ) FROM <<
                        { 'a': { 'b': 1 } },
                        { 'a': { 'b': 'hello' } },
                        { 'a': 'world' },
                        { 'a': 4.5 },
                        { }
                    >> AS t
                """,
                expected = BagType(
                    StaticType.unionOf(
                        StructType(
                            fields = listOf(
                                StructType.Field("b", StaticType.INT4),
                            ),
                            contentClosed = true,
                            constraints = setOf(
                                TupleConstraint.Open(false),
                                TupleConstraint.UniqueAttrs(true),
                            )
                        ),
                        StructType(
                            fields = listOf(
                                StructType.Field("b", StaticType.STRING),
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
                    StaticType.unionOf(
                        StructType(
                            fields = listOf(
                                StructType.Field("first", StaticType.STRING),
                                StructType.Field("last", StaticType.STRING),
                            ),
                            contentClosed = false,
                            constraints = setOf(
                                TupleConstraint.Open(true),
                                TupleConstraint.UniqueAttrs(false),
                            )
                        ),
                        StructType(
                            fields = listOf(
                                StructType.Field("full_name", StaticType.STRING),
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
                    StaticType.unionOf(
                        StructType(
                            fields = listOf(
                                StructType.Field("first", StaticType.STRING),
                                StructType.Field("last", StaticType.STRING),
                                StructType.Field("first", StaticType.STRING),
                                StructType.Field("last", StaticType.STRING),
                            ),
                            contentClosed = false,
                            constraints = setOf(
                                TupleConstraint.Open(true),
                                TupleConstraint.UniqueAttrs(false),
                            )
                        ),
                        StructType(
                            fields = listOf(
                                StructType.Field("first", StaticType.STRING),
                                StructType.Field("last", StaticType.STRING),
                                StructType.Field("full_name", StaticType.STRING),
                            ),
                            contentClosed = false,
                            constraints = setOf(
                                TupleConstraint.Open(true),
                                TupleConstraint.UniqueAttrs(false),
                            )
                        ),
                        StructType(
                            fields = listOf(
                                StructType.Field("full_name", StaticType.STRING),
                                StructType.Field("first", StaticType.STRING),
                                StructType.Field("last", StaticType.STRING),
                            ),
                            contentClosed = false,
                            constraints = setOf(
                                TupleConstraint.Open(true),
                                TupleConstraint.UniqueAttrs(false),
                            )
                        ),
                        StructType(
                            fields = listOf(
                                StructType.Field("full_name", StaticType.STRING),
                                StructType.Field("full_name", StaticType.STRING),
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

        @JvmStatic
        fun caseWhens() = listOf(
            SuccessTestCase(
                name = "Easy case when",
                query = """
                    CASE
                        WHEN FALSE THEN 0
                        WHEN TRUE THEN 1
                        ELSE 2
                    END;
                """,
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                name = "Folded case when to grab the true",
                query = """
                    CASE
                        WHEN FALSE THEN 0
                        WHEN TRUE THEN 'hello'
                    END;
                """,
                expected = unionOf(INT4, StaticType.STRING)
            ),
            SuccessTestCase(
                name = "Boolean case when",
                query = """
                    CASE 'Hello World'
                        WHEN 'Hello World' THEN TRUE
                        ELSE FALSE
                    END;
                """,
                expected = StaticType.BOOL
            ),
            SuccessTestCase(
                name = "Typing even with false condition",
                query = """
                    CASE
                        WHEN FALSE THEN 'IMPOSSIBLE TO GET'
                        ELSE TRUE
                    END;
                """,
                expected = unionOf(StaticType.STRING, StaticType.BOOL)
            ),
            SuccessTestCase(
                name = "Folded out false without default",
                query = """
                    CASE
                        WHEN FALSE THEN 'IMPOSSIBLE TO GET'
                    END;
                """,
                expected = StaticType.STRING
            ),
            SuccessTestCase(
                name = "Not folded gives us a nullable without default",
                query = """
                    CASE 1
                        WHEN 1 THEN TRUE
                        WHEN 2 THEN FALSE
                    END;
                """,
                expected = StaticType.BOOL
            ),
            SuccessTestCase(
                name = "Not folded gives us a nullable without default for query",
                query = """
                    SELECT
                        CASE breed
                            WHEN 'golden retriever' THEN 'fluffy dog'
                            WHEN 'pitbull' THEN 'short-haired dog'
                        END AS breed_descriptor
                    FROM dogs
                """,
                catalog = "pql",
                catalogPath = listOf("main"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "breed_descriptor" to StaticType.STRING,
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
                name = "Query",
                query = """
                    SELECT
                        CASE breed
                            WHEN 'golden retriever' THEN 'fluffy dog'
                            WHEN 'pitbull' THEN 'short-haired dog'
                            ELSE 'something else'
                        END AS breed_descriptor
                    FROM dogs
                """,
                catalog = "pql",
                catalogPath = listOf("main"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "breed_descriptor" to StaticType.STRING,
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
                name = "Query with heterogeneous data",
                query = """
                    SELECT
                        CASE breed
                            WHEN 'golden retriever' THEN 'fluffy dog'
                            WHEN 'pitbull' THEN 2
                            ELSE 2.0
                        END AS breed_descriptor
                    FROM dogs
                """,
                catalog = "pql",
                catalogPath = listOf("main"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "breed_descriptor" to StaticType.unionOf(
                                StaticType.STRING,
                                StaticType.INT4,
                                StaticType.DECIMAL
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
            //
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-00"),
                catalog = "pql",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-02"),
                catalog = "pql",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-03"),
                catalog = "pql",
                expected = StaticType.INT8
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-04"),
                catalog = "pql",
                expected = StaticType.INT
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-05"),
                catalog = "pql",
                expected = StaticType.INT
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-06"),
                catalog = "pql",
                expected = StaticType.INT
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-07"),
                catalog = "pql",
                expected = StaticType.INT8
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-09"),
                catalog = "pql",
                expected = StaticType.INT,
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-10"),
                catalog = "pql",
                expected = StaticType.DECIMAL,
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-11"),
                catalog = "pql",
                expected = StaticType.INT4,
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-12"),
                catalog = "pql",
                expected = StaticType.FLOAT
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-13"),
                catalog = "pql",
                expected = StaticType.FLOAT,
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-14"),
                catalog = "pql",
                expected = StaticType.STRING,
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-15"),
                catalog = "pql",
                expected = StaticType.STRING,
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-16"),
                catalog = "pql",
                expected = StaticType.CLOB,
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-17"),
                catalog = "pql",
                expected = StaticType.CLOB,
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-18"),
                catalog = "pql",
                expected = StaticType.STRING,
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-19"),
                catalog = "pql",
                expected = StaticType.STRING,
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-20"),
                catalog = "pql",
                expected = StaticType.ANY,
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-21"),
                catalog = "pql",
                expected = StaticType.STRING,
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-24"),
                catalog = "pql",
                expected = unionOf(StaticType.INT4, StaticType.INT8, StaticType.STRING),
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-25"),
                catalog = "pql",
                expected = unionOf(StaticType.INT4, StaticType.INT8, StaticType.STRING),
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-26"),
                catalog = "pql",
                expected = unionOf(StaticType.INT4, StaticType.INT8, StaticType.STRING),
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-27"),
                catalog = "pql",
                expected = unionOf(
                    StaticType.INT2,
                    StaticType.INT4,
                    StaticType.INT8,
                    StaticType.INT,
                    StaticType.DECIMAL,
                    StaticType.STRING,
                    StaticType.CLOB
                ),
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-28"),
                catalog = "pql",
                expected = unionOf(StaticType.INT2, StaticType.INT4, StaticType.INT8, StaticType.INT, StaticType.DECIMAL, StaticType.STRING, StaticType.CLOB),
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-29"),
                catalog = "pql",
                expected = STRUCT
            ),
            SuccessTestCase(
                name = "CASE-WHEN always MISSING",
                key = PartiQLTest.Key("basics", "case-when-30"),
                catalog = "pql",
                expected = ANY
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-31"),
                catalog = "pql",
                expected = StaticType.ANY
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-32"),
                catalog = "pql",
                expected = StaticType.ANY
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-33"),
                catalog = "pql",
                expected = StaticType.ANY
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "case-when-34"),
                catalog = "pql",
                expected = StaticType.ANY
            ),
        )

        @JvmStatic
        fun nullIf() = listOf(
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "nullif-00"),
                catalog = "pql",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "nullif-01"),
                catalog = "pql",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "nullif-02"),
                catalog = "pql",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "nullif-03"),
                catalog = "pql",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "nullif-04"),
                catalog = "pql",
                expected = StaticType.INT8
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "nullif-05"),
                catalog = "pql",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "nullif-06"),
                catalog = "pql",
                expected = ANY
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "nullif-07"),
                catalog = "pql",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "nullif-08"),
                catalog = "pql",
                expected = ANY
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "nullif-09"),
                catalog = "pql",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "nullif-11"),
                catalog = "pql",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "nullif-12"),
                catalog = "pql",
                expected = StaticType.INT8
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "nullif-13"),
                catalog = "pql",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "nullif-14"),
                catalog = "pql",
                expected = StaticType.STRING
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "nullif-15"),
                catalog = "pql",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "nullif-16"),
                catalog = "pql",
                expected = unionOf(StaticType.INT2, StaticType.INT4, StaticType.INT8, StaticType.INT, StaticType.DECIMAL)
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "nullif-17"),
                catalog = "pql",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "nullif-18"),
                catalog = "pql",
                expected = StaticType.ANY
            ),
        )

        @JvmStatic
        fun coalesce() = listOf(
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "coalesce-00"),
                catalog = "pql",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "coalesce-01"),
                catalog = "pql",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "coalesce-02"),
                catalog = "pql",
                expected = StaticType.DECIMAL
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "coalesce-03"),
                catalog = "pql",
                expected = StaticType.DECIMAL
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "coalesce-04"),
                catalog = "pql",
                expected = StaticType.DECIMAL
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "coalesce-05"),
                catalog = "pql",
                expected = StaticType.DECIMAL
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "coalesce-06"),
                catalog = "pql",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "coalesce-07"),
                catalog = "pql",
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "coalesce-08"),
                catalog = "pql",
                expected = StaticType.INT8
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "coalesce-09"),
                catalog = "pql",
                expected = StaticType.INT8
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "coalesce-10"),
                catalog = "pql",
                expected = INT8
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "coalesce-11"),
                catalog = "pql",
                expected = unionOf(StaticType.INT8, StaticType.STRING)
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "coalesce-12"),
                catalog = "pql",
                expected = unionOf(StaticType.INT8, StaticType.STRING)
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "coalesce-13"),
                catalog = "pql",
                expected = unionOf(
                    StaticType.INT2,
                    StaticType.INT4,
                    StaticType.INT8,
                    StaticType.INT,
                    StaticType.DECIMAL
                )
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "coalesce-14"),
                catalog = "pql",
                expected = unionOf(
                    StaticType.INT2,
                    StaticType.INT4,
                    StaticType.INT8,
                    StaticType.INT,
                    StaticType.DECIMAL,
                    StaticType.STRING
                )
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "coalesce-15"),
                catalog = "pql",
                expected = unionOf(StaticType.INT2, StaticType.INT4, StaticType.INT8, StaticType.INT, StaticType.DECIMAL, StaticType.STRING)
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "coalesce-16"),
                catalog = "pql",
                expected = StaticType.ANY
            ),
            SuccessTestCase(
                key = PartiQLTest.Key("basics", "coalesce-17"),
                catalog = "pql",
                expected = StaticType.ANY
            ),
        )

        @JvmStatic
        fun pathExpressions() = listOf(
            SuccessTestCase(
                name = "Index on literal list",
                query = """
                    [0, 1, 2, 3][0]
                """,
                expected = StaticType.INT4
            ),
            SuccessTestCase(
                name = "Index on global list",
                query = """
                    dogs[0].breed
                """,
                catalog = "pql",
                catalogPath = listOf("main"),
                expected = StaticType.STRING
            ),
            SuccessTestCase(
                name = "Index on list attribute of global table",
                query = """
                    SELECT typical_allergies[0] AS main_allergy FROM dogs
                """,
                catalog = "pql",
                catalogPath = listOf("main"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "main_allergy" to StaticType.STRING,
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
                name = "Pathing into resolved local variable without qualification",
                query = """
                    SELECT address.street AS s FROM employer;
                """,
                catalog = "pql",
                catalogPath = listOf("main"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "s" to StaticType.STRING,
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
                name = "Pathing into resolved local variable without qualification and with sensitivity",
                query = """
                    SELECT address."street" AS s FROM employer;
                """,
                catalog = "pql",
                catalogPath = listOf("main"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "s" to StaticType.STRING,
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
                name = "Pathing into resolved local variable without qualification and with indexing syntax",
                query = """
                    SELECT address['street'] AS s FROM employer;
                """,
                catalog = "pql",
                catalogPath = listOf("main"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "s" to StaticType.STRING,
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
                name = "Pathing into resolved local variable without qualification and with indexing syntax and fully-qualified FROM",
                query = """
                    SELECT e.address['street'] AS s FROM "pql"."main"."employer" AS e;
                """,
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "s" to StaticType.STRING,
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
                name = "Show that we can't use [<string>] to reference a value in a schema. It can only be used on tuples.",
                query = """
                    SELECT VALUE 1 FROM "pql"."main"['employer'] AS e;
                """,
                expected = BagType(StaticType.INT4),
                problemHandler = assertProblemExists(
                    ProblemGenerator.undefinedVariable(
                        id(sensitive("pql"), sensitive("main"))
                    )
                )
            ),
            ErrorTestCase(
                name = "Show that we can't use [<string>] to reference a schema in a catalog. It can only be used on tuples.",
                query = """
                    SELECT VALUE 1 FROM "pql"['main']."employer" AS e;
                """,
                expected = BagType(StaticType.INT4),
                problemHandler = assertProblemExists(
                    ProblemGenerator.undefinedVariable(sensitive("pql"))
                )
            ),
            SuccessTestCase(
                name = "Tuple indexing syntax on literal tuple with literal string key",
                query = """
                    { 'aBc': 1, 'AbC': 2.0 }['AbC'];
                """,
                expected = StaticType.DECIMAL
            ),
            // This should fail because the Spec says tuple indexing MUST use a literal string or explicit cast.
            ErrorTestCase(
                name = "Array indexing syntax on literal tuple with non-literal and non-cast key",
                query = """
                    { 'aBc': 1, 'AbC': 2.0 }['Ab' || 'C'];
                """,
                expected = ANY,
                problemHandler = assertProblemExists(
                    ProblemGenerator.expressionAlwaysReturnsMissing("Collections must be indexed with integers, found STRING")
                )
            ),
            // The reason this is ANY is because we do not have support for constant-folding. We don't know what
            //  CAST('Ab' || 'C' AS STRING) will evaluate to, and therefore, we don't know what the indexing operation
            //  will return.
            SuccessTestCase(
                name = "Tuple indexing syntax on literal tuple with explicit cast key",
                query = """
                    { 'aBc': 1, 'AbC': 2.0 }[CAST('Ab' || 'C' AS STRING)];
                """,
                expected = StaticType.ANY
            ),
        )

        @JvmStatic
        fun scalarFunctions() = listOf(
            SuccessTestCase(
                name = "UPPER on binding tuple of literal string",
                query = """
                    SELECT
                        UPPER(some_str) AS upper_str
                    FROM
                        << { 'some_str': 'hello world!' } >>
                        AS t
                """,
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "upper_str" to StaticType.STRING,
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
                name = "UPPER on literal string",
                query = """
                    UPPER('hello world')
                """,
                expected = StaticType.STRING
            ),
            SuccessTestCase(
                name = "UPPER on global string",
                query = """
                    UPPER(os)
                """,
                catalog = "pql",
                catalogPath = listOf("main"),
                expected = StaticType.STRING
            ),
            SuccessTestCase(
                name = "UPPER on global string",
                query = """
                    UPPER(os)
                """,
                catalog = "pql",
                catalogPath = listOf("main"),
                expected = StaticType.STRING
            ),
            SuccessTestCase(
                name = "UPPER on global struct",
                query = """
                    UPPER(person.ssn)
                """,
                catalog = "pql",
                catalogPath = listOf("main"),
                expected = StaticType.STRING
            ),
            SuccessTestCase(
                name = "UPPER on global nested struct",
                query = """
                    UPPER(person.name."first")
                """,
                catalog = "pql",
                catalogPath = listOf("main"),
                expected = StaticType.STRING
            ),
            SuccessTestCase(
                name = "UPPER on global table",
                query = """
                    SELECT UPPER(breed) AS upper_breed
                    FROM dogs
                """,
                catalog = "pql",
                catalogPath = listOf("main"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "upper_breed" to StaticType.STRING,
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
        fun aggregationCases() = listOf(
            SuccessTestCase(
                name = "AGGREGATE over INTS, without alias",
                query = "SELECT a, COUNT(*), COUNT(a), SUM(a), MIN(b), MAX(a) FROM << {'a': 1, 'b': 2} >> GROUP BY a",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StaticType.INT4,
                            "_1" to StaticType.INT8,
                            "_2" to StaticType.INT8,
                            "_3" to StaticType.INT4,
                            "_4" to StaticType.INT4,
                            "_5" to StaticType.INT4,
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
                name = "AGGREGATE over INTS, with alias",
                query = "SELECT a, COUNT(*) AS c_s, COUNT(a) AS c, SUM(a) AS s, MIN(b) AS m FROM << {'a': 1, 'b': 2} >> GROUP BY a",
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StaticType.INT4,
                            "c_s" to StaticType.INT8,
                            "c" to StaticType.INT8,
                            "s" to StaticType.INT4,
                            "m" to StaticType.INT4,
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
                            "c" to StaticType.INT8,
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
                name = "AGGREGATE over nullable integers",
                query = """
                    SELECT T1.a
                    FROM T1
                        LEFT JOIN T2 AS T2_1
                            ON T2_1.d =
                            (
                                SELECT
                                    CASE WHEN COUNT(f) = 1 THEN MAX(f) ELSE 0 END AS e
                                FROM T3 AS T3_mapping
                            )
                        LEFT JOIN T2 AS T2_2
                            ON T2_2.d =
                            (
                                SELECT
                                    CASE WHEN COUNT(f) = 1 THEN MAX(f) ELSE 0 END AS e
                                FROM T3 AS T3_mapping
                            )
                    ;
                """.trimIndent(),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StaticType.BOOL
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                ),
                catalog = "aggregations"
            )
        )

        @JvmStatic
        fun dynamicCalls() = listOf(
            SuccessTestCase(
                name = "unary plus on varying numeric types -- this cannot return missing!",
                query = """
                    SELECT +t.a AS a
                    FROM <<
                        { 'a': CAST(1 AS INT8) },
                        { 'a': CAST(1 AS INT4) }
                    >> AS t
                """.trimIndent(),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StaticType.unionOf(StaticType.INT4, StaticType.INT8),
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
                name = "unary plus on varying numeric types including missing -- this may return missing",
                query = """
                    SELECT +t.a AS a
                    FROM <<
                        { 'a': CAST(1 AS INT8) },
                        { 'a': CAST(1 AS INT4) },
                        { }
                    >> AS t
                """.trimIndent(),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StaticType.unionOf(StaticType.INT4, StaticType.INT8),
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
                name = "unary plus on varying numeric types including string -- this may return missing",
                query = """
                    SELECT +t.a AS a
                    FROM <<
                        { 'a': CAST(1 AS INT8) },
                        { 'a': CAST(1 AS INT4) },
                        { 'a': 'hello world!' }
                    >> AS t
                """.trimIndent(),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "a" to StaticType.unionOf(StaticType.INT4, StaticType.INT8),
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
                name = "binary plus on varying types -- this will return missing if one of the operands is not a number",
                query = """
                    SELECT t.a + t.b AS c
                    FROM <<
                        { 'a': CAST(1 AS INT8), 'b': CAST(1.0 AS DECIMAL) },
                        { 'a': CAST(1 AS INT4), 'b': TRUE },
                        { 'a': 'hello world!!', 'b': DATE '2023-01-01' }
                    >> AS t
                """.trimIndent(),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "c" to ANY,
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
                name = """
                    unary plus on non-compatible type -- this cannot resolve to a dynamic call since no function
                    will ever be invoked.
                """.trimIndent(),
                query = """
                    SELECT VALUE +t.a
                    FROM <<
                        { 'a': 'hello world!'  }
                    >> AS t
                """.trimIndent(),
                expected = BagType(ANY),
                problemHandler = assertProblemExists(
                    ProblemGenerator.incompatibleTypesForOp(
                        listOf(StaticType.STRING),
                        "POS",
                    )
                )
            ),
            SuccessTestCase(
                name = """
                    unary plus on dynamic types
                """.trimIndent(),
                query = """
                    SELECT VALUE +t.a
                    FROM <<
                        { 'a': 'hello world!'  },
                        { 'a': <<>> }
                    >> AS t
                """.trimIndent(),
                expected = BagType(ANY),
            ),
            ErrorTestCase(
                name = """
                    unary plus on missing type -- this cannot resolve to a dynamic call since no function
                    will ever be invoked.
                """.trimIndent(),
                query = """
                    SELECT VALUE +t.a
                    FROM <<
                        { 'NOT_A': 1 }
                    >> AS t
                """.trimIndent(),
                expected = BagType(ANY),
                problemHandler = assertProblemExists(
                    ProblemGenerator.undefinedVariable(
                        Identifier.Symbol("a", Identifier.CaseSensitivity.INSENSITIVE),
                        setOf("t"),
                    )
                )
            ),
            ErrorTestCase(
                name = """
                    unary plus on missing type -- this cannot resolve to a dynamic call since no function
                    will ever be invoked.
                """.trimIndent(),
                query = """
                    +MISSING
                """.trimIndent(),
                expected = StaticType.DECIMAL, // This is due to it being the highest precedence type
                problemHandler = assertProblemExists(
                    ProblemGenerator.expressionAlwaysReturnsMissing("Static function always receives MISSING arguments.")
                )
            ),
        )

        @JvmStatic
        fun subqueryCases() = listOf(
            SuccessTestCase(
                name = "Subquery IN collection",
                catalog = "subqueries",
                key = PartiQLTest.Key("subquery", "subquery-00"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "x" to StaticType.INT4,
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
                name = "Subquery scalar coercion",
                catalog = "subqueries",
                key = PartiQLTest.Key("subquery", "subquery-01"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "x" to StaticType.INT4,
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
                name = "Subquery simple JOIN",
                catalog = "subqueries",
                key = PartiQLTest.Key("subquery", "subquery-02"),
                expected = BagType(
                    StructType(
                        fields = mapOf(
                            "x" to StaticType.INT4,
                            "y" to StaticType.INT4,
                            "z" to StaticType.INT4,
                            "a" to StaticType.INT4,
                            "b" to StaticType.INT4,
                            "c" to StaticType.INT4,
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
                name = "Subquery scalar coercion",
                catalog = "subqueries",
                key = PartiQLTest.Key("subquery", "subquery-03"),
                expected = StaticType.BOOL,
            ),
        )

        // --------- Parameterized Test Source Finished ------------
    }

    private val testProvider = PartiQLTestProvider()

    init {
        // load test inputs
        testProvider.load()
    }

    //
    // Parameterized Tests
    //

    @Test
    @Disabled("We currently don't have the concept of an ordered struct.")
    fun orderedTuple() {
        val tc =
            SuccessTestCase(
                name = "Duplicate fields in ordered STRUCT. NOTE: b.b.d is an ordered struct with two attributes (e). First is INT4.",
                query = """
                    SELECT d.e AS e
                    FROM << b.b.d >> AS d
                """,
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("e", StaticType.INT4)
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                )
            )
        runTest(tc)
    }

    @Test
    @Disabled("The planner doesn't support heterogeneous input to aggregation functions (yet?).")
    fun failingTest() {
        val tc = SuccessTestCase(
            name = "AGGREGATE over heterogeneous data",
            query = """
                    SELECT
                        a AS a,
                        COUNT(*) AS count_star,
                        COUNT(a) AS count_a,
                        COUNT(b) AS count_b,
                        SUM(a) AS sum_a,
                        SUM(b) AS sum_b,
                        MIN(a) AS min_a,
                        MIN(b) AS min_b,
                        MAX(a) AS max_a,
                        MAX(b) AS max_b,
                        AVG(a) AS avg_a,
                        AVG(b) AS avg_b
                    FROM <<
                        { 'a': 1.0, 'b': 2.0 },
                        { 'a': 3, 'b': 4 },
                        { 'a': 5, 'b': NULL }
                    >> GROUP BY a
            """.trimIndent(),
            expected = BagType(
                StructType(
                    fields = mapOf(
                        "a" to StaticType.DECIMAL,
                        "count_star" to StaticType.INT8,
                        "count_a" to StaticType.INT8,
                        "count_b" to StaticType.INT8,
                        "sum_a" to StaticType.DECIMAL,
                        "sum_b" to StaticType.DECIMAL,
                        "min_a" to StaticType.DECIMAL,
                        "min_b" to StaticType.DECIMAL,
                        "max_a" to StaticType.DECIMAL,
                        "max_b" to StaticType.DECIMAL,
                        "avg_a" to StaticType.DECIMAL,
                        "avg_b" to StaticType.DECIMAL,
                    ),
                    contentClosed = true,
                    constraints = setOf(
                        TupleConstraint.Open(false),
                        TupleConstraint.UniqueAttrs(true),
                        TupleConstraint.Ordered
                    )
                )
            )
        )
        runTest(tc)
    }

    @Test
    fun developmentTest() {
        val tc = SuccessTestCase(
            name = "DEV TEST",
            query = "CAST('' AS STRING) < CAST('' AS SYMBOL);",
            expected = PType.bool().toCType()
        )
        runTest(tc)
    }

    @Test
    fun developmentTest3() {
        val tc =
            SuccessTestCase(
                name = "MISSING IS MISSING",
                key = key("is-type-06"),
                catalog = "pql",
                expected = StaticType.BOOL,
            )
        runTest(tc)
    }

    @Test
    fun developmentTest4() {
        val tc = SuccessTestCase(
            name = "DEV TEST 4",
            query = "NULLIF([], [])",
            expected = PType.array().toCType()
        )
        runTest(tc)
    }

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

    @ParameterizedTest
    @MethodSource("aggregationCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testAggregations(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("scalarFunctions")
    @Execution(ExecutionMode.CONCURRENT)
    fun testScalarFunctions(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("distinctClauseCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testDistinctClause(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("pathExpressions")
    @Execution(ExecutionMode.CONCURRENT)
    fun testPathExpressions(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("caseWhens")
    @Execution(ExecutionMode.CONCURRENT)
    fun testCaseWhens(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("nullIf")
    @Execution(ExecutionMode.CONCURRENT)
    fun testNullIf(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("coalesce")
    @Execution(ExecutionMode.CONCURRENT)
    fun testCoalesce(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("subqueryCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testSubqueries(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("dynamicCalls")
    @Execution(ExecutionMode.CONCURRENT)
    fun testDynamicCalls(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("scanCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testScan(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("pivotCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testPivot(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("isTypeCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testIsType(tc: TestCase) = runTest(tc)

    @ParameterizedTest
    @MethodSource("castCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testCasts(tc: TestCase) = runTest(tc)

    // --------- Finish Parameterized Tests ------

    //
    // Testing Utility
    //
    private fun infer(
        query: String,
        session: Session,
        problemCollector: ProblemCollector,
    ): PartiQLPlan {
        val ast = parser.parse(query).root
        return planner.plan(ast, session, problemCollector).plan
    }

    private fun runTest(tc: TestCase) = when (tc) {
        is SuccessTestCase -> runTest(tc)
        is ErrorTestCase -> runTest(tc)
        is ThrowingExceptionTestCase -> runTest(tc)
    }

    private fun runTest(tc: SuccessTestCase) {
        val session = Session.builder()
            .catalog(tc.catalog)
            .catalogs(*catalogs.toTypedArray())
            .namespace(tc.catalogPath)
            .build()

        val hasQuery = tc.query != null
        val hasKey = tc.key != null
        if (hasQuery == hasKey) {
            error("Test must have one of either `query` or `key`")
        }
        val input = tc.query ?: testProvider[tc.key!!]!!.statement

        val collector = ProblemCollector()
        val plan = infer(input, session, collector)
        when (val statement = plan.statement) {
            is Statement.Query -> {
                assert(collector.problems.isEmpty()) {
                    buildString {
                        appendLine(collector.problems.toString())
                        appendLine()
                        PlanPrinter.append(this, plan)
                    }
                }
                val actual = statement.root.type
                assert(tc.expected == actual) {
                    buildString {
                        appendLine()
                        appendLine("Expect: ${tc.expected}")
                        appendLine("Actual: $actual")
                        appendLine()
                        PlanPrinter.append(this, plan)
                    }
                }
            }
        }
    }

    private fun runTest(tc: ErrorTestCase) {
        val session = Session.builder()
            .catalog(tc.catalog)
            .catalogs(*catalogs.toTypedArray())
            .namespace(tc.catalogPath)
            .build()
        val collector = ProblemCollector()

        val hasQuery = tc.query != null
        val hasKey = tc.key != null
        if (hasQuery == hasKey) {
            error("Test must have one of either `query` or `key`")
        }
        val input = tc.query ?: testProvider[tc.key!!]!!.statement
        val plan = infer(input, session, collector)

        when (val statement = plan.statement) {
            is Statement.Query -> {
                assert(collector.problems.isNotEmpty()) {
                    buildString {
                        appendLine("Expected to find problems, but none were found.")
                        appendLine()
                        PlanPrinter.append(this, plan)
                    }
                }
                if (tc.expected != null) {
                    assert(tc.expected == statement.root.type) {
                        buildString {
                            appendLine()
                            appendLine("Expect: ${tc.expected}")
                            appendLine("Actual: ${statement.root.type}")
                            appendLine()
                            PlanPrinter.append(this, plan)
                        }
                    }
                }
                assert(collector.problems.isNotEmpty()) {
                    "Expected to find problems, but none were found."
                }
                tc.problemHandler?.handle(collector.problems, true)
            }
        }
    }

    private fun runTest(tc: ThrowingExceptionTestCase) {
        val session = Session.builder()
            .catalog(tc.catalog)
            .catalogs(*catalogs.toTypedArray())
            .namespace(tc.catalogPath)
            .build()
        val collector = ProblemCollector()
        val exception = assertThrows<Throwable> {
            infer(tc.query, session, collector)
            Unit
        }
        val cause = exception.cause
        assertNotNull(cause)
        assertEquals(tc.expectedThrowable, cause::class)
    }

    //
    // Additional Test
    //
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
                            fields = listOf(
                                StructType.Field("_1", ANY)
                            ),
                            contentClosed = true,
                            constraints = setOf(
                                TupleConstraint.Open(false),
                                TupleConstraint.UniqueAttrs(true),
                            )
                        ),
                    )
                ),
                problemHandler = assertProblemExists(
                    ProblemGenerator.undefinedVariable(insensitive("pets"))
                )
            ),
            TestCase.ErrorTestCase(
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
                            fields = listOf(
                                StructType.Field("_1", ANY)
                            ),
                            contentClosed = true,
                            constraints = setOf(
                                TupleConstraint.Open(false),
                                TupleConstraint.UniqueAttrs(true),
                            )
                        ),
                    )
                ),
                problemHandler = assertProblemExists(
                    ProblemGenerator.undefinedVariable(insensitive("pets"))
                )
            ),
            TestCase.SuccessTestCase(
                name = "Project all explicitly",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM pets",
                expected = TABLE_AWS_DDB_PETS
            ),
            TestCase.SuccessTestCase(
                name = "Project all implicitly",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT id, breed FROM pets",
                expected = TABLE_AWS_DDB_PETS
            ),
            TestCase.SuccessTestCase(
                name = "Test #4",
                catalog = CATALOG_B,
                catalogPath = listOf("b"),
                query = "b",
                expected = TYPE_B_B_B
            ),
            TestCase.SuccessTestCase(
                name = "Test #5",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM b",
                expected = TABLE_AWS_DDB_B
            ),
            TestCase.SuccessTestCase(
                name = "Test #6",
                catalog = CATALOG_AWS,
                catalogPath = listOf("b"),
                query = "SELECT * FROM b",
                expected = TABLE_AWS_B_B
            ),
            TestCase.ErrorTestCase(
                name = "Test #7",
                query = "SELECT * FROM ddb.pets",
                expected = BagType(
                    unionOf(
                        StructType(
                            fields = emptyList(),
                            contentClosed = false,
                            constraints = setOf(
                                TupleConstraint.Open(true),
                                TupleConstraint.UniqueAttrs(false),
                            )
                        ),
                        StructType(
                            fields = listOf(
                                StructType.Field("_1", ANY)
                            ),
                            contentClosed = true,
                            constraints = setOf(
                                TupleConstraint.Open(false),
                                TupleConstraint.UniqueAttrs(true),
                            )
                        ),
                    )
                ),
                problemHandler = assertProblemExists(
                    ProblemGenerator.undefinedVariable(id(insensitive("ddb"), insensitive("pets")))
                )
            ),
            TestCase.SuccessTestCase(
                name = "Test #10",
                catalog = CATALOG_B,
                query = "b.b",
                expected = TYPE_B_B_B
            ),
            TestCase.SuccessTestCase(
                name = "Test #11",
                catalog = CATALOG_B,
                catalogPath = listOf("b"),
                query = "b.b",
                expected = TYPE_B_B_B_B
            ),
            TestCase.SuccessTestCase(
                name = "Test #12",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM b",
                expected = TABLE_AWS_DDB_B
            ),
            TestCase.SuccessTestCase(
                name = "Test #13",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM ddb.b",
                expected = TABLE_AWS_DDB_B
            ),
            TestCase.SuccessTestCase(
                name = "Test #14",
                query = "SELECT * FROM aws.ddb.pets",
                expected = TABLE_AWS_DDB_PETS
            ),
            TestCase.SuccessTestCase(
                name = "Test #15",
                catalog = CATALOG_AWS,
                query = "SELECT * FROM aws.b.b",
                expected = TABLE_AWS_B_B
            ),
            TestCase.SuccessTestCase(
                name = "Test #16",
                catalog = CATALOG_B,
                query = "b.b.b",
                expected = TYPE_B_B_B_B
            ),
            TestCase.SuccessTestCase(
                name = "Test #17",
                catalog = CATALOG_B,
                query = "b.b.c",
                expected = TYPE_B_B_C
            ),
            TestCase.SuccessTestCase(
                name = "Test #18",
                catalog = CATALOG_B,
                catalogPath = listOf("b"),
                query = "b.b.b",
                expected = TYPE_B_B_B_B_B
            ),
            TestCase.SuccessTestCase(
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
                expected = TYPE_B_B_B_B_B
            ),
            SuccessTestCase(
                name = "Test #22",
                catalog = CATALOG_B,
                query = "b.b.c",
                expected = TYPE_B_B_B_C
            ),
            SuccessTestCase(
                name = "Test #23",
                catalog = CATALOG_B,
                catalogPath = listOf("b"),
                query = "b.b.b",
                expected = TYPE_B_B_B_B_B
            ),
            SuccessTestCase(
                name = "Test #24",
                query = "b.b.b.b.b",
                expected = TYPE_B_B_B_B_B
            ),
            SuccessTestCase(
                name = "Test #25",
                catalog = CATALOG_B,
                query = "b.b.b.b",
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
                expected = ANY,
                problemHandler = assertProblemExists(
                    ProblemGenerator.incompatibleTypesForOp(
                        listOf(StaticType.INT4, StaticType.STRING),
                        "" +
                            "IN_COLLECTION",
                    )
                )
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
                expected = ANY,
                problemHandler = assertProblemExists(
                    ProblemGenerator.incompatibleTypesForOp(
                        listOf(
                            StaticType.INT4,
                            StaticType.INT4,
                            StaticType.STRING
                        ),
                        "BETWEEN",
                    )
                )
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
                expected = ANY,
                problemHandler = assertProblemExists(
                    ProblemGenerator.incompatibleTypesForOp(
                        listOf(StaticType.STRING, StaticType.INT4),
                        "LIKE",
                    )
                )
            ),
            SuccessTestCase(
                name = "Case Insensitive success",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.CUSTOMER_ID = 1",
                expected = TYPE_BOOL
            ),
            // MISSING = 1
            // TODO: Semantic not finalized
            ErrorTestCase(
                name = "Case Sensitive failure",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.\"CUSTOMER_ID\" = 1",
                expected = StaticType.BOOL
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
                expected = StaticType.BOOL
            ),
            SuccessTestCase(
                name = "2-Level Junction",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "(order_info.customer_id = 1) AND (order_info.marketplace_id = 2) OR (order_info.customer_id = 3) AND (order_info.marketplace_id = 4)",
                expected = StaticType.BOOL
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
                // non_existing_column get typed as missing
                // Function resolves to EQ__ANY_ANY__BOOL
                // Which can return BOOL Or NULL
                expected = StaticType.BOOL,
                problemHandler = assertProblemExists(
                    ProblemGenerator.undefinedVariable(insensitive("non_existing_column"), emptySet())
                )
            ),
            ErrorTestCase(
                name = "Bad comparison",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "order_info.customer_id = 1 AND 1",
                expected = ANY,
                problemHandler = assertProblemExists(
                    ProblemGenerator.incompatibleTypesForOp(
                        listOf(StaticType.BOOL, StaticType.INT4),
                        "AND",
                    )
                )
            ),
            ErrorTestCase(
                name = "Bad comparison",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "1 AND order_info.customer_id = 1",
                expected = ANY,
                problemHandler = assertProblemExists(
                    ProblemGenerator.incompatibleTypesForOp(
                        listOf(StaticType.INT4, StaticType.BOOL),
                        "AND",
                    )
                )
            ),
            ErrorTestCase(
                name = "Unknown column",
                catalog = CATALOG_DB,
                catalogPath = DB_SCHEMA_MARKETS,
                query = "SELECT unknown_col FROM orders WHERE customer_id = 1",
                expected = BagType(
                    StructType(
                        fields = listOf(
                            StructType.Field("unknown_col", ANY)
                        ),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                ),
                problemHandler = assertProblemExists(
                    ProblemGenerator.undefinedVariable(insensitive("unknown_col"), setOf("orders"))
                )
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
                problemHandler = assertProblemExists(
                    ProblemGenerator.unexpectedType(
                        StaticType.STRING, setOf(StaticType.INT)
                    )
                )
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
                problemHandler = assertProblemExists(
                    ProblemGenerator.unexpectedType(StaticType.STRING, setOf(StaticType.INT))
                )
            ),
            SuccessTestCase(
                name = "CAST",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT CAST(breed AS INT) AS cast_breed FROM pets",
                expected = BagType(
                    StructType(
                        fields = mapOf("cast_breed" to StaticType.INT4),
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
                        fields = mapOf("upper_breed" to StaticType.STRING),
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
                        fields = mapOf("a" to ListType(StaticType.unionOf(StaticType.INT4, StaticType.DECIMAL))),
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
                BagType(ListType(StaticType.unionOf(StaticType.INT4, StaticType.DECIMAL)))
            ),
            SuccessTestCase(
                name = "SELECT VALUE",
                query = "SELECT VALUE [1, 1.0] FROM <<>>",
                expected =
                BagType(ListType(StaticType.unionOf(StaticType.INT4, StaticType.DECIMAL)))
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
                            StructType.Field("a", StaticType.unionOf(StaticType.INT4, StaticType.STRING))
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
                            StructType.Field("a", StaticType.unionOf(StaticType.INT4, StaticType.STRING))
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
                expected = StaticType.STRING
            ),
            SuccessTestCase(
                name = "Trim",
                query = "trim(' ')",
                expected = StaticType.STRING
            ),
            SuccessTestCase(
                name = "Current User Concat",
                query = "CURRENT_USER || 'hello'",
                expected = StaticType.STRING
            ),
            SuccessTestCase(
                name = "Current User Concat in WHERE",
                query = "SELECT VALUE a FROM [ 0 ] AS a WHERE CURRENT_USER = 'hello'",
                expected = BagType(StaticType.INT4)
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
                expected = ANY,
                problemHandler = assertProblemExists(
                    ProblemGenerator.incompatibleTypesForOp(
                        listOf(StaticType.STRING, StaticType.INT4),
                        "TRIM_CHARS",
                    )
                )
            ),
        )
    }
}

fun interface ProblemHandler {
    fun handle(problems: List<Problem>, ignoreSourceLocation: Boolean)
}
