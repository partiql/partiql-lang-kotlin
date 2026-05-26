/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.cli.format

import org.partiql.ast.AstNode
import org.partiql.ast.AstVisitor
import org.partiql.ast.Exclude
import org.partiql.ast.From
import org.partiql.ast.FromExpr
import org.partiql.ast.FromJoin
import org.partiql.ast.FromTableRef
import org.partiql.ast.GroupBy
import org.partiql.ast.GroupByStrategy
import org.partiql.ast.JoinType
import org.partiql.ast.Let
import org.partiql.ast.OrderBy
import org.partiql.ast.Query
import org.partiql.ast.QueryBody
import org.partiql.ast.Select
import org.partiql.ast.SelectItem
import org.partiql.ast.SelectList
import org.partiql.ast.SelectPivot
import org.partiql.ast.SelectStar
import org.partiql.ast.SelectValue
import org.partiql.ast.SetOpType
import org.partiql.ast.SetQuantifier
import org.partiql.ast.WindowClause
import org.partiql.ast.With
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprAnd
import org.partiql.ast.expr.ExprArray
import org.partiql.ast.expr.ExprBag
import org.partiql.ast.expr.ExprBetween
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprCase
import org.partiql.ast.expr.ExprCast
import org.partiql.ast.expr.ExprCoalesce
import org.partiql.ast.expr.ExprInCollection
import org.partiql.ast.expr.ExprLike
import org.partiql.ast.expr.ExprMatch
import org.partiql.ast.expr.ExprNot
import org.partiql.ast.expr.ExprOperator
import org.partiql.ast.expr.ExprOr
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprStruct
import org.partiql.ast.sql.SqlDialect
import org.partiql.ast.sql.SqlLayout
import org.partiql.ast.sql.sql

/**
 * Pretty-prints a PartiQL [AstNode] using Wadler's document algebra with full AST traversal.
 *
 * Walks the AST to produce a [Doc] tree, allowing width-aware line breaks at:
 * - Clause boundaries (SELECT, FROM, WHERE, GROUP BY, HAVING, ORDER BY, LIMIT, OFFSET, LET, EXCLUDE, WINDOW)
 * - Join boundaries (each JOIN on its own line)
 * - Boolean operators (AND/OR within WHERE/ON)
 * - Comma-separated lists (SELECT items, function args, GROUP BY keys)
 * - Subqueries and collection constructors in brackets
 */
internal fun AstNode.pretty(width: Int = 80): String {
    val doc = DocVisitor().visit(this, Unit)
    return render(doc, width)
}

// ============================================================================================
// AST-to-Doc Visitor
// ============================================================================================

private class DocVisitor : AstVisitor<Doc, Unit>() {

    // --- A. Defaults ---

    override fun defaultReturn(node: AstNode, ctx: Unit): Doc {
        return text(node.sql(layout = SqlLayout.ONELINE, dialect = SqlDialect.STANDARD))
    }

    override fun defaultVisit(node: AstNode, ctx: Unit): Doc {
        return defaultReturn(node, ctx)
    }

    // --- B. Entry Point ---

    override fun visitQuery(node: Query, ctx: Unit): Doc {
        return visit(node.expr, ctx)
    }

    // --- C. Query-Level: ExprQuerySet ---

    override fun visitExprQuerySet(node: ExprQuerySet, ctx: Unit): Doc {
        val clauses = mutableListOf<Doc>()

        val w = node.with
        if (w != null) {
            clauses.add(visitWithClause(w))
        }

        when (val body = node.body) {
            is QueryBody.SFW -> appendSFWClauses(body, clauses)
            is QueryBody.SetOp -> clauses.add(visitSetOp(body))
            else -> clauses.add(defaultReturn(node, ctx))
        }

        val ob = node.orderBy
        if (ob != null) {
            clauses.add(visitOrderByClause(ob))
        }
        val lim = node.limit
        if (lim != null) {
            clauses.add(clause("LIMIT", visit(lim, ctx)))
        }
        val off = node.offset
        if (off != null) {
            clauses.add(clause("OFFSET", visit(off, ctx)))
        }

        return if (clauses.size <= 1) {
            clauses.firstOrNull() ?: Doc.Nil
        } else {
            group(join(clauses, Doc.Line))
        }
    }

    // --- D. SFW Clause Assembly ---

    private fun appendSFWClauses(sfw: QueryBody.SFW, clauses: MutableList<Doc>) {
        clauses.add(visitSelect(sfw.select))
        appendFromClauses(sfw.from, clauses)
        val let = sfw.let; if (let != null) clauses.add(visitLetClause(let))
        val where = sfw.where; if (where != null) clauses.add(visitWhereClause(where))
        val groupBy = sfw.groupBy; if (groupBy != null) clauses.add(visitGroupByClause(groupBy))
        val having = sfw.having; if (having != null) clauses.add(visitHavingClause(having))
        val window = sfw.window; if (window != null) clauses.add(visitWindowClause(window))
        val exclude = sfw.exclude; if (exclude != null) clauses.add(visitExcludeClause(exclude))
    }

    // --- E. SELECT Variants ---

    private fun visitSelect(select: Select): Doc {
        return when (select) {
            is SelectStar -> {
                val kw = when (select.setq?.code()) {
                    SetQuantifier.ALL -> "SELECT ALL *"
                    SetQuantifier.DISTINCT -> "SELECT DISTINCT *"
                    else -> "SELECT *"
                }
                text(kw)
            }
            is SelectList -> {
                val kw = when (select.setq?.code()) {
                    SetQuantifier.ALL -> "SELECT ALL"
                    SetQuantifier.DISTINCT -> "SELECT DISTINCT"
                    else -> "SELECT"
                }
                val items = select.items.map { visitSelectItem(it) }
                commaSeparated(kw, items)
            }
            is SelectValue -> {
                val kw = when (select.setq?.code()) {
                    SetQuantifier.ALL -> "SELECT ALL VALUE"
                    SetQuantifier.DISTINCT -> "SELECT DISTINCT VALUE"
                    else -> "SELECT VALUE"
                }
                clause(kw, visit(select.constructor, Unit))
            }
            is SelectPivot -> {
                clause("PIVOT", visit(select.key, Unit) cat text(" AT ") cat visit(select.value, Unit))
            }
            else -> defaultReturn(select, Unit)
        }
    }

    private fun visitSelectItem(item: SelectItem): Doc {
        return when (item) {
            is SelectItem.Expr -> {
                val base = visit(item.expr, Unit)
                val alias = item.asAlias
                if (alias != null) base cat text(" AS ${alias.sql()}") else base
            }
            is SelectItem.Star -> {
                visit(item.expr, Unit) cat text(".*")
            }
            else -> defaultReturn(item, Unit)
        }
    }

    // --- F. FROM / JOIN ---

    private fun appendFromClauses(from: From, clauses: MutableList<Doc>) {
        val refs = from.tableRefs
        if (refs.size == 1 && refs[0] is FromJoin) {
            flattenJoins(refs[0], clauses)
        } else {
            val refDocs = refs.map { leafExpr(it) }
            clauses.add(commaSeparated("FROM", refDocs))
        }
    }

    private fun flattenJoins(ref: FromTableRef, parts: MutableList<Doc>) {
        when (ref) {
            is FromJoin -> {
                flattenJoins(ref.lhs, parts)
                val joinKw = joinKeyword(ref.joinType)
                val rhsDoc = leafExpr(ref.rhs)
                val cond = ref.condition
                if (cond != null) {
                    val condDoc = visitBooleanExpr(cond)
                    parts.add(
                        group(
                            text(joinKw) cat
                                nest(4, Doc.Line cat rhsDoc) cat
                                nest(4, Doc.Line cat text("ON ") cat condDoc)
                        )
                    )
                } else {
                    parts.add(
                        group(text(joinKw) cat nest(4, Doc.Line cat rhsDoc))
                    )
                }
            }
            is FromExpr -> {
                parts.add(clause("FROM", leafExpr(ref)))
            }
            else -> {
                parts.add(clause("FROM", leafExpr(ref)))
            }
        }
    }

    private fun joinKeyword(joinType: JoinType?): String {
        return when (joinType?.code()) {
            JoinType.INNER -> "INNER JOIN"
            JoinType.LEFT -> "LEFT JOIN"
            JoinType.LEFT_OUTER -> "LEFT OUTER JOIN"
            JoinType.RIGHT -> "RIGHT JOIN"
            JoinType.RIGHT_OUTER -> "RIGHT OUTER JOIN"
            JoinType.FULL -> "FULL JOIN"
            JoinType.FULL_OUTER -> "FULL OUTER JOIN"
            JoinType.CROSS -> "CROSS JOIN"
            JoinType.LEFT_CROSS -> "LEFT CROSS JOIN"
            null -> "JOIN"
            else -> "JOIN"
        }
    }

    // --- G. Boolean Expressions (AND/OR/NOT) ---

    private fun visitBooleanExpr(expr: Expr): Doc {
        return when (expr) {
            is ExprAnd -> {
                val operands = flattenAnd(expr)
                val docs = operands.map { visitBooleanExpr(it) }
                group(
                    docs.reduceIndexed { idx, acc, doc ->
                        if (idx > 0) acc cat Doc.Line cat text("AND ") cat doc else doc
                    }
                )
            }
            is ExprOr -> {
                val operands = flattenOr(expr)
                val docs = operands.map { visitBooleanExpr(it) }
                group(
                    docs.reduceIndexed { idx, acc, doc ->
                        if (idx > 0) acc cat Doc.Line cat text("OR ") cat doc else doc
                    }
                )
            }
            is ExprNot -> {
                text("NOT ") cat bracket("(", visitBooleanExpr(expr.value), ")")
            }
            is ExprQuerySet -> {
                bracket("(", visitExprQuerySet(expr, Unit), ")")
            }
            else -> visit(expr, Unit)
        }
    }

    private fun flattenAnd(expr: ExprAnd): List<Expr> {
        val result = mutableListOf<Expr>()
        fun collect(e: Expr) {
            if (e is ExprAnd) { collect(e.lhs); collect(e.rhs) } else result.add(e)
        }
        collect(expr)
        return result
    }

    private fun flattenOr(expr: ExprOr): List<Expr> {
        val result = mutableListOf<Expr>()
        fun collect(e: Expr) {
            if (e is ExprOr) { collect(e.lhs); collect(e.rhs) } else result.add(e)
        }
        collect(expr)
        return result
    }

    // --- H. Expressions with Internal Breakpoints ---

    override fun visitExprCall(node: ExprCall, ctx: Unit): Doc {
        val funcName = node.function.sql(layout = SqlLayout.ONELINE, dialect = SqlDialect.STANDARD)
        if (node.args.isEmpty()) {
            return text("$funcName()")
        }
        val setq = when (node.setq?.code()) {
            SetQuantifier.ALL -> "ALL "
            SetQuantifier.DISTINCT -> "DISTINCT "
            else -> ""
        }
        val args = node.args.map { visit(it, ctx) }
        val argsDoc = if (setq.isNotEmpty()) {
            text(setq) cat commaJoin(args)
        } else {
            commaJoin(args)
        }
        return text(funcName) cat bracket("(", argsDoc, ")")
    }

    override fun visitExprCase(node: ExprCase, ctx: Unit): Doc {
        val parts = mutableListOf<Doc>()
        val caseKw = if (node.expr != null) {
            text("CASE ") cat visit(node.expr, ctx)
        } else {
            text("CASE")
        }
        parts.add(caseKw)
        for (branch in node.branches) {
            parts.add(
                nest(
                    4,
                    Doc.HardLine cat text("WHEN ") cat visit(branch.condition, ctx) cat
                        text(" THEN ") cat visit(branch.expr, ctx)
                )
            )
        }
        if (node.defaultExpr != null) {
            parts.add(nest(4, Doc.HardLine cat text("ELSE ") cat visit(node.defaultExpr, ctx)))
        }
        parts.add(Doc.HardLine cat text("END"))
        return parts.reduce { acc, doc -> acc cat doc }
    }

    override fun visitExprCoalesce(node: ExprCoalesce, ctx: Unit): Doc {
        val args = node.args.map { visit(it, ctx) }
        return text("COALESCE") cat bracket("(", commaJoin(args), ")")
    }

    override fun visitExprCast(node: ExprCast, ctx: Unit): Doc {
        val valueDoc = visit(node.value, ctx)
        val typeText = node.asType.sql(layout = SqlLayout.ONELINE, dialect = SqlDialect.STANDARD)
        return text("CAST") cat bracket("(", valueDoc cat text(" AS $typeText"), ")")
    }

    override fun visitExprBag(node: ExprBag, ctx: Unit): Doc {
        if (node.values.isEmpty()) return text("<<>>")
        val items = node.values.map { visit(it, ctx) }
        return bracket("<<", commaJoin(items), ">>")
    }

    override fun visitExprArray(node: ExprArray, ctx: Unit): Doc {
        if (node.values.isEmpty()) return text("[]")
        val items = node.values.map { visit(it, ctx) }
        return bracket("[", commaJoin(items), "]")
    }

    override fun visitExprStruct(node: ExprStruct, ctx: Unit): Doc {
        if (node.fields.isEmpty()) return text("{}")
        val fields = node.fields.map { f ->
            visit(f.name, ctx) cat text(": ") cat visit(f.value, ctx)
        }
        return bracket("{", commaJoin(fields), "}")
    }

    override fun visitExprOperator(node: ExprOperator, ctx: Unit): Doc {
        val lhs = node.lhs
        if (lhs == null) {
            return text(node.symbol) cat visit(node.rhs, ctx)
        }
        val terms = flattenOperator(node)
        val docs = terms.mapIndexed { idx, term ->
            val operandDoc = visitOperand(term.operand, operatorPrecedence(node.symbol), isRight = idx > 0)
            if (idx == 0) operandDoc
            else text(" ${term.precedingOp} ") cat operandDoc
        }
        return group(docs.reduce { acc, doc -> acc cat doc })
    }

    private data class OpTerm(val operand: Expr, val precedingOp: String?)

    private fun flattenOperator(node: ExprOperator): List<OpTerm> {
        val symbol = node.symbol
        val result = mutableListOf<OpTerm>()
        fun collect(e: Expr, op: String?) {
            if (e is ExprOperator && e.lhs != null && e.symbol == symbol) {
                collect(e.lhs!!, op)
                collect(e.rhs, e.symbol)
            } else {
                result.add(OpTerm(e, op))
            }
        }
        collect(node.lhs!!, null)
        collect(node.rhs, node.symbol)
        return result
    }

    private fun visitOperand(expr: Expr, parentPrecedence: Int, isRight: Boolean = false): Doc {
        return when {
            expr is ExprQuerySet -> bracket("(", visitExprQuerySet(expr, Unit), ")")
            expr is ExprOperator && expr.lhs != null -> {
                val childPrec = operatorPrecedence(expr.symbol)
                val needsParens = if (isRight) childPrec <= parentPrecedence else childPrec < parentPrecedence
                if (needsParens) bracket("(", visit(expr, Unit), ")") else visit(expr, Unit)
            }
            else -> visit(expr, Unit)
        }
    }

    private fun operatorPrecedence(symbol: String): Int = when (symbol) {
        "*", "/", "%" -> 3
        "+", "-" -> 2
        else -> 0
    }

    override fun visitExprInCollection(node: ExprInCollection, ctx: Unit): Doc {
        val lhsDoc = visit(node.lhs, ctx)
        val notStr = if (node.isNot) " NOT IN " else " IN "
        val rhsDoc = if (node.rhs is ExprQuerySet) {
            bracket("(", visitExprQuerySet(node.rhs as ExprQuerySet, ctx), ")")
        } else {
            visit(node.rhs, ctx)
        }
        return lhsDoc cat text(notStr) cat rhsDoc
    }

    override fun visitExprLike(node: ExprLike, ctx: Unit): Doc {
        val valueDoc = visit(node.value, ctx)
        val kw = if (node.isNot) " NOT LIKE " else " LIKE "
        val patternDoc = visit(node.pattern, ctx)
        val escape = node.escape
        return if (escape != null) {
            valueDoc cat text(kw) cat patternDoc cat text(" ESCAPE ") cat visit(escape, ctx)
        } else {
            valueDoc cat text(kw) cat patternDoc
        }
    }

    override fun visitExprBetween(node: ExprBetween, ctx: Unit): Doc {
        val valueDoc = visit(node.value, ctx)
        val kw = if (node.isNot) " NOT BETWEEN " else " BETWEEN "
        val fromDoc = visit(node.from, ctx)
        val toDoc = visit(node.to, ctx)
        return group(
            valueDoc cat text(kw) cat fromDoc cat Doc.Line cat text("AND ") cat toDoc
        )
    }

    override fun visitExprMatch(node: ExprMatch, ctx: Unit): Doc {
        val exprDoc = visit(node.expr, ctx)
        val patternDoc = visit(node.pattern, ctx)
        return group(exprDoc cat Doc.Line cat text("MATCH ") cat patternDoc)
    }

    override fun visitExprAnd(node: ExprAnd, ctx: Unit): Doc = visitBooleanExpr(node)
    override fun visitExprOr(node: ExprOr, ctx: Unit): Doc = visitBooleanExpr(node)
    override fun visitExprNot(node: ExprNot, ctx: Unit): Doc = visitBooleanExpr(node)

    // --- I. Set Operations ---

    private fun visitSetOp(setOp: QueryBody.SetOp): Doc {
        val lhsDoc = visitExprAsDoc(setOp.lhs)
        val rhsDoc = visitExprAsDoc(setOp.rhs)
        val op = buildString {
            if (setOp.isOuter) append("OUTER ")
            when (setOp.type.setOpType.code()) {
                SetOpType.UNION -> append("UNION")
                SetOpType.INTERSECT -> append("INTERSECT")
                SetOpType.EXCEPT -> append("EXCEPT")
            }
            when (setOp.type.setq?.code()) {
                SetQuantifier.ALL -> append(" ALL")
                SetQuantifier.DISTINCT -> append(" DISTINCT")
            }
        }
        return group(lhsDoc cat Doc.Line cat text(op) cat Doc.Line cat rhsDoc)
    }

    private fun visitExprAsDoc(expr: Expr): Doc {
        return when (expr) {
            is ExprQuerySet -> visitExprQuerySet(expr, Unit)
            else -> visit(expr, Unit)
        }
    }

    // --- J. Other Clauses ---

    private fun visitWhereClause(expr: Expr): Doc {
        return group(text("WHERE") cat nest(4, Doc.Line cat visitBooleanExpr(expr)))
    }

    private fun visitHavingClause(expr: Expr): Doc {
        return group(text("HAVING") cat nest(4, Doc.Line cat visitBooleanExpr(expr)))
    }

    private fun visitGroupByClause(groupBy: GroupBy): Doc {
        val kw = when (groupBy.strategy.code()) {
            GroupByStrategy.PARTIAL -> "GROUP PARTIAL BY"
            else -> "GROUP BY"
        }
        val keys = groupBy.keys.map { key ->
            val keyDoc = visit(key.expr, Unit)
            val alias = key.asAlias
            if (alias != null) keyDoc cat text(" AS ${alias.sql()}") else keyDoc
        }
        val base = commaSeparated(kw, keys)
        val groupAlias = groupBy.asAlias
        return if (groupAlias != null) {
            base cat text(" GROUP AS ${groupAlias.sql()}")
        } else {
            base
        }
    }

    private fun visitOrderByClause(orderBy: OrderBy): Doc {
        val sorts = orderBy.sorts.map { leafExpr(it) }
        return commaSeparated("ORDER BY", sorts)
    }

    private fun visitLetClause(let: Let): Doc {
        val bindings = let.bindings.map { b ->
            visit(b.expr, Unit) cat text(" AS ${b.asAlias.sql()}")
        }
        return commaSeparated("LET", bindings)
    }

    private fun visitExcludeClause(exclude: Exclude): Doc {
        val paths = exclude.excludePaths.map { leafExpr(it) }
        return commaSeparated("EXCLUDE", paths)
    }

    private fun visitWithClause(with: With): Doc {
        val kw = if (with.isRecursive) "WITH RECURSIVE" else "WITH"
        val elements = with.elements.map { elem ->
            val name = elem.queryName.sql()
            val cols = elem.columnList
            val colsPart = if (cols != null && cols.isNotEmpty()) {
                text("(${cols.joinToString(", ") { it.sql() }}) ")
            } else {
                Doc.Nil
            }
            text("$name ") cat colsPart cat text("AS ") cat
                bracket("(", visitExprQuerySet(elem.asQuery, Unit), ")")
        }
        return commaSeparated(kw, elements)
    }

    private fun visitWindowClause(window: WindowClause): Doc {
        val defs = window.definitions.map { leafExpr(it) }
        return commaSeparated("WINDOW", defs)
    }

    // --- K. Helpers ---

    private fun clause(kw: String, body: Doc): Doc {
        return group(text(kw) cat nest(4, Doc.Line cat body))
    }

    private fun commaSeparated(kw: String, items: List<Doc>): Doc {
        if (items.isEmpty()) return text(kw)
        return group(text(kw) cat nest(4, Doc.Line cat commaJoin(items)))
    }

    private fun leafExpr(node: AstNode): Doc {
        return text(node.sql(layout = SqlLayout.ONELINE, dialect = SqlDialect.STANDARD))
    }
}
