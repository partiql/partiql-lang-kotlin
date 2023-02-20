package org.partiql.plan

import com.amazon.ionelement.api.MetaContainer
import org.partiql.lang.domains.PartiqlAst
import org.partiql.plan.ir.Binding
import org.partiql.plan.ir.Case
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
     * Helper so the visitor "body" looks like it has Rex as the return value
     */
    private fun visit(node: PartiqlAst.PartiqlAstNode, block: () -> Rex) = Ctx(node, block())

    /**
     * !! DEFAULT VISIT !!
     *
     * The PIG visitor doesn't give us control over the default "visit"
     * We can override walkMetas (which appears on every super.walk call) as if it were a default "visit"
     * MetaContainer isn't actually a domain node, and we don't have any context as to where the MetaContainer
     * is coming from which is why the current node is stuffed into Ctx
     */
    override fun walkMetas(node: MetaContainer, ctx: Ctx) = Planner.unsupported(ctx.node)

    override fun walkExprMissing(node: PartiqlAst.Expr.Missing, accumulator: Ctx): Ctx {
        TODO()
    }

    override fun walkExprLit(node: PartiqlAst.Expr.Lit, accumulator: Ctx) = visit(node) {
        Rex.Lit(node.value)
    }

    override fun walkExprId(node: PartiqlAst.Expr.Id, accumulator: Ctx) = visit(node) {
        Rex.Id(
            name = node.name.text,
            case = convertCase(node.case),
            qualifier = when (node.qualifier) {
                is PartiqlAst.ScopeQualifier.LocalsFirst -> Rex.Id.Qualifier.LOCALS_FIRST
                is PartiqlAst.ScopeQualifier.Unqualified -> Rex.Id.Qualifier.UNQUALIFIED
            }
        )
    }

    override fun walkExprPath(node: PartiqlAst.Expr.Path, accumulator: Ctx) = visit(node) {
        Rex.Path(
            root = convert(node.root),
            steps = node.steps.map {
                when (it) {
                    is PartiqlAst.PathStep.PathExpr -> Step.Rex(
                        index = convert(it.index),
                        case = convertCase(it.case)
                    )
                    is PartiqlAst.PathStep.PathUnpivot -> Step.Unpivot()
                    is PartiqlAst.PathStep.PathWildcard -> Step.Wildcard()
                }
            }
        )
    }

    override fun walkExprNot(node: PartiqlAst.Expr.Not, accumulator: Ctx) = visit(node) {
        Rex.Unary(
            rex = convert(node.expr),
            op = Rex.Unary.Op.NOT,
        )
    }

    override fun walkExprPos(node: PartiqlAst.Expr.Pos, accumulator: Ctx) = visit(node) {
        Rex.Unary(
            rex = convert(node.expr),
            op = Rex.Unary.Op.POS,
        )
    }

    override fun walkExprNeg(node: PartiqlAst.Expr.Neg, accumulator: Ctx) = visit(node) {
        Rex.Unary(
            rex = convert(node.expr),
            op = Rex.Unary.Op.NEG,
        )
    }

    override fun walkExprPlus(node: PartiqlAst.Expr.Plus, accumulator: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.PLUS,
        )
    }

    override fun walkExprMinus(node: PartiqlAst.Expr.Minus, accumulator: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.MINUS,
        )
    }

    override fun walkExprTimes(node: PartiqlAst.Expr.Times, accumulator: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.TIMES,
        )
    }

    override fun walkExprDivide(node: PartiqlAst.Expr.Divide, accumulator: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.DIV,
        )
    }

    override fun walkExprModulo(node: PartiqlAst.Expr.Modulo, accumulator: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.MODULO,
        )
    }

    override fun walkExprConcat(node: PartiqlAst.Expr.Concat, accumulator: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.CONCAT,
        )
    }

    override fun walkExprAnd(node: PartiqlAst.Expr.And, accumulator: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.AND,
        )
    }

    override fun walkExprOr(node: PartiqlAst.Expr.Or, accumulator: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.OR,
        )
    }

    override fun walkExprEq(node: PartiqlAst.Expr.Eq, accumulator: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.EQ,
        )
    }

    override fun walkExprNe(node: PartiqlAst.Expr.Ne, accumulator: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.NEQ,
        )
    }

    override fun walkExprGt(node: PartiqlAst.Expr.Gt, accumulator: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.GT,
        )
    }

    override fun walkExprGte(node: PartiqlAst.Expr.Gte, accumulator: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.GTE,
        )
    }

    override fun walkExprLt(node: PartiqlAst.Expr.Lt, accumulator: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.LT,
        )
    }

    override fun walkExprLte(node: PartiqlAst.Expr.Lte, accumulator: Ctx) = visit(node) {
        Rex.Binary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.LTE,
        )
    }

    override fun walkExprLike(node: PartiqlAst.Expr.Like, accumulator: Ctx) = visit(node) {
        when (node.escape) {
            null -> Rex.Call(
                id = "like",
                args = listOf(convert(node.value), convert(node.pattern))
            )
            else -> Rex.Call(
                id = "like_escape",
                args = listOf(convert(node.value), convert(node.pattern), convert(node.escape!!))
            )
        }
    }

    override fun walkExprBetween(node: PartiqlAst.Expr.Between, accumulator: Ctx) = visit(node) {
        Rex.Call(
            id = "between",
            args = listOf(convert(node.value), convert(node.from), convert(node.to)),
        )
    }

    override fun walkExprInCollection(node: PartiqlAst.Expr.InCollection, accumulator: Ctx) = visit(node) {
        val lhs = convert(node.operands[0])
        var rhs = convert(node.operands[1])
        if (rhs is Rex.Query.Scalar.Coerce) {
            rhs = rhs.query // unpack a scalar subquery coercion
        }
        Rex.Call(
            id = "in_collection",
            args = listOf(lhs, rhs),
        )
    }

    override fun walkExprStruct(node: PartiqlAst.Expr.Struct, accumulator: Ctx) = visit(node) {
        Rex.Struct(
            fields = node.fields.map {
                Binding(
                    name = convert(it.first),
                    rex = convert(it.second)
                )
            }
        )
    }

    override fun walkExprBag(node: PartiqlAst.Expr.Bag, accumulator: Ctx) = visit(node) {
        Rex.Collection(
            type = Rex.Collection.Type.BAG,
            values = convert(node.values),
        )
    }

    override fun walkExprList(node: PartiqlAst.Expr.List, accumulator: Ctx) = visit(node) {
        Rex.Collection(
            type = Rex.Collection.Type.LIST,
            values = convert(node.values),
        )
    }

    override fun walkExprCall(node: PartiqlAst.Expr.Call, accumulator: Ctx) = visit(node) {
        Rex.Call(
            id = node.funcName.text,
            args = convert(node.args),
        )
    }

    override fun walkExprCallAgg(node: PartiqlAst.Expr.CallAgg, accumulator: Ctx) = visit(node) {
        Rex.Agg(
            id = node.funcName.text,
            args = listOf(convert(node.arg)),
            modifier = when (node.setq) {
                is PartiqlAst.SetQuantifier.All -> Rex.Agg.Modifier.ALL
                is PartiqlAst.SetQuantifier.Distinct -> Rex.Agg.Modifier.DISTINCT
            }
        )
    }

    override fun walkExprSelect(node: PartiqlAst.Expr.Select, accumulator: Ctx) = visit(node) {
        when (val query = RelConverter.convert(node)) {
            is Rex.Query.Collection -> Rex.Query.Scalar.Coerce(query)
            is Rex.Query.Scalar -> query
        }
    }

    private fun convertCase(case: PartiqlAst.CaseSensitivity) = when (case) {
        is PartiqlAst.CaseSensitivity.CaseInsensitive -> Case.INSENSITIVE
        is PartiqlAst.CaseSensitivity.CaseSensitive -> Case.SENSITIVE
    }
}
