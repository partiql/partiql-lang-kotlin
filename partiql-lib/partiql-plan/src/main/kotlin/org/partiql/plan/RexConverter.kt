package org.partiql.plan

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.ionNull
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.types.StaticType
import org.partiql.plan.ir.Case
import org.partiql.plan.ir.Field
import org.partiql.plan.ir.Rex
import org.partiql.plan.ir.Step

/**
 * Some workarounds for transforming a PIG tree without having to create another visitor:
 * - Using the VisitorFold with ctx struct to create a parameterized return and scoped arguments/context
 * - Using walks to control traversal, also walks have generated if/else blocks for sum types so its more useful
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
internal object RexConverter : PartiqlAst.VisitorFold<RexConverter.Ctx>() {

    /**
     * Workaround for PIG visitor where:
     *  - Args != null when Ctx is the accumulator IN
     *  - Rex  != null when Ctx is the accumulator OUT
     *
     * Destructuring ordering chosen for val (in, out) = ...
     *
     * @property node   Node to invoke the behavior on
     * @property rex    Return value
     */
    data class Ctx(
        val node: PartiqlAst.PartiqlAstNode,
        var rex: Rex? = null,
    )

    /**
     * Read as `val rex = node.accept(visitor = RexVisitor.INSTANCE, args = emptyList())`
     * Only works because RexConverter errs for all non Expr AST nodes, and Expr is one sum type.
     */
    internal fun convert(node: PartiqlAst.Expr) = RexConverter.walkExpr(node, Ctx(node)).rex!!

    /**
     * List version of hacked "accept"
     */
    internal fun convert(nodes: List<PartiqlAst.Expr>) = nodes.map { convert(it) }

    /**
     * Vararg ''
     */
    internal fun convert(vararg nodes: PartiqlAst.Expr) = nodes.map { convert(it) }

    /**
     * Helper so the visitor "body" looks like it has Rex as the return value
     */
    private inline fun visit(node: PartiqlAst.PartiqlAstNode, block: () -> Rex) = Ctx(node, block())

    /**
     * !! DEFAULT VISIT !!
     *
     * The PIG visitor doesn't give us control over the default "visit"
     * We can override walkMetas (which appears on every super.walk call) as if it were a default "visit"
     * MetaContainer isn't actually a domain node, and we don't have any context as to where the MetaContainer
     * is coming from which is why the current node is stuffed into Ctx
     */
    override fun walkMetas(node: MetaContainer, ctx: Ctx) = Planner.unsupported(ctx.node)

    override fun walkExprMissing(node: PartiqlAst.Expr.Missing, ctx: Ctx) = visit(node) {
        Rex.Lit(ionNull(), StaticType.MISSING)
    }

    override fun walkExprLit(node: PartiqlAst.Expr.Lit, ctx: Ctx) = visit(node) {
        val ionType = node.value.type.toIonType()
        Rex.Lit(
            value = node.value,
            type = TypeConverter.convert(ionType)
        )
    }

    override fun walkExprId(node: PartiqlAst.Expr.Id, ctx: Ctx) = visit(node) {
        Rex.Id(
            name = node.name.text,
            case = convertCase(node.case),
            qualifier = when (node.qualifier) {
                is PartiqlAst.ScopeQualifier.LocalsFirst -> Rex.Id.Qualifier.LOCALS_FIRST
                is PartiqlAst.ScopeQualifier.Unqualified -> Rex.Id.Qualifier.UNQUALIFIED
            },
            type = null,
        )
    }

    override fun walkExprPath(node: PartiqlAst.Expr.Path, ctx: Ctx) = visit(node) {
        Rex.Path(
            root = convert(node.root),
            steps = node.steps.map {
                when (it) {
                    is PartiqlAst.PathStep.PathExpr -> Step.Key(
                        value = convert(it.index),
                        case = convertCase(it.case)
                    )
                    is PartiqlAst.PathStep.PathUnpivot -> Step.Unpivot()
                    is PartiqlAst.PathStep.PathWildcard -> Step.Wildcard()
                }
            },
            type = null,
        )
    }

    override fun walkExprNot(node: PartiqlAst.Expr.Not, ctx: Ctx) = visit(node) {
        Rex.Unary(
            value = convert(node.expr),
            op = Rex.Unary.Op.NOT,
            type = StaticType.BOOL,
        )
    }

    override fun walkExprPos(node: PartiqlAst.Expr.Pos, ctx: Ctx) = visit(node) {
        Rex.Unary(
            value = convert(node.expr),
            op = Rex.Unary.Op.POS,
            type = StaticType.NUMERIC,
        )
    }

    override fun walkExprNeg(node: PartiqlAst.Expr.Neg, ctx: Ctx) = visit(node) {
        Rex.Unary(
            value = convert(node.expr),
            op = Rex.Unary.Op.NEG,
            type = StaticType.NUMERIC,
        )
    }

    override fun walkExprPlus(node: PartiqlAst.Expr.Plus, ctx: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.PLUS,
            type = StaticType.NUMERIC,
        )
    }

    override fun walkExprMinus(node: PartiqlAst.Expr.Minus, ctx: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.MINUS,
            type = StaticType.NUMERIC,
        )
    }

    override fun walkExprTimes(node: PartiqlAst.Expr.Times, ctx: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.TIMES,
            type = StaticType.NUMERIC,
        )
    }

    override fun walkExprDivide(node: PartiqlAst.Expr.Divide, ctx: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.DIV,
            type = StaticType.NUMERIC,
        )
    }

    override fun walkExprModulo(node: PartiqlAst.Expr.Modulo, ctx: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.MODULO,
            type = StaticType.NUMERIC,
        )
    }

    override fun walkExprConcat(node: PartiqlAst.Expr.Concat, ctx: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.CONCAT,
            type = StaticType.TEXT,
        )
    }

    override fun walkExprAnd(node: PartiqlAst.Expr.And, ctx: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.AND,
            type = StaticType.BOOL,
        )
    }

    override fun walkExprOr(node: PartiqlAst.Expr.Or, ctx: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.OR,
            type = StaticType.BOOL,
        )
    }

    override fun walkExprEq(node: PartiqlAst.Expr.Eq, ctx: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.EQ,
            type = StaticType.BOOL,
        )
    }

    override fun walkExprNe(node: PartiqlAst.Expr.Ne, ctx: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.NEQ,
            type = StaticType.BOOL,
        )
    }

    override fun walkExprGt(node: PartiqlAst.Expr.Gt, ctx: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.GT,
            type = StaticType.BOOL,
        )
    }

    override fun walkExprGte(node: PartiqlAst.Expr.Gte, ctx: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.GTE,
            type = StaticType.BOOL,
        )
    }

    override fun walkExprLt(node: PartiqlAst.Expr.Lt, ctx: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.LT,
            type = StaticType.BOOL,
        )
    }

    override fun walkExprLte(node: PartiqlAst.Expr.Lte, ctx: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.LTE,
            type = StaticType.BOOL,
        )
    }

    override fun walkExprLike(node: PartiqlAst.Expr.Like, ctx: Ctx) = visit(node) {
        when (node.escape) {
            null -> Rex.Call(
                id = "like",
                args = convert(node.value, node.pattern),
                type = StaticType.BOOL,
            )
            else -> Rex.Call(
                id = "like_escape",
                args = convert(node.value, node.pattern, node.escape!!),
                type = StaticType.BOOL,
            )
        }
    }

    override fun walkExprBetween(node: PartiqlAst.Expr.Between, ctx: Ctx) = visit(node) {
        Rex.Call(
            id = "between",
            args = convert(node.value, node.from, node.to),
            type = StaticType.BOOL,
        )
    }

    override fun walkExprInCollection(node: PartiqlAst.Expr.InCollection, ctx: Ctx) = visit(node) {
        val lhs = convert(node.operands[0])
        var rhs = convert(node.operands[1])
        if (rhs is Rex.Query.Scalar.Coerce) {
            rhs = rhs.query // unpack a scalar subquery coercion
        }
        Rex.Call(
            id = "in_collection",
            args = listOf(lhs, rhs),
            type = StaticType.BOOL,
        )
    }

    override fun walkExprStruct(node: PartiqlAst.Expr.Struct, ctx: Ctx) = visit(node) {
        Rex.Tuple(
            fields = node.fields.map {
                Field(
                    name = convert(it.first),
                    value = convert(it.second)
                )
            },
            type = StaticType.STRUCT,
        )
    }

    override fun walkExprBag(node: PartiqlAst.Expr.Bag, ctx: Ctx) = visit(node) {
        Rex.Collection.Bag(
            values = convert(node.values),
            type = null,
        )
    }

    override fun walkExprList(node: PartiqlAst.Expr.List, ctx: Ctx) = visit(node) {
        Rex.Collection.Array(
            values = convert(node.values),
            type = null,
        )
    }

    override fun walkExprCall(node: PartiqlAst.Expr.Call, ctx: Ctx) = visit(node) {
        Rex.Call(
            id = node.funcName.text,
            args = convert(node.args),
            type = null,
        )
    }

    override fun walkExprCallAgg(node: PartiqlAst.Expr.CallAgg, ctx: Ctx) = visit(node) {
        Rex.Agg(
            id = node.funcName.text,
            args = listOf(convert(node.arg)),
            modifier = when (node.setq) {
                is PartiqlAst.SetQuantifier.All -> Rex.Agg.Modifier.ALL
                is PartiqlAst.SetQuantifier.Distinct -> Rex.Agg.Modifier.DISTINCT
            },
            type = StaticType.NUMERIC,
        )
    }

    override fun walkExprIsType(node: PartiqlAst.Expr.IsType, ctx: Ctx) = visit(node) {
        Rex.Call(
            id = "is_type",
            args = listOf(convert(node.value)),
            type = StaticType.BOOL,
        )
    }

    override fun walkExprSimpleCase(node: PartiqlAst.Expr.SimpleCase, ctx: Ctx) = visit(node) {
        val cond = convert(node.expr)
        val cases = node.cases.pairs.flatMap { convert(it.first, it.second) }
        val args = listOf(cond) + cases
        when (val default = node.default) {
            null -> Rex.Call(
                id = "case",
                args = args,
                type = null,
            )
            else -> Rex.Call(
                id = "case_default",
                args = args + convert(default),
                type = null,
            )
        }
    }

    override fun walkExprSearchedCase(node: PartiqlAst.Expr.SearchedCase, ctx: Ctx) = visit(node) {
        val args = node.cases.pairs.flatMap { convert(it.first, it.second) }
        when (val default = node.default) {
            null -> Rex.Call(
                id = "searched_case",
                args = args,
                type = null,
            )
            else -> Rex.Call(
                id = "searched_case_default",
                args = args + convert(default),
                type = null,
            )
        }
    }

    override fun walkExprDate(node: PartiqlAst.Expr.Date, ctx: Ctx): Ctx {
        error("Date class undetermined at the moment")
    }

    override fun walkExprLitTime(node: PartiqlAst.Expr.LitTime, ctx: Ctx): Ctx {
        error("Time class undetermined at the moment")
    }

    override fun walkExprBagOp(node: PartiqlAst.Expr.BagOp, ctx: Ctx) = visit(node) {
        val prefix = when (node.quantifier) {
            is PartiqlAst.SetQuantifier.All -> "bag"
            is PartiqlAst.SetQuantifier.Distinct -> "set"
        }
        // Hack for UNION / INTERSECT / EXCEPT because they are missing from the parser
        val suffix = when (node.op) {
            is PartiqlAst.BagOpType.Except,
            is PartiqlAst.BagOpType.OuterExcept -> "except_outer"
            is PartiqlAst.BagOpType.Intersect,
            is PartiqlAst.BagOpType.OuterIntersect -> "intersect_outer"
            is PartiqlAst.BagOpType.Union,
            is PartiqlAst.BagOpType.OuterUnion -> "union_outer"
        }
        Rex.Call(
            id = "$prefix::$suffix",
            args = convert(node.operands),
            type = StaticType.BAG,
        )
    }

    override fun walkExprCast(node: PartiqlAst.Expr.Cast, ctx: Ctx) = visit(node) {
        Rex.Call(
            id = "cast",
            args = listOf(convert(node.value)),
            type = TypeConverter.convert(node.asType),
        )
    }

    override fun walkExprCanCast(node: PartiqlAst.Expr.CanCast, ctx: Ctx) = visit(node) {
        Rex.Call(
            id = "can_cast",
            args = listOf(convert(node.value)),
            type = StaticType.BOOL,
        )
    }

    override fun walkExprCanLosslessCast(node: PartiqlAst.Expr.CanLosslessCast, ctx: Ctx) = visit(node) {
        Rex.Call(
            id = "can_lossless_cast",
            args = listOf(convert(node.value)),
            type = StaticType.BOOL,
        )
    }

    override fun walkExprNullIf(node: PartiqlAst.Expr.NullIf, ctx: Ctx) = visit(node) {
        Rex.Call(
            id = "can_lossless_cast",
            args = convert(node.expr1, node.expr2),
            type = StaticType.BOOL,
        )
    }

    override fun walkExprCoalesce(node: PartiqlAst.Expr.Coalesce, ctx: Ctx) = visit(node) {
        Rex.Call(
            id = "coalesce",
            args = convert(node.args),
            type = null,
        )
    }

    override fun walkExprSelect(node: PartiqlAst.Expr.Select, ctx: Ctx) = visit(node) {
        when (val query = RelConverter.convert(node)) {
            is Rex.Query.Collection -> Rex.Query.Scalar.Coerce(query, null)
            is Rex.Query.Scalar -> query
        }
    }

    private fun convertCase(case: PartiqlAst.CaseSensitivity) = when (case) {
        is PartiqlAst.CaseSensitivity.CaseInsensitive -> Case.INSENSITIVE
        is PartiqlAst.CaseSensitivity.CaseSensitive -> Case.SENSITIVE
    }
}
