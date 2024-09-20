@file:OptIn(PartiQLValueExperimental::class)

package org.partiql.ast.helpers

import com.amazon.ion.Decimal
import com.amazon.ionelement.api.DecimalElement
import com.amazon.ionelement.api.FloatElement
import com.amazon.ionelement.api.IntElement
import com.amazon.ionelement.api.IntElementSize
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.ionDecimal
import com.amazon.ionelement.api.ionFloat
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionSymbol
import com.amazon.ionelement.api.metaContainerOf
import org.partiql.ast.AstNode
import org.partiql.ast.DatetimeField
import org.partiql.ast.Exclude
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.GraphMatch
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
import org.partiql.ast.TableDefinition
import org.partiql.ast.Type
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.lang.ast.IsListParenthesizedMeta
import org.partiql.lang.ast.IsValuesExprMeta
import org.partiql.lang.ast.Meta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.value.DateValue
import org.partiql.value.MissingValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.TimeValue
import org.partiql.value.TimestampValue
import org.partiql.value.datetime.TimeZone
import org.partiql.value.toIon
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Translates an [AstNode] tree to the legacy PIG AST.
 *
 * Optionally, you can provide a Map of MetaContainers to attach to the legacy AST nodes.
 */
public fun AstNode.toLegacyAst(metas: Map<String, MetaContainer> = emptyMap()): PartiqlAst.PartiqlAstNode {
    val translator = AstTranslator(metas)
    return accept(translator, Ctx())
}

/**
 * Empty visitor method arguments
 */
private class Ctx

/**
 * Traverses an [AstNode] tree, folding to a [PartiqlAst.PartiqlAstNode] tree.
 */
@OptIn(PartiQLValueExperimental::class)
private class AstTranslator(val metas: Map<String, MetaContainer>) : AstBaseVisitor<PartiqlAst.PartiqlAstNode, Ctx>() {

    private val pig = PartiqlAst.BUILDER()
    // Currently hard-coded in legacy code
    private val aggregates = setOf("count", "avg", "sum", "min", "max", "any", "some", "every")

    override fun defaultReturn(node: AstNode, ctx: Ctx): Nothing {
        val fromClass = node::class.qualifiedName
        val toClass = PartiqlAst.PartiqlAstNode::class.qualifiedName
        throw IllegalArgumentException("$fromClass cannot be translated to $toClass")
    }

    override fun defaultVisit(node: AstNode, ctx: Ctx) = defaultReturn(node, ctx)

    /**
     * Attach Metas if-any
     */
    private inline fun <T : PartiqlAst.PartiqlAstNode> translate(
        node: AstNode,
        block: PartiqlAst.Builder.(metas: MetaContainer) -> T,
    ): T {
        val metas = metas[node.tag] ?: emptyMetaContainer()
        return pig.block(metas)
    }

    override fun visitStatement(node: Statement, ctx: Ctx) = super.visitStatement(node, ctx) as PartiqlAst.Statement

    override fun visitStatementQuery(node: Statement.Query, ctx: Ctx) = translate(node) { metas ->
        val expr = visitExpr(node.expr, ctx)
        query(expr, metas)
    }

    override fun visitStatementExplain(node: Statement.Explain, ctx: Ctx) = translate(node) { metas ->
        val target = visitStatementExplainTarget(node.target, ctx)
        explain(target, metas)
    }

    override fun visitStatementExplainTarget(node: Statement.Explain.Target, ctx: Ctx) =
        super.visitStatementExplainTarget(node, ctx) as PartiqlAst.ExplainTarget

    override fun visitStatementExplainTargetDomain(node: Statement.Explain.Target.Domain, ctx: Ctx) =
        translate(node) { metas ->
            val statement = visitStatement(node.statement, ctx)
            val type = node.type
            val format = node.format
            domain(statement, type, format, metas)
        }

    override fun visitStatementDDL(node: Statement.DDL, ctx: Ctx) = super.visit(node, ctx) as PartiqlAst.Statement.Ddl

    override fun visitStatementDDLCreateTable(
        node: Statement.DDL.CreateTable,
        ctx: Ctx,
    ) = translate(node) { metas ->
        if (node.name !is Identifier.Symbol) {
            error("The legacy AST does not support qualified identifiers as table names")
        }
        val tableName = (node.name as Identifier.Symbol).symbol
        val def = node.definition?.let { visitTableDefinition(it, ctx) }
        ddl(createTable(tableName, def), metas)
    }

    override fun visitStatementDDLCreateIndex(
        node: Statement.DDL.CreateIndex,
        ctx: Ctx,
    ) = translate(node) { metas ->
        if (node.index != null) {
            error("The legacy AST does not support index names")
        }
        if (node.table !is Identifier.Symbol) {
            error("The legacy AST does not support qualified identifiers as table names")
        }
        val tableName = visitIdentifierSymbol((node.table as Identifier.Symbol), ctx)
        val fields = node.fields.map { visitPathUnpack(it, ctx) }
        ddl(createIndex(tableName, fields), metas)
    }

    override fun visitStatementDDLDropTable(node: Statement.DDL.DropTable, ctx: Ctx) = translate(node) { metas ->
        if (node.table !is Identifier.Symbol) {
            error("The legacy AST does not support qualified identifiers as table names")
        }
        // !! Legacy AST "index_name" mix up !!
        val tableName = visitIdentifierSymbol((node.table as Identifier.Symbol), ctx)
        ddl(dropTable(tableName), metas)
    }

    override fun visitStatementDDLDropIndex(node: Statement.DDL.DropIndex, ctx: Ctx) = translate(node) { metas ->
        if (node.index !is Identifier.Symbol) {
            error("The legacy AST does not support qualified identifiers as index names")
        }
        if (node.table !is Identifier.Symbol) {
            error("The legacy AST does not support qualified identifiers as table names")
        }
        // !! Legacy AST "table" mix up !!
        val index = visitIdentifierSymbol(node.index as Identifier.Symbol, ctx)
        // !! Legacy AST "keys" mix up !!
        val table = visitIdentifierSymbol(node.table as Identifier.Symbol, ctx)
        ddl(dropIndex(table, index), metas)
    }

    override fun visitTableDefinition(node: TableDefinition, ctx: Ctx) = translate(node) { metas ->
        val parts = node.columns.translate<PartiqlAst.TableDefPart>(ctx)
        tableDef(parts, metas)
    }

    override fun visitTableDefinitionColumn(node: TableDefinition.Column, ctx: Ctx) = translate(node) { metas ->
        val name = node.name
        val type = visitType(node.type, ctx)
        val constraints = node.constraints.translate<PartiqlAst.ColumnConstraint>(ctx)
        columnDeclaration(name, type, constraints, metas)
    }

    override fun visitTableDefinitionColumnConstraint(
        node: TableDefinition.Column.Constraint,
        ctx: Ctx,
    ) = translate(node) { metas ->
        val name = node.name
        val def = when (node.body) {
            is TableDefinition.Column.Constraint.Body.Check -> {
                throw IllegalArgumentException("PIG AST does not support CHECK (<expr>) constraint")
            }
            is TableDefinition.Column.Constraint.Body.NotNull -> columnNotnull()
            is TableDefinition.Column.Constraint.Body.Nullable -> columnNull()
        }
        columnConstraint(name, def, metas)
    }

    /**
     * IDENTIFIERS / PATHS - Always expressions in legacy AST
     */

    override fun visitIdentifier(node: Identifier, ctx: Ctx) = when (node) {
        is Identifier.Qualified -> visitIdentifierQualified(node, ctx)
        is Identifier.Symbol -> visitIdentifierSymbolAsExpr(node)
    }

    override fun visitIdentifierSymbol(node: Identifier.Symbol, ctx: Ctx) = translate(node) { metas ->
        val name = node.symbol
        val case = node.caseSensitivity.toLegacyCaseSensitivity()
        // !! NOT AN EXPRESSION!!
        identifier(name, case, metas)
    }

    fun visitIdentifierSymbolAsExpr(node: Identifier.Symbol) = translate(node) { metas ->
        val name = node.symbol
        val case = node.caseSensitivity.toLegacyCaseSensitivity()
        val scope = unqualified()
        // !! ID EXPRESSION!!
        id(name, case, scope, metas)
    }

    override fun visitIdentifierQualified(node: Identifier.Qualified, ctx: Ctx) = translate(node) { metas ->
        // !! Legacy AST represents qualified identifiers as Expr.Path !!
        val root = visitIdentifierSymbolAsExpr(node.root)
        val steps = node.steps.map {
            // Legacy AST wraps id twice and always uses CaseSensitive
            val expr = visitIdentifierSymbolAsExpr(it)
            pathExpr(expr, caseSensitive())
        }
        path(root, steps, metas)
    }

    override fun visitPath(node: Path, ctx: Ctx) = translate(node) { metas ->
        val root = visitIdentifierSymbolAsExpr(node.root)
        val steps = node.steps.translate<PartiqlAst.PathStep>(ctx)
        path(root, steps, metas)
    }

    override fun visitPathStep(node: Path.Step, ctx: Ctx) = super.visitPathStep(node, ctx) as PartiqlAst.PathStep

    override fun visitPathStepSymbol(node: Path.Step.Symbol, ctx: Ctx) = translate(node) { metas ->
        // val index = visitIdentifierSymbolAsExpr(node.symbol, ctx)
        val index = lit(ionString(node.symbol.symbol), metas)
        val case = node.symbol.caseSensitivity.toLegacyCaseSensitivity()
        pathExpr(index, case, metas)
    }

    override fun visitPathStepIndex(node: Path.Step.Index, ctx: Ctx) = translate(node) { metas ->
        val index = lit(ionInt(node.index.toLong()))
        val case = caseSensitive() // ???
        pathExpr(index, case, metas)
    }

    /**
     * EXPRESSIONS
     */

    override fun visitExpr(node: Expr, ctx: Ctx): PartiqlAst.Expr = super.visitExpr(node, ctx) as PartiqlAst.Expr

    override fun visitExprLit(node: Expr.Lit, ctx: Ctx) = translate(node) { metas ->
        when (val v = node.value) {
            is MissingValue -> missing(metas)
            is DateValue -> v.toLegacyAst(metas)
            is TimeValue -> v.toLegacyAst(metas)
            is TimestampValue -> v.toLegacyAst(metas)
            else -> {
                val ion = v.toIon()
                lit(ion, metas)
            }
        }
    }

    override fun visitExprIon(node: Expr.Ion, ctx: Ctx) = translate(node) { metas ->
        lit(node.value, metas)
    }

    override fun visitExprVar(node: Expr.Var, ctx: Ctx) = translate(node) { metas ->
        if (node.identifier is Identifier.Qualified) {
            error("Qualified identifiers not allowed in legacy AST `id` variable references")
        }
        val v = node.identifier as Identifier.Symbol
        val name = v.symbol
        val case = v.caseSensitivity.toLegacyCaseSensitivity()
        val qualifier = node.scope.toLegacyScope()
        id(name, case, qualifier, metas)
    }

    override fun visitExprCall(node: Expr.Call, ctx: Ctx) = translate(node) { metas ->
        if (node.function is Identifier.Qualified) {
            error("Qualified identifiers are not allowed in legacy AST `call` function identifiers")
        }
        val funcName = (node.function as Identifier.Symbol).symbol.lowercase()
        val args = node.args.translate<PartiqlAst.Expr>(ctx)
        when (funcName.isAggregateCall() || node.setq != null) { // Use existing assumption that function call with set quantifier is an aggregation function
            true -> {
                val setq = node.setq?.toLegacySetQuantifier() ?: all()
                // COUNT(*) is represented as COUNT() in default AST
                // Legacy AST translates COUNT(*) to COUNT(1)
                if (funcName == "count" && args.isEmpty()) {
                    return callAgg(setq, "count", lit(ionInt(1)), metas)
                }
                // Default Case
                if (node.args.size != 1) {
                    error("Cannot translate `call_agg` with more than one argument")
                }
                val arg = visitExpr(node.args[0], ctx)
                callAgg(setq, funcName, arg, metas)
            }
            false -> call(funcName, args, metas)
        }
    }

    private fun String.isAggregateCall(): Boolean {
        // like PartiQLPigVisitor, keep legacy behavior the same as before
        // since it is legacy, hard-coded aggregation logic
        return aggregates.contains(this)
    }

    override fun visitExprOperator(node: Expr.Operator, ctx: Ctx) = translate(node) { metas ->
        val lhs = node.lhs?.let { visitExpr(it, ctx) }
        val rhs = visitExpr(node.rhs, ctx)
        if (lhs == null) {
            when (node.symbol) {
                "+" -> {
                    when {
                        rhs !is PartiqlAst.Expr.Lit -> pos(rhs)
                        rhs.value is IntElement -> rhs
                        rhs.value is FloatElement -> rhs
                        rhs.value is DecimalElement -> rhs
                        else -> pos(rhs)
                    }
                }
                "-" -> {
                    when {
                        rhs !is PartiqlAst.Expr.Lit -> neg(rhs, metas)
                        rhs.value is IntElement -> {
                            val intValue = when (rhs.value.integerSize) {
                                IntElementSize.LONG -> ionInt(-rhs.value.longValue)
                                IntElementSize.BIG_INTEGER -> when (rhs.value.bigIntegerValue) {
                                    Long.MAX_VALUE.toBigInteger() + (1L).toBigInteger() -> ionInt(Long.MIN_VALUE)
                                    else -> ionInt(rhs.value.bigIntegerValue * BigInteger.valueOf(-1L))
                                }
                            }
                            rhs.copy(
                                value = intValue.asAnyElement(),
                                metas = metas,
                            )
                        }
                        rhs.value is FloatElement -> rhs.copy(
                            value = ionFloat(-(rhs.value.doubleValue)).asAnyElement(),
                            metas = metas,
                        )
                        rhs.value is DecimalElement -> rhs.copy(
                            value = ionDecimal(Decimal.valueOf(-(rhs.value.decimalValue))).asAnyElement(),
                            metas = metas,
                        )
                        else -> neg(rhs, metas)
                    }
                }
                else -> error("unsupported unary expr operator $node")
            }
        } else {
            val operands = listOf(lhs, rhs)
            when (node.symbol) {
                "+" -> plus(operands, metas)
                "-" -> minus(operands, metas)
                "*" -> times(operands, metas)
                "/" -> divide(operands, metas)
                "%" -> modulo(operands, metas)
                "||" -> concat(operands, metas)
                "=" -> eq(operands, metas)
                "<>" -> ne(operands, metas)
                "!=" -> ne(operands, metas)
                ">" -> gt(operands, metas)
                ">=" -> gte(operands, metas)
                "<" -> lt(operands, metas)
                "<=" -> lte(operands, metas)
                "&" -> bitwiseAnd(operands, metas)
                else -> error("unsupported binary expr operator $node")
            }
        }
    }

    override fun visitExprAnd(node: Expr.And, ctx: Ctx) = translate(node) { metas ->
        val lhs = visitExpr(node.lhs, ctx)
        val rhs = visitExpr(node.rhs, ctx)
        and(lhs, rhs)
    }

    override fun visitExprOr(node: Expr.Or, ctx: Ctx) = translate(node) { metas ->
        val lhs = visitExpr(node.lhs, ctx)
        val rhs = visitExpr(node.rhs, ctx)
        or(lhs, rhs)
    }

    override fun visitExprNot(node: Expr.Not, ctx: Ctx) = translate(node) { metas ->
        val rhs = visitExpr(node.value, ctx)
        not(rhs)
    }

    override fun visitExprPath(node: Expr.Path, ctx: Ctx) = translate(node) { metas ->
        val root = visitExpr(node.root, ctx)
        val steps = node.steps.map { visitExprPathStep(it, ctx) }
        path(root, steps, metas)
    }

    override fun visitExprPathStep(node: Expr.Path.Step, ctx: Ctx) =
        super.visitExprPathStep(node, ctx) as PartiqlAst.PathStep

    override fun visitExprPathStepSymbol(node: Expr.Path.Step.Symbol, ctx: Ctx) = translate(node) { metas ->
        val index = lit(ionString(node.symbol.symbol))
        val case = node.symbol.caseSensitivity.toLegacyCaseSensitivity()
        pathExpr(index, case, metas)
    }

    override fun visitExprPathStepIndex(node: Expr.Path.Step.Index, ctx: Ctx) = translate(node) { metas ->
        val index = visitExpr(node.key, ctx)
        // Legacy AST marks every index step as CaseSensitive
        val case = when (index) {
            is PartiqlAst.Expr.Id -> index.case
            else -> PartiqlAst.CaseSensitivity.CaseSensitive()
        }
        pathExpr(index, case, metas)
    }

    override fun visitExprPathStepWildcard(node: Expr.Path.Step.Wildcard, ctx: Ctx) = translate(node) { metas ->
        pathWildcard(metas)
    }

    override fun visitExprPathStepUnpivot(node: Expr.Path.Step.Unpivot, ctx: Ctx) = translate(node) { metas ->
        pathUnpivot(metas)
    }

    override fun visitExprParameter(node: Expr.Parameter, ctx: Ctx) = translate(node) { metas ->
        parameter(node.index.toLong(), metas)
    }

    override fun visitExprValues(node: Expr.Values, ctx: Ctx) = translate(node) { metas ->
        val rows = node.rows.map { visitExprValuesRow(it, ctx) }
        bag(rows, metas + metaContainerOf(IsValuesExprMeta.instance))
    }

    override fun visitExprValuesRow(node: Expr.Values.Row, ctx: Ctx) = translate(node) { metas ->
        val exprs = node.items.translate<PartiqlAst.Expr>(ctx)
        list(exprs, metas + metaContainerOf(IsListParenthesizedMeta))
    }

    override fun visitExprCollection(node: Expr.Collection, ctx: Ctx) = translate(node) { metas ->
        val values = node.values.translate<PartiqlAst.Expr>(ctx)
        when (node.type) {
            Expr.Collection.Type.BAG -> bag(values, metas)
            Expr.Collection.Type.ARRAY -> list(values, metas)
            Expr.Collection.Type.VALUES -> list(values, metas + metaContainerOf(IsValuesExprMeta.instance))
            Expr.Collection.Type.LIST -> list(values, metas + metaContainerOf(IsListParenthesizedMeta))
            Expr.Collection.Type.SEXP -> sexp(values, metas)
        }
    }

    override fun visitExprStruct(node: Expr.Struct, ctx: Ctx) = translate(node) { metas ->
        val fields = node.fields.translate<PartiqlAst.ExprPair>(ctx)
        struct(fields, metas)
    }

    override fun visitExprStructField(node: Expr.Struct.Field, ctx: Ctx) = translate(node) { metas ->
        val first = visitExpr(node.name, ctx)
        val second = visitExpr(node.value, ctx)
        exprPair(first, second, metas)
    }

    override fun visitExprLike(node: Expr.Like, ctx: Ctx) = translate(node) { metas ->
        val value = visitExpr(node.value, ctx)
        val pattern = visitExpr(node.pattern, ctx)
        val escape = visitOrNull<PartiqlAst.Expr>(node.escape, ctx)
        if (node.not != null && node.not!!) {
            not(like(value, pattern, escape), metas)
        } else {
            like(value, pattern, escape, metas)
        }
    }

    override fun visitExprBetween(node: Expr.Between, ctx: Ctx) = translate(node) { metas ->
        val value = visitExpr(node.value, ctx)
        val from = visitExpr(node.from, ctx)
        val to = visitExpr(node.to, ctx)
        if (node.not != null && node.not!!) {
            not(between(value, from, to), metas)
        } else {
            between(value, from, to, metas)
        }
    }

    override fun visitExprInCollection(node: Expr.InCollection, ctx: Ctx) = translate(node) { metas ->
        val lhs = visitExpr(node.lhs, ctx)
        val rhs = visitExpr(node.rhs, ctx)
        val operands = listOf(lhs, rhs)
        if (node.not != null && node.not!!) {
            not(inCollection(operands), metas)
        } else {
            inCollection(operands, metas)
        }
    }

    override fun visitExprIsType(node: Expr.IsType, ctx: Ctx) = translate(node) { metas ->
        val value = visitExpr(node.value, ctx)
        val type = visitType(node.type, ctx)
        if (node.not != null && node.not!!) {
            not(isType(value, type), metas)
        } else {
            isType(value, type, metas)
        }
    }

    override fun visitExprCase(node: Expr.Case, ctx: Ctx) = translate(node) { metas ->
        val cases = exprPairList(node.branches.translate<PartiqlAst.ExprPair>(ctx))
        val condition = visitOrNull<PartiqlAst.Expr>(node.expr, ctx)
        val default = visitOrNull<PartiqlAst.Expr>(node.default, ctx)
        when (condition) {
            null -> searchedCase(cases, default, metas)
            else -> simpleCase(condition, cases, default, metas)
        }
    }

    override fun visitExprCaseBranch(node: Expr.Case.Branch, ctx: Ctx) = translate(node) { metas ->
        val first = visitExpr(node.condition, ctx)
        val second = visitExpr(node.expr, ctx)
        exprPair(first, second, metas)
    }

    override fun visitExprCoalesce(node: Expr.Coalesce, ctx: Ctx) = translate(node) { metas ->
        val args = node.args.translate<PartiqlAst.Expr>(ctx)
        coalesce(args, metas)
    }

    override fun visitExprNullIf(node: Expr.NullIf, ctx: Ctx) = translate(node) { metas ->
        val expr1 = visitExpr(node.value, ctx)
        val expr2 = visitExpr(node.nullifier, ctx)
        nullIf(expr1, expr2, metas)
    }

    override fun visitExprSubstring(node: Expr.Substring, ctx: Ctx) = translate(node) { metas ->
        val value = visitExpr(node.value, ctx)
        val start = visitOrNull<PartiqlAst.Expr>(node.start, ctx)
        val length = visitOrNull<PartiqlAst.Expr>(node.length, ctx)
        val operands = listOfNotNull(value, start, length)
        call("substring", operands, metas)
    }

    override fun visitExprPosition(node: Expr.Position, ctx: Ctx) = translate(node) { metas ->
        val lhs = visitExpr(node.lhs, ctx)
        val rhs = visitExpr(node.rhs, ctx)
        val operands = listOf(lhs, rhs)
        call("position", operands, metas)
    }

    override fun visitExprTrim(node: Expr.Trim, ctx: Ctx) = translate(node) { metas ->
        val operands = mutableListOf<PartiqlAst.Expr>()
        // Legacy AST requires adding the spec as an argument
        val spec = node.spec?.name?.lowercase()
        val chars = node.chars?.let { visitExpr(it, ctx) }
        val value = visitExpr(node.value, ctx)
        if (spec != null) operands.add(lit(ionSymbol(spec)))
        if (chars != null) operands.add(chars)
        operands.add(value)
        call("trim", operands, metas)
    }

    override fun visitExprOverlay(node: Expr.Overlay, ctx: Ctx) = translate(node) { metas ->
        val value = visitExpr(node.value, ctx)
        val overlay = visitExpr(node.overlay, ctx)
        val start = visitExpr(node.start, ctx)
        val length = visitOrNull<PartiqlAst.Expr>(node.length, ctx)
        val operands = listOfNotNull(value, overlay, start, length)
        call("overlay", operands, metas)
    }

    override fun visitExprExtract(node: Expr.Extract, ctx: Ctx) = translate(node) { metas ->
        val field = node.field.toLegacyDatetimePart()
        val source = visitExpr(node.source, ctx)
        val operands = listOf(field, source)
        call("extract", operands, metas)
    }

    override fun visitExprCast(node: Expr.Cast, ctx: Ctx) = translate(node) { metas ->
        val value = visitExpr(node.value, ctx)
        val asType = visitType(node.asType, ctx)
        cast(value, asType, metas)
    }

    override fun visitExprDateAdd(node: Expr.DateAdd, ctx: Ctx) = translate(node) { metas ->
        val field = node.field.toLegacyDatetimePart()
        val lhs = visitExpr(node.lhs, ctx)
        val rhs = visitExpr(node.rhs, ctx)
        val operands = listOf(field, lhs, rhs)
        call("date_add", operands, metas)
    }

    override fun visitExprDateDiff(node: Expr.DateDiff, ctx: Ctx) = translate(node) { metas ->
        val field = node.field.toLegacyDatetimePart()
        val lhs = visitExpr(node.lhs, ctx)
        val rhs = visitExpr(node.rhs, ctx)
        val operands = listOf(field, lhs, rhs)
        call("date_diff", operands, metas)
    }

    override fun visitExprMatch(node: Expr.Match, ctx: Ctx) = translate(node) { metas ->
        val expr = visitExpr(node.expr, ctx)
        val match = visitGraphMatch(node.pattern, ctx)
        graphMatch(expr, match, metas)
    }

    override fun visitExprWindow(node: Expr.Window, ctx: Ctx) = translate(node) { metas ->
        val funcName = node.function.name.lowercase()
        val over = visitExprWindowOver(node.over, ctx)
        val args = listOfNotNull(node.expression, node.offset, node.default).translate<PartiqlAst.Expr>(ctx)
        callWindow(funcName, over, args, metas)
    }

    override fun visitExprWindowOver(node: Expr.Window.Over, ctx: Ctx) = translate(node) { metas ->
        val partitionBy = node.partitions?.let {
            val partitions = it.translate<PartiqlAst.Expr>(ctx)
            windowPartitionList(partitions)
        }
        val orderBy = node.sorts?.let {
            val sorts = it.translate<PartiqlAst.SortSpec>(ctx)
            windowSortSpecList(sorts)
        }
        over(partitionBy, orderBy, metas)
    }

    override fun visitExprSessionAttribute(node: Expr.SessionAttribute, ctx: Ctx) = translate(node) { metas ->
        sessionAttribute(node.attribute.name.lowercase(), metas)
    }

    /**
     * SELECT-FROM-WHERE
     */
    override fun visitExprQuerySet(node: Expr.QuerySet, ctx: Ctx) = translate(node) { metas ->
        val orderBy = node.orderBy?.let { visitOrderBy(it, ctx) }
        val limit = node.limit?.let { visitExpr(it, ctx) }
        val offset = node.offset?.let { visitExpr(it, ctx) }
        when (val body = node.body) {
            is QueryBody.SFW -> {
                var setq = when (val s = body.select) {
                    is Select.Pivot -> null
                    is Select.Project -> s.setq?.toLegacySetQuantifier()
                    is Select.Star -> s.setq?.toLegacySetQuantifier()
                    is Select.Value -> s.setq?.toLegacySetQuantifier()
                }
                // Legacy AST removes (setq (all))
                if (setq != null && setq is PartiqlAst.SetQuantifier.All) {
                    setq = null
                }
                val project = visitSelect(body.select, ctx)
                val from = visitFrom(body.from, ctx)
                val exclude = body.exclude?.let { visitExclude(it, ctx) }
                val fromLet = body.let?.let { visitLet(it, ctx) }
                val where = body.where?.let { visitExpr(it, ctx) }
                val groupBy = body.groupBy?.let { visitGroupBy(it, ctx) }
                val having = body.having?.let { visitExpr(it, ctx) }
                select(setq, project, exclude, from, fromLet, where, groupBy, having, orderBy, limit, offset, metas)
            }
            is QueryBody.SetOp -> {
                val lhs = visitExpr(body.lhs, ctx)
                val rhs = visitExpr(body.rhs, ctx)
                val outer = body.isOuter
                val op = when (body.type.type) {
                    SetOp.Type.UNION -> if (outer) {
                        outerUnion()
                    } else {
                        union()
                    }
                    SetOp.Type.INTERSECT -> if (outer) {
                        outerIntersect()
                    } else {
                        intersect()
                    }
                    SetOp.Type.EXCEPT -> if (outer) {
                        outerExcept()
                    } else {
                        except()
                    }
                }
                val setq = body.type.setq?.toLegacySetQuantifier() ?: distinct()
                val operands = listOf(lhs, rhs)
                bagOp(op, setq, operands, metas)
            }
        }
    }

    /**
     * UNSUPPORTED in legacy AST
     */
    override fun visitSelect(node: Select, ctx: Ctx) = super.visitSelect(node, ctx) as PartiqlAst.Projection

    override fun visitSelectStar(node: Select.Star, ctx: Ctx) = translate(node) { metas ->
        projectStar(metas)
    }

    override fun visitSelectProject(node: Select.Project, ctx: Ctx) = translate(node) { metas ->
        val items = node.items.translate<PartiqlAst.ProjectItem>(ctx)
        projectList(items, metas)
    }

    override fun visitSelectProjectItem(node: Select.Project.Item, ctx: Ctx) =
        super.visitSelectProjectItem(node, ctx) as PartiqlAst.ProjectItem

    override fun visitSelectProjectItemAll(node: Select.Project.Item.All, ctx: Ctx) = translate(node) { metas ->
        val expr = visitExpr(node.expr, ctx)
        projectAll(expr, metas)
    }

    override fun visitSelectProjectItemExpression(node: Select.Project.Item.Expression, ctx: Ctx) =
        translate(node) { metas ->
            val expr = visitExpr(node.expr, ctx)
            val alias = node.asAlias?.symbol
            projectExpr(expr, alias, metas)
        }

    // !!
    // Legacy AST mislabels key and value in PIVOT, swapping the order here to recreate bug for compatibility.
    // !!
    override fun visitSelectPivot(node: Select.Pivot, ctx: Ctx) = translate(node) { metas ->
        val key = visitExpr(node.value, ctx) // SWAP val -> key
        val value = visitExpr(node.key, ctx) // SWAP key -> val
        projectPivot(value, key, metas)
    }

    override fun visitSelectValue(node: Select.Value, ctx: Ctx) = translate(node) { metas ->
        val value = visitExpr(node.constructor, ctx)
        projectValue(value, metas)
    }

    override fun visitFrom(node: From, ctx: Ctx) = super.visitFrom(node, ctx) as PartiqlAst.FromSource
    override fun visitFromValue(node: From.Value, ctx: Ctx) = translate(node) { metas ->
        val expr = visitExpr(node.expr, ctx)
        val asAlias = node.asAlias?.symbol
        val atAlias = node.atAlias?.symbol
        val byAlias = node.byAlias?.symbol
        when (node.type) {
            From.Value.Type.SCAN -> scan(expr, asAlias, atAlias, byAlias, metas)
            From.Value.Type.UNPIVOT -> unpivot(expr, asAlias, atAlias, byAlias, metas)
        }
    }

    // Legacy AST models CROSS JOIN and COMMA-syntax CROSS JOIN as FULL JOIN
    // Legacy AST does not have OUTER variants
    override fun visitFromJoin(node: From.Join, ctx: Ctx) = translate(node) { metas ->
        val type = when (node.type) {
            From.Join.Type.INNER -> inner()
            From.Join.Type.LEFT -> left()
            From.Join.Type.LEFT_OUTER -> left()
            From.Join.Type.RIGHT -> right()
            From.Join.Type.RIGHT_OUTER -> right()
            From.Join.Type.FULL -> full()
            From.Join.Type.FULL_OUTER -> full()
            From.Join.Type.CROSS -> full()
            From.Join.Type.COMMA -> full()
            null -> inner()
        }
        val lhs = visitFrom(node.lhs, ctx)
        val rhs = visitFrom(node.rhs, ctx)
        val condition = visitOrNull<PartiqlAst.Expr>(node.condition, ctx)
        join(type, lhs, rhs, condition, metas)
    }

    override fun visitExclude(node: Exclude, ctx: Ctx): PartiqlAst.ExcludeOp = translate(node) { metas ->
        val excludeExprs = node.items.translate<PartiqlAst.ExcludeExpr>(ctx)
        excludeOp(excludeExprs, metas)
    }

    override fun visitExcludeItem(node: Exclude.Item, ctx: Ctx) = translate(node) { metas ->
        val root = visitExprVar(node.root, ctx)
        val steps = node.steps.translate<PartiqlAst.ExcludeStep>(ctx)
        excludeExpr(root = identifier_(root.name, root.case), steps = steps, metas)
    }

    override fun visitExcludeStep(node: Exclude.Step, ctx: Ctx) =
        super.visitExcludeStep(node, ctx) as PartiqlAst.ExcludeStep

    override fun visitExcludeStepStructField(node: Exclude.Step.StructField, ctx: Ctx) = translate(node) { metas ->
        val attr = node.symbol.symbol
        val case = node.symbol.caseSensitivity.toLegacyCaseSensitivity()
        excludeTupleAttr(identifier(attr, case), metas)
    }

    override fun visitExcludeStepCollIndex(
        node: Exclude.Step.CollIndex,
        ctx: Ctx
    ) = translate(node) { metas ->
        val index = node.index.toLong()
        excludeCollectionIndex(index, metas)
    }

    override fun visitExcludeStepStructWildcard(
        node: Exclude.Step.StructWildcard,
        ctx: Ctx
    ) = translate(node) { metas ->
        excludeTupleWildcard(metas)
    }

    override fun visitExcludeStepCollWildcard(
        node: Exclude.Step.CollWildcard,
        ctx: Ctx
    ) = translate(node) { metas ->
        excludeCollectionWildcard(metas)
    }

    override fun visitLet(node: Let, ctx: Ctx) = translate(node) { metas ->
        val bindings = node.bindings.translate<PartiqlAst.LetBinding>(ctx)
        let(bindings, metas)
    }

    override fun visitLetBinding(node: Let.Binding, ctx: Ctx) = translate(node) { metas ->
        val expr = visitExpr(node.expr, ctx)
        val name = node.asAlias.symbol
        letBinding(expr, name, metas)
    }

    override fun visitGroupBy(node: GroupBy, ctx: Ctx) = translate(node) { metas ->
        val strategy = when (node.strategy) {
            GroupBy.Strategy.FULL -> groupFull()
            GroupBy.Strategy.PARTIAL -> groupPartial()
        }
        val keyList = groupKeyList(node.keys.translate<PartiqlAst.GroupKey>(ctx))
        val groupAsAlias = node.asAlias?.symbol
        groupBy(strategy, keyList, groupAsAlias, metas)
    }

    override fun visitGroupByKey(node: GroupBy.Key, ctx: Ctx) = translate(node) { metas ->
        val expr = visitExpr(node.expr, ctx)
        val asAlias = node.asAlias?.symbol
        groupKey(expr, asAlias, metas)
    }

    override fun visitOrderBy(node: OrderBy, ctx: Ctx) = translate(node) { metas ->
        val sortSpecs = node.sorts.translate<PartiqlAst.SortSpec>(ctx)
        orderBy(sortSpecs, metas)
    }

    override fun visitSort(node: Sort, ctx: Ctx) = translate(node) { metas ->
        val expr = visitExpr(node.expr, ctx)
        val orderingSpec = when (node.dir) {
            Sort.Dir.ASC -> asc()
            Sort.Dir.DESC -> desc()
            null -> null
        }
        val nullsSpec = when (node.nulls) {
            Sort.Nulls.FIRST -> nullsFirst()
            Sort.Nulls.LAST -> nullsLast()
            null -> null
        }
        sortSpec(expr, orderingSpec, nullsSpec, metas)
    }

    /**
     * UNSUPPORTED in legacy AST
     */
    override fun visitSetOp(node: SetOp, ctx: Ctx) = defaultVisit(node, ctx)

    /**
     * GPML
     */

    override fun visitGraphMatch(node: GraphMatch, ctx: Ctx) = translate(node) { metas ->
        val selector = node.selector?.let { visitGraphMatchSelector(it, ctx) }
        val patterns = node.patterns.translate<PartiqlAst.GraphMatchPattern>(ctx)
        gpmlPattern(selector, patterns, metas)
    }

    override fun visitGraphMatchPattern(node: GraphMatch.Pattern, ctx: Ctx) = translate(node) { metas ->
        val restrictor = when (node.restrictor) {
            GraphMatch.Restrictor.TRAIL -> restrictorTrail()
            GraphMatch.Restrictor.ACYCLIC -> restrictorAcyclic()
            GraphMatch.Restrictor.SIMPLE -> restrictorSimple()
            null -> null
        }
        val prefilter = node.prefilter?.let { visitExpr(it, ctx) }
        val variable = node.variable
        val quantifier = node.quantifier?.let { visitGraphMatchQuantifier(it, ctx) }
        val parts = node.parts.translate<PartiqlAst.GraphMatchPatternPart>(ctx)
        graphMatchPattern(restrictor, prefilter, variable, quantifier, parts, metas)
    }

    override fun visitGraphMatchPatternPart(node: GraphMatch.Pattern.Part, ctx: Ctx) =
        super.visitGraphMatchPatternPart(node, ctx) as PartiqlAst.GraphMatchPatternPart

    override fun visitGraphMatchPatternPartNode(node: GraphMatch.Pattern.Part.Node, ctx: Ctx) =
        translate(node) { metas ->
            val prefilter = node.prefilter?.let { visitExpr(it, ctx) }
            val variable = node.variable
            val label = node.label?.let { visitGraphMatchLabel(it, ctx) }
            node(prefilter, variable, label, metas)
        }

    override fun visitGraphMatchPatternPartEdge(node: GraphMatch.Pattern.Part.Edge, ctx: Ctx) =
        translate(node) { metas ->
            val direction = when (node.direction) {
                GraphMatch.Direction.LEFT -> edgeLeft()
                GraphMatch.Direction.UNDIRECTED -> edgeUndirected()
                GraphMatch.Direction.RIGHT -> edgeRight()
                GraphMatch.Direction.LEFT_OR_UNDIRECTED -> edgeLeftOrUndirected()
                GraphMatch.Direction.UNDIRECTED_OR_RIGHT -> edgeUndirectedOrRight()
                GraphMatch.Direction.LEFT_OR_RIGHT -> edgeLeftOrRight()
                GraphMatch.Direction.LEFT_UNDIRECTED_OR_RIGHT -> edgeLeftOrUndirectedOrRight()
            }
            val quantifier = node.quantifier?.let { visitGraphMatchQuantifier(it, ctx) }
            val prefilter = node.prefilter?.let { visitExpr(it, ctx) }
            val variable = node.variable
            val label = node.label?.let { visitGraphMatchLabel(it, ctx) }
            edge(direction, quantifier, prefilter, variable, label, metas)
        }

    override fun visitGraphMatchPatternPartPattern(node: GraphMatch.Pattern.Part.Pattern, ctx: Ctx) =
        translate(node) { metas ->
            pattern(visitGraphMatchPattern(node.pattern, ctx), metas)
        }

    override fun visitGraphMatchQuantifier(node: GraphMatch.Quantifier, ctx: Ctx) = translate(node) { metas ->
        val lower = node.lower
        val upper = node.upper
        graphMatchQuantifier(lower, upper, metas)
    }

    override fun visitGraphMatchSelector(node: GraphMatch.Selector, ctx: Ctx) =
        super.visitGraphMatchSelector(node, ctx) as PartiqlAst.GraphMatchSelector

    override fun visitGraphMatchSelectorAnyShortest(node: GraphMatch.Selector.AnyShortest, ctx: Ctx) =
        translate(node) { metas ->
            selectorAnyShortest(metas)
        }

    override fun visitGraphMatchSelectorAllShortest(node: GraphMatch.Selector.AllShortest, ctx: Ctx) =
        translate(node) { metas ->
            selectorAllShortest(metas)
        }

    override fun visitGraphMatchSelectorAny(node: GraphMatch.Selector.Any, ctx: Ctx) = translate(node) { metas ->
        selectorAny(metas)
    }

    override fun visitGraphMatchSelectorAnyK(node: GraphMatch.Selector.AnyK, ctx: Ctx) = translate(node) { metas ->
        val k = node.k
        selectorAnyK(k, metas)
    }

    override fun visitGraphMatchSelectorShortestK(
        node: GraphMatch.Selector.ShortestK,
        ctx: Ctx,
    ) = translate(node) { metas ->
        val k = node.k
        selectorShortestK(k, metas)
    }

    override fun visitGraphMatchSelectorShortestKGroup(
        node: GraphMatch.Selector.ShortestKGroup,
        ctx: Ctx,
    ) = translate(node) {
        val k = node.k
        selectorShortestKGroup(k)
    }

    override fun visitGraphMatchLabel(node: GraphMatch.Label, ctx: Ctx) =
        super.visitGraphMatchLabel(node, ctx) as PartiqlAst.GraphLabelSpec

    override fun visitGraphMatchLabelName(node: GraphMatch.Label.Name, ctx: Ctx) =
        translate(node) { metas ->
            graphLabelName(node.name, metas)
        }

    override fun visitGraphMatchLabelWildcard(node: GraphMatch.Label.Wildcard, ctx: Ctx) =
        translate(node) { metas ->
            graphLabelWildcard(metas)
        }

    override fun visitGraphMatchLabelNegation(node: GraphMatch.Label.Negation, ctx: Ctx) =
        translate(node) { metas ->
            val arg = visitGraphMatchLabel(node.arg, ctx)
            graphLabelNegation(arg, metas)
        }

    override fun visitGraphMatchLabelConj(node: GraphMatch.Label.Conj, ctx: Ctx) =
        translate(node) { metas ->
            val lhs = visitGraphMatchLabel(node.lhs, ctx)
            val rhs = visitGraphMatchLabel(node.rhs, ctx)
            graphLabelConj(lhs, rhs, metas)
        }

    override fun visitGraphMatchLabelDisj(node: GraphMatch.Label.Disj, ctx: Ctx) =
        translate(node) { metas ->
            val lhs = visitGraphMatchLabel(node.lhs, ctx)
            val rhs = visitGraphMatchLabel(node.rhs, ctx)
            graphLabelDisj(lhs, rhs, metas)
        }

    /**
     * TYPE
     */

    override fun visitType(node: Type, ctx: Ctx) = super.visitType(node, ctx) as PartiqlAst.Type

    override fun visitTypeNullType(node: Type.NullType, ctx: Ctx) = translate(node) { metas -> nullType(metas) }

    override fun visitTypeMissing(node: Type.Missing, ctx: Ctx) = translate(node) { metas -> missingType(metas) }

    override fun visitTypeBool(node: Type.Bool, ctx: Ctx) = translate(node) { metas -> booleanType(metas) }

    override fun visitTypeTinyint(node: Type.Tinyint, ctx: Ctx) =
        throw IllegalArgumentException("TINYINT type not supported")

    override fun visitTypeSmallint(node: Type.Smallint, ctx: Ctx) = translate(node) { metas -> smallintType(metas) }

    override fun visitTypeInt2(node: Type.Int2, ctx: Ctx) = translate(node) { metas -> smallintType(metas) }

    override fun visitTypeInt4(node: Type.Int4, ctx: Ctx) = translate(node) { metas -> integer4Type(metas) }

    override fun visitTypeBigint(node: Type.Bigint, ctx: Ctx) = translate(node) { metas -> integer8Type(metas) }

    override fun visitTypeInt8(node: Type.Int8, ctx: Ctx) = translate(node) { metas -> integer8Type(metas) }

    override fun visitTypeInt(node: Type.Int, ctx: Ctx) = translate(node) { metas -> integerType(metas) }

    override fun visitTypeReal(node: Type.Real, ctx: Ctx) = translate(node) { metas -> realType(metas) }

    override fun visitTypeFloat32(node: Type.Float32, ctx: Ctx) = translate(node) { metas -> floatType(null, metas) }

    override fun visitTypeFloat64(node: Type.Float64, ctx: Ctx) =
        translate(node) { metas -> doublePrecisionType(metas) }

    override fun visitTypeDecimal(node: Type.Decimal, ctx: Ctx) = translate(node) { metas ->
        decimalType(
            precision = node.precision?.toLong(),
            scale = node.scale?.toLong(),
            metas = metas,
        )
    }

    override fun visitTypeNumeric(node: Type.Numeric, ctx: Ctx) = translate(node) { metas ->
        numericType(
            precision = node.precision?.toLong(),
            scale = node.scale?.toLong(),
            metas = metas,
        )
    }

    override fun visitTypeChar(node: Type.Char, ctx: Ctx) =
        translate(node) { metas -> characterType(node.length?.toLong(), metas) }

    override fun visitTypeVarchar(node: Type.Varchar, ctx: Ctx) =
        translate(node) { metas -> characterVaryingType(node.length?.toLong(), metas) }

    override fun visitTypeString(node: Type.String, ctx: Ctx) = translate(node) { metas -> stringType(metas) }

    override fun visitTypeSymbol(node: Type.Symbol, ctx: Ctx) = translate(node) { metas -> symbolType(metas) }

    override fun visitTypeBit(node: Type.Bit, ctx: Ctx) = throw IllegalArgumentException("BIT type not supported")

    override fun visitTypeBitVarying(node: Type.BitVarying, ctx: Ctx) =
        throw IllegalArgumentException("BIT VARYING type not supported")

    override fun visitTypeByteString(node: Type.ByteString, ctx: Ctx) =
        throw IllegalArgumentException("BYTESTRING type not supported")

    override fun visitTypeBlob(node: Type.Blob, ctx: Ctx) = translate(node) { metas -> blobType(metas) }

    override fun visitTypeClob(node: Type.Clob, ctx: Ctx) = translate(node) { metas -> clobType(metas) }

    override fun visitTypeDate(node: Type.Date, ctx: Ctx) = translate(node) { metas -> dateType(metas) }

    override fun visitTypeTime(node: Type.Time, ctx: Ctx) =
        translate(node) { metas -> timeType(node.precision?.toLong(), metas) }

    override fun visitTypeTimeWithTz(node: Type.TimeWithTz, ctx: Ctx) =
        translate(node) { metas -> timeWithTimeZoneType(node.precision?.toLong(), metas) }

    override fun visitTypeTimestamp(node: Type.Timestamp, ctx: Ctx) =
        translate(node) { metas -> timestampType(node.precision?.toLong(), metas) }

    override fun visitTypeTimestampWithTz(node: Type.TimestampWithTz, ctx: Ctx) =
        translate(node) { metas -> timestampWithTimeZoneType(node.precision?.toLong(), metas) }

    override fun visitTypeInterval(node: Type.Interval, ctx: Ctx) =
        throw IllegalArgumentException("INTERVAL type not supported")

    override fun visitTypeBag(node: Type.Bag, ctx: Ctx) = translate(node) { metas -> bagType(metas) }

    override fun visitTypeList(node: Type.List, ctx: Ctx) = translate(node) { metas -> listType(metas) }

    override fun visitTypeSexp(node: Type.Sexp, ctx: Ctx) = translate(node) { metas -> sexpType(metas) }

    override fun visitTypeTuple(node: Type.Tuple, ctx: Ctx) = translate(node) { metas -> tupleType(metas) }

    override fun visitTypeStruct(node: Type.Struct, ctx: Ctx) = translate(node) { metas -> structType(metas) }

    override fun visitTypeAny(node: Type.Any, ctx: Ctx) = translate(node) { metas -> anyType(metas) }

    override fun visitTypeCustom(node: Type.Custom, ctx: Ctx) =
        translate(node) { metas -> customType(node.name.lowercase(), metas) }

    /**
     * HELPERS
     */

    private inline fun <reified S : PartiqlAst.PartiqlAstNode> List<AstNode>.translate(ctx: Ctx): List<S> =
        this.map { visit(it, ctx) as S }

    private inline fun <reified T : PartiqlAst.PartiqlAstNode> visitOrNull(node: AstNode?, ctx: Ctx): T? =
        node?.let { visit(it, ctx) as T }

    private fun Identifier.CaseSensitivity.toLegacyCaseSensitivity() = when (this) {
        Identifier.CaseSensitivity.SENSITIVE -> PartiqlAst.CaseSensitivity.CaseSensitive()
        Identifier.CaseSensitivity.INSENSITIVE -> PartiqlAst.CaseSensitivity.CaseInsensitive()
    }

    private fun Expr.Var.Scope.toLegacyScope() = when (this) {
        Expr.Var.Scope.DEFAULT -> PartiqlAst.ScopeQualifier.Unqualified()
        Expr.Var.Scope.LOCAL -> PartiqlAst.ScopeQualifier.LocalsFirst()
    }

    private fun SetQuantifier.toLegacySetQuantifier() = when (this) {
        SetQuantifier.ALL -> PartiqlAst.SetQuantifier.All()
        SetQuantifier.DISTINCT -> PartiqlAst.SetQuantifier.Distinct()
    }

    private fun DatetimeField.toLegacyDatetimePart(): PartiqlAst.Expr.Lit {
        val symbol = this.name.lowercase()
        return pig.lit(ionSymbol(symbol))
    }

    // Legacy AST models targets as expressions
    private fun visitPathUnpack(path: Path, ctx: Ctx): PartiqlAst.Expr {
        val ex = visitPath(path, ctx)
        return if (ex.steps.isEmpty()) ex.root else ex
    }

    private fun metaContainerOf(vararg metas: Meta): MetaContainer = metaContainerOf(metas.map { Pair(it.tag, it) })

    // Time Value is not an Expr.Lit in the legacy AST; needs special treatment.
    private fun TimeValue.toLegacyAst(metas: MetaContainer): PartiqlAst.Expr.LitTime {
        val v = this.value
        if (v == null) {
            throw IllegalArgumentException("TimeValue was null, but shouldn't have been")
        }
        val d = v.decimalSecond
        val seconds = d.toLong()
        val nano = d.subtract(BigDecimal(seconds)).scaleByPowerOfTen(9).toLong()
        val time = pig.timeValue(
            hour = v.hour.toLong(),
            minute = v.minute.toLong(),
            second = seconds,
            nano = nano,
            precision = v.decimalSecond.precision().toLong(),
            withTimeZone = v.timeZone != null,
            tzMinutes = v.timeZone?.let {
                when (it) {
                    is TimeZone.UtcOffset -> it.totalOffsetMinutes.toLong()
                    else -> 0
                }
            },
        )
        return pig.litTime(time, metas)
    }

    // Timestamp Value is not an Expr.Lit in the legacy AST; needs special treatment.
    private fun TimestampValue.toLegacyAst(metas: MetaContainer): PartiqlAst.Expr.Timestamp {
        val v = this.value
        if (v == null) {
            throw IllegalArgumentException("TimeStampValue was null, but shouldn't have been")
        }
        val timeZone = v.timeZone?.toLegacyAst(metas)
        val precision = v.decimalSecond.precision().toLong()
        return pig.timestamp(
            pig.timestampValue(
                v.year.toLong(), v.month.toLong(), v.day.toLong(),
                v.hour.toLong(), v.minute.toLong(), ionDecimal(Decimal.valueOf(v.decimalSecond)),
                timeZone, precision
            )
        )
    }

    // Date Value is not an Expr.Lit in the legacy AST; needs special treatment.
    private fun DateValue.toLegacyAst(metas: MetaContainer): PartiqlAst.Expr.Date {
        val v = this.value
        if (v == null) {
            throw IllegalArgumentException("DateValue was null, but shouldn't have been")
        }
        return pig.date(
            year = v.year.toLong(),
            month = v.month.toLong(),
            day = v.day.toLong(),
            metas = metas,
        )
    }

    private fun TimeZone.toLegacyAst(metas: MetaContainer): PartiqlAst.Timezone {
        return when (this) {
            TimeZone.UnknownTimeZone -> pig.unknownTimezone(metas)
            is TimeZone.UtcOffset -> pig.utcOffset(totalOffsetMinutes.toLong(), metas)
        }
    }
}
