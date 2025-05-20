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
    companion object {
        val parser = PartiQLParser.standard()
        val planner = PartiQLPlanner.standard()
        val compiler = PartiQLCompiler.standard()
    }

    @Test
    fun absent_scalar_extension_function() {
        val query = "SELECT abs(x) as a, foo_bar_baz(x) as b, FOO_BAR_BAZ(x) as C FROM << {'x':1}, {'x':2} >>"
        val catalog = object : Catalog {
            override fun getName(): String = "default"
        }
        val result = testQuery(query, catalog)
        assert(result is TestResult.Failure)
        val errs = (result as TestResult.Failure).err
        assertTrue(errs.errors.isNotEmpty())
        assertEquals(PError.FUNCTION_NOT_FOUND, errs.errors[0].code())
    }

    @Test
    fun scalar_extension_function() {
        val query = "SELECT abs(x) as a, foo_bar_baz(x) as b, FOO_BAR_BAZ(x) as C FROM << {'x':1}, {'x':2} >>"
        val catalog = object : Catalog {
            override fun getName(): String = "default"
            override fun getFunctions(session: Session, name: String): Collection<FnOverload> {
                return listOf(
                    // Just a `abs(F64, F64) -> F64` with a different name
                    FnOverload.Builder("foo_bar_baz")
                        .addParameters(Parameter("value", PType.doublePrecision()))
                        .returns(PType.doublePrecision())
                        .body { args ->
                            val value = args[0].double
                            Datum.doublePrecision(value.absoluteValue)
                        }
                        .build()
                )
            }
        }
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
    fun absent_aggregate_extension_function() {
        val query = "SELECT SUM(x) as a, foo_bar_baz(x) as b, FOO_BAR_BAZ(x) as C FROM << {'x':1}, {'x':2} >>"
        val catalog = object : Catalog {
            override fun getName(): String = "default"
        }
        val result = testQuery(query, catalog)
        assert(result is TestResult.Failure)
        val errs = (result as TestResult.Failure).err
        assertTrue(errs.errors.isNotEmpty())
        assertEquals(PError.FUNCTION_NOT_FOUND, errs.errors[0].code())
    }

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

    @Test
    fun aggregate_extension_function() {
        val query = "SELECT SUM(x) as a, foo_bar_baz(x) as b, FOO_BAR_BAZ(x) as C FROM << {'x':1}, {'x':2} >>"
        val catalog = object : Catalog {
            override fun getName(): String = "default"
            override fun getAggregations(session: Session, name: String): Collection<AggOverload> {
                return listOf(
                    // Just a `SUM(F64) -> F64` with a different name
                    AggOverload.Builder("foo_bar_baz")
                        .addParameters(PType.doublePrecision())
                        .returns(PType.doublePrecision())
                        .body { AccumulatorSumDouble() }
                        .build()
                )
            }
        }
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

    sealed class TestResult {
        class Failure(val err: PErrorCollector) : TestResult()
        class Success(val result: Datum) : TestResult()
    }

    private fun testQuery(query: String, catalog: Catalog): TestResult {
        val pc = PErrorCollector()
        val planResult = planQuery(query, catalog, pc)
        println("pc.errors = ${pc.errors}")
        if (pc.errors.isNotEmpty()) {
            return TestResult.Failure(pc)
        }
        val exec = compiler.prepare(planResult.plan, Mode.STRICT())
        return TestResult.Success(exec.execute())
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
