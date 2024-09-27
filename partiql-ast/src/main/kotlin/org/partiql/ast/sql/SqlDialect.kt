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

import org.partiql.ast.AstNode
import org.partiql.ast.Exclude
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.GroupBy
import org.partiql.ast.Identifier
import org.partiql.ast.Let
import org.partiql.ast.OrderBy
import org.partiql.ast.Path
import org.partiql.ast.QueryBody
import org.partiql.ast.Select
import org.partiql.ast.SetOp
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Sort
import org.partiql.ast.Statement
import org.partiql.ast.Type
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.value.MissingValue
import org.partiql.value.NullValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.io.PartiQLValueTextWriter
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * SqlDialect represents the base behavior for transforming an [AstNode] tree into a [SqlBlock] tree.
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
public abstract class SqlDialect : AstBaseVisitor<SqlBlock, SqlBlock>() {

    public companion object {

        @JvmStatic
        public val STANDARD: SqlDialect = object : SqlDialect() {}
    }

    /**
     * Default entry-point, can also be us.
     */
    public fun transform(node: AstNode): SqlBlock {
        val head = SqlBlock.none()
        val tail = head
        node.accept(this, tail)
        return head
    }

    override fun defaultReturn(node: AstNode, tail: SqlBlock): SqlBlock =
        throw UnsupportedOperationException("Cannot print $node")

    // STATEMENTS

    override fun visitStatementQuery(node: Statement.Query, tail: SqlBlock): SqlBlock = visitExpr(node.expr, tail)

    // IDENTIFIERS & PATHS

    /**
     * Default behavior is to wrap all SFW queries with parentheses.
     *
     * @param node
     * @param tail
     */
    public open fun visitExprWrapped(node: Expr, tail: SqlBlock): SqlBlock = when (node) {
        is Expr.QuerySet -> {
            var t = tail
            t = t concat "("
            t = visit(node, t)
            t = t concat ")"
            t
        }
        else -> visitExpr(node, tail)
    }

    override fun visitIdentifierSymbol(node: Identifier.Symbol, tail: SqlBlock): SqlBlock = tail concat node.sql()

    override fun visitIdentifierQualified(node: Identifier.Qualified, tail: SqlBlock): SqlBlock {
        val path = node.steps.fold(node.root.sql()) { p, step -> p + "." + step.sql() }
        return tail concat path
    }

    override fun visitPath(node: Path, tail: SqlBlock): SqlBlock {
        val path = node.steps.fold(node.root.sql()) { p, step ->
            when (step) {
                is Path.Step.Index -> p + "[${step.index}]"
                is Path.Step.Symbol -> p + "." + step.symbol.sql()
            }
        }
        return tail concat path
    }

    override fun visitExclude(node: Exclude, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat " EXCLUDE "
        t = t concat list(start = null, end = null) { node.items }
        return t
    }

    override fun visitExcludeItem(node: Exclude.Item, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprVar(node.root, t)
        t = t concat list(delimiter = null, start = null, end = null) { node.steps }
        return t
    }

    override fun visitExcludeStepCollIndex(node: Exclude.Step.CollIndex, tail: SqlBlock): SqlBlock {
        return tail concat "[${node.index}]"
    }

    override fun visitExcludeStepStructWildcard(node: Exclude.Step.StructWildcard, tail: SqlBlock): SqlBlock {
        return tail concat ".*"
    }

    override fun visitExcludeStepStructField(node: Exclude.Step.StructField, tail: SqlBlock): SqlBlock {
        var t = tail concat "."
        t = visitIdentifierSymbol(node.symbol, t)
        return t
    }

    override fun visitExcludeStepCollWildcard(node: Exclude.Step.CollWildcard, tail: SqlBlock): SqlBlock {
        return tail concat "[*]"
    }

    // cannot write path step outside the context of a path as we don't want it to reflow
    override fun visitPathStep(node: Path.Step, tail: SqlBlock): SqlBlock =
        error("path step cannot be written directly")

    override fun visitPathStepSymbol(node: Path.Step.Symbol, tail: SqlBlock): SqlBlock = visitPathStep(node, tail)

    override fun visitPathStepIndex(node: Path.Step.Index, tail: SqlBlock): SqlBlock = visitPathStep(node, tail)

    // TYPES

    override fun visitTypeNullType(node: Type.NullType, tail: SqlBlock): SqlBlock = tail concat "NULL"

    override fun visitTypeMissing(node: Type.Missing, tail: SqlBlock): SqlBlock = tail concat "MISSING"

    override fun visitTypeBool(node: Type.Bool, tail: SqlBlock): SqlBlock = tail concat "BOOL"

    override fun visitTypeTinyint(node: Type.Tinyint, tail: SqlBlock): SqlBlock = tail concat "TINYINT"

    override fun visitTypeSmallint(node: Type.Smallint, tail: SqlBlock): SqlBlock = tail concat "SMALLINT"

    override fun visitTypeInt2(node: Type.Int2, tail: SqlBlock): SqlBlock = tail concat "INT2"

    override fun visitTypeInt4(node: Type.Int4, tail: SqlBlock): SqlBlock = tail concat "INT4"

    override fun visitTypeBigint(node: Type.Bigint, tail: SqlBlock): SqlBlock = tail concat "BIGINT"

    override fun visitTypeInt8(node: Type.Int8, tail: SqlBlock): SqlBlock = tail concat "INT8"

    override fun visitTypeInt(node: Type.Int, tail: SqlBlock): SqlBlock = tail concat "INT"

    override fun visitTypeReal(node: Type.Real, tail: SqlBlock): SqlBlock = tail concat "REAL"

    override fun visitTypeFloat32(node: Type.Float32, tail: SqlBlock): SqlBlock = tail concat "FLOAT32"

    override fun visitTypeFloat64(node: Type.Float64, tail: SqlBlock): SqlBlock = tail concat "DOUBLE PRECISION"

    override fun visitTypeDecimal(node: Type.Decimal, tail: SqlBlock): SqlBlock =
        tail concat type("DECIMAL", node.precision, node.scale)

    override fun visitTypeNumeric(node: Type.Numeric, tail: SqlBlock): SqlBlock =
        tail concat type("NUMERIC", node.precision, node.scale)

    override fun visitTypeChar(node: Type.Char, tail: SqlBlock): SqlBlock = tail concat type("CHAR", node.length)

    override fun visitTypeVarchar(node: Type.Varchar, tail: SqlBlock): SqlBlock =
        tail concat type("VARCHAR", node.length)

    override fun visitTypeString(node: Type.String, tail: SqlBlock): SqlBlock = tail concat "STRING"

    override fun visitTypeSymbol(node: Type.Symbol, tail: SqlBlock): SqlBlock = tail concat "SYMBOL"

    override fun visitTypeBit(node: Type.Bit, tail: SqlBlock): SqlBlock = tail concat type("BIT", node.length)

    override fun visitTypeBitVarying(node: Type.BitVarying, tail: SqlBlock): SqlBlock =
        tail concat type("BINARY", node.length)

    override fun visitTypeByteString(node: Type.ByteString, tail: SqlBlock): SqlBlock =
        tail concat type("BYTE", node.length)

    override fun visitTypeBlob(node: Type.Blob, tail: SqlBlock): SqlBlock = tail concat type("BLOB", node.length)

    override fun visitTypeClob(node: Type.Clob, tail: SqlBlock): SqlBlock = tail concat type("CLOB", node.length)

    override fun visitTypeBag(node: Type.Bag, tail: SqlBlock): SqlBlock = tail concat "BAG"

    override fun visitTypeList(node: Type.List, tail: SqlBlock): SqlBlock = tail concat "LIST"

    override fun visitTypeSexp(node: Type.Sexp, tail: SqlBlock): SqlBlock = tail concat "SEXP"

    override fun visitTypeTuple(node: Type.Tuple, tail: SqlBlock): SqlBlock = tail concat "TUPLE"

    override fun visitTypeStruct(node: Type.Struct, tail: SqlBlock): SqlBlock = tail concat "STRUCT"

    override fun visitTypeAny(node: Type.Any, tail: SqlBlock): SqlBlock = tail concat "ANY"

    override fun visitTypeDate(node: Type.Date, tail: SqlBlock): SqlBlock = tail concat "DATE"

    override fun visitTypeTime(node: Type.Time, tail: SqlBlock): SqlBlock = tail concat type("TIME", node.precision)

    override fun visitTypeTimeWithTz(node: Type.TimeWithTz, tail: SqlBlock): SqlBlock =
        tail concat type("TIME WITH TIMEZONE", node.precision, gap = true)

    override fun visitTypeTimestamp(node: Type.Timestamp, tail: SqlBlock): SqlBlock =
        tail concat type("TIMESTAMP", node.precision)

    override fun visitTypeTimestampWithTz(node: Type.TimestampWithTz, tail: SqlBlock): SqlBlock =
        tail concat type("TIMESTAMP WITH TIMEZONE", node.precision, gap = true)

    override fun visitTypeInterval(node: Type.Interval, tail: SqlBlock): SqlBlock =
        tail concat type("INTERVAL", node.precision)

    // unsupported
    override fun visitTypeCustom(node: Type.Custom, tail: SqlBlock): SqlBlock = defaultReturn(node, tail)

    // Expressions

    @OptIn(PartiQLValueExperimental::class)
    override fun visitExprLit(node: Expr.Lit, tail: SqlBlock): SqlBlock {
        // Simplified PartiQL Value writing, as this intentionally omits formatting
        val value = when (node.value) {
            is MissingValue -> "MISSING" // force uppercase
            is NullValue -> "NULL" // force uppercase
            else -> {
                val buffer = ByteArrayOutputStream()
                val valueWriter = PartiQLValueTextWriter(PrintStream(buffer), false)
                valueWriter.append(node.value)
                buffer.toString()
            }
        }
        return tail concat value
    }

    override fun visitExprVariant(node: Expr.Variant, tail: SqlBlock): SqlBlock {
        if (node.encoding != "ion") {
            error("Unsupported encoding ${node.encoding}")
        }
        val value = node.value
        return tail concat "`$value`"
    }

    override fun visitExprOperator(node: Expr.Operator, tail: SqlBlock): SqlBlock {
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

    override fun visitExprAnd(node: Expr.And, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.lhs, t)
        t = t concat " AND "
        t = visitExprWrapped(node.rhs, t)
        return t
    }

    override fun visitExprOr(node: Expr.Or, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.lhs, t)
        t = t concat " OR "
        t = visitExprWrapped(node.rhs, t)
        return t
    }

    override fun visitExprNot(node: Expr.Not, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "NOT ("
        t = visitExprWrapped(node.value, t)
        t = t concat ")"
        return t
    }

    override fun visitExprVar(node: Expr.Var, tail: SqlBlock): SqlBlock {
        var t = tail
        // Prepend @
        if (node.scope == Expr.Var.Scope.LOCAL) {
            t = t concat "@"
        }
        t = visitIdentifier(node.identifier, t)
        return t
    }

    override fun visitExprSessionAttribute(node: Expr.SessionAttribute, tail: SqlBlock): SqlBlock =
        tail concat node.attribute.name

    override fun visitExprPath(node: Expr.Path, tail: SqlBlock): SqlBlock {
        var t = visitExprWrapped(node.root, tail)
        t = node.steps.fold(t) { b, step -> visitExprPathStep(step, b) }
        return t
    }

    override fun visitExprPathStepSymbol(node: Expr.Path.Step.Symbol, tail: SqlBlock): SqlBlock =
        tail concat ".${node.symbol.sql()}"

    override fun visitExprPathStepIndex(node: Expr.Path.Step.Index, tail: SqlBlock): SqlBlock {
        var t = tail
        val key = node.key
        // use [ ] syntax
        t = t concat "["
        t = visitExprWrapped(key, t)
        t = t concat "]"
        return t
    }

    override fun visitExprPathStepWildcard(node: Expr.Path.Step.Wildcard, tail: SqlBlock): SqlBlock = tail concat "[*]"

    override fun visitExprPathStepUnpivot(node: Expr.Path.Step.Unpivot, tail: SqlBlock): SqlBlock = tail concat ".*"

    override fun visitExprCall(node: Expr.Call, tail: SqlBlock): SqlBlock {
        var t = tail
        val f = node.function
        // Special case -- COUNT() maps to COUNT(*)
        if (f is Identifier.Symbol && f.symbol == "COUNT" && node.args.isEmpty()) {
            return t concat "COUNT(*)"
        }
        val start = if (node.setq != null) "(${node.setq.name} " else "("
        t = visitIdentifier(f, t)
        t = t concat list(start) { node.args }
        return t
    }

    override fun visitExprParameter(node: Expr.Parameter, tail: SqlBlock): SqlBlock = tail concat "?"

    override fun visitExprValues(node: Expr.Values, tail: SqlBlock): SqlBlock =
        tail concat list("VALUES (") { node.rows }

    override fun visitExprValuesRow(node: Expr.Values.Row, tail: SqlBlock): SqlBlock = tail concat list { node.items }

    override fun visitExprCollection(node: Expr.Collection, tail: SqlBlock): SqlBlock {
        val (start, end) = when (node.type) {
            Expr.Collection.Type.BAG -> "<<" to ">>"
            Expr.Collection.Type.ARRAY -> "[" to "]"
            Expr.Collection.Type.VALUES -> "VALUES (" to ")"
            Expr.Collection.Type.LIST -> "(" to ")"
            Expr.Collection.Type.SEXP -> "SEXP (" to ")"
        }
        return tail concat list(start, end) { node.values }
    }

    override fun visitExprStruct(node: Expr.Struct, tail: SqlBlock): SqlBlock =
        tail concat list("{", "}") { node.fields }

    override fun visitExprStructField(node: Expr.Struct.Field, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.name, t)
        t = t concat ": "
        t = visitExprWrapped(node.value, t)
        return t
    }

    override fun visitExprLike(node: Expr.Like, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.value, t)
        t = t concat if (node.not == true) " NOT LIKE " else " LIKE "
        t = visitExprWrapped(node.pattern, t)
        if (node.escape != null) {
            t = t concat " ESCAPE "
            t = visitExprWrapped(node.escape!!, t)
        }
        return t
    }

    override fun visitExprBetween(node: Expr.Between, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.value, t)
        t = t concat if (node.not == true) " NOT BETWEEN " else " BETWEEN "
        t = visitExprWrapped(node.from, t)
        t = t concat " AND "
        t = visitExprWrapped(node.to, t)
        return t
    }

    override fun visitExprInCollection(node: Expr.InCollection, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.lhs, t)
        t = t concat if (node.not == true) " NOT IN " else " IN "
        t = visitExprWrapped(node.rhs, t)
        return t
    }

    override fun visitExprIsType(node: Expr.IsType, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.value, t)
        t = t concat if (node.not == true) " IS NOT " else " IS "
        t = visitType(node.type, t)
        return t
    }

    override fun visitExprCase(node: Expr.Case, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "CASE"
        t = when (node.expr) {
            null -> t
            else -> visitExprWrapped(node.expr!!, t concat " ")
        }
        // WHEN(s)
        t = node.branches.fold(t) { acc, branch -> visitExprCaseBranch(branch, acc) }
        // ELSE
        t = when (node.default) {
            null -> t
            else -> {
                t = t concat " ELSE "
                visitExprWrapped(node.default!!, t)
            }
        }
        t = t concat " END"
        return t
    }

    override fun visitExprCaseBranch(node: Expr.Case.Branch, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat " WHEN "
        t = visitExprWrapped(node.condition, t)
        t = t concat " THEN "
        t = visitExprWrapped(node.expr, t)
        return t
    }

    override fun visitExprCoalesce(node: Expr.Coalesce, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "COALESCE"
        t = t concat list { node.args }
        return t
    }

    override fun visitExprNullIf(node: Expr.NullIf, tail: SqlBlock): SqlBlock {
        val args = listOf(node.value, node.nullifier)
        var t = tail
        t = t concat "NULLIF"
        t = t concat list { args }
        return t
    }

    override fun visitExprSubstring(node: Expr.Substring, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "SUBSTRING("
        t = visitExprWrapped(node.value, t)
        if (node.start != null) {
            t = t concat " FROM "
            t = visitExprWrapped(node.start!!, t)
        }
        if (node.length != null) {
            t = t concat " FOR "
            t = visitExprWrapped(node.length!!, t)
        }
        t = t concat ")"
        return t
    }

    override fun visitExprPosition(node: Expr.Position, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "POSITION("
        t = visitExprWrapped(node.lhs, t)
        t = t concat " IN "
        t = visitExprWrapped(node.rhs, t)
        t = t concat ")"
        return t
    }

    override fun visitExprTrim(node: Expr.Trim, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "TRIM("
        // [LEADING|TRAILING|BOTH]
        if (node.spec != null) {
            t = t concat "${node.spec!!.name} "
        }
        // [<chars> FROM]
        if (node.chars != null) {
            t = visitExprWrapped(node.chars!!, t)
            t = t concat " FROM "
        }
        t = visitExprWrapped(node.value, t)
        t = t concat ")"
        return t
    }

    override fun visitExprOverlay(node: Expr.Overlay, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "OVERLAY("
        t = visitExprWrapped(node.value, t)
        t = t concat " PLACING "
        t = visitExprWrapped(node.overlay, t)
        t = t concat " FROM "
        t = visitExprWrapped(node.start, t)
        if (node.length != null) {
            t = t concat " FOR "
            t = visitExprWrapped(node.length!!, t)
        }
        t = t concat ")"
        return t
    }

    override fun visitExprExtract(node: Expr.Extract, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "EXTRACT("
        t = t concat node.field.name
        t = t concat " FROM "
        t = visitExprWrapped(node.source, t)
        t = t concat ")"
        return t
    }

    override fun visitExprCast(node: Expr.Cast, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "CAST("
        t = visitExprWrapped(node.value, t)
        t = t concat " AS "
        t = visitType(node.asType, t)
        t = t concat ")"
        return t
    }

    override fun visitExprDateAdd(node: Expr.DateAdd, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "DATE_ADD("
        t = t concat node.field.name
        t = t concat ", "
        t = visitExprWrapped(node.lhs, t)
        t = t concat ", "
        t = visitExprWrapped(node.rhs, t)
        t = t concat ")"
        return t
    }

    override fun visitExprDateDiff(node: Expr.DateDiff, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "DATE_DIFF("
        t = t concat node.field.name
        t = t concat ", "
        t = visitExprWrapped(node.lhs, t)
        t = t concat ", "
        t = visitExprWrapped(node.rhs, t)
        t = t concat ")"
        return t
    }

    override fun visitExprQuerySet(node: Expr.QuerySet, tail: SqlBlock): SqlBlock {
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
        t = visit(node.from, t concat " FROM ")
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
        when (node.type.type) {
            SetOp.Type.UNION -> op.add("UNION")
            SetOp.Type.INTERSECT -> op.add("INTERSECT")
            SetOp.Type.EXCEPT -> op.add("EXCEPT")
        }
        when (node.type.setq) {
            SetQuantifier.ALL -> op.add("ALL")
            SetQuantifier.DISTINCT -> op.add("DISTINCT")
            null -> {}
        }
        var t = tail
        t = visitExprWrapped(node.lhs, t)
        t = t concat " ${op.joinToString(" ")} "
        t = visitExprWrapped(node.rhs, t)
        return t
    }

    // SELECT

    override fun visitSelectStar(node: Select.Star, tail: SqlBlock): SqlBlock {
        val select = when (node.setq) {
            SetQuantifier.ALL -> "SELECT ALL *"
            SetQuantifier.DISTINCT -> "SELECT DISTINCT *"
            null -> "SELECT *"
        }
        return tail concat select
    }

    override fun visitSelectProject(node: Select.Project, tail: SqlBlock): SqlBlock {
        val select = when (node.setq) {
            SetQuantifier.ALL -> "SELECT ALL "
            SetQuantifier.DISTINCT -> "SELECT DISTINCT "
            null -> "SELECT "
        }
        return tail concat list(select, "") { node.items }
    }

    override fun visitSelectProjectItemAll(node: Select.Project.Item.All, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        t = t concat ".*"
        return t
    }

    override fun visitSelectProjectItemExpression(node: Select.Project.Item.Expression, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        t = if (node.asAlias != null) t concat " AS ${node.asAlias!!.sql()}" else t
        return t
    }

    override fun visitSelectPivot(node: Select.Pivot, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "PIVOT "
        t = visitExprWrapped(node.key, t)
        t = t concat " AT "
        t = visitExprWrapped(node.value, t)
        return t
    }

    override fun visitSelectValue(node: Select.Value, tail: SqlBlock): SqlBlock {
        val select = when (node.setq) {
            SetQuantifier.ALL -> "SELECT ALL VALUE "
            SetQuantifier.DISTINCT -> "SELECT DISTINCT VALUE "
            null -> "SELECT VALUE "
        }
        var t = tail
        t = t concat select
        t = visitExprWrapped(node.constructor, t)
        return t
    }

    // FROM

    override fun visitFromValue(node: From.Value, tail: SqlBlock): SqlBlock {
        var t = tail
        t = when (node.type) {
            From.Value.Type.SCAN -> t
            From.Value.Type.UNPIVOT -> t concat "UNPIVOT "
        }
        t = visitExprWrapped(node.expr, t)
        t = if (node.asAlias != null) t concat " AS ${node.asAlias!!.sql()}" else t
        t = if (node.atAlias != null) t concat " AT ${node.atAlias!!.sql()}" else t
        t = if (node.byAlias != null) t concat " BY ${node.byAlias!!.sql()}" else t
        return t
    }

    override fun visitFromJoin(node: From.Join, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitFrom(node.lhs, t)
        t = t concat when (node.type) {
            From.Join.Type.INNER -> " INNER JOIN "
            From.Join.Type.LEFT -> " LEFT JOIN "
            From.Join.Type.LEFT_OUTER -> " LEFT OUTER JOIN "
            From.Join.Type.RIGHT -> " RIGHT JOIN "
            From.Join.Type.RIGHT_OUTER -> " RIGHT OUTER JOIN "
            From.Join.Type.FULL -> " FULL JOIN "
            From.Join.Type.FULL_OUTER -> " FULL OUTER JOIN "
            From.Join.Type.CROSS -> " CROSS JOIN "
            From.Join.Type.COMMA -> ", "
            null -> " JOIN "
        }
        t = visitFrom(node.rhs, t)
        t = if (node.condition != null) visit(node.condition!!, t concat " ON ") else t
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
        t = t concat when (node.strategy) {
            GroupBy.Strategy.FULL -> "GROUP BY "
            GroupBy.Strategy.PARTIAL -> "GROUP PARTIAL BY "
        }
        t = t concat list("", "") { node.keys }
        t = if (node.asAlias != null) t concat " GROUP AS ${node.asAlias!!.sql()}" else t
        return t
    }

    override fun visitGroupByKey(node: GroupBy.Key, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        t = if (node.asAlias != null) t concat " AS ${node.asAlias!!.sql()}" else t
        return t
    }

    // SET OPERATORS

    override fun visitSetOp(node: SetOp, tail: SqlBlock): SqlBlock {
        val op = when (node.setq) {
            null -> node.type.name
            else -> "${node.type.name} ${node.setq!!.name}"
        }
        return tail concat op
    }

    // ORDER BY

    override fun visitOrderBy(node: OrderBy, tail: SqlBlock): SqlBlock =
        tail concat list("ORDER BY ", "") { node.sorts }

    override fun visitSort(node: Sort, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        t = when (node.dir) {
            Sort.Dir.ASC -> t concat " ASC"
            Sort.Dir.DESC -> t concat " DESC"
            null -> t
        }
        t = when (node.nulls) {
            Sort.Nulls.FIRST -> t concat " NULLS FIRST"
            Sort.Nulls.LAST -> t concat " NULLS LAST"
            null -> t
        }
        return t
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

    private fun Identifier.Symbol.sql() = when (caseSensitivity) {
        Identifier.CaseSensitivity.SENSITIVE -> "\"$symbol\""
        Identifier.CaseSensitivity.INSENSITIVE -> symbol // verbatim ..
    }
}
