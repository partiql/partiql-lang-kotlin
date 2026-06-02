package org.partiql.eval.internal

import org.junit.jupiter.api.RepeatedTest
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
import org.partiql.spi.value.Datum
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.test.assertEquals

class ThreadSafetyTest {

    private val parser = PartiQLParser.standard()
    private val planner = PartiQLPlanner.builder().useRefs().build()
    private val compiler = PartiQLCompiler.standard()
    private val vm = PartiQLVM.standard()

    @RepeatedTest(5)
    fun concurrentComplexQuery() {
        // Query with JOIN, aggregation, and function calls
        val query = """
            SELECT
                o.product_id,
                SUM(o.quantity * o.price) AS total_revenue,
                COUNT(o.quantity) AS order_count,
                UPPER(p.name) AS product_name
            FROM orders AS o
            JOIN products AS p ON o.product_id = p.id
            WHERE o.quantity > 0
            GROUP BY o.product_id, p.name
        """.trimIndent()

        // Shared schema for planning — actual data varies per thread
        val orders = Datum.bagVararg(
            struct("product_id" to Datum.integer(1), "quantity" to Datum.integer(1), "price" to Datum.decimal(java.math.BigDecimal.TEN)),
        )
        val products = Datum.bagVararg(
            struct("id" to Datum.integer(1), "name" to Datum.string("widget")),
        )
        val catalog = Catalog.builder()
            .name("store")
            .define(Table.standard(Name.of("orders"), orders))
            .define(Table.standard(Name.of("products"), products))
            .build()
        val session = Session.builder()
            .catalog("store")
            .catalogs(catalog)
            .build()

        val ast = parser.parse(query).statements[0]
        val result = planner.plan(ast, session)
        val execPlan = compiler.compile(result.plan, Mode.PERMISSIVE())
        val symbols = result.symbols
        val baseCatalogs = buildExecutionCatalogs(symbols, session)

        val executor = Executors.newFixedThreadPool(8)
        try {
            val futures = (1..8).map { threadId ->
                CompletableFuture.supplyAsync({
                    // Each thread has different order quantities
                    val threadOrders = Datum.bagVararg(
                        struct("product_id" to Datum.integer(1), "quantity" to Datum.integer(threadId), "price" to Datum.decimal(java.math.BigDecimal.TEN)),
                        struct("product_id" to Datum.integer(1), "quantity" to Datum.integer(threadId * 2), "price" to Datum.decimal(java.math.BigDecimal.TEN)),
                    )
                    val threadProducts = Datum.bagVararg(
                        struct("id" to Datum.integer(1), "name" to Datum.string("widget")),
                    )
                    val threadCatalogs = baseCatalogs.copyOf()
                    threadCatalogs[0] = ExecutionCatalog { tableId ->
                        when (val name = symbols.getTables(0)[tableId].name.getName()) {
                            "orders" -> Table.standard(Name.of("orders"), threadOrders)
                            "products" -> Table.standard(Name.of("products"), threadProducts)
                            else -> error("Unknown table: $name")
                        }
                    }
                    val datum = vm.execute(execPlan, threadCatalogs)
                    DatumMaterialize.materialize(datum)
                }, executor)
            }

            val results = futures.map { it.get() }
            results.forEachIndexed { i, datum ->
                val threadId = i + 1
                // total_revenue = (threadId * 10) + (threadId * 2 * 10) = threadId * 30
                val expectedRevenue = java.math.BigDecimal.valueOf((threadId * 30).toLong())
                val row = datum.iterator().next()
                val actualRevenue = row.get("total_revenue")
                assertEquals(
                    0,
                    actualRevenue.bigDecimal.compareTo(expectedRevenue),
                    "Thread $threadId: expected revenue $expectedRevenue but got ${actualRevenue.bigDecimal}"
                )
                val actualName = row.get("product_name")
                assertEquals("WIDGET", actualName.string, "Thread $threadId: UPPER should produce WIDGET")
                val actualCount = row.get("order_count")
                assertEquals(2L, actualCount.long, "Thread $threadId: should have 2 orders")
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
        val execPlan = compiler.compile(result.plan, Mode.PERMISSIVE())

        val executor = Executors.newFixedThreadPool(8)
        try {
            val futures = (1..8).map {
                CompletableFuture.supplyAsync({
                    val catalogs = buildExecutionCatalogs(result.symbols, session)
                    vm.execute(execPlan, catalogs)
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

    private fun struct(vararg fields: Pair<String, Datum>): Datum {
        val f = fields.map { (k, v) -> org.partiql.spi.value.Field.of(k, v) }
        return Datum.struct(f)
    }
}
