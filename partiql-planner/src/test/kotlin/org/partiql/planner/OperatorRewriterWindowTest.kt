package org.partiql.planner

import org.junit.jupiter.api.Test
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Action
import org.partiql.plan.Operator
import org.partiql.plan.OperatorRewriter
import org.partiql.plan.Operators
import org.partiql.plan.rel.RelSort
import org.partiql.plan.rel.RelWindow
import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexMap
import org.partiql.plan.rex.RexMapDynamic
import org.partiql.plan.rex.RexVar
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests that [OperatorRewriter] rewrites a [RelWindow] (its input, window function arguments, partitions, and
 * collations), a [RexMap], and a [RexMapDynamic].
 */
@Suppress("DEPRECATION")
class OperatorRewriterWindowTest {

    private val session = Session.builder()
        .catalog("default")
        .catalogs(Catalog.builder().name("default").build())
        .build()

    /**
     * Adds 1 to every INTEGER literal, and records the values it saw.
     */
    private class IncrementIntegers : OperatorRewriter<Unit>() {
        val seen = mutableListOf<Int>()

        override fun visitLit(rex: RexLit, ctx: Unit): Operator {
            if (rex.datum.type.code() != PType.INTEGER) {
                return rex
            }
            seen.add(rex.datum.int)
            return RexLit.create(Datum.integer(rex.datum.int + 1))
        }
    }

    /**
     * A rewriter that does not change anything.
     */
    private class Identity : OperatorRewriter<Unit>()

    @Test
    fun rewritesWindowInputFunctionsAndCollations() {
        // A literal 0 in the window's input (WHERE), an aggregate window function's argument, a navigation function's
        // argument, and a collation. (A partition is a column reference; see rewritesWindowPartitions.)
        val query = "SELECT t.a AS a, SUM(t.b + 0) OVER (PARTITION BY t.a ORDER BY t.b + 0) AS s, " +
            "LAG(t.b + 0) OVER (ORDER BY t.b) AS l FROM << { 'a': 1, 'b': 2 } >> AS t WHERE t.b > 0"
        val root = plan(query)
        val windows = windows(root)
        assertEquals(2, windows.size)

        val rewriter = IncrementIntegers()
        val rewritten = rewriter.visitRex(root, Unit)
        assertEquals(listOf(0, 0, 0, 0), rewriter.seen.filter { it == 0 }, "Literals seen: ${rewriter.seen}")

        // The rewritten plan holds the new literals, and each window keeps its type (including its bindings).
        val again = IncrementIntegers()
        again.visitRex(rewritten, Unit)
        assertEquals(listOf(1, 1, 1, 1), again.seen.filter { it <= 1 }, "Literals seen: ${again.seen}")
        val rewrittenWindows = windows(rewritten)
        assertEquals(windows.size, rewrittenWindows.size)
        windows.zip(rewrittenWindows).forEach { (original, new) ->
            assertNotSame(original, new)
            assertEquals(original.type.fields.map { it.name }, new.type.fields.map { it.name })
            assertEquals(original.name, new.name)
            assertEquals(original.windowFunctions.map { it.signature }, new.windowFunctions.map { it.signature })
            assertEquals(original.windowFunctions.map { it.aggregate }, new.windowFunctions.map { it.aggregate })
            assertEquals(original.windowFunctions.map { it.routineRef }, new.windowFunctions.map { it.routineRef })
        }
    }

    /**
     * Replaces every variable with a literal.
     */
    private class VarsToLiterals : OperatorRewriter<Unit>() {
        override fun visitVar(rex: RexVar, ctx: Unit): Operator = RexLit.create(Datum.integer(-1))
    }

    @Test
    fun rewritesWindowPartitions() {
        val root = plan("SELECT SUM(t.b) OVER (PARTITION BY t.a ORDER BY t.b) AS s FROM << { 'a': 1, 'b': 2 } >> AS t")
        val window = windows(VarsToLiterals().visitRex(root, Unit)).single()
        // `t.a` is a path on the variable `t`; the rewriter replaces the variable.
        assertTrue(window.partitions.none { containsVar(it) }, "Partitions: ${window.partitions}")
        assertTrue(window.collations.none { containsVar(it.column) }, "Collations: ${window.collations}")
        assertTrue(window.windowFunctions.single().arguments.none { containsVar(it) })
    }

    @Test
    fun rewritesSortCollations() {
        val root = plan("SELECT t.b AS b FROM << { 'a': 1, 'b': 2 } >> AS t ORDER BY t.b + 0 DESC NULLS LAST")
        val rewriter = IncrementIntegers()
        val rewritten = rewriter.visitRex(root, Unit)
        assertEquals(listOf(0), rewriter.seen.filter { it == 0 }, "Literals seen: ${rewriter.seen}")
        val sort = sorts(rewritten).single()
        assertEquals(listOf(1), integers(sort.collations.single().column))
        // The order and null ordering are kept.
        val original = sorts(root).single().collations.single()
        assertEquals(original.order.code(), sort.collations.single().order.code())
        assertEquals(original.nulls.code(), sort.collations.single().nulls.code())
    }

    @Test
    fun keepsWindowWhenNothingChanges() {
        val root = plan("SELECT SUM(t.b) OVER (PARTITION BY t.a ORDER BY t.b) AS s FROM << { 'a': 1, 'b': 2 } >> AS t")
        assertSame(root, Identity().visitRex(root, Unit))
    }

    @Test
    fun rewritesMapEntries() {
        val operators = Operators.STANDARD
        val zero = { operators.lit(Datum.integer(0)) }
        val map = operators.map(PType.integer(), PType.integer(), listOf(RexMap.entry(zero(), zero())))
        val dynamic = operators.mapDynamic(listOf(RexMap.entry(zero(), zero())))

        val rewrittenMap = IncrementIntegers().visitRex(map, Unit) as RexMap
        assertEquals(listOf(1, 1), rewrittenMap.entries.flatMap { listOf(it.key, it.value) }.map { (it as RexLit).datum.int })
        assertEquals(map.type, rewrittenMap.type)
        val rewrittenDynamic = IncrementIntegers().visitRex(dynamic, Unit) as RexMapDynamic
        assertEquals(listOf(1, 1), rewrittenDynamic.entries.flatMap { listOf(it.key, it.value) }.map { (it as RexLit).datum.int })

        assertSame(map, Identity().visitRex(map, Unit))
        assertSame(dynamic, Identity().visitRex(dynamic, Unit))
    }

    private fun containsVar(op: Operator): Boolean = op is RexVar || op.operands.flatMap { it.toList() }.any { containsVar(it) }

    private fun plan(query: String): Rex {
        val statement = PartiQLParser.standard().parse(query).statements.single()
        return (PartiQLPlanner.standard().plan(statement, session).plan.action as Action.Query).rex
    }

    private fun integers(op: Operator): List<Int> {
        val self = if (op is RexLit && op.datum.type.code() == PType.INTEGER) listOf(op.datum.int) else emptyList()
        return self + op.operands.flatMap { it.toList() }.flatMap { integers(it) }
    }

    private fun sorts(op: Operator): List<RelSort> {
        val self = if (op is RelSort) listOf(op) else emptyList()
        return self + op.operands.flatMap { it.toList() }.flatMap { sorts(it) }
    }

    private fun windows(op: Operator): List<RelWindow> {
        val self = if (op is RelWindow) listOf(op) else emptyList()
        return self + op.operands.flatMap { it.toList() }.flatMap { windows(it) }
    }
}
