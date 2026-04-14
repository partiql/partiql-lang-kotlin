package org.partiql.planner

import org.partiql.plan.Operator
import org.partiql.plan.OperatorVisitor
import org.partiql.plan.rel.RelAggregate
import org.partiql.plan.rel.RelCorrelate
import org.partiql.plan.rel.RelDistinct
import org.partiql.plan.rel.RelExcept
import org.partiql.plan.rel.RelExclude
import org.partiql.plan.rel.RelFilter
import org.partiql.plan.rel.RelIntersect
import org.partiql.plan.rel.RelIterate
import org.partiql.plan.rel.RelJoin
import org.partiql.plan.rel.RelLimit
import org.partiql.plan.rel.RelOffset
import org.partiql.plan.rel.RelProject
import org.partiql.plan.rel.RelScan
import org.partiql.plan.rel.RelSort
import org.partiql.plan.rel.RelUnion
import org.partiql.plan.rel.RelUnpivot
import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexArray
import org.partiql.plan.rex.RexBag
import org.partiql.plan.rex.RexCall
import org.partiql.plan.rex.RexCase
import org.partiql.plan.rex.RexCast
import org.partiql.plan.rex.RexCoalesce
import org.partiql.plan.rex.RexDispatch
import org.partiql.plan.rex.RexError
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexNullIf
import org.partiql.plan.rex.RexPathIndex
import org.partiql.plan.rex.RexPathKey
import org.partiql.plan.rex.RexPathSymbol
import org.partiql.plan.rex.RexPivot
import org.partiql.plan.rex.RexSelect
import org.partiql.plan.rex.RexSpread
import org.partiql.plan.rex.RexStruct
import org.partiql.plan.rex.RexSubquery
import org.partiql.plan.rex.RexSubqueryComp
import org.partiql.plan.rex.RexSubqueryIn
import org.partiql.plan.rex.RexSubqueryTest
import org.partiql.plan.rex.RexTable
import org.partiql.plan.rex.RexVar
import org.partiql.spi.value.Datum
import kotlin.test.assertEquals

/**
 * Asserts structural equivalence of two operator trees.
 *
 * Implemented visitors assert and return Unit. Unimplemented visitors throw [NotImplementedError].
 *
 * TODO: Implement remaining visitors as needed.
 */
internal object AssertingEquivalenceOperatorVisitor : OperatorVisitor<Unit, Any> {

    fun assertEquals(a: Rex, b: Rex) = a.accept(this, b)

    override fun defaultReturn(operator: Operator, other: Any) {
        throw NotImplementedError("Equivalence not implemented for ${operator::class.java.name}")
    }

    // --- Rel visitors ---

    override fun visitAggregate(rel: RelAggregate, other: Any) = defaultReturn(rel, other)
    override fun visitCorrelate(rel: RelCorrelate, other: Any) = defaultReturn(rel, other)
    override fun visitDistinct(rel: RelDistinct, other: Any) = defaultReturn(rel, other)
    override fun visitExclude(rel: RelExclude, other: Any) = defaultReturn(rel, other)
    override fun visitFilter(rel: RelFilter, other: Any) = defaultReturn(rel, other)
    override fun visitIterate(rel: RelIterate, other: Any) = defaultReturn(rel, other)
    override fun visitJoin(rel: RelJoin, other: Any) = defaultReturn(rel, other)
    override fun visitLimit(rel: RelLimit, other: Any) = defaultReturn(rel, other)
    override fun visitOffset(rel: RelOffset, other: Any) = defaultReturn(rel, other)
    override fun visitScan(rel: RelScan, other: Any) = defaultReturn(rel, other)
    override fun visitSort(rel: RelSort, other: Any) = defaultReturn(rel, other)
    override fun visitUnpivot(rel: RelUnpivot, other: Any) = defaultReturn(rel, other)

    override fun visitExcept(rel: RelExcept, other: Any) {
        assert(other is RelExcept) { "Expected RelExcept, got ${other::class.java.name}" }
        other as RelExcept
        assertEquals(rel.isAll, other.isAll, "RelExcept.isAll mismatch")
        rel.left.accept(this, other.left)
        rel.right.accept(this, other.right)
    }

    override fun visitIntersect(rel: RelIntersect, other: Any) {
        assert(other is RelIntersect) { "Expected RelIntersect, got ${other::class.java.name}" }
        other as RelIntersect
        assertEquals(rel.isAll, other.isAll, "RelIntersect.isAll mismatch")
        rel.left.accept(this, other.left)
        rel.right.accept(this, other.right)
    }

    override fun visitProject(rel: RelProject, other: Any) {
        assert(other is RelProject) { "Expected RelProject, got ${other::class.java.name}" }
        other as RelProject
        assertEquals(rel.projections.size, other.projections.size, "RelProject projection count mismatch")
        rel.input.accept(this, other.input)
        rel.projections.zip(other.projections).forEach { (a, b) -> a.accept(this, b) }
    }

    override fun visitUnion(rel: RelUnion, other: Any) {
        assert(other is RelUnion) { "Expected RelUnion, got ${other::class.java.name}" }
        other as RelUnion
        assertEquals(rel.isAll, other.isAll, "RelUnion.isAll mismatch")
        rel.left.accept(this, other.left)
        rel.right.accept(this, other.right)
    }

    // --- Rex visitors ---

    override fun visitArray(rex: RexArray, other: Any) = defaultReturn(rex, other)
    override fun visitCall(rex: RexCall, other: Any) = defaultReturn(rex, other)
    override fun visitCase(rex: RexCase, other: Any) = defaultReturn(rex, other)
    override fun visitCoalesce(rex: RexCoalesce, other: Any) = defaultReturn(rex, other)
    override fun visitDispatch(rex: RexDispatch, other: Any) = defaultReturn(rex, other)
    override fun visitError(rex: RexError, other: Any) = defaultReturn(rex, other)
    override fun visitNullIf(rex: RexNullIf, other: Any) = defaultReturn(rex, other)
    override fun visitPathIndex(rex: RexPathIndex, other: Any) = defaultReturn(rex, other)
    override fun visitPathSymbol(rex: RexPathSymbol, other: Any) = defaultReturn(rex, other)
    override fun visitPivot(rex: RexPivot, other: Any) = defaultReturn(rex, other)
    override fun visitSpread(rex: RexSpread, other: Any) = defaultReturn(rex, other)
    override fun visitSubquery(rex: RexSubquery, other: Any) = defaultReturn(rex, other)
    override fun visitSubqueryComp(rex: RexSubqueryComp, other: Any) = defaultReturn(rex, other)
    override fun visitSubqueryIn(rex: RexSubqueryIn, other: Any) = defaultReturn(rex, other)
    override fun visitSubqueryTest(rex: RexSubqueryTest, other: Any) = defaultReturn(rex, other)
    override fun visitTable(rex: RexTable, other: Any) = defaultReturn(rex, other)

    override fun visitPathKey(rex: RexPathKey, other: Any) {
        assert(other is RexPathKey) { "Expected RexPathKey, got ${other::class.java.name}" }
        other as RexPathKey
        rex.operand.accept(this, other.operand)
        rex.key.accept(this, other.key)
    }

    override fun visitVar(rex: RexVar, other: Any) {
        assert(other is RexVar) { "Expected RexVar, got ${other::class.java.name}" }
        other as RexVar
        assertEquals(rex.scope, other.scope, "RexVar.scope mismatch")
        assertEquals(rex.offset, other.offset, "RexVar.offset mismatch")
    }

    override fun visitCast(rex: RexCast, other: Any) {
        assert(other is RexCast) { "Expected RexCast, got ${other::class.java.name}" }
        other as RexCast
        assertEquals(rex.target, other.target, "RexCast.target mismatch")
        rex.operand.accept(this, other.operand)
    }

    override fun visitLit(rex: RexLit, other: Any) {
        assert(other is RexLit) { "Expected RexLit, got ${other::class.java.name}" }
        other as RexLit
        assertEquals(0, Datum.comparator().compare(rex.datum, other.datum), "RexLit datum mismatch")
    }

    override fun visitSelect(rex: RexSelect, other: Any) {
        assert(other is RexSelect) { "Expected RexSelect, got ${other::class.java.name}" }
        other as RexSelect
        rex.input.accept(this, other.input)
        rex.constructor.accept(this, other.constructor)
    }

    override fun visitStruct(rex: RexStruct, other: Any) {
        assert(other is RexStruct) { "Expected RexStruct, got ${other::class.java.name}" }
        other as RexStruct
        assertEquals(rex.fields.size, other.fields.size, "RexStruct field count mismatch")
        rex.fields.zip(other.fields).forEach { (a, b) ->
            a.key.accept(this, b.key)
            a.value.accept(this, b.value)
        }
    }

    override fun visitBag(rex: RexBag, other: Any) {
        assert(other is RexBag) { "Expected RexBag, got ${other::class.java.name}" }
        other as RexBag
        val a = rex.values.toList()
        val b = other.values.toList()
        assertEquals(a.size, b.size, "RexBag size mismatch")
        a.zip(b).forEach { (x, y) -> x.accept(this, y) }
    }
}
