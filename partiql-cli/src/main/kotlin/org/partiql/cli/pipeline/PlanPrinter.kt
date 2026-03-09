package org.partiql.cli.pipeline

import org.partiql.plan.Action
import org.partiql.plan.Operand
import org.partiql.plan.Operator
import org.partiql.plan.OperatorVisitor
import org.partiql.plan.Plan
import org.partiql.plan.rel.Rel
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
import org.partiql.plan.rel.RelWindow
import org.partiql.plan.rel.RelWith
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
import java.io.PrintStream

/**
 * An internal plan printer that walks the plan operator tree using the [OperatorVisitor] interface,
 * printing each node's name and type information in a tree-like indented format.
 *
 * The context parameter [Int] represents the current indentation depth.
 */
internal class PlanPrinter(
    private val out: PrintStream,
) : OperatorVisitor<Unit, Int> {

    companion object {

        /**
         * Prints the plan tree to the given [PrintStream].
         */
        @JvmStatic
        fun print(plan: Plan, out: PrintStream) {
            val action = plan.action
            if (action is Action.Query) {
                val root = action.rex
                val printer = PlanPrinter(out)
                out.println("=== PLAN ===")
                root.accept(printer, 0)
                out.println()
            }
        }
    }

    // --- Helpers ---

    private fun indent(depth: Int): String = "  ".repeat(depth)

    private fun printRex(name: String, rex: Rex, depth: Int) {
        out.println("${indent(depth)}$name :: ${rex.type}")
        visitChildren(rex, depth + 1)
    }

    private fun printRel(name: String, rel: Rel, depth: Int) {
        val type = rel.type
        val fields = type.fields.joinToString(", ") { "${it.name}: ${it.type}" }
        out.println("${indent(depth)}$name :: [$fields]")
        visitChildren(rel, depth + 1)
    }

    /**
     * Returns a compact, inline summary of a Rex expression tree (no recursion into the visitor).
     */
    private fun summarize(rex: Rex): String = when (rex) {
        is RexVar -> "\$v(${rex.scope}, ${rex.offset})"
        is RexLit -> "lit(${rex.datum})"
        is RexCall -> {
            val fnName = rex.function.signature.name
            val args = rex.args.joinToString(", ") { summarize(it) }
            "$fnName($args)"
        }
        is RexDispatch -> {
            val fnName = rex.name
            val args = rex.args.joinToString(", ") { summarize(it) }
            "$fnName($args)"
        }
        is RexPathSymbol -> "${summarize(rex.operand)}.${rex.symbol}"
        is RexPathIndex -> "${summarize(rex.operand)}[${summarize(rex.index)}]"
        is RexPathKey -> "${summarize(rex.operand)}[${summarize(rex.key)}]"
        is RexCast -> "CAST(${summarize(rex.operand)} AS ${rex.target})"
        is RexCase -> "CASE(..)"
        is RexCoalesce -> "COALESCE(${rex.args.joinToString(", ") { summarize(it) }})"
        is RexNullIf -> "NULLIF(${summarize(rex.v1)}, ${summarize(rex.v2)})"
        is RexArray -> "[${rex.values.joinToString(", ") { summarize(it) }}]"
        is RexBag -> "<<${rex.values.joinToString(", ") { summarize(it) }}>>"
        is RexStruct -> "{ ${rex.fields.joinToString(", ") { "${summarize(it.key)}: ${summarize(it.value)}" }} }"
        is RexSelect -> "SELECT(..)"
        is RexSubquery -> "SUBQUERY(..)"
        is RexSubqueryComp -> "SUBQUERY_COMP(..)"
        is RexSubqueryIn -> "SUBQUERY_IN(..)"
        is RexSubqueryTest -> "SUBQUERY_TEST(..)"
        is RexTable -> "TABLE(${rex.table.getName()})"
        is RexError -> "ERROR"
        is RexSpread -> "SPREAD(..)"
        is RexPivot -> "PIVOT(..)"
        else -> rex.javaClass.simpleName
    }

    private fun visitChildren(operator: Operator, depth: Int) {
        for (operand: Operand in operator.operands) {
            for (child: Operator in operand) {
                child.accept(this, depth)
            }
        }
    }

    // --- OperatorVisitor ---

    override fun defaultReturn(operator: Operator, ctx: Int) = Unit

    override fun defaultVisit(operator: Operator, ctx: Int) {
        // Fallback for any operator not explicitly handled
        val name = operator.javaClass.simpleName
        when (operator) {
            is Rex -> printRex(name, operator, ctx)
            is Rel -> printRel(name, operator, ctx)
            else -> {
                out.println("${indent(ctx)}$name")
                visitChildren(operator, ctx + 1)
            }
        }
    }

    // --[Rel]---

    override fun visitAggregate(rel: RelAggregate, ctx: Int) = printRel("RelAggregate", rel, ctx)
    override fun visitCorrelate(rel: RelCorrelate, ctx: Int) = printRel("RelCorrelate", rel, ctx)
    override fun visitDistinct(rel: RelDistinct, ctx: Int) = printRel("RelDistinct", rel, ctx)
    override fun visitExcept(rel: RelExcept, ctx: Int) = printRel("RelExcept", rel, ctx)
    override fun visitExclude(rel: RelExclude, ctx: Int) = printRel("RelExclude", rel, ctx)
    override fun visitFilter(rel: RelFilter, ctx: Int) = printRel("RelFilter", rel, ctx)
    override fun visitIntersect(rel: RelIntersect, ctx: Int) = printRel("RelIntersect", rel, ctx)
    override fun visitIterate(rel: RelIterate, ctx: Int) = printRel("RelIterate", rel, ctx)
    override fun visitJoin(rel: RelJoin, ctx: Int) = printRel("RelJoin", rel, ctx)
    override fun visitLimit(rel: RelLimit, ctx: Int) = printRel("RelLimit", rel, ctx)
    override fun visitOffset(rel: RelOffset, ctx: Int) = printRel("RelOffset", rel, ctx)
    override fun visitProject(rel: RelProject, ctx: Int) {
        val type = rel.type
        val fields = type.fields.joinToString(", ") { "${it.name}: ${it.type}" }
        out.println("${indent(ctx)}RelProject :: [$fields]")
        // Print each projection expression with its binding name
        val projections = rel.projections
        val typeFields = type.fields
        for (i in projections.indices) {
            val bindingName = if (i < typeFields.size) typeFields[i].name else "?"
            val expr = summarize(projections[i])
            out.println("${indent(ctx + 1)}$bindingName := $expr")
        }
        // Visit input children (the input rel)
        visitChildren(rel, ctx + 1)
    }
    override fun visitScan(rel: RelScan, ctx: Int) = printRel("RelScan", rel, ctx)
    override fun visitSort(rel: RelSort, ctx: Int) = printRel("RelSort", rel, ctx)
    override fun visitWindow(rel: RelWindow, ctx: Int) = printRel("RelWindow", rel, ctx)
    override fun visitUnion(rel: RelUnion, ctx: Int) = printRel("RelUnion", rel, ctx)
    override fun visitUnpivot(rel: RelUnpivot, ctx: Int) = printRel("RelUnpivot", rel, ctx)
    override fun visitWith(rel: RelWith, ctx: Int) = printRel("RelWith", rel, ctx)

    // --[Rex]---

    override fun visitArray(rex: RexArray, ctx: Int) = printRex("RexArray", rex, ctx)
    override fun visitBag(rex: RexBag, ctx: Int) = printRex("RexBag", rex, ctx)
    override fun visitCall(rex: RexCall, ctx: Int) = printRex("RexCall", rex, ctx)
    override fun visitCase(rex: RexCase, ctx: Int) = printRex("RexCase", rex, ctx)
    override fun visitCast(rex: RexCast, ctx: Int) = printRex("RexCast", rex, ctx)
    override fun visitCoalesce(rex: RexCoalesce, ctx: Int) = printRex("RexCoalesce", rex, ctx)
    override fun visitDispatch(rex: RexDispatch, ctx: Int) = printRex("RexDispatch", rex, ctx)
    override fun visitError(rex: RexError, ctx: Int) = printRex("RexError", rex, ctx)
    override fun visitLit(rex: RexLit, ctx: Int) = printRex("RexLit", rex, ctx)
    override fun visitNullIf(rex: RexNullIf, ctx: Int) = printRex("RexNullIf", rex, ctx)
    override fun visitPathIndex(rex: RexPathIndex, ctx: Int) = printRex("RexPathIndex", rex, ctx)
    override fun visitPathKey(rex: RexPathKey, ctx: Int) = printRex("RexPathKey", rex, ctx)
    override fun visitPathSymbol(rex: RexPathSymbol, ctx: Int) = printRex("RexPathSymbol", rex, ctx)
    override fun visitPivot(rex: RexPivot, ctx: Int) = printRex("RexPivot", rex, ctx)
    override fun visitSelect(rex: RexSelect, ctx: Int) = printRex("RexSelect", rex, ctx)
    override fun visitStruct(rex: RexStruct, ctx: Int) = printRex("RexStruct", rex, ctx)
    override fun visitSubquery(rex: RexSubquery, ctx: Int) = printRex("RexSubquery", rex, ctx)
    override fun visitSubqueryComp(rex: RexSubqueryComp, ctx: Int) = printRex("RexSubqueryComp", rex, ctx)
    override fun visitSubqueryIn(rex: RexSubqueryIn, ctx: Int) = printRex("RexSubqueryIn", rex, ctx)
    override fun visitSubqueryTest(rex: RexSubqueryTest, ctx: Int) = printRex("RexSubqueryTest", rex, ctx)
    override fun visitSpread(rex: RexSpread, ctx: Int) = printRex("RexSpread", rex, ctx)
    override fun visitTable(rex: RexTable, ctx: Int) = printRex("RexTable", rex, ctx)
    override fun visitVar(rex: RexVar, ctx: Int) = printRex("RexVar", rex, ctx)
}