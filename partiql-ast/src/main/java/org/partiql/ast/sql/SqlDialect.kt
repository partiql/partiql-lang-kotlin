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

package org.partiql.ast.sql

import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.identifier
import org.partiql.ast.Ast.identifierChain
import org.partiql.ast.AstNode
import org.partiql.ast.AstVisitor
import org.partiql.ast.DataType
import org.partiql.ast.Exclude
import org.partiql.ast.ExcludePath
import org.partiql.ast.ExcludeStep
import org.partiql.ast.From
import org.partiql.ast.FromExpr
import org.partiql.ast.FromJoin
import org.partiql.ast.FromType
import org.partiql.ast.GroupBy
import org.partiql.ast.GroupByStrategy
import org.partiql.ast.Identifier
import org.partiql.ast.IdentifierChain
import org.partiql.ast.JoinType
import org.partiql.ast.Let
import org.partiql.ast.Nulls
import org.partiql.ast.Order
import org.partiql.ast.OrderBy
import org.partiql.ast.Query
import org.partiql.ast.QueryBody
import org.partiql.ast.SelectItem
import org.partiql.ast.SelectList
import org.partiql.ast.SelectPivot
import org.partiql.ast.SelectStar
import org.partiql.ast.SelectValue
import org.partiql.ast.SetOp
import org.partiql.ast.SetOpType
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Sort
import org.partiql.ast.ddl.Ddl
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprAnd
import org.partiql.ast.expr.ExprArray
import org.partiql.ast.expr.ExprBag
import org.partiql.ast.expr.ExprBetween
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprCase
import org.partiql.ast.expr.ExprCast
import org.partiql.ast.expr.ExprCoalesce
import org.partiql.ast.expr.ExprExtract
import org.partiql.ast.expr.ExprInCollection
import org.partiql.ast.expr.ExprIsType
import org.partiql.ast.expr.ExprLike
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprNot
import org.partiql.ast.expr.ExprNullIf
import org.partiql.ast.expr.ExprOperator
import org.partiql.ast.expr.ExprOr
import org.partiql.ast.expr.ExprOverlay
import org.partiql.ast.expr.ExprParameter
import org.partiql.ast.expr.ExprPath
import org.partiql.ast.expr.ExprPosition
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprRowValue
import org.partiql.ast.expr.ExprSessionAttribute
import org.partiql.ast.expr.ExprStruct
import org.partiql.ast.expr.ExprSubstring
import org.partiql.ast.expr.ExprTrim
import org.partiql.ast.expr.ExprValues
import org.partiql.ast.expr.ExprVarRef
import org.partiql.ast.expr.ExprVariant
import org.partiql.ast.expr.PathStep
import org.partiql.ast.expr.Scope
import org.partiql.ast.literal.LiteralKind

/**
 * SqlDialect represents the base behavior for transforming an [AstNode] tree into a [SqlBlock] tree.
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
public abstract class SqlDialect : AstVisitor<SqlBlock, SqlBlock>() {

    public companion object {

        @JvmStatic
        public val STANDARD: SqlDialect = object : SqlDialect() {}
    }

    /**
     * Default entry-point, can also be us.
     */
    public fun transform(node: AstNode): SqlBlock {
        val head = SqlBlock.none()
        node.accept(this, head)
        return head
    }

    override fun defaultReturn(node: AstNode, tail: SqlBlock): SqlBlock =
        throw UnsupportedOperationException("Cannot print $node")

    // STATEMENTS

    override fun visitQuery(node: Query, tail: SqlBlock): SqlBlock = visitExpr(node.expr, tail)

    // IDENTIFIERS & PATHS

    /**
     * Default behavior is to wrap all SFW queries with parentheses.
     *
     * @param node
     * @param tail
     */
    public open fun visitExprWrapped(node: Expr, tail: SqlBlock): SqlBlock = when (node) {
        is ExprQuerySet -> {
            var t = tail
            t = t concat "("
            t = visit(node, t)
            t = t concat ")"
            t
        }
        else -> visitExpr(node, tail)
    }

    override fun visitIdentifier(node: Identifier, tail: SqlBlock): SqlBlock = tail concat node.sql()

    override fun visitIdentifierChain(node: IdentifierChain, tail: SqlBlock): SqlBlock {
        var path = node.root.sql()
        if (node.next != null) {
            path += ".${node.next.sql()}"
        }
        return tail concat path
    }

    override fun visitExclude(node: Exclude, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat " EXCLUDE "
        t = t concat list(start = null, end = null) { node.excludePaths }
        return t
    }

    override fun visitExcludePath(node: ExcludePath, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprVarRef(node.root, t)
        t = t concat list(delimiter = null, start = null, end = null) { node.excludeSteps }
        return t
    }

    override fun visitExcludeStepCollIndex(node: ExcludeStep.CollIndex, tail: SqlBlock): SqlBlock {
        return tail concat "[${node.index}]"
    }

    override fun visitExcludeStepStructWildcard(node: ExcludeStep.StructWildcard, tail: SqlBlock): SqlBlock {
        return tail concat ".*"
    }

    override fun visitExcludeStepStructField(node: ExcludeStep.StructField, tail: SqlBlock): SqlBlock {
        var t = tail concat "."
        t = visitIdentifier(node.symbol, t)
        return t
    }

    override fun visitExcludeStepCollWildcard(node: ExcludeStep.CollWildcard, tail: SqlBlock): SqlBlock {
        return tail concat "[*]"
    }

    // TYPES
    override fun visitDataType(node: DataType, tail: SqlBlock): SqlBlock {
        return when (node.code()) {
            // <absent types>
            DataType.NULL, DataType.MISSING -> tail concat node.name()
            // <character string type>
            //   no params
            DataType.STRING, DataType.SYMBOL -> tail concat node.name()
            //   with params
            DataType.CHARACTER, DataType.CHAR, DataType.VARCHAR, DataType.CLOB -> tail concat type(node.name(), node.length)
            DataType.CHARACTER_VARYING -> tail concat type("CHARACTER VARYING", node.length)
            DataType.CHAR_VARYING -> tail concat type("CHAR VARYING", node.length)
            DataType.CHARACTER_LARGE_OBJECT -> tail concat type("CHARACTER LARGE OBJECT", node.length)
            DataType.CHAR_LARGE_OBJECT -> tail concat type("CHAR LARGE OBJECT", node.length)
            // <binary large object string type>
            DataType.BLOB -> tail concat type(node.name(), node.length)
            DataType.BINARY_LARGE_OBJECT -> tail concat type("BINARY LARGE OBJECT", node.length)
            // <bit string type>
            DataType.BIT -> tail concat type(node.name(), node.length)
            DataType.BIT_VARYING -> tail concat type("BIT VARYING", node.length)
            // <numeric type> - <exact numeric type>
            //   no params
            DataType.BIGINT, DataType.INT8, DataType.INTEGER8, DataType.INT4, DataType.INTEGER4, DataType.INTEGER,
            DataType.INT, DataType.INT2, DataType.INTEGER2, DataType.SMALLINT, DataType.TINYINT -> tail concat node.name()
            //   with params
            DataType.NUMERIC, DataType.DECIMAL, DataType.DEC -> tail concat type(node.name(), node.precision, node.scale)
            // <numeric type> - <approximate numeric type>
            DataType.FLOAT, DataType.REAL -> tail concat node.name()
            DataType.DOUBLE_PRECISION -> tail concat "DOUBLE PRECISION"
            // <boolean type>
            DataType.BOOLEAN, DataType.BOOL -> tail concat node.name()
            // <datetime type>
            //   no params
            DataType.DATE -> tail concat node.name()
            //   with params
            DataType.TIME, DataType.TIMESTAMP -> tail concat type(node.name(), node.precision, gap = true)
            DataType.TIME_WITH_TIME_ZONE -> tail concat type("TIME", node.precision, gap = true) concat(" WITH TIME ZONE")
            DataType.TIMESTAMP_WITH_TIME_ZONE -> tail concat type("TIMESTAMP", node.precision, gap = true) concat(" WITH TIME ZONE")
            // <interval type>
            DataType.INTERVAL -> tail concat type("INTERVAL", node.precision)
            // <container type>
            DataType.STRUCT, DataType.TUPLE -> tail concat node.name()
            // <collection type>
            DataType.LIST, DataType.BAG, DataType.SEXP -> tail concat node.name()
            // <user defined type>
            DataType.USER_DEFINED -> visitIdentifierChain(node.name, tail)
            else -> defaultReturn(node, tail)
        }
    }

    // Expressions

    override fun visitExprLit(node: ExprLit, tail: SqlBlock): SqlBlock {
        val lit = node.lit
        var t = tail
        val litText = when (lit.kind().code()) {
            LiteralKind.NULL -> "NULL"
            LiteralKind.MISSING -> "MISSING"
            LiteralKind.BOOLEAN -> lit.booleanValue().toString()
            LiteralKind.NUM_APPROX -> lit.numberValue()
            LiteralKind.NUM_EXACT -> lit.numberValue()
            LiteralKind.NUM_INT -> lit.numberValue()
            LiteralKind.STRING -> String.format("'%s'", lit.stringValue())
            LiteralKind.TYPED_STRING -> {
                t = visitDataType(lit.dataType(), t)
                String.format(" '%s'", lit.stringValue())
            }
            else -> error("Unsupported literal kind ${lit.kind()}")
        }
        return t concat litText
    }

    override fun visitExprVariant(node: ExprVariant, tail: SqlBlock): SqlBlock {
        if (node.encoding != "ion") {
            error("Unsupported encoding ${node.encoding}")
        }
        val value = node.value
        return tail concat "`$value`"
    }

    override fun visitExprOperator(node: ExprOperator, tail: SqlBlock): SqlBlock {
        val lhs = node.lhs
        return if (lhs != null) {
            var t = tail
            t = visitExprWrapped(node.lhs, t)
            t = t concat " ${node.symbol} "
            t = visitExprWrapped(node.rhs, t)
            t
        } else {
            var t = tail
            t = t concat node.symbol + "("
            t = visitExprWrapped(node.rhs, t)
            t = t concat ")"
            return t
        }
    }

    override fun visitExprAnd(node: ExprAnd, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.lhs, t)
        t = t concat " AND "
        t = visitExprWrapped(node.rhs, t)
        return t
    }

    override fun visitExprOr(node: ExprOr, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.lhs, t)
        t = t concat " OR "
        t = visitExprWrapped(node.rhs, t)
        return t
    }

    override fun visitExprNot(node: ExprNot, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "NOT ("
        t = visitExprWrapped(node.value, t)
        t = t concat ")"
        return t
    }

    override fun visitExprVarRef(node: ExprVarRef, tail: SqlBlock): SqlBlock {
        var t = tail
        // Prepend @
        if (node.scope.code() == Scope.LOCAL) {
            t = t concat "@"
        }
        t = visitIdentifierChain(node.identifierChain, t)
        return t
    }

    override fun visitExprSessionAttribute(node: ExprSessionAttribute, tail: SqlBlock): SqlBlock =
        tail concat node.sessionAttribute.name()

    override fun visitExprPath(node: ExprPath, tail: SqlBlock): SqlBlock {
        var t = visitExprWrapped(node.root, tail)
        var cur = node.next
        while (cur != null) {
            t = visitPathStep(cur, t)
            cur = cur.next
        }
        return t
    }

    override fun visitPathStepField(node: PathStep.Field, tail: SqlBlock): SqlBlock =
        tail concat ".${node.field.sql()}"

    override fun visitPathStepElement(node: PathStep.Element, tail: SqlBlock): SqlBlock {
        var t = tail
        val key = node.element
        // use [ ] syntax
        t = t concat "["
        t = visitExprWrapped(key, t)
        t = t concat "]"
        return t
    }

    override fun visitPathStepAllElements(node: PathStep.AllElements, tail: SqlBlock): SqlBlock = tail concat "[*]"

    override fun visitPathStepAllFields(node: PathStep.AllFields, tail: SqlBlock): SqlBlock = tail concat ".*"

    override fun visitExprCall(node: ExprCall, tail: SqlBlock): SqlBlock {
        var t = tail
        val f = node.function
        // Special case -- COUNT() maps to COUNT(*)
        if (f.next == null && f.root.symbol.uppercase() == "COUNT" && node.args.isEmpty()) {
            return t concat "COUNT(*)"
        }
        // Special case -- DATE_ADD('<datetime_field>', <lhs>, <rhs>) -> DATE_ADD(<datetime_field>, <lhs>, <rhs>)
        // Special case -- DATE_DIFF('<datetime_field>', <lhs>, <rhs>) -> DATE_DIFF(<datetime_field>, <lhs>, <rhs>)
        if (f.next == null &&
            (f.root.symbol.uppercase() == "DATE_ADD" || f.root.symbol.uppercase() == "DATE_DIFF") &&
            node.args.size == 3
        ) {
            val dtField = (node.args[0] as ExprLit).lit.stringValue()
            // Represent as an `ExprVarRef` to mimic a literal symbol.
            // TODO consider some other representation for unquoted strings
            val newArgs = listOf(exprVarRef(identifierChain(identifier(dtField, isDelimited = false), next = null), scope = Scope.DEFAULT())) + node.args.drop(1)
            t = visitIdentifierChain(f, t)
            t = t concat list { newArgs }
            return t
        }
        val start = if (node.setq != null) "(${node.setq.name()} " else "("
        t = visitIdentifierChain(f, t)
        t = t concat list(start) { node.args }
        return t
    }

    override fun visitExprParameter(node: ExprParameter, tail: SqlBlock): SqlBlock = tail concat "?"

    override fun visitExprValues(node: ExprValues, tail: SqlBlock): SqlBlock =
        tail concat list(start = "VALUES ", end = "") { node.rows }

    override fun visitExprRowValue(node: ExprRowValue, tail: SqlBlock): SqlBlock = tail concat list { node.values }

    override fun visitExprBag(node: ExprBag, tail: SqlBlock): SqlBlock {
        return tail concat list("<<", ">>") { node.values }
    }

    override fun visitExprArray(node: ExprArray, tail: SqlBlock): SqlBlock {
        return tail concat list("[", "]") { node.values }
    }

    override fun visitExprStruct(node: ExprStruct, tail: SqlBlock): SqlBlock =
        tail concat list("{", "}") { node.fields }

    override fun visitExprStructField(node: ExprStruct.Field, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.name, t)
        t = t concat ": "
        t = visitExprWrapped(node.value, t)
        return t
    }

    override fun visitExprLike(node: ExprLike, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.value, t)
        t = t concat if (node.not) " NOT LIKE " else " LIKE "
        t = visitExprWrapped(node.pattern, t)
        if (node.escape != null) {
            t = t concat " ESCAPE "
            t = visitExprWrapped(node.escape, t)
        }
        return t
    }

    override fun visitExprBetween(node: ExprBetween, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.value, t)
        t = t concat if (node.not) " NOT BETWEEN " else " BETWEEN "
        t = visitExprWrapped(node.from, t)
        t = t concat " AND "
        t = visitExprWrapped(node.to, t)
        return t
    }

    override fun visitExprInCollection(node: ExprInCollection, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.lhs, t)
        t = t concat if (node.not) " NOT IN " else " IN "
        t = visitExprWrapped(node.rhs, t)
        return t
    }

    override fun visitExprIsType(node: ExprIsType, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.value, t)
        t = t concat if (node.not) " IS NOT " else " IS "
        t = visitDataType(node.type, t)
        return t
    }

    override fun visitExprCase(node: ExprCase, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "CASE"
        t = when (node.expr) {
            null -> t
            else -> visitExprWrapped(node.expr, t concat " ")
        }
        // WHEN(s)
        t = node.branches.fold(t) { acc, branch -> visitExprCaseBranch(branch, acc) }
        // ELSE
        t = when (node.defaultExpr) {
            null -> t
            else -> {
                t = t concat " ELSE "
                visitExprWrapped(node.defaultExpr, t)
            }
        }
        t = t concat " END"
        return t
    }

    override fun visitExprCaseBranch(node: ExprCase.Branch, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat " WHEN "
        t = visitExprWrapped(node.condition, t)
        t = t concat " THEN "
        t = visitExprWrapped(node.expr, t)
        return t
    }

    override fun visitExprCoalesce(node: ExprCoalesce, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "COALESCE"
        t = t concat list { node.args }
        return t
    }

    override fun visitExprNullIf(node: ExprNullIf, tail: SqlBlock): SqlBlock {
        val args = listOf(node.v1, node.v2)
        var t = tail
        t = t concat "NULLIF"
        t = t concat list { args }
        return t
    }

    override fun visitExprSubstring(node: ExprSubstring, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "SUBSTRING("
        t = visitExprWrapped(node.value, t)
        if (node.start != null) {
            t = t concat " FROM "
            t = visitExprWrapped(node.start, t)
        }
        if (node.length != null) {
            t = t concat " FOR "
            t = visitExprWrapped(node.length, t)
        }
        t = t concat ")"
        return t
    }

    override fun visitExprPosition(node: ExprPosition, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "POSITION("
        t = visitExprWrapped(node.lhs, t)
        t = t concat " IN "
        t = visitExprWrapped(node.rhs, t)
        t = t concat ")"
        return t
    }

    override fun visitExprTrim(node: ExprTrim, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "TRIM("
        // [LEADING|TRAILING|BOTH]
        if (node.trimSpec != null) {
            t = t concat "${node.trimSpec.name()} "
        }
        // [<chars> FROM]
        if (node.chars != null) {
            t = visitExprWrapped(node.chars, t)
            t = t concat " FROM "
        }
        t = visitExprWrapped(node.value, t)
        t = t concat ")"
        return t
    }

    override fun visitExprOverlay(node: ExprOverlay, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "OVERLAY("
        t = visitExprWrapped(node.value, t)
        t = t concat " PLACING "
        t = visitExprWrapped(node.placing, t)
        t = t concat " FROM "
        t = visitExprWrapped(node.from, t)
        if (node.forLength != null) {
            t = t concat " FOR "
            t = visitExprWrapped(node.forLength, t)
        }
        t = t concat ")"
        return t
    }

    override fun visitExprExtract(node: ExprExtract, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "EXTRACT("
        t = t concat node.field.name()
        t = t concat " FROM "
        t = visitExprWrapped(node.source, t)
        t = t concat ")"
        return t
    }

    override fun visitExprCast(node: ExprCast, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "CAST("
        t = visitExprWrapped(node.value, t)
        t = t concat " AS "
        t = visitDataType(node.asType, t)
        t = t concat ")"
        return t
    }

    override fun visitExprQuerySet(node: ExprQuerySet, tail: SqlBlock): SqlBlock {
        var t = tail
        // visit body (SFW or other SQL set op)
        t = visit(node.body, t)
        // ORDER BY
        t = if (node.orderBy != null) visitOrderBy(node.orderBy, t concat " ") else t
        // LIMIT
        t = if (node.limit != null) visitExprWrapped(node.limit, t concat " LIMIT ") else t
        // OFFSET
        t = if (node.offset != null) visitExprWrapped(node.offset, t concat " OFFSET ") else t
        return t
    }

    // SELECT-FROM-WHERE

    override fun visitQueryBodySFW(node: QueryBody.SFW, tail: SqlBlock): SqlBlock {
        var t = tail
        // SELECT
        t = visit(node.select, t)
        // EXCLUDE
        t = node.exclude?.let { visit(it, t) } ?: t
        // FROM
        t = visitFrom(node.from, t concat " FROM ")
        // LET
        t = if (node.let != null) visitLet(node.let, t concat " ") else t
        // WHERE
        t = if (node.where != null) visitExprWrapped(node.where, t concat " WHERE ") else t
        // GROUP BY
        t = if (node.groupBy != null) visitGroupBy(node.groupBy, t concat " ") else t
        // HAVING
        t = if (node.having != null) visitExprWrapped(node.having, t concat " HAVING ") else t
        return t
    }

    override fun visitQueryBodySetOp(node: QueryBody.SetOp, tail: SqlBlock): SqlBlock {
        val op = mutableListOf<String>()
        when (node.isOuter) {
            true -> op.add("OUTER")
            else -> {}
        }
        when (node.type.setOpType.code()) {
            SetOpType.UNION -> op.add("UNION")
            SetOpType.INTERSECT -> op.add("INTERSECT")
            SetOpType.EXCEPT -> op.add("EXCEPT")
            else -> defaultReturn(node, tail)
        }
        when (node.type.setq?.code()) {
            SetQuantifier.ALL -> op.add("ALL")
            SetQuantifier.DISTINCT -> op.add("DISTINCT")
            null -> {}
            else -> defaultReturn(node, tail)
        }
        var t = tail
        t = visitExprWrapped(node.lhs, t)
        t = t concat " ${op.joinToString(" ")} "
        t = visitExprWrapped(node.rhs, t)
        return t
    }

    // SELECT

    override fun visitSelectStar(node: SelectStar, tail: SqlBlock): SqlBlock {
        val select = when (node.setq?.code()) {
            SetQuantifier.ALL -> "SELECT ALL *"
            SetQuantifier.DISTINCT -> "SELECT DISTINCT *"
            null -> "SELECT *"
            else -> throw UnsupportedOperationException("Cannot print $node")
        }
        return tail concat select
    }

    override fun visitSelectList(node: SelectList, tail: SqlBlock): SqlBlock {
        val select = when (node.setq?.code()) {
            SetQuantifier.ALL -> "SELECT ALL "
            SetQuantifier.DISTINCT -> "SELECT DISTINCT "
            null -> "SELECT "
            else -> throw UnsupportedOperationException("Cannot print $node")
        }
        return tail concat list(select, "") { node.items }
    }

    override fun visitSelectItemStar(node: SelectItem.Star, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        t = t concat ".*"
        return t
    }

    override fun visitSelectItemExpr(node: SelectItem.Expr, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        t = if (node.asAlias != null) t concat " AS ${node.asAlias.sql()}" else t
        return t
    }

    override fun visitSelectPivot(node: SelectPivot, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "PIVOT "
        t = visitExprWrapped(node.key, t)
        t = t concat " AT "
        t = visitExprWrapped(node.value, t)
        return t
    }

    override fun visitSelectValue(node: SelectValue, tail: SqlBlock): SqlBlock {
        val select = when (node.setq?.code()) {
            SetQuantifier.ALL -> "SELECT ALL VALUE "
            SetQuantifier.DISTINCT -> "SELECT DISTINCT VALUE "
            null -> "SELECT VALUE "
            else -> throw UnsupportedOperationException("Cannot print $node")
        }
        var t = tail
        t = t concat select
        t = visitExprWrapped(node.constructor, t)
        return t
    }

    // FROM
    override fun visitFrom(node: From, tail: SqlBlock): SqlBlock {
        return tail concat list("", "") { node.tableRefs }
    }

    override fun visitFromExpr(node: FromExpr, tail: SqlBlock): SqlBlock {
        var t = tail
        t = when (node.fromType.code()) {
            FromType.SCAN -> t
            FromType.UNPIVOT -> t concat "UNPIVOT "
            else -> defaultReturn(node, tail)
        }
        t = visitExprWrapped(node.expr, t)
        t = if (node.asAlias != null) t concat " AS ${node.asAlias.sql()}" else t
        t = if (node.atAlias != null) t concat " AT ${node.atAlias.sql()}" else t
        return t
    }

    override fun visitFromJoin(node: FromJoin, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitFromTableRef(node.lhs, t)
        t = t concat when (node.joinType?.code()) {
            JoinType.INNER -> " INNER JOIN "
            JoinType.LEFT -> " LEFT JOIN "
            JoinType.LEFT_OUTER -> " LEFT OUTER JOIN "
            JoinType.RIGHT -> " RIGHT JOIN "
            JoinType.RIGHT_OUTER -> " RIGHT OUTER JOIN "
            JoinType.FULL -> " FULL JOIN "
            JoinType.FULL_OUTER -> " FULL OUTER JOIN "
            JoinType.CROSS -> " CROSS JOIN "
            JoinType.LEFT_CROSS -> " LEFT CROSS JOIN "
            null -> " JOIN "
            else -> throw UnsupportedOperationException("Cannot print $node")
        }
        t = visitFromTableRef(node.rhs, t)
        t = if (node.condition != null) visit(node.condition, t concat " ON ") else t
        return t
    }

    // LET

    override fun visitLet(node: Let, tail: SqlBlock): SqlBlock = tail concat list("LET ", "") { node.bindings }

    override fun visitLetBinding(node: Let.Binding, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        t = t concat " AS ${node.asAlias.sql()}"
        return t
    }

    // GROUP BY

    override fun visitGroupBy(node: GroupBy, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat when (node.strategy.code()) {
            GroupByStrategy.FULL -> "GROUP BY "
            GroupByStrategy.PARTIAL -> "GROUP PARTIAL BY "
            else -> throw UnsupportedOperationException("Cannot print $node")
        }
        t = t concat list("", "") { node.keys }
        t = if (node.asAlias != null) t concat " GROUP AS ${node.asAlias.sql()}" else t
        return t
    }

    override fun visitGroupByKey(node: GroupBy.Key, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        t = if (node.asAlias != null) t concat " AS ${node.asAlias.sql()}" else t
        return t
    }

    // SET OPERATORS

    override fun visitSetOp(node: SetOp, tail: SqlBlock): SqlBlock {
        val op = when (node.setq) {
            null -> node.setOpType.name()
            else -> "${node.setOpType.name()} ${node.setq.name()}"
        }
        return tail concat op
    }

    // ORDER BY

    override fun visitOrderBy(node: OrderBy, tail: SqlBlock): SqlBlock =
        tail concat list("ORDER BY ", "") { node.sorts }

    override fun visitSort(node: Sort, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        t = when (node.order?.code()) {
            Order.ASC -> t concat " ASC"
            Order.DESC -> t concat " DESC"
            null -> t
            else -> throw UnsupportedOperationException("Cannot print $node")
        }
        t = when (node.nulls?.code()) {
            Nulls.FIRST -> t concat " NULLS FIRST"
            Nulls.LAST -> t concat " NULLS LAST"
            null -> t
            else -> throw UnsupportedOperationException("Cannot print $node")
        }
        return t
    }

    // TODO: DDL
    override fun visitDdl(node: Ddl, ctx: SqlBlock): SqlBlock {
        throw UnsupportedOperationException("DDL has not been supported yet in SqlDialect")
    }

    // --- Block Constructor Helpers

    private infix fun SqlBlock.concat(rhs: String): SqlBlock {
        next = SqlBlock.Text(rhs)
        return next!!
    }

    private infix fun SqlBlock.concat(rhs: SqlBlock): SqlBlock {
        next = rhs
        return next!!
    }

    private fun type(symbol: String, vararg args: Int?, gap: Boolean = false): SqlBlock {
        val p = args.filterNotNull()
        val t = when {
            p.isEmpty() -> symbol
            else -> {
                val a = p.joinToString(",")
                when (gap) {
                    true -> "$symbol ($a)"
                    else -> "$symbol($a)"
                }
            }
        }
        // types are modeled as text; as we don't way to reflow
        return SqlBlock.Text(t)
    }

    private fun list(
        start: String? = "(",
        end: String? = ")",
        delimiter: String? = ", ",
        children: () -> List<AstNode>,
    ): SqlBlock {
        val kids = children()
        val h = SqlBlock.none()
        var t = h
        kids.forEachIndexed { i, child ->
            t = child.accept(this, t)
            t = if (delimiter != null && (i + 1) < kids.size) t concat delimiter else t
        }
        return SqlBlock.Nest(
            prefix = start,
            postfix = end,
            child = h,
        )
    }

    private fun Identifier.sql() = when (isDelimited) {
        true -> "\"$symbol\""
        false -> symbol // verbatim ..
    }
}
