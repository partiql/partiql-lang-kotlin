package org.partiql.planner

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Action
import org.partiql.plan.Operator
import org.partiql.plan.OperatorRewriter
import org.partiql.plan.Operators
import org.partiql.plan.rel.RelFilter
import org.partiql.plan.rel.RelSort
import org.partiql.plan.rel.RelType
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
 * Tests that [OperatorRewriter] rewrites the children of a [RelWindow] (its input, window function arguments,
 * partitions, and collations), the columns of a [RelSort]'s collations, and the entries of a [RexMap] and a
 * [RexMapDynamic].
 */
@Suppress("DEPRECATION")
class OperatorRewriterChildrenTest {

    private val session = Session.builder()
        .catalog("default")
        .catalogs(Catalog.builder().name("default").build())
        .build()

    /**
     * Adds 1 to every INTEGER literal.
     */
    private class IncrementIntegers : OperatorRewriter<Unit>() {
        override fun visitLit(rex: RexLit, ctx: Unit): Operator {
            if (rex.datum.type.code() != PType.INTEGER) {
                return rex
            }
            return RexLit.create(Datum.integer(rex.datum.int + 1))
        }
    }

    /**
     * Replaces every variable with a literal.
     */
    private class VarsToLiterals : OperatorRewriter<Unit>() {
        override fun visitVar(rex: RexVar, ctx: Unit): Operator = RexLit.create(Datum.integer(-1))
    }

    /**
     * A rewriter that does not change anything.
     */
    private class Identity : OperatorRewriter<Unit>()

    @Test
    fun rewritesWindowInputFunctionsAndCollations() {
        // A literal 0 in the windows' input (WHERE), an aggregate window function's argument, a navigation function's
        // argument, and a window ORDER BY. (A partition is a column reference; see rewritesWindowPartitions.)
        val query = "SELECT t.a AS a, SUM(t.b + 0) OVER (PARTITION BY t.a ORDER BY t.b + 0) AS s, " +
            "LAG(t.b + 0) OVER (ORDER BY t.b) AS l FROM << { 'a': 1, 'b': 2 } >> AS t WHERE t.b > 0"
        val root = plan(query)
        val rewritten = IncrementIntegers().visitRex(root, Unit)

        // The SUM window is planned above the LAG window, which is above the filter.
        val (sum, lag) = windows(rewritten)
        assertEquals(listOf("sum"), sum.windowFunctions.map { it.signature.name })
        assertEquals(listOf("lag"), lag.windowFunctions.map { it.signature.name })
        assertEquals(listOf(1), integers(filters(rewritten).single().predicate))
        assertEquals(listOf(1), sum.windowFunctions.single().arguments.flatMap { integers(it) })
        assertEquals(listOf(1), sum.collations.flatMap { integers(it.column) })
        // LAG's offset is a BIGINT literal, which the rewriter leaves unchanged.
        assertEquals(listOf(1), integers(lag.windowFunctions.single().arguments[0]))
        assertEquals(emptyList(), lag.collations.flatMap { integers(it.column) })

        // Each window keeps its name, type (including its bindings), and window functions.
        windows(root).zip(windows(rewritten)).forEach { (original, new) ->
            assertNotSame(original, new)
            assertEquals(original.name, new.name)
            assertEquals(original.type.fields.map { it.name }, new.type.fields.map { it.name })
            assertEquals(original.windowFunctions.map { it.signature }, new.windowFunctions.map { it.signature })
            assertEquals(original.windowFunctions.map { it.aggregate }, new.windowFunctions.map { it.aggregate })
            assertEquals(original.windowFunctions.map { it.isDistinct }, new.windowFunctions.map { it.isDistinct })
            assertEquals(original.windowFunctions.map { it.routineRef }, new.windowFunctions.map { it.routineRef })
        }
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
    fun rejectsWindowWithFewerFieldsThanFunctions() {
        val window = windows(plan("SELECT SUM(t.b + 0) OVER () AS s FROM << { 'a': 1, 'b': 2 } >> AS t")).single()
        // A window whose type reports no fields, so its bindings can't be recovered.
        val custom = RelWindow.create(window.name, window.input, listOf("s"), window.windowFunctions, window.collations, window.partitions)
        custom.setType(RelType.of())
        val e = assertThrows<IllegalStateException> { IncrementIntegers().visitRel(custom, Unit) }
        assertEquals("Cannot rewrite window: its type has 0 fields, fewer than its 1 window functions", e.message)
    }

    @Test
    fun rewritesSortCollations() {
        val root = plan("SELECT t.b AS b FROM << { 'a': 1, 'b': 2 } >> AS t ORDER BY t.b + 0 DESC NULLS LAST")
        val sort = sorts(IncrementIntegers().visitRex(root, Unit)).single()
        assertEquals(listOf(1), integers(sort.collations.single().column))
        // The order and null ordering are kept.
        val original = sorts(root).single().collations.single()
        assertEquals(original.order.code(), sort.collations.single().order.code())
        assertEquals(original.nulls.code(), sort.collations.single().nulls.code())
    }

    @Test
    fun keepsPlanWhenNothingChanges() {
        val window = plan("SELECT SUM(t.b) OVER (PARTITION BY t.a ORDER BY t.b) AS s FROM << { 'a': 1, 'b': 2 } >> AS t")
        assertSame(window, Identity().visitRex(window, Unit))
        val sort = plan("SELECT t.b AS b FROM << { 'a': 1, 'b': 2 } >> AS t ORDER BY t.b + 0 DESC NULLS LAST")
        assertSame(sort, Identity().visitRex(sort, Unit))
    }

    @Test
    fun rewritesMapEntries() {
        val operators = Operators.STANDARD
        val zero = { operators.lit(Datum.integer(0)) }
        val map = operators.map(PType.integer(), PType.integer(), listOf(RexMap.entry(zero(), zero())))
        val dynamic = operators.mapDynamic(listOf(RexMap.entry(zero(), zero())))

        val rewrittenMap = IncrementIntegers().visitRex(map, Unit) as RexMap
        assertEquals(listOf(1, 1), rewrittenMap.entries.flatMap { listOf(it.key, it.value) }.flatMap { integers(it) })
        assertEquals(map.type, rewrittenMap.type)
        val rewrittenDynamic = IncrementIntegers().visitRex(dynamic, Unit) as RexMapDynamic
        assertEquals(listOf(1, 1), rewrittenDynamic.entries.flatMap { listOf(it.key, it.value) }.flatMap { integers(it) })
        assertEquals(dynamic.type, rewrittenDynamic.type)

        assertSame(map, Identity().visitRex(map, Unit))
        assertSame(dynamic, Identity().visitRex(dynamic, Unit))
    }

    private fun plan(query: String): Rex {
        val statement = PartiQLParser.standard().parse(query).statements.single()
        return (PartiQLPlanner.standard().plan(statement, session).plan.action as Action.Query).rex
    }

    private fun containsVar(op: Operator): Boolean = op is RexVar || children(op).any { containsVar(it) }

    private fun integers(op: Operator): List<Int> {
        val self = if (op is RexLit && op.datum.type.code() == PType.INTEGER) listOf(op.datum.int) else emptyList()
        return self + children(op).flatMap { integers(it) }
    }

    private fun filters(op: Operator): List<RelFilter> = (if (op is RelFilter) listOf(op) else emptyList()) + children(op).flatMap { filters(it) }

    private fun sorts(op: Operator): List<RelSort> = (if (op is RelSort) listOf(op) else emptyList()) + children(op).flatMap { sorts(it) }

    private fun windows(op: Operator): List<RelWindow> = (if (op is RelWindow) listOf(op) else emptyList()) + children(op).flatMap { windows(it) }

    private fun children(op: Operator): List<Operator> = op.operands.flatMap { it.toList() } +
        when (op) {
            is RelFilter -> listOf(op.predicate)
            else -> emptyList()
        }
}
