package org.partiql.transpiler.dialects

import org.partiql.ast.AstNode
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
import org.partiql.transpiler.block.Block
import org.partiql.transpiler.block.concat
import org.partiql.transpiler.Dialect
import org.partiql.value.MissingValue
import org.partiql.value.NullValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.io.PartiQLValueTextWriter
import java.io.ByteArrayOutputStream
import java.io.PrintStream

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
abstract class PartiQLDialect : Dialect() {

    companion object {

        @JvmStatic
        val INSTANCE = object : PartiQLDialect() {}
    }

    override fun defaultReturn(node: AstNode, head: Block) = throw UnsupportedOperationException("Cannot print $node")

    // STATEMENTS

    override fun visitStatementQuery(node: Statement.Query, head: Block) = visitExpr(node.expr, head)

    // IDENTIFIERS & PATHS

    override fun visitIdentifierSymbol(node: Identifier.Symbol, head: Block) = head concat r(node.sql())

    override fun visitIdentifierQualified(node: Identifier.Qualified, head: Block): Block {
        val path = node.steps.fold(node.root.sql()) { p, step -> p + "." + step.sql() }
        return head concat r(path)
    }

    override fun visitPath(node: Path, head: Block): Block {
        val path = node.steps.fold(node.root.sql()) { p, step ->
            when (step) {
                is Path.Step.Index -> p + "[${step.index}]"
                is Path.Step.Symbol -> p + "." + step.symbol.sql()
            }
        }
        return head concat r(path)
    }

    // cannot write path step outside the context of a path as we don't want it to reflow
    override fun visitPathStep(node: Path.Step, head: Block) = error("path step cannot be written directly")

    override fun visitPathStepSymbol(node: Path.Step.Symbol, head: Block) = visitPathStep(node, head)

    override fun visitPathStepIndex(node: Path.Step.Index, head: Block) = visitPathStep(node, head)

    // TYPES

    override fun visitTypeNullType(node: Type.NullType, head: Block) = head concat r("NULL")

    override fun visitTypeMissing(node: Type.Missing, head: Block) = head concat r("MISSING")

    override fun visitTypeBool(node: Type.Bool, head: Block) = head concat r("BOOL")

    override fun visitTypeTinyint(node: Type.Tinyint, head: Block) = head concat r("TINYINT")

    override fun visitTypeSmallint(node: Type.Smallint, head: Block) = head concat r("SMALLINT")

    override fun visitTypeInt2(node: Type.Int2, head: Block) = head concat r("INT2")

    override fun visitTypeInt4(node: Type.Int4, head: Block) = head concat r("INT4")

    override fun visitTypeBigint(node: Type.Bigint, head: Block) = head concat r("BIGINT")

    override fun visitTypeInt8(node: Type.Int8, head: Block) = head concat r("INT8")

    override fun visitTypeInt(node: Type.Int, head: Block) = head concat r("INT")

    override fun visitTypeReal(node: Type.Real, head: Block) = head concat r("REAL")

    override fun visitTypeFloat32(node: Type.Float32, head: Block) = head concat r("FLOAT32")

    override fun visitTypeFloat64(node: Type.Float64, head: Block) = head concat r("DOUBLE PRECISION")

    override fun visitTypeDecimal(node: Type.Decimal, head: Block) =
        head concat type("DECIMAL", node.precision, node.scale)

    override fun visitTypeNumeric(node: Type.Numeric, head: Block) =
        head concat type("NUMERIC", node.precision, node.scale)

    override fun visitTypeChar(node: Type.Char, head: Block) = head concat type("CHAR", node.length)

    override fun visitTypeVarchar(node: Type.Varchar, head: Block) = head concat type("VARCHAR", node.length)

    override fun visitTypeString(node: Type.String, head: Block) = head concat r("STRING")

    override fun visitTypeSymbol(node: Type.Symbol, head: Block) = head concat r("SYMBOL")

    override fun visitTypeBit(node: Type.Bit, head: Block) = head concat type("BIT", node.length)

    override fun visitTypeBitVarying(node: Type.BitVarying, head: Block) = head concat type("BINARY", node.length)

    override fun visitTypeByteString(node: Type.ByteString, head: Block) = head concat type("BYTE", node.length)

    override fun visitTypeBlob(node: Type.Blob, head: Block) = head concat type("BLOB", node.length)

    override fun visitTypeClob(node: Type.Clob, head: Block) = head concat type("CLOB", node.length)

    override fun visitTypeBag(node: Type.Bag, head: Block) = head concat r("BAG")

    override fun visitTypeList(node: Type.List, head: Block) = head concat r("LIST")

    override fun visitTypeSexp(node: Type.Sexp, head: Block) = head concat r("SEXP")

    override fun visitTypeTuple(node: Type.Tuple, head: Block) = head concat r("TUPLE")

    override fun visitTypeStruct(node: Type.Struct, head: Block) = head concat r("STRUCT")

    override fun visitTypeAny(node: Type.Any, head: Block) = head concat r("ANY")

    override fun visitTypeDate(node: Type.Date, head: Block) = head concat r("DATE")

    override fun visitTypeTime(node: Type.Time, head: Block): Block = head concat type("TIME", node.precision)

    override fun visitTypeTimeWithTz(node: Type.TimeWithTz, head: Block) =
        head concat type("TIME WITH TIMEZONE", node.precision, gap = true)

    override fun visitTypeTimestamp(node: Type.Timestamp, head: Block) = head concat type("TIMESTAMP", node.precision)

    override fun visitTypeTimestampWithTz(node: Type.TimestampWithTz, head: Block) =
        head concat type("TIMESTAMP WITH TIMEZONE", node.precision, gap = true)

    override fun visitTypeInterval(node: Type.Interval, head: Block) = head concat type("INTERVAL", node.precision)

    // unsupported
    override fun visitTypeCustom(node: Type.Custom, head: Block) = defaultReturn(node, head)

    // Expressions

    @OptIn(PartiQLValueExperimental::class)
    override fun visitExprLit(node: Expr.Lit, head: Block): Block {
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
        return head concat r(value)
    }

    override fun visitExprIon(node: Expr.Ion, head: Block): Block {
        // simplified Ion value writing, as this intentionally omits formatting
        val value = node.value.toString()
        return head concat r("`$value`")
    }

    override fun visitExprUnary(node: Expr.Unary, head: Block): Block {
        val op = when (node.op) {
            Expr.Unary.Op.NOT -> "NOT "
            Expr.Unary.Op.POS -> "+"
            Expr.Unary.Op.NEG -> "-"
        }
        var h = head
        h = h concat r(op)
        return visitExpr(node.expr, h)
    }

    override fun visitExprBinary(node: Expr.Binary, head: Block): Block {
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
        }
        var h = head
        h = visitExpr(node.lhs, h)
        h = h concat r(" $op ")
        h = visitExpr(node.rhs, h)
        return h
    }

    override fun visitExprVar(node: Expr.Var, head: Block): Block {
        var h = head
        // Prepend @
        if (node.scope == Expr.Var.Scope.LOCAL) {
            h = h concat r("@")
        }
        h = visitIdentifier(node.identifier, h)
        return h
    }

    override fun visitExprSessionAttribute(node: Expr.SessionAttribute, head: Block) =
        head concat r(node.attribute.name)

    override fun visitExprPath(node: Expr.Path, head: Block): Block {
        var h = visitExpr(node.root, head)
        h = node.steps.fold(h) { b, step -> visitExprPathStep(step, b) }
        return h
    }

    override fun visitExprPathStepSymbol(node: Expr.Path.Step.Symbol, head: Block) =
        head concat r(".${node.symbol.sql()}")

    override fun visitExprPathStepIndex(node: Expr.Path.Step.Index, head: Block): Block {
        var h = head
        h = head concat r("[")
        h = visitExpr(node.key, h)
        h = h concat r("]")
        return h
    }

    override fun visitExprPathStepWildcard(node: Expr.Path.Step.Wildcard, head: Block) = head concat r("[*]")

    override fun visitExprPathStepUnpivot(node: Expr.Path.Step.Unpivot, head: Block) = head concat r(".*")

    override fun visitExprCall(node: Expr.Call, head: Block): Block {
        var h = head
        h = h concat visitIdentifier(node.function, h)
        h = h concat list { node.args }
        return h
    }

    override fun visitExprAgg(node: Expr.Agg, head: Block): Block {
        var h = head
        val f = node.function
        // Special case
        if (f is Identifier.Symbol && f.symbol == "COUNT_STAR") {
            return h concat r("COUNT(*)")
        }
        val start = if (node.setq != null) "(${node.setq} " else "("
        h = h concat visitIdentifier(f, h)
        h = h concat list(start) { node.args }
        return h
    }

    override fun visitExprParameter(node: Expr.Parameter, head: Block) = head concat r("?")

    override fun visitExprValues(node: Expr.Values, head: Block) = head concat list("VALUES (") { node.rows }

    override fun visitExprValuesRow(node: Expr.Values.Row, head: Block) = head concat list { node.items }

    override fun visitExprCollection(node: Expr.Collection, head: Block): Block {
        val (start, end) = when (node.type) {
            Expr.Collection.Type.BAG -> "<<" to ">>"
            Expr.Collection.Type.ARRAY -> "[" to "]"
            Expr.Collection.Type.VALUES -> "VALUES (" to ")"
            Expr.Collection.Type.LIST -> "(" to ")"
            Expr.Collection.Type.SEXP -> "SEXP (" to ")"
        }
        return head concat list(start, end) { node.values }
    }

    override fun visitExprStruct(node: Expr.Struct, head: Block) = head concat list("{", "}") { node.fields }

    override fun visitExprStructField(node: Expr.Struct.Field, head: Block): Block {
        var h = head
        h = visitExpr(node.name, h)
        h = h concat r(": ")
        h = visitExpr(node.value, h)
        return h
    }

    override fun visitExprLike(node: Expr.Like, head: Block): Block {
        var h = head
        h = visitExpr(node.value, h)
        h = h concat if (node.not == true) r(" NOT LIKE ") else r(" LIKE ")
        h = visitExpr(node.pattern, h)
        if (node.escape != null) {
            h = h concat r(" ESCAPE ")
            h = visitExpr(node.escape!!, h)
        }
        return h
    }

    override fun visitExprBetween(node: Expr.Between, head: Block): Block {
        var h = head
        h = visitExpr(node.value, h)
        h = h concat if (node.not == true) r(" NOT BETWEEN ") else r(" BETWEEN ")
        h = visitExpr(node.from, h)
        h = h concat r(" AND ")
        h = visitExpr(node.to, h)
        return h
    }

    override fun visitExprInCollection(node: Expr.InCollection, head: Block): Block {
        var h = head
        h = visitExpr(node.lhs, h)
        h = h concat if (node.not == true) r(" NOT IN ") else r(" IN ")
        h = visitExpr(node.rhs, h)
        return h
    }

    override fun visitExprIsType(node: Expr.IsType, head: Block): Block {
        var h = head
        h = visitExpr(node.value, h)
        h = h concat if (node.not == true) r(" IS NOT ") else r(" IS ")
        h = visitType(node.type, h)
        return h
    }

    override fun visitExprCase(node: Expr.Case, head: Block): Block {
        var h = head
        h = h concat r("CASE")
        h = when (node.expr) {
            null -> h
            else -> visitExpr(node.expr!!, h concat r(" "))
        }
        // WHEN(s)
        h = node.branches.fold(h) { acc, branch -> visitExprCaseBranch(branch, acc) }
        // ELSE
        h = when (node.default) {
            null -> h
            else -> {
                h = h concat r(" ELSE ")
                visitExpr(node.default!!, h)
            }
        }
        h = h concat r(" END")
        return h
    }

    override fun visitExprCaseBranch(node: Expr.Case.Branch, head: Block): Block {
        var h = head
        h = h concat r(" WHEN ")
        h = visitExpr(node.condition, h)
        h = h concat r(" THEN ")
        h = visitExpr(node.expr, h)
        return h
    }

    override fun visitExprCoalesce(node: Expr.Coalesce, head: Block): Block {
        var h = head
        h = h concat r("COALESCE")
        h = h concat list { node.args }
        return h
    }

    override fun visitExprNullIf(node: Expr.NullIf, head: Block): Block {
        val args = listOf(node.value, node.nullifier)
        var h = head
        h = h concat r("NULLIF")
        h = h concat list { args }
        return h
    }

    override fun visitExprSubstring(node: Expr.Substring, head: Block): Block {
        var h = head
        h = h concat r("SUBSTRING(")
        h = visitExpr(node.value, h)
        if (node.start != null) {
            h = h concat r(" FROM ")
            h = visitExpr(node.start!!, h)
        }
        if (node.length != null) {
            h = h concat r(" FOR ")
            h = visitExpr(node.length!!, h)
        }
        h = h concat r(")")
        return h
    }

    override fun visitExprPosition(node: Expr.Position, head: Block): Block {
        var h = head
        h = h concat r("POSITION(")
        h = visitExpr(node.lhs, h)
        h = h concat r(" IN ")
        h = visitExpr(node.rhs, h)
        h = h concat r(")")
        return h
    }

    override fun visitExprTrim(node: Expr.Trim, head: Block): Block {
        var h = head
        h = h concat r("TRIM(")
        // [LEADING|TRAILING|BOTH]
        if (node.spec != null) {
            h = h concat r("${node.spec} ")
        }
        // [<chars> FROM]
        if (node.chars != null) {
            h = visitExpr(node.chars!!, h)
            h = h concat r(" FROM ")
        }
        h = visitExpr(node.value, h)
        h = h concat r(")")
        return h
    }

    override fun visitExprOverlay(node: Expr.Overlay, head: Block): Block {
        var h = head
        h = h concat r("OVERLAY(")
        h = visitExpr(node.value, h)
        h = h concat r(" PLACING ")
        h = visitExpr(node.overlay, h)
        h = h concat r(" FROM ")
        h = visitExpr(node.start, h)
        if (node.length != null) {
            h = h concat r(" FOR ")
            h = visitExpr(node.length!!, h)
        }
        h = h concat r(")")
        return h
    }

    override fun visitExprExtract(node: Expr.Extract, head: Block): Block {
        var h = head
        h = h concat r("EXTRACT(")
        h = h concat r(node.field.name)
        h = h concat r(" FROM ")
        h = visitExpr(node.source, h)
        h = h concat r(")")
        return h
    }

    override fun visitExprCast(node: Expr.Cast, head: Block): Block {
        var h = head
        h = h concat r("CAST(")
        h = visitExpr(node.value, h)
        h = h concat r(" AS ")
        h = visitType(node.asType, h)
        h = h concat r(")")
        return h
    }

    override fun visitExprCanCast(node: Expr.CanCast, head: Block): Block {
        var h = head
        h = h concat r("CAN_CAST(")
        h = visitExpr(node.value, h)
        h = h concat r(" AS ")
        h = visitType(node.asType, h)
        h = h concat r(")")
        return h
    }

    override fun visitExprCanLosslessCast(node: Expr.CanLosslessCast, head: Block): Block {
        var h = head
        h = h concat r("CAN_LOSSLESS_CAST(")
        h = visitExpr(node.value, h)
        h = h concat r(" AS ")
        h = visitType(node.asType, h)
        h = h concat r(")")
        return h
    }

    override fun visitExprDateAdd(node: Expr.DateAdd, head: Block): Block {
        var h = head
        h = h concat r("DATE_ADD(")
        h = h concat r(node.field.name)
        h = h concat r(", ")
        h = visitExpr(node.lhs, h)
        h = h concat r(", ")
        h = visitExpr(node.rhs, h)
        h = h concat r(")")
        return h
    }

    override fun visitExprDateDiff(node: Expr.DateDiff, head: Block): Block {
        var h = head
        h = h concat r("DATE_DIFF(")
        h = h concat r(node.field.name)
        h = h concat r(", ")
        h = visitExpr(node.lhs, h)
        h = h concat r(", ")
        h = visitExpr(node.rhs, h)
        h = h concat r(")")
        return h
    }

    override fun visitExprBagOp(node: Expr.BagOp, head: Block): Block {
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
        var h = head
        h = visitExpr(node.lhs, h)
        h = h concat r(" ${op.joinToString(" ")} ")
        h = visitExpr(node.rhs, h)
        return h
    }

    // SELECT-FROM-WHERE

    override fun visitExprSFW(node: Expr.SFW, head: Block): Block {
        var h = head
        // SELECT
        h = visit(node.select, h)
        // FROM
        h = visit(node.from, h concat r(" FROM "))
        // LET
        h = if (node.let != null) visitLet(node.let!!, h concat r(" ")) else h
        // WHERE
        h = if (node.where != null) visitExpr(node.where!!, h concat r(" WHERE ")) else h
        // GROUP BY
        h = if (node.groupBy != null) visitGroupBy(node.groupBy!!, h concat r(" ")) else h
        // HAVING
        h = if (node.having != null) visitExpr(node.having!!, h concat r(" HAVING ")) else h
        // SET OP
        h = if (node.setOp != null) visitExprSFWSetOp(node.setOp!!, h concat r(" ")) else h
        // ORDER BY
        h = if (node.orderBy != null) visitOrderBy(node.orderBy!!, h concat r(" ")) else h
        // LIMIT
        h = if (node.limit != null) visitExpr(node.limit!!, h concat r(" LIMIT ")) else h
        // OFFSET
        h = if (node.offset != null) visitExpr(node.offset!!, h concat r(" OFFSET ")) else h
        return h
    }

    // SELECT

    override fun visitSelectStar(node: Select.Star, head: Block): Block {
        val select = when (node.setq) {
            SetQuantifier.ALL -> "SELECT ALL *"
            SetQuantifier.DISTINCT -> "SELECT DISTINCT *"
            null -> "SELECT *"
        }
        return head concat r(select)
    }

    override fun visitSelectProject(node: Select.Project, head: Block): Block {
        val select = when (node.setq) {
            SetQuantifier.ALL -> "SELECT ALL "
            SetQuantifier.DISTINCT -> "SELECT DISTINCT "
            null -> "SELECT "
        }
        return head concat list(select, "") { node.items }
    }

    override fun visitSelectProjectItemAll(node: Select.Project.Item.All, head: Block): Block {
        var h = head
        h = visitExpr(node.expr, h)
        h = h concat r(".*")
        return h
    }

    override fun visitSelectProjectItemExpression(node: Select.Project.Item.Expression, head: Block): Block {
        var h = head
        h = visitExpr(node.expr, h)
        h = if (node.asAlias != null) h concat r(" AS ${node.asAlias}") else h
        return h
    }

    override fun visitSelectPivot(node: Select.Pivot, head: Block): Block {
        var h = head
        h = h concat r("PIVOT ")
        h = visitExpr(node.key, h)
        h = h concat r(" AT ")
        h = visitExpr(node.value, h)
        return h
    }

    override fun visitSelectValue(node: Select.Value, head: Block): Block {
        val select = when (node.setq) {
            SetQuantifier.ALL -> "SELECT ALL VALUE "
            SetQuantifier.DISTINCT -> "SELECT DISTINCT VALUE "
            null -> "SELECT VALUE "
        }
        var h = head
        h = h concat r(select)
        h = visitExpr(node.constructor, h)
        return h
    }

    // FROM

    override fun visitFromValue(node: From.Value, head: Block): Block {
        var h = head
        h = when (node.type) {
            From.Value.Type.SCAN -> h
            From.Value.Type.UNPIVOT -> h concat r("UNPIVOT ")
        }
        h = visitExpr(node.expr, h)
        h = if (node.asAlias != null) h concat r(" AS ${node.asAlias}") else h
        h = if (node.atAlias != null) h concat r(" AT ${node.atAlias}") else h
        h = if (node.byAlias != null) h concat r(" BY ${node.byAlias}") else h
        return h
    }

    override fun visitFromJoin(node: From.Join, head: Block): Block {
        var h = head
        h = visitFrom(node.lhs, h)
        h = h concat when (node.type) {
            From.Join.Type.INNER -> r(" INNER JOIN ")
            From.Join.Type.LEFT -> r(" LEFT JOIN ")
            From.Join.Type.LEFT_OUTER -> r(" LEFT OUTER JOIN ")
            From.Join.Type.RIGHT -> r(" RIGHT JOIN ")
            From.Join.Type.RIGHT_OUTER -> r(" RIGHT OUTER JOIN ")
            From.Join.Type.FULL -> r(" FULL JOIN ")
            From.Join.Type.FULL_OUTER -> r(" FULL OUTER JOIN ")
            From.Join.Type.CROSS -> r(" CROSS JOIN ")
            From.Join.Type.COMMA -> r(", ")
            null -> r(" JOIN ")
        }
        h = visitFrom(node.rhs, h)
        h = if (node.condition != null) visit(node.condition!!, h concat r(" ON ")) else h
        return h
    }

    // LET

    override fun visitLet(node: Let, head: Block) = head concat list("LET ", "") { node.bindings }

    override fun visitLetBinding(node: Let.Binding, head: Block): Block {
        var h = head
        h = visitExpr(node.expr, h)
        h = h concat r(" AS ${node.asAlias}")
        return h
    }

    // GROUP BY

    override fun visitGroupBy(node: GroupBy, head: Block): Block {
        var h = head
        h = h concat when (node.strategy) {
            GroupBy.Strategy.FULL -> r("GROUP BY ")
            GroupBy.Strategy.PARTIAL -> r("GROUP PARTIAL BY ")
        }
        h = h concat list("", "") { node.keys }
        h = if (node.asAlias != null) h concat r(" GROUP AS ${node.asAlias}") else h
        return h
    }

    override fun visitGroupByKey(node: GroupBy.Key, head: Block): Block {
        var h = head
        h = visitExpr(node.expr, h)
        h = if (node.asAlias != null) h concat r(" AS ${node.asAlias}") else h
        return h
    }

    // SET OPERATORS

    override fun visitSetOp(node: SetOp, head: Block): Block {
        val op = when (node.setq) {
            null -> node.type.name
            else -> "${node.type.name} ${node.setq!!.name}"
        }
        return head concat r(op)
    }

    override fun visitExprSFWSetOp(node: Expr.SFW.SetOp, head: Block): Block {
        var h = head
        h = visitSetOp(node.type, h)
        h = h concat r(" ")
        h = h concat r("(")
        val subquery = visitExprSFW(node.operand, Block.Nil)
        h = h concat Block.Nest(subquery)
        h = h concat r(")")
        return h
    }

    // ORDER BY

    override fun visitOrderBy(node: OrderBy, head: Block) = head concat list("ORDER BY ", "") { node.sorts }

    override fun visitSort(node: Sort, head: Block): Block {
        var h = head
        h = visitExpr(node.expr, h)
        h = when (node.dir) {
            Sort.Dir.ASC -> h concat r(" ASC")
            Sort.Dir.DESC -> h concat r(" DESC")
            null -> h
        }
        h = when (node.nulls) {
            Sort.Nulls.FIRST -> h concat r(" NULLS FIRST")
            Sort.Nulls.LAST -> h concat r(" NULLS LAST")
            null -> h
        }
        return h
    }

    // --- Block Constructor Helpers

    private fun type(symbol: String, vararg args: Int?, gap: Boolean = false): Block {
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
        return r(t)
    }

    //  > infix fun Block.concat(rhs: String): Block.Link = Block.Link(this, Block.Raw(rhs))
    //  > head concat "foo"
    private fun r(text: String): Block = Block.Text(text)

    private fun list(
        start: String? = "(",
        end: String? = ")",
        delimiter: String? = ", ",
        children: () -> List<AstNode>,
    ): Block {
        val kids = children()
        var h = start?.let { r(it) } ?: Block.Nil
        kids.forEachIndexed { i, child ->
            h = child.accept(this, h)
            h = if (delimiter != null && (i + 1) < kids.size) h concat r(delimiter) else h
        }
        h = if (end != null) h concat r(end) else h
        return h
    }

    private fun Identifier.Symbol.sql() = when (caseSensitivity) {
        Identifier.CaseSensitivity.SENSITIVE -> "\"$symbol\""
        Identifier.CaseSensitivity.INSENSITIVE -> symbol // verbatim ..
    }
}