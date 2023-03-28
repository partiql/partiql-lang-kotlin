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
import org.partiql.lang.errors.ProblemCollector
import org.partiql.lang.planner.transforms.PartiQLSchemaInferencerTests.TestCase.ErrorTestCase
import org.partiql.lang.planner.transforms.PartiQLSchemaInferencerTests.TestCase.SuccessTestCase
import org.partiql.spi.plugins.local.LocalPlugin
import org.partiql.spi.sources.ColumnMetadata
import org.partiql.spi.sources.ValueDescriptor
import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.BlobType
import org.partiql.types.BoolType
import org.partiql.types.ClobType
import org.partiql.types.DateType
import org.partiql.types.DecimalType
import org.partiql.types.FloatType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.SymbolType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType
import java.net.URL
import java.time.Instant
import java.util.stream.Stream

class PartiQLSchemaInferencerTests {

    private val plugins = listOf(LocalPlugin())

    @Test
    public fun test() {
        val session = PlannerSession("1", "test")
        val ctx = PartiQLSchemaInferencer.Context(session, plugins, ProblemCollector())
        PartiQLSchemaInferencer.infer("1", ctx)
    }

    private val userId = "TEST_USER"
    private val catalogMap = listOf("aws", "b").associate { catalogName ->
        val catalogUrl: URL =
            PartiQLSchemaInferencerTests::class.java.classLoader.getResource("catalogs/$catalogName") ?: error("Couldn't be found")
        catalogName to ionStructOf(
            field("connector_name", ionString("localdb")),
            field("localdb_root", ionString(catalogUrl.path))
        )
    }

    companion object {
        private const val DEFAULT_TABLE_NAME = "UNSPECIFIED"
        const val CATALOG_AWS = "aws"
        const val CATALOG_B = "b"
        private val TYPE_AWS_DDB_PETS_ID = ValueDescriptor.TypeDescriptor(StaticType.INT)
        private val TYPE_AWS_DDB_PETS_BREED = ValueDescriptor.TypeDescriptor(StaticType.STRING)
        val TABLE_AWS_DDB_PETS = ValueDescriptor.TableDescriptor(
            name = DEFAULT_TABLE_NAME,
            attributes = listOf(
                ColumnMetadata("id", TYPE_AWS_DDB_PETS_ID.type),
                ColumnMetadata("breed", TYPE_AWS_DDB_PETS_BREED.type)
            )
        )
        val TABLE_AWS_DDB_B = ValueDescriptor.TableDescriptor(
            name = DEFAULT_TABLE_NAME,
            attributes = listOf(ColumnMetadata("identifier", StaticType.STRING))
        )
        val TABLE_AWS_B_B = ValueDescriptor.TableDescriptor(
            name = DEFAULT_TABLE_NAME,
            attributes = listOf(ColumnMetadata("identifier", StaticType.INT))
        )
        val TYPE_B_B_B_B_B = ValueDescriptor.TypeDescriptor(StaticType.INT)
        private val TYPE_B_B_B_B = ValueDescriptor.TypeDescriptor(StructType(mapOf("b" to TYPE_B_B_B_B_B.type)))
        val TYPE_B_B_B_C = ValueDescriptor.TypeDescriptor(StaticType.INT)
        val TYPE_B_B_C = ValueDescriptor.TypeDescriptor(StaticType.INT)
        val TYPE_B_B_B = ValueDescriptor.TypeDescriptor(
            StructType(
                fields = mapOf(
                    "b" to TYPE_B_B_B_B.type,
                    "c" to TYPE_B_B_B_C.type
                )
            )
        )

        // TODO: Need to throw errors on bad references
        val TABLE_ERROR = ValueDescriptor.TableDescriptor(
            DEFAULT_TABLE_NAME,
            attributes = listOf(ColumnMetadata("_1", StaticType.ANY))
        )
    }

    // TODO: Currently, SELECT a.a, a FROM <<{'a': 1}>> AS a isn't working, and it's causing issues. To remediate,
    //  we need to add the input binding tuple to the PROJ operator's schema. This causes SELECT * FROM anything to fail.
    @ParameterizedTest
    @ArgumentsSource(TestProvider::class)
    fun test(tc: TestCase): Unit = when (tc) {
        is SuccessTestCase -> {
            val session = PlannerSession(
                tc.query.hashCode().toString(),
                userId,
                tc.catalog,
                tc.catalogPath,
                catalogMap,
                Instant.now()
            )
            val ctx = PartiQLSchemaInferencer.Context(session, plugins, ProblemCollector())
            val result = PartiQLSchemaInferencer.infer(tc.query, ctx)
            println(tc.name)
            println("${tc.name} Expected: ${tc.expected}")
            println("${tc.name} Actual: $result")
            assert(descriptorEquals(tc.expected, result)) {
                println("Expected: ${tc.expected}")
                println("Actual: $result")
            }
        }
        is ErrorTestCase -> {
            val session = PlannerSession(
                tc.query.hashCode().toString(),
                userId,
                tc.catalog,
                tc.catalogPath,
                catalogMap,
                Instant.now()
            )
            // TODO: Need to pass in an ErrorHandler and assert the error thrown
            val ctx = PartiQLSchemaInferencer.Context(session, plugins, ProblemCollector())
            val result = PartiQLSchemaInferencer.infer(tc.query, ctx)
            println(tc.name)
            println("${tc.name} Expected: $TABLE_ERROR")
            println("${tc.name} Actual: $result")
            assert(descriptorEquals(TABLE_ERROR, result)) {
                println("Expected: $TABLE_ERROR")
                println("Actual: $result")
            }
        }
    }

    @Test
    fun singleTest() {
        val tc = SuccessTestCase(
            name = "Single Test",
            catalog = CATALOG_AWS,
            catalogPath = listOf("ddb"),
            query = "SELECT * FROM pets",
            expected = TABLE_AWS_DDB_PETS
        )
        test(tc)
    }

    sealed class TestCase() {
        class SuccessTestCase(
            val name: String,
            val query: String,
            val catalog: String? = null,
            val catalogPath: List<String> = emptyList(),
            val expected: ValueDescriptor
        ) : TestCase() {
            override fun toString(): String = "$name : $query"
        }

        class ErrorTestCase(
            val name: String,
            val query: String,
            val catalog: String? = null,
            val catalogPath: List<String> = emptyList()
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
                name = "Test #1",
                query = "SELECT * FROM pets"
            ),
            ErrorTestCase(
                name = "Test #2",
                catalog = CATALOG_AWS,
                query = "SELECT * FROM pets"
            ),
            SuccessTestCase(
                name = "Test #3",
                catalog = CATALOG_AWS,
                catalogPath = listOf("ddb"),
                query = "SELECT * FROM pets",
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
                query = "SELECT * FROM ddb.pets"
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
                expected = TYPE_B_B_B
            ),
            SuccessTestCase(
                name = "Test #21",
                catalog = CATALOG_B,
                query = "b.b.b.b",
                expected = TYPE_B_B_B
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
                expected = TYPE_B_B_B
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
            )
        )
    }

    private fun descriptorEquals(expected: ValueDescriptor, actual: ValueDescriptor): Boolean = when (expected) {
        is ValueDescriptor.TypeDescriptor -> when (actual) {
            is ValueDescriptor.TypeDescriptor -> expected.type.isEqualTo(actual.type)
            else -> false
        }
        is ValueDescriptor.TableDescriptor -> when (actual) {
            is ValueDescriptor.TableDescriptor -> expected == actual
            else -> false
        }
    }

    private fun StaticType.isEqualTo(type: StaticType): Boolean = when (this) {
        is AnyOfType -> when (type) {
            is AnyOfType -> {
                this.types.all { lhs ->
                    type.types.any { rhs -> lhs.isEqualTo(rhs) }
                }
            }
            else -> false
        }
        is AnyType -> type is AnyType
        is BlobType -> type is BlobType
        is BoolType -> type is BoolType
        is ClobType -> type is ClobType
        is BagType -> when (type) {
            is BagType -> this.elementType.isEqualTo(type.elementType)
            else -> false
        }
        is IntType -> when (type) {
            is IntType -> this.rangeConstraint == type.rangeConstraint
            else -> false
        }
        is ListType -> when (type) {
            is ListType -> this.elementType.isEqualTo(type.elementType)
            else -> false
        }
        is SexpType -> when (type) {
            is SexpType -> this.elementType.isEqualTo(type.elementType)
            else -> false
        }
        is DateType -> type is DateType
        is DecimalType -> type is DecimalType // TODO: Params
        is FloatType -> type is FloatType // TODO: Params
        MissingType -> type is MissingType
        is NullType -> type is NullType
        is StringType -> type is StringType // TODO: Params
        is StructType -> type is StructType // TODO: Params
        is SymbolType -> type is SymbolType // TODO: Params
        is TimeType -> type is TimeType // TODO: Params
        is TimestampType -> type is TimestampType // TODO: Params
    }
}
