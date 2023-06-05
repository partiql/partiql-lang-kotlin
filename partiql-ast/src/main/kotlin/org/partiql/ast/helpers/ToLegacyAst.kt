@file:JvmName("ToLegacyAst")

package org.partiql.ast.helpers

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.ionNull
import com.amazon.ionelement.api.ionSymbol
import com.amazon.ionelement.api.metaContainerOf
import org.partiql.ast.AstNode
import org.partiql.ast.DatetimeField
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.SetOp
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Statement
import org.partiql.ast.Type
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.lang.ast.IsListParenthesizedMeta
import org.partiql.lang.ast.IsValuesExprMeta
import org.partiql.lang.ast.Meta
import org.partiql.lang.domains.PartiqlAst

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
private class AstTranslator(val metas: Map<String, MetaContainer>) : AstBaseVisitor<PartiqlAst.PartiqlAstNode, Ctx>() {

    private val pig = PartiqlAst.BUILDER()

    override fun defaultReturn(node: AstNode, ctx: Ctx): Nothing {
        val fromClass = node::class.qualifiedName
        val toClass = PartiqlAst.PartiqlAstNode::class.qualifiedName
        throw IllegalArgumentException("$fromClass cannot be translated to $toClass")
    }

    /**
     * Attach Metas if-any
     */
    private inline fun <T : PartiqlAst.PartiqlAstNode> translate(
        node: AstNode,
        block: PartiqlAst.Builder.(metas: MetaContainer) -> T,
    ): T {
        val metas = metas[node._id] ?: emptyMetaContainer()
        return pig.block(metas)
    }

    override fun visitStatement(node: Statement, ctx: Ctx) = super.visitStatement(node, ctx) as PartiqlAst.Statement

    override fun visitStatementQuery(node: Statement.Query, ctx: Ctx) = translate(node) { metas ->
        val expr = visitExpr(node.expr, ctx)
        query(expr, metas)
    }

    override fun visitExpr(node: Expr, ctx: Ctx): PartiqlAst.Expr = super.visitExpr(node, ctx) as PartiqlAst.Expr

    override fun visitExprMissingValue(node: Expr.MissingValue, ctx: Ctx) = translate(node) { metas ->
        lit(ionNull().withAnnotations("\$missing"), metas)
    }

    override fun visitExprNullValue(node: Expr.NullValue, ctx: Ctx) = translate(node) { metas ->
        lit(ionNull(), metas)
    }

    override fun visitExprLiteral(node: Expr.Literal, ctx: Ctx) = translate(node) { metas ->
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
        val funcName = (node.function as Identifier.Symbol).symbol
        val args = node.args.translate<PartiqlAst.Expr>(ctx)
        call(funcName, args, metas)
    }

    override fun visitExprAgg(node: Expr.Agg, ctx: Ctx) = translate(node) { metas ->
        if (node.args.size != 1) {
            error("Legacy `call_agg` must have exactly one argument")
        }
        if (node.function is Identifier.Qualified) {
            error("Qualified identifiers are not allowed in legacy AST `call_agg` function identifiers")
        }
        // Legacy parser/ast always inserts ALL quantifier
        val setq = node.setq?.toLegacySetQuantifier() ?: all()
        val funcName = (node.function as Identifier.Symbol).symbol
        val arg = visitExpr(node.args[0], ctx)
        callAgg(setq, funcName, arg, metas)
    }

    override fun visitExprUnary(node: Expr.Unary, ctx: Ctx) = translate(node) { metas ->
        val expr = visitExpr(node.expr, ctx)
        when (node.op) {
            Expr.Unary.Op.NOT -> not(expr, metas)
            Expr.Unary.Op.POS -> pos(expr, metas)
            Expr.Unary.Op.NEG -> neg(expr, metas)
        }
    }

    override fun visitExprBinary(node: Expr.Binary, ctx: Ctx) = translate(node) { metas ->
        val lhs = visitExpr(node.lhs, ctx)
        val rhs = visitExpr(node.rhs, ctx)
        val operands = listOf(lhs, rhs)
        when (node.op) {
            Expr.Binary.Op.PLUS -> plus(operands, metas)
            Expr.Binary.Op.MINUS -> minus(operands, metas)
            Expr.Binary.Op.TIMES -> times(operands, metas)
            Expr.Binary.Op.DIVIDE -> divide(operands, metas)
            Expr.Binary.Op.MODULO -> modulo(operands, metas)
            Expr.Binary.Op.CONCAT -> concat(operands, metas)
            Expr.Binary.Op.AND -> and(operands, metas)
            Expr.Binary.Op.OR -> or(operands, metas)
            Expr.Binary.Op.EQ -> eq(operands, metas)
            Expr.Binary.Op.NE -> ne(operands, metas)
            Expr.Binary.Op.GT -> gt(operands, metas)
            Expr.Binary.Op.GTE -> gte(operands, metas)
            Expr.Binary.Op.LT -> lt(operands, metas)
            Expr.Binary.Op.LTE -> lte(operands, metas)
        }
    }

    override fun visitExprPath(node: Expr.Path, ctx: Ctx) = translate(node) { metas ->
        val root = visitExpr(node.root, ctx)
        val steps = node.steps.map { visitExprPathStep(it, ctx) }
        path(root, steps, metas)
    }

    override fun visitExprPathStep(node: Expr.Path.Step, ctx: Ctx) =
        super.visitExprPathStep(node, ctx) as PartiqlAst.PathStep

    override fun visitExprPathStepIndex(node: Expr.Path.Step.Index, ctx: Ctx) = translate(node) { metas ->
        val index = visitExpr(node.key, ctx)
        // Legacy AST adds a required CaseSensitivity to every indexed path step.
        // This doesn't make sense unless the expression is an expression variable whose identifier.
        val case = when (index) {
            is PartiqlAst.Expr.Id -> index.case
            else -> PartiqlAst.CaseSensitivity.CaseInsensitive()
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

    override fun visitExprDate(node: Expr.Date, ctx: Ctx) = translate(node) { metas ->
        TODO()
    }

    override fun visitExprTime(node: Expr.Time, ctx: Ctx) = translate(node) { metas ->
        TODO()
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
        val first = visitExpr(node.expr, ctx)
        val second = visitExpr(node.condition, ctx)
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
        val spec = node.spec?.toString()?.toLowerCase()
        val chars = node.chars?.let { visitExpr(it, ctx) }
        val value = visitExpr(node.value, ctx)
        if (spec != null) operands.add(id(spec, caseInsensitive(), unqualified()))
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

    override fun visitExprCanCast(node: Expr.CanCast, ctx: Ctx) = translate(node) { metas ->
        val value = visitExpr(node.value, ctx)
        val asType = visitType(node.asType, ctx)
        canCast(value, asType, metas)
    }

    override fun visitExprCanLosslessCast(node: Expr.CanLosslessCast, ctx: Ctx) = translate(node) { metas ->
        val value = visitExpr(node.value, ctx)
        val asType = visitType(node.asType, ctx)
        canLosslessCast(value, asType, metas)
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

    override fun visitExprOuterSetOp(node: Expr.OuterSetOp, ctx: Ctx) = translate(node) { metas ->
        val lhs = visitExpr(node.lhs, ctx)
        val rhs = visitExpr(node.rhs, ctx)
        val op = when (node.type.type) {
            SetOp.Type.UNION -> outerUnion()
            SetOp.Type.INTERSECT -> TODO()
            SetOp.Type.EXCEPT -> TODO()
        }
        val setq = node.type.setq?.toLegacySetQuantifier() ?: all()
        val operands = listOf(lhs, rhs)
        bagOp(op, setq, operands, metas)
    }

    override fun visitExprSFW(node: Expr.SFW, ctx: Ctx) = translate(node) { metas ->
        TODO()
    }

    override fun visitExprSFWSetOp(node: Expr.SFW.SetOp, ctx: Ctx) = translate(node) { metas ->
        TODO()
    }

    override fun visitExprMatch(node: Expr.Match, ctx: Ctx) = translate(node) { metas ->
        TODO()
    }

    override fun visitExprWindow(node: Expr.Window, ctx: Ctx) = translate(node) { metas ->
        TODO()
    }

    override fun visitExprWindowOver(node: Expr.Window.Over, ctx: Ctx) = translate(node) { metas ->
        TODO()
    }

    override fun visitType(node: Type, ctx: Ctx) = translate(node) { metas ->
        val parameters = node.parameters.map { it.asAnyElement().longValue }
        when (node.identifier) {
            "null" -> nullType(metas)
            "missing" -> missingType(metas)
            "any" -> anyType(metas)
            "blob" -> blobType(metas)
            "bool" -> booleanType(metas)
            "bag" -> bagType(metas)
            "array" -> listType(metas)
            "sexp" -> sexpType(metas)
            "date" -> dateType(metas)
            "time" -> timeType(null, metas)
            "timestamp" -> timestampType(metas)
            "numeric" -> {
                when (parameters.size) {
                    0 -> numericType(null, null, metas)
                    1 -> numericType(parameters[0], null, metas)
                    2 -> numericType(parameters[0], parameters[1], metas)
                    else -> throw IllegalArgumentException("Too many parameters for numeric type")
                }
            }
            "decimal" -> {
                when (parameters.size) {
                    0 -> decimalType(null, null, metas)
                    1 -> decimalType(parameters[0], null, metas)
                    2 -> decimalType(parameters[0], parameters[1], metas)
                    else -> throw IllegalArgumentException("Too many parameters for decimal type")
                }
            }
            "float" -> floatType(null, metas)
            "int" -> integerType(metas)
            "varchar" -> {
                if (parameters.isNotEmpty()) {
                    characterVaryingType(parameters[0], metas)
                } else {
                    characterVaryingType(null, metas)
                }
            }
            "tuple" -> structType(metas)
            "string" -> stringType(metas)
            else -> customType(node.identifier.toLowerCase(), metas)
        }
    }

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
        val symbol = this.toString().toLowerCase()
        return pig.lit(ionSymbol(symbol))
    }

    private fun metaContainerOf(vararg metas: Meta): MetaContainer = metaContainerOf(metas.map { Pair(it.tag, it) })
}
