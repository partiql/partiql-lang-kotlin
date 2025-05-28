package org.partiql.eval

import org.junit.jupiter.api.Test
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.Context
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorListener
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.function.AggOverload
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import kotlin.math.absoluteValue
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogTest {

    internal class AccumulatorSumDouble : org.partiql.spi.function.Accumulator {
        var sum: Double = 0.0
        var init = false

        override fun next(args: Array<out Datum>?) {
            val value = args!![0]
            if (!init) {
                init = true
            }
            val arg1 = value.double
            sum += arg1
        }

        override fun value(): Datum {
            return if (init) Datum.doublePrecision(sum) else Datum.nullValue(PType.doublePrecision())
        }
    }

    companion object {
        val parser = PartiQLParser.standard()
        val planner = PartiQLPlanner.standard()
        val compiler = PartiQLCompiler.standard()

        // Just a `abs(F64) -> F64` with a different name
        val scalar_overload: (name: String) -> FnOverload = { name: String ->
            FnOverload.Builder(name)
                .addParameters(Parameter("value", PType.doublePrecision()))
                .returns(PType.doublePrecision())
                .body { args ->
                    val value = args[0].double
                    Datum.doublePrecision(value.absoluteValue)
                }
                .build()
        }

        // Just a `SUM(F64) -> F64` with a different name
        val aggregate_overload: (name: String) -> AggOverload = { name: String ->
            AggOverload.Builder(name)
                .addParameters(PType.doublePrecision())
                .returns(PType.doublePrecision())
                .body { AccumulatorSumDouble() }
                .build()
        }
    }

    private fun catalog_with(fns: Collection<FnOverload>, aggs: Collection<AggOverload>): Catalog {
        val catalog = object : Catalog {
            val catalogFns: Map<String, Collection<FnOverload>>
            val catalogAggs: Map<String, Collection<AggOverload>>

            init {
                val fnsBuilder: MutableMap<String, MutableList<FnOverload>> = mutableMapOf()
                fns.map { fn -> fn.signature.name.lowercase() to fn }.forEach {
                    fnsBuilder.getOrPut(it.first, { mutableListOf() }).add(it.second)
                }
                catalogFns = fnsBuilder

                val aggsBuilder: MutableMap<String, MutableList<AggOverload>> = mutableMapOf()
                aggs.map { agg -> agg.signature.name.lowercase() to agg }.forEach {
                    aggsBuilder.getOrPut(it.first, { mutableListOf() }).add(it.second)
                }
                catalogAggs = aggsBuilder
            }

            override fun getName(): String = "default"
            override fun getFunctions(session: Session, name: String) = catalogFns.get(name.lowercase()) ?: emptyList()
            override fun getAggregations(session: Session, name: String) = catalogAggs.get(name.lowercase()) ?: emptyList()
        }

        return catalog
    }

    @Test
    fun absent_scalar_extension_function() {
        val query = "SELECT abs(x) as a, foo_bar_baz(x) as b, FOO_BAR_BAZ(x) as C FROM << {'x':1}, {'x':2} >>"
        val catalog = catalog_with(emptyList(), emptyList())
        val result = testQuery(query, catalog)
        assert(result is TestResult.Failure)
        val errs = (result as TestResult.Failure).err
        assertTrue(errs.errors.isNotEmpty())
        assertEquals(PError.FUNCTION_NOT_FOUND, errs.errors[0].code())
    }

    @Test
    fun scalar_extension_function() {
        val query = "SELECT abs(x) as a, foo_bar_baz(x) as b, FOO_BAR_BAZ(x) as C FROM << {'x':1}, {'x':2} >>"
        val catalog = catalog_with(listOf(scalar_overload("foo_bar_baz")), emptyList())
        val result = testQuery(query, catalog)
        assert(result is TestResult.Success)
        val datum = (result as TestResult.Success).result
        val expected = Datum.bagVararg(
            Datum.struct(
                Field.of("a", Datum.doublePrecision(1.0)),
                Field.of("b", Datum.doublePrecision(1.0)),
                Field.of("C", Datum.doublePrecision(1.0))
            ),
            Datum.struct(
                Field.of("a", Datum.doublePrecision(2.0)),
                Field.of("b", Datum.doublePrecision(2.0)),
                Field.of("C", Datum.doublePrecision(2.0))
            )
        )
        assertEquals(0, Datum.comparator().compare(expected, datum))
    }

    @Test
    fun scalar_extension_function_builtin_collision() {
        val query = "SELECT abs(x) as a, count(x) as b, COUNT(x) as C FROM << {'x':1}, {'x':2} >>"
        val catalog = catalog_with(listOf(scalar_overload("count")), emptyList())
        val result = testQuery(query, catalog)

        assert(result is TestResult.Failure)
        val errs = (result as TestResult.Failure).err
        assertTrue(errs.errors.isNotEmpty())
        assertEquals(PError.INTERNAL_ERROR, errs.errors[0].code())
    }

    @Test
    fun absent_aggregate_extension_function() {
        val query = "SELECT SUM(x) as a, foo_bar_baz(x) as b, FOO_BAR_BAZ(x) as C FROM << {'x':1}, {'x':2} >>"
        val catalog = catalog_with(emptyList(), emptyList())
        val result = testQuery(query, catalog)
        assert(result is TestResult.Failure)
        val errs = (result as TestResult.Failure).err
        assertTrue(errs.errors.isNotEmpty())
        assertEquals(PError.FUNCTION_NOT_FOUND, errs.errors[0].code())
    }

    @Test
    fun aggregate_extension_function() {
        val query = "SELECT SUM(x) as a, foo_bar_baz(x) as b, FOO_BAR_BAZ(x) as C FROM << {'x':1}, {'x':2} >>"
        val catalog = catalog_with(emptyList(), listOf(aggregate_overload("foo_bar_baz")))
        val result = testQuery(query, catalog)
        assert(result is TestResult.Success)
        val datum = (result as TestResult.Success).result
        val expected = Datum.bagVararg(
            Datum.struct(
                Field.of("a", Datum.doublePrecision(3.0)),
                Field.of("b", Datum.doublePrecision(3.0)),
                Field.of("C", Datum.doublePrecision(3.0))
            ),
        )
        assertEquals(0, Datum.comparator().compare(expected, datum))
    }

    @Test
    fun aggregate_extension_function_builtin_collision() {
        val query = "SELECT SUM(x) as a, upper(x) as b, UPPER(x) as C FROM << {'x':1}, {'x':2} >>"
        val catalog = catalog_with(emptyList(), listOf(aggregate_overload("upper")))
        val result = testQuery(query, catalog)

        assert(result is TestResult.Failure)
        val errs = (result as TestResult.Failure).err
        assertTrue(errs.errors.isNotEmpty())
        assertEquals(PError.INTERNAL_ERROR, errs.errors[0].code())
    }

    @Test
    fun scalar_and_aggregate_extension_function_collision() {
        val query = "SELECT SUM(x) as a, foo_bar_baz(x) as b, FOO_BAR_BAZ(x) as C FROM << {'x':1}, {'x':2} >>"
        val catalog = catalog_with(listOf(scalar_overload("foo_bar_baz")), listOf(aggregate_overload("foo_bar_baz")))
        val result = testQuery(query, catalog)

        assert(result is TestResult.Failure)
        val errs = (result as TestResult.Failure).err
        assertTrue(errs.errors.isNotEmpty())
        assertEquals(PError.INTERNAL_ERROR, errs.errors[0].code())
    }

    sealed class TestResult {
        class Failure(val err: PErrorCollector) : TestResult()
        class Success(val result: Datum) : TestResult()
        class Exception(val e: PRuntimeException) : TestResult()
    }

    private fun testQuery(query: String, catalog: Catalog): TestResult {
        val pc = PErrorCollector()
        val planResult = planQuery(query, catalog, pc)
        println("pc.errors = ${pc.errors}")
        if (pc.errors.isNotEmpty()) {
            return TestResult.Failure(pc)
        }

        try {
            val exec = compiler.prepare(planResult.plan, Mode.STRICT())
            return TestResult.Success(exec.execute())
        } catch (e: PRuntimeException) {
            return TestResult.Exception(e)
        }
    }

    private fun planQuery(query: String, catalog: Catalog, collector: PErrorListener): PartiQLPlanner.Result {
        val parseResult = parser.parse(query)
        assertEquals(1, parseResult.statements.size)
        val ast = parseResult.statements[0]
        val config = Context.of(collector)
        val session = Session.builder().catalog("default").catalogs(catalog).build()
        return planner.plan(ast, session, config)
    }
}
