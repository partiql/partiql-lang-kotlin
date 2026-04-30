package org.partiql.eval.internal

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.partiql.eval.Mode
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Session
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Tests that the V1 evaluation engine respects [Thread.interrupt] and throws [InterruptedException]
 * when interrupted during long-running operations.
 *
 * This mirrors the V0 tests in `EvaluatingCompilerInterruptTests` (added in PR #1211 / #1331).
 */
class
InterruptTests {

    private val parser = PartiQLParser.standard()
    private val planner = PartiQLPlanner.standard()
    private val compiler = PartiQLCompiler.standard()

    private val session = Session.builder()
        .catalog("memory")
        .catalogs(Catalog.builder().name("memory").build())
        .build()

    companion object {
        /** How long (in millis) to wait after starting a thread to set the interrupted flag. */
        const val INTERRUPT_AFTER_MS: Long = 100

        /** How long (in millis) to wait for a thread to terminate after setting the interrupted flag. */
        const val WAIT_FOR_THREAD_TERMINATION_MS: Long = 3000
    }

    /**
     * Cross joins are materialized lazily during iteration. This test ensures that the inner join
     * operator checks for thread interruption during its nested loop execution.
     */
    @Test
    fun crossJoin() {
        val query = """
            SELECT *
            FROM
                ([1, 2, 3, 4]) as x1,
                ([1, 2, 3, 4]) as x2,
                ([1, 2, 3, 4]) as x3,
                ([1, 2, 3, 4]) as x4,
                ([1, 2, 3, 4]) as x5,
                ([1, 2, 3, 4]) as x6,
                ([1, 2, 3, 4]) as x7,
                ([1, 2, 3, 4]) as x8,
                ([1, 2, 3, 4]) as x9,
                ([1, 2, 3, 4]) as x10,
                ([1, 2, 3, 4]) as x11,
                ([1, 2, 3, 4]) as x12,
                ([1, 2, 3, 4]) as x13,
                ([1, 2, 3, 4]) as x14,
                ([1, 2, 3, 4]) as x15
        """.trimIndent()
        val statement = prepare(query)
        val result = statement.execute()
        testThreadInterrupt {
            // Materializing the result drives the join operators
            for (row in result) { /* consume */ }
        }
    }

    /**
     * Left joins should also be interruptible during their nested loop execution.
     */
    @Test
    fun leftJoin() {
        val query = """
            SELECT *
            FROM
                [1, 2, 3, 4] LEFT JOIN
                ([1, 2, 3, 4] LEFT JOIN
                ([1, 2, 3, 4] LEFT JOIN
                ([1, 2, 3, 4] LEFT JOIN
                ([1, 2, 3, 4] LEFT JOIN
                ([1, 2, 3, 4] LEFT JOIN
                ([1, 2, 3, 4] LEFT JOIN
                ([1, 2, 3, 4] LEFT JOIN
                ([1, 2, 3, 4] LEFT JOIN
                ([1, 2, 3, 4] LEFT JOIN
                ([1, 2, 3, 4] LEFT JOIN
                ([1, 2, 3, 4] LEFT JOIN
                ([1, 2, 3, 4] LEFT JOIN
                ([1, 2, 3, 4] LEFT JOIN ([1, 2, 3, 4]) ON TRUE) ON TRUE) ON TRUE) ON TRUE) ON TRUE) ON TRUE) ON TRUE) ON TRUE) ON TRUE) ON TRUE) ON TRUE) ON TRUE) ON TRUE) ON TRUE
        """.trimIndent()
        val statement = prepare(query)
        val result = statement.execute()
        testThreadInterrupt {
            for (row in result) { /* consume */ }
        }
    }

    /**
     * Aggregations eagerly materialize all input rows during [open]. This test ensures that the
     * aggregate operator checks for thread interruption while consuming its input.
     *
     * Note: This test will pass once checkInterrupted() is added to RelOpAggregate.
     * For now it validates the test infrastructure works with joins.
     */
    @Test
    fun aggregation() {
        val query = """
            SELECT COUNT(*)
            FROM
                ([1, 2, 3, 4]) as x1,
                ([1, 2, 3, 4]) as x2,
                ([1, 2, 3, 4]) as x3,
                ([1, 2, 3, 4]) as x4,
                ([1, 2, 3, 4]) as x5,
                ([1, 2, 3, 4]) as x6,
                ([1, 2, 3, 4]) as x7,
                ([1, 2, 3, 4]) as x8,
                ([1, 2, 3, 4]) as x9,
                ([1, 2, 3, 4]) as x10,
                ([1, 2, 3, 4]) as x11,
                ([1, 2, 3, 4]) as x12,
                ([1, 2, 3, 4]) as x13,
                ([1, 2, 3, 4]) as x14,
                ([1, 2, 3, 4]) as x15
        """.trimIndent()
        val statement = prepare(query)
        testThreadInterrupt {
            // For aggregations, execute() triggers open() which eagerly materializes
            val result = statement.execute()
            for (row in result) { /* consume */ }
        }
    }

    private fun prepare(query: String): org.partiql.eval.Statement {
        val parsed = parser.parse(query)
        val plan = planner.plan(parsed.statements[0], session).plan
        return compiler.prepare(plan, Mode.PERMISSIVE())
    }

    /**
     * Executes a prepared statement and materializes the result, returning all rows as a list.
     */
    private fun materialize(statement: org.partiql.eval.Statement): List<Any> {
        val result = statement.execute()
        val rows = mutableListOf<Any>()
        for (row in result) { rows.add(row) }
        return rows
    }

    // ========================================================================
    // Pre-set interrupt flag tests
    //
    // These tests prepare the statement first (parse + plan + compile), then
    // set the interrupt flag, then execute. This ensures the flag is caught
    // by the eval operators, not the parser/planner.
    // ========================================================================

    /**
     * Verifies that RelOpScan checks for interruption when producing rows.
     */
    @Test
    fun scanRespectsPreSetInterrupt() {
        val statement = prepare("SELECT * FROM [1, 2, 3]")
        testPreSetInterrupt { materialize(statement) }
    }

    /**
     * Verifies that RelOpFilter checks for interruption when filtering rows.
     */
    @Test
    fun filterRespectsPreSetInterrupt() {
        val statement = prepare("SELECT * FROM [1, 2, 3] AS x WHERE x > 1")
        testPreSetInterrupt { materialize(statement) }
    }

    /**
     * Verifies that RelOpSort checks for interruption when sorting rows.
     */
    @Test
    fun sortRespectsPreSetInterrupt() {
        val statement = prepare("SELECT * FROM [3, 1, 2] AS x ORDER BY x")
        testPreSetInterrupt { materialize(statement) }
    }

    /**
     * Verifies that RelOpDistinct checks for interruption when deduplicating rows.
     */
    @Test
    fun distinctRespectsPreSetInterrupt() {
        val statement = prepare("SELECT DISTINCT x FROM [1, 1, 2, 2, 3] AS x")
        testPreSetInterrupt { materialize(statement) }
    }

    /**
     * Verifies that RelOpUnionAll checks for interruption.
     */
    @Test
    fun unionAllRespectsPreSetInterrupt() {
        val statement = prepare("SELECT x FROM [1, 2] AS x UNION ALL SELECT x FROM [3, 4] AS x")
        testPreSetInterrupt { materialize(statement) }
    }

    /**
     * Verifies that RelOpLimit checks for interruption.
     */
    @Test
    fun limitRespectsPreSetInterrupt() {
        val statement = prepare("SELECT * FROM [1, 2, 3] LIMIT 2")
        testPreSetInterrupt { materialize(statement) }
    }

    /**
     * Verifies that RelOpOffset checks for interruption when skipping rows.
     */
    @Test
    fun offsetRespectsPreSetInterrupt() {
        val statement = prepare("SELECT * FROM [1, 2, 3] LIMIT 2 OFFSET 1")
        testPreSetInterrupt { materialize(statement) }
    }

    /**
     * Verifies that RelOpAggregate checks for interruption during eager materialization.
     */
    @Test
    fun aggregateRespectsPreSetInterrupt() {
        val statement = prepare("SELECT COUNT(*) FROM [1, 2, 3]")
        testPreSetInterrupt { materialize(statement) }
    }

    /**
     * Prepares a statement, sets the interrupt flag, then runs [block].
     * Asserts that an [InterruptedException] is thrown (directly or as a cause).
     */
    private fun testPreSetInterrupt(block: () -> Unit) {
        val wasInterrupted = AtomicBoolean(false)
        val t = thread(start = false) {
            try {
                Thread.currentThread().interrupt() // pre-set the flag AFTER prepare
                block()
            } catch (_: InterruptedException) {
                wasInterrupted.set(true)
            } catch (e: Exception) {
                if (isCausedByInterruptedException(e)) {
                    wasInterrupted.set(true)
                }
            }
        }
        t.setUncaughtExceptionHandler { _, _ -> }
        t.start()
        t.join(WAIT_FOR_THREAD_TERMINATION_MS)
        assertTrue(wasInterrupted.get(), "Thread should have been interrupted.")
        assertTrue(!t.isAlive, "Thread should have terminated after interruption.")
    }

    /**
     * Runs [block] in a separate thread, interrupts it after [interruptAfter] ms, and asserts
     * that the thread terminates (via InterruptedException) within [interruptWait] ms.
     */
    private fun testThreadInterrupt(
        interruptAfter: Long = INTERRUPT_AFTER_MS,
        interruptWait: Long = WAIT_FOR_THREAD_TERMINATION_MS,
        block: () -> Unit
    ) {
        val wasInterrupted = AtomicBoolean(false)
        val t = thread(start = false) {
            try {
                block()
            } catch (_: InterruptedException) {
                wasInterrupted.set(true)
            } catch (e: Exception) {
                if (isCausedByInterruptedException(e)) {
                    wasInterrupted.set(true)
                }
            }
        }
        t.setUncaughtExceptionHandler { _, _ -> }
        t.start()
        Thread.sleep(interruptAfter)
        t.interrupt()
        t.join(interruptWait)
        assertTrue(wasInterrupted.get(), "Thread should have been interrupted.")
        assertTrue(!t.isAlive, "Thread should have terminated after interruption.")
    }

    private tailrec fun isCausedByInterruptedException(throwable: Throwable?): Boolean = when (throwable) {
        null -> false
        is InterruptedException -> true
        else -> isCausedByInterruptedException(throwable.cause)
    }
}
