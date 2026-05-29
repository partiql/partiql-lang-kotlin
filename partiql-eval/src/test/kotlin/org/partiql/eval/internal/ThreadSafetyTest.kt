package org.partiql.eval.internal

import org.junit.jupiter.api.RepeatedTest
import org.partiql.eval.ExecutionPlan
import org.partiql.eval.Mode
import org.partiql.eval.PartiQLVM
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.ExecutionCatalog
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.function.Agg
import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.value.Datum
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.test.assertEquals

class ThreadSafetyTest {

    private val parser = PartiQLParser.standard()
    private val planner = PartiQLPlanner.builder().useRefs().build()
    private val compiler = PartiQLCompiler.standard()
    private val vm = PartiQLVM.standard()

    private fun compileQuery(query: String, session: Session): Pair<ExecutionPlan, Session> {
        val ast = parser.parse(query).statements[0]
        val result = planner.plan(ast, session)
        val execPlan = compiler.compile(result.plan)
        return execPlan to session
    }

    @RepeatedTest(5)
    fun concurrentExecutionWithDifferentData() {
        val catalog = Catalog.builder()
            .name("test")
            .define(Table.standard(Name.of("t"), Datum.bagVararg(Datum.integer(0))))
            .build()
        val session = Session.builder()
            .catalog("test")
            .catalogs(catalog)
            .build()

        val ast = parser.parse("SELECT VALUE x + 1 FROM t AS x").statements[0]
        val result = planner.plan(ast, session)
        val execPlan = compiler.compile(result.plan)
        val symbols = result.symbols

        // Build base catalogs from session (includes $system for functions)
        val baseCatalogs = buildExecutionCatalogs(symbols, session)

        val executor = Executors.newFixedThreadPool(8)
        try {
            val futures = (1..8).map { threadData ->
                CompletableFuture.supplyAsync({
                    // Override only the "test" catalog (index 0) with per-thread data
                    val threadCatalogs = baseCatalogs.copyOf()
                    threadCatalogs[0] = object : ExecutionCatalog {
                        override fun getTable(id: Int): Table {
                            return Table.standard(Name.of("t"), Datum.bagVararg(Datum.integer(threadData)))
                        }
                        override fun getFn(id: Int): Fn = baseCatalogs[0].getFn(id)
                        override fun getFnOverload(id: Int): FnOverload = baseCatalogs[0].getFnOverload(id)
                        override fun getAgg(id: Int): Agg = baseCatalogs[0].getAgg(id)
                    }
                    val datum = vm.execute(execPlan, Mode.PERMISSIVE(), threadCatalogs)
                    DatumMaterialize.materialize(datum)
                }, executor)
            }

            val results = futures.map { it.get() }
            results.forEachIndexed { i, datum ->
                val expected = Datum.bagVararg(Datum.integer(i + 1 + 1))
                assertEquals(
                    0,
                    Datum.comparator(true, true).compare(expected, datum),
                    "Thread ${i + 1} expected $expected but got $datum"
                )
            }
        } finally {
            executor.shutdown()
        }
    }

    @RepeatedTest(5)
    fun concurrentScalarExecution() {
        val session = Session.builder()
            .catalog("test")
            .catalogs(Catalog.builder().name("test").build())
            .build()

        val ast = parser.parse("1 + 2").statements[0]
        val result = planner.plan(ast, session)
        val execPlan = compiler.compile(result.plan)

        val executor = Executors.newFixedThreadPool(8)
        try {
            val futures = (1..8).map {
                CompletableFuture.supplyAsync({
                    val catalogs = buildExecutionCatalogs(result.symbols, session)
                    vm.execute(execPlan, Mode.PERMISSIVE(), catalogs)
                }, executor)
            }

            val results = futures.map { it.get() }
            results.forEach { datum ->
                assertEquals(3, datum.int)
            }
        } finally {
            executor.shutdown()
        }
    }
}
