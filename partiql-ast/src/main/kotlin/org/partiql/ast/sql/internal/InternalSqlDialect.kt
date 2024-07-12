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

package org.partiql.ast.sql.internal

import org.partiql.ast.AstNode
import org.partiql.ast.Binder
import org.partiql.ast.Exclude
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.GroupBy
import org.partiql.ast.Identifier
import org.partiql.ast.Let
import org.partiql.ast.OrderBy
import org.partiql.ast.Path
import org.partiql.ast.Select
import org.partiql.ast.SetOp
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Sort
import org.partiql.ast.Statement
import org.partiql.ast.Type
import org.partiql.ast.sql.sql
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.value.MissingValue
import org.partiql.value.NullValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.io.PartiQLValueTextWriter
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * SqlDialect represents the base behavior for transforming an [AstNode] tree into a [InternalSqlBlock] tree.
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
internal abstract class InternalSqlDialect : AstBaseVisitor<InternalSqlBlock, InternalSqlBlock>() {

    /**
     * Default entry-point, can also be us.
     */
    internal fun apply(node: AstNode): InternalSqlBlock {
        val head = InternalSqlBlock.root()
        val tail = head
        node.accept(this, tail)
        return head
    }

    internal companion object {

        @JvmStatic
        val PARTIQL = object : InternalSqlDialect() {}
    }

    override fun defaultReturn(node: AstNode, tail: InternalSqlBlock): InternalSqlBlock =
        throw UnsupportedOperationException("Cannot print $node")

    // STATEMENTS

    override fun visitStatementQuery(node: Statement.Query, tail: InternalSqlBlock): InternalSqlBlock = visitExpr(node.expr, tail)

    // IDENTIFIERS & PATHS

    /**
     * Default behavior is to wrap all SFW queries with parentheses.
     *
     * @param node
     * @param tail
     */
    open fun visitExprWrapped(node: Expr, tail: InternalSqlBlock): InternalSqlBlock = when (node) {
        is Expr.SFW -> {
            var t = tail
            t = t concat "("
            t = visitExprSFW(node, t)
            t = t concat ")"
            t
        }
        else -> visitExpr(node, tail)
    }

    override fun visitIdentifierSymbol(node: Identifier.Symbol, tail: InternalSqlBlock): InternalSqlBlock = tail concat node.sql()

    override fun visitIdentifierQualified(node: Identifier.Qualified, tail: InternalSqlBlock): InternalSqlBlock {
        val path = node.steps.fold(node.root.sql()) { p, step -> p + "." + step.sql() }
        return tail concat path
    }

    override fun visitBinder(node: Binder, tail: InternalSqlBlock): InternalSqlBlock = tail concat node.sql()

    override fun visitPath(node: Path, tail: InternalSqlBlock): InternalSqlBlock {
        val path = node.steps.fold(node.root.sql()) { p, step ->
            when (step) {
                is Path.Step.Index -> p + "[${step.index}]"
                is Path.Step.Symbol -> p + "." + step.symbol.sql()
            }
        }
        return tail concat path
    }

    override fun visitExclude(node: Exclude, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        t = t concat " EXCLUDE "
        t = t concat list(start = null, end = null) { node.items }
        return t
    }

    override fun visitExcludeItem(node: Exclude.Item, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        t = visitExprVar(node.root, t)
        t = t concat list(delimiter = null, start = null, end = null) { node.steps }
        return t
    }

    override fun visitExcludeStepCollIndex(node: Exclude.Step.CollIndex, tail: InternalSqlBlock): InternalSqlBlock {
        return tail concat "[${node.index}]"
    }

    override fun visitExcludeStepStructWildcard(node: Exclude.Step.StructWildcard, tail: InternalSqlBlock): InternalSqlBlock {
        return tail concat ".*"
    }

    override fun visitExcludeStepStructField(node: Exclude.Step.StructField, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail concat "."
        t = visitIdentifierSymbol(node.symbol, t)
        return t
    }

    override fun visitExcludeStepCollWildcard(node: Exclude.Step.CollWildcard, tail: InternalSqlBlock): InternalSqlBlock {
        return tail concat "[*]"
    }

    // cannot write path step outside the context of a path as we don't want it to reflow
    override fun visitPathStep(node: Path.Step, tail: InternalSqlBlock): InternalSqlBlock =
        error("path step cannot be written directly")

    override fun visitPathStepSymbol(node: Path.Step.Symbol, tail: InternalSqlBlock): InternalSqlBlock = visitPathStep(node, tail)

    override fun visitPathStepIndex(node: Path.Step.Index, tail: InternalSqlBlock): InternalSqlBlock = visitPathStep(node, tail)

    // TYPES

    override fun visitTypeNullType(node: Type.NullType, tail: InternalSqlBlock): InternalSqlBlock = tail concat "NULL"

    override fun visitTypeMissing(node: Type.Missing, tail: InternalSqlBlock): InternalSqlBlock = tail concat "MISSING"

    override fun visitTypeBool(node: Type.Bool, tail: InternalSqlBlock): InternalSqlBlock = tail concat "BOOL"

    override fun visitTypeTinyint(node: Type.Tinyint, tail: InternalSqlBlock): InternalSqlBlock = tail concat "TINYINT"

    override fun visitTypeSmallint(node: Type.Smallint, tail: InternalSqlBlock): InternalSqlBlock = tail concat "SMALLINT"

    override fun visitTypeInt2(node: Type.Int2, tail: InternalSqlBlock): InternalSqlBlock = tail concat "INT2"

    override fun visitTypeInt4(node: Type.Int4, tail: InternalSqlBlock): InternalSqlBlock = tail concat "INT4"

    override fun visitTypeBigint(node: Type.Bigint, tail: InternalSqlBlock): InternalSqlBlock = tail concat "BIGINT"

    override fun visitTypeInt8(node: Type.Int8, tail: InternalSqlBlock): InternalSqlBlock = tail concat "INT8"

    override fun visitTypeInt(node: Type.Int, tail: InternalSqlBlock): InternalSqlBlock = tail concat "INT"

    override fun visitTypeReal(node: Type.Real, tail: InternalSqlBlock): InternalSqlBlock = tail concat "REAL"

    override fun visitTypeFloat32(node: Type.Float32, tail: InternalSqlBlock): InternalSqlBlock = tail concat "FLOAT32"

    override fun visitTypeFloat64(node: Type.Float64, tail: InternalSqlBlock): InternalSqlBlock = tail concat "DOUBLE PRECISION"

    override fun visitTypeDecimal(node: Type.Decimal, tail: InternalSqlBlock): InternalSqlBlock =
        tail concat type("DECIMAL", node.precision, node.scale)

    override fun visitTypeNumeric(node: Type.Numeric, tail: InternalSqlBlock): InternalSqlBlock =
        tail concat type("NUMERIC", node.precision, node.scale)

    override fun visitTypeChar(node: Type.Char, tail: InternalSqlBlock): InternalSqlBlock = tail concat type("CHAR", node.length)

    override fun visitTypeVarchar(node: Type.Varchar, tail: InternalSqlBlock): InternalSqlBlock =
        tail concat type("VARCHAR", node.length)

    override fun visitTypeString(node: Type.String, tail: InternalSqlBlock): InternalSqlBlock = tail concat "STRING"

    override fun visitTypeSymbol(node: Type.Symbol, tail: InternalSqlBlock): InternalSqlBlock = tail concat "SYMBOL"

    override fun visitTypeBit(node: Type.Bit, tail: InternalSqlBlock): InternalSqlBlock = tail concat type("BIT", node.length)

    override fun visitTypeBitVarying(node: Type.BitVarying, tail: InternalSqlBlock): InternalSqlBlock =
        tail concat type("BINARY", node.length)

    override fun visitTypeByteString(node: Type.ByteString, tail: InternalSqlBlock): InternalSqlBlock =
        tail concat type("BYTE", node.length)

    override fun visitTypeBlob(node: Type.Blob, tail: InternalSqlBlock): InternalSqlBlock = tail concat type("BLOB", node.length)

    override fun visitTypeClob(node: Type.Clob, tail: InternalSqlBlock): InternalSqlBlock = tail concat type("CLOB", node.length)

    override fun visitTypeBag(node: Type.Bag, tail: InternalSqlBlock): InternalSqlBlock = tail concat "BAG"

    override fun visitTypeList(node: Type.List, tail: InternalSqlBlock): InternalSqlBlock = tail concat "LIST"

    // TODO: Support List Element
    override fun visitTypeArray(node: Type.Array, tail: InternalSqlBlock): InternalSqlBlock = tail concat "ARRAY"
    override fun visitTypeSexp(node: Type.Sexp, tail: InternalSqlBlock): InternalSqlBlock = tail concat "SEXP"

    override fun visitTypeTuple(node: Type.Tuple, tail: InternalSqlBlock): InternalSqlBlock = tail concat "TUPLE"

    // TODO: Support Struct Field
    override fun visitTypeStruct(node: Type.Struct, tail: InternalSqlBlock): InternalSqlBlock = tail concat "STRUCT"

    override fun visitTypeAny(node: Type.Any, tail: InternalSqlBlock): InternalSqlBlock = tail concat "ANY"

    override fun visitTypeDate(node: Type.Date, tail: InternalSqlBlock): InternalSqlBlock = tail concat "DATE"

    override fun visitTypeTime(node: Type.Time, tail: InternalSqlBlock): InternalSqlBlock = tail concat type("TIME", node.precision)

    override fun visitTypeTimeWithTz(node: Type.TimeWithTz, tail: InternalSqlBlock): InternalSqlBlock =
        tail concat type("TIME WITH TIMEZONE", node.precision, gap = true)

    override fun visitTypeTimestamp(node: Type.Timestamp, tail: InternalSqlBlock): InternalSqlBlock =
        tail concat type("TIMESTAMP", node.precision)

    override fun visitTypeTimestampWithTz(node: Type.TimestampWithTz, tail: InternalSqlBlock): InternalSqlBlock =
        tail concat type("TIMESTAMP WITH TIMEZONE", node.precision, gap = true)

    override fun visitTypeInterval(node: Type.Interval, tail: InternalSqlBlock): InternalSqlBlock =
        tail concat type("INTERVAL", node.precision)

    // unsupported
    override fun visitTypeCustom(node: Type.Custom, tail: InternalSqlBlock): InternalSqlBlock = defaultReturn(node, tail)

    // Expressions

    @OptIn(PartiQLValueExperimental::class)
    override fun visitExprLit(node: Expr.Lit, tail: InternalSqlBlock): InternalSqlBlock {
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

    override fun visitExprIon(node: Expr.Ion, tail: InternalSqlBlock): InternalSqlBlock {
        // simplified Ion value writing, as this intentionally omits formatting
        val value = node.value.toString()
        return tail concat "`$value`"
    }

    override fun visitExprUnary(node: Expr.Unary, tail: InternalSqlBlock): InternalSqlBlock {
        val op = when (node.op) {
            Expr.Unary.Op.NOT -> "NOT ("
            Expr.Unary.Op.POS -> "+("
            Expr.Unary.Op.NEG -> "-("
        }
        var t = tail
        t = t concat op
        t = visitExprWrapped(node.expr, t)
        t = t concat ")"
        return t
    }

    override fun visitExprBinary(node: Expr.Binary, tail: InternalSqlBlock): InternalSqlBlock {
        val op = when (node.op) {
            Expr.Binary.Op.PLUS -> "+"
            Expr.Binary.Op.MINUS -> "-"
            Expr.Binary.Op.TIMES -> "*"
            Expr.Binary.Op.DIVIDE -> "/"
            Expr.Binary.Op.MODULO -> "%"
            Expr.Binary.Op.CONCAT -> "||"
            Expr.Binary.Op.AND -> "AND"
            Expr.Binary.Op.OR -> "OR"
            Expr.Binary.Op.EQ -> "="
            Expr.Binary.Op.NE -> "<>"
            Expr.Binary.Op.GT -> ">"
            Expr.Binary.Op.GTE -> ">="
            Expr.Binary.Op.LT -> "<"
            Expr.Binary.Op.LTE -> "<="
            Expr.Binary.Op.BITWISE_AND -> "&"
        }
        var t = tail
        t = visitExprWrapped(node.lhs, t)
        t = t concat " $op "
        t = visitExprWrapped(node.rhs, t)
        return t
    }

    override fun visitExprVar(node: Expr.Var, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        // Prepend @
        if (node.scope == Expr.Var.Scope.LOCAL) {
            t = t concat "@"
        }
        t = visitIdentifier(node.identifier, t)
        return t
    }

    override fun visitExprSessionAttribute(node: Expr.SessionAttribute, tail: InternalSqlBlock): InternalSqlBlock =
        tail concat node.attribute.name

    override fun visitExprPath(node: Expr.Path, tail: InternalSqlBlock): InternalSqlBlock {
        var t = visitExprWrapped(node.root, tail)
        t = node.steps.fold(t) { b, step -> visitExprPathStep(step, b) }
        return t
    }

    override fun visitExprPathStepSymbol(node: Expr.Path.Step.Symbol, tail: InternalSqlBlock): InternalSqlBlock =
        tail concat ".${node.symbol.sql()}"

    override fun visitExprPathStepIndex(node: Expr.Path.Step.Index, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        val key = node.key
        // use [ ] syntax
        t = t concat "["
        t = visitExprWrapped(key, t)
        t = t concat "]"
        return t
    }

    override fun visitExprPathStepWildcard(node: Expr.Path.Step.Wildcard, tail: InternalSqlBlock): InternalSqlBlock = tail concat "[*]"

    override fun visitExprPathStepUnpivot(node: Expr.Path.Step.Unpivot, tail: InternalSqlBlock): InternalSqlBlock = tail concat ".*"

    override fun visitExprCall(node: Expr.Call, tail: InternalSqlBlock): InternalSqlBlock {
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

    override fun visitExprParameter(node: Expr.Parameter, tail: InternalSqlBlock): InternalSqlBlock = tail concat "?"

    override fun visitExprValues(node: Expr.Values, tail: InternalSqlBlock): InternalSqlBlock =
        tail concat list("VALUES (") { node.rows }

    override fun visitExprValuesRow(node: Expr.Values.Row, tail: InternalSqlBlock): InternalSqlBlock = tail concat list { node.items }

    override fun visitExprCollection(node: Expr.Collection, tail: InternalSqlBlock): InternalSqlBlock {
        val (start, end) = when (node.type) {
            Expr.Collection.Type.BAG -> "<<" to ">>"
            Expr.Collection.Type.ARRAY -> "[" to "]"
            Expr.Collection.Type.VALUES -> "VALUES (" to ")"
            Expr.Collection.Type.LIST -> "(" to ")"
            Expr.Collection.Type.SEXP -> "SEXP (" to ")"
        }
        return tail concat list(start, end) { node.values }
    }

    override fun visitExprStruct(node: Expr.Struct, tail: InternalSqlBlock): InternalSqlBlock =
        tail concat list("{", "}") { node.fields }

    override fun visitExprStructField(node: Expr.Struct.Field, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        t = visitExprWrapped(node.name, t)
        t = t concat ": "
        t = visitExprWrapped(node.value, t)
        return t
    }

    override fun visitExprLike(node: Expr.Like, tail: InternalSqlBlock): InternalSqlBlock {
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

    override fun visitExprBetween(node: Expr.Between, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        t = visitExprWrapped(node.value, t)
        t = t concat if (node.not == true) " NOT BETWEEN " else " BETWEEN "
        t = visitExprWrapped(node.from, t)
        t = t concat " AND "
        t = visitExprWrapped(node.to, t)
        return t
    }

    override fun visitExprInCollection(node: Expr.InCollection, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        t = visitExprWrapped(node.lhs, t)
        t = t concat if (node.not == true) " NOT IN " else " IN "
        t = visitExprWrapped(node.rhs, t)
        return t
    }

    override fun visitExprIsType(node: Expr.IsType, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        t = visitExprWrapped(node.value, t)
        t = t concat if (node.not == true) " IS NOT " else " IS "
        t = visitType(node.type, t)
        return t
    }

    override fun visitExprCase(node: Expr.Case, tail: InternalSqlBlock): InternalSqlBlock {
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

    override fun visitExprCaseBranch(node: Expr.Case.Branch, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        t = t concat " WHEN "
        t = visitExprWrapped(node.condition, t)
        t = t concat " THEN "
        t = visitExprWrapped(node.expr, t)
        return t
    }

    override fun visitExprCoalesce(node: Expr.Coalesce, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        t = t concat "COALESCE"
        t = t concat list { node.args }
        return t
    }

    override fun visitExprNullIf(node: Expr.NullIf, tail: InternalSqlBlock): InternalSqlBlock {
        val args = listOf(node.value, node.nullifier)
        var t = tail
        t = t concat "NULLIF"
        t = t concat list { args }
        return t
    }

    override fun visitExprSubstring(node: Expr.Substring, tail: InternalSqlBlock): InternalSqlBlock {
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

    override fun visitExprPosition(node: Expr.Position, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        t = t concat "POSITION("
        t = visitExprWrapped(node.lhs, t)
        t = t concat " IN "
        t = visitExprWrapped(node.rhs, t)
        t = t concat ")"
        return t
    }

    override fun visitExprTrim(node: Expr.Trim, tail: InternalSqlBlock): InternalSqlBlock {
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

    override fun visitExprOverlay(node: Expr.Overlay, tail: InternalSqlBlock): InternalSqlBlock {
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

    override fun visitExprExtract(node: Expr.Extract, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        t = t concat "EXTRACT("
        t = t concat node.field.name
        t = t concat " FROM "
        t = visitExprWrapped(node.source, t)
        t = t concat ")"
        return t
    }

    override fun visitExprCast(node: Expr.Cast, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        t = t concat "CAST("
        t = visitExprWrapped(node.value, t)
        t = t concat " AS "
        t = visitType(node.asType, t)
        t = t concat ")"
        return t
    }

    override fun visitExprCanCast(node: Expr.CanCast, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        t = t concat "CAN_CAST("
        t = visitExprWrapped(node.value, t)
        t = t concat " AS "
        t = visitType(node.asType, t)
        t = t concat ")"
        return t
    }

    override fun visitExprCanLosslessCast(node: Expr.CanLosslessCast, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        t = t concat "CAN_LOSSLESS_CAST("
        t = visitExprWrapped(node.value, t)
        t = t concat " AS "
        t = visitType(node.asType, t)
        t = t concat ")"
        return t
    }

    override fun visitExprDateAdd(node: Expr.DateAdd, tail: InternalSqlBlock): InternalSqlBlock {
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

    override fun visitExprDateDiff(node: Expr.DateDiff, tail: InternalSqlBlock): InternalSqlBlock {
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

    override fun visitExprBagOp(node: Expr.BagOp, tail: InternalSqlBlock): InternalSqlBlock {
        // [OUTER] [UNION|INTERSECT|EXCEPT] [ALL|DISTINCT]
        val op = mutableListOf<String>()
        when (node.outer) {
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

    // SELECT-FROM-WHERE

    override fun visitExprSFW(node: Expr.SFW, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        // SELECT
        t = visit(node.select, t)
        // EXCLUDE
        t = node.exclude?.let { visit(it, t) } ?: t
        // FROM
        t = visit(node.from, t concat " FROM ")
        // LET
        t = if (node.let != null) visitLet(node.let!!, t concat " ") else t
        // WHERE
        t = if (node.where != null) visitExprWrapped(node.where!!, t concat " WHERE ") else t
        // GROUP BY
        t = if (node.groupBy != null) visitGroupBy(node.groupBy!!, t concat " ") else t
        // HAVING
        t = if (node.having != null) visitExprWrapped(node.having!!, t concat " HAVING ") else t
        // SET OP
        t = if (node.setOp != null) visitExprSFWSetOp(node.setOp!!, t concat " ") else t
        // ORDER BY
        t = if (node.orderBy != null) visitOrderBy(node.orderBy!!, t concat " ") else t
        // LIMIT
        t = if (node.limit != null) visitExprWrapped(node.limit!!, t concat " LIMIT ") else t
        // OFFSET
        t = if (node.offset != null) visitExprWrapped(node.offset!!, t concat " OFFSET ") else t
        return t
    }

    // SELECT

    override fun visitSelectStar(node: Select.Star, tail: InternalSqlBlock): InternalSqlBlock {
        val select = when (node.setq) {
            SetQuantifier.ALL -> "SELECT ALL *"
            SetQuantifier.DISTINCT -> "SELECT DISTINCT *"
            null -> "SELECT *"
        }
        return tail concat select
    }

    override fun visitSelectProject(node: Select.Project, tail: InternalSqlBlock): InternalSqlBlock {
        val select = when (node.setq) {
            SetQuantifier.ALL -> "SELECT ALL "
            SetQuantifier.DISTINCT -> "SELECT DISTINCT "
            null -> "SELECT "
        }
        return tail concat list(select, "") { node.items }
    }

    override fun visitSelectProjectItemAll(node: Select.Project.Item.All, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        t = t concat ".*"
        return t
    }

    override fun visitSelectProjectItemExpression(node: Select.Project.Item.Expression, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        t = if (node.asAlias != null) t concat " AS ${node.asAlias!!.sql()}" else t
        return t
    }

    override fun visitSelectPivot(node: Select.Pivot, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        t = t concat "PIVOT "
        t = visitExprWrapped(node.key, t)
        t = t concat " AT "
        t = visitExprWrapped(node.value, t)
        return t
    }

    override fun visitSelectValue(node: Select.Value, tail: InternalSqlBlock): InternalSqlBlock {
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

    override fun visitFromValue(node: From.Value, tail: InternalSqlBlock): InternalSqlBlock {
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

    override fun visitFromJoin(node: From.Join, tail: InternalSqlBlock): InternalSqlBlock {
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

    override fun visitLet(node: Let, tail: InternalSqlBlock): InternalSqlBlock = tail concat list("LET ", "") { node.bindings }

    override fun visitLetBinding(node: Let.Binding, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        t = t concat " AS ${node.asAlias.sql()}"
        return t
    }

    // GROUP BY

    override fun visitGroupBy(node: GroupBy, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        t = t concat when (node.strategy) {
            GroupBy.Strategy.FULL -> "GROUP BY "
            GroupBy.Strategy.PARTIAL -> "GROUP PARTIAL BY "
        }
        t = t concat list("", "") { node.keys }
        t = if (node.asAlias != null) t concat " GROUP AS ${node.asAlias!!.sql()}" else t
        return t
    }

    override fun visitGroupByKey(node: GroupBy.Key, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        t = if (node.asAlias != null) t concat " AS ${node.asAlias!!.sql()}" else t
        return t
    }

    // SET OPERATORS

    override fun visitSetOp(node: SetOp, tail: InternalSqlBlock): InternalSqlBlock {
        val op = when (node.setq) {
            null -> node.type.name
            else -> "${node.type.name} ${node.setq!!.name}"
        }
        return tail concat op
    }

    override fun visitExprSFWSetOp(node: Expr.SFW.SetOp, tail: InternalSqlBlock): InternalSqlBlock {
        var t = tail
        t = visitSetOp(node.type, t)
        t = t concat InternalSqlBlock.Nest(
            prefix = " (",
            postfix = ")",
            child = InternalSqlBlock.root().apply { visitExprSFW(node.operand, this) },
        )
        return t
    }

    // ORDER BY

    override fun visitOrderBy(node: OrderBy, tail: InternalSqlBlock): InternalSqlBlock =
        tail concat list("ORDER BY ", "") { node.sorts }

    override fun visitSort(node: Sort, tail: InternalSqlBlock): InternalSqlBlock {
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

    private infix fun InternalSqlBlock.concat(rhs: String): InternalSqlBlock {
        next = InternalSqlBlock.Text(rhs)
        return next!!
    }

    private infix fun InternalSqlBlock.concat(rhs: InternalSqlBlock): InternalSqlBlock {
        next = rhs
        return next!!
    }

    private fun type(symbol: String, vararg args: Int?, gap: Boolean = false): InternalSqlBlock {
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
        return InternalSqlBlock.Text(t)
    }

    private fun list(
        start: String? = "(",
        end: String? = ")",
        delimiter: String? = ", ",
        children: () -> List<AstNode>,
    ): InternalSqlBlock {
        val kids = children()
        val h = InternalSqlBlock.root()
        var t = h
        kids.forEachIndexed { i, child ->
            t = child.accept(this, t)
            t = if (delimiter != null && (i + 1) < kids.size) t concat delimiter else t
        }
        return InternalSqlBlock.Nest(
            prefix = start,
            postfix = end,
            child = h,
        )
    }

    private fun Identifier.Symbol.sql() = when (caseSensitivity) {
        Identifier.CaseSensitivity.SENSITIVE -> "\"$symbol\""
        Identifier.CaseSensitivity.INSENSITIVE -> symbol // verbatim ..
    }

    private fun Binder.sql() = when (caseSensitivity) {
        Binder.CaseSensitivity.SENSITIVE -> "\"$symbol\""
        Binder.CaseSensitivity.INSENSITIVE -> symbol
    }
}
