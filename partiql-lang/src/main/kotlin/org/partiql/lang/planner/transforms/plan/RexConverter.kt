package org.partiql.lang.planner.transforms.plan

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.ionNull
import org.partiql.errors.ErrorCode
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.builtins.ExprFunctionCurrentUser
import org.partiql.lang.eval.err
import org.partiql.lang.eval.errorContextFrom
import org.partiql.lang.planner.transforms.AstToPlan
import org.partiql.lang.planner.transforms.plan.PlanTyper.isProjectAll
import org.partiql.plan.Case
import org.partiql.plan.Plan
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.types.StaticType
import java.util.Locale

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
     * List version of `accept`
     */
    private fun convert(nodes: List<PartiqlAst.Expr>) = nodes.map { convert(it) }

    /**
     * Vararg version of `accept`
     */
    private fun convert(vararg nodes: PartiqlAst.Expr) = nodes.map { convert(it) }

    private fun arg(name: String, node: PartiqlAst.PartiqlAstNode) = when (node) {
        is PartiqlAst.Expr -> Plan.argValue(
            name = name,
            value = convert(node),
        )
        is PartiqlAst.Type -> Plan.argType(
            name = name,
            type = TypeConverter.convert(node)
        )
        else -> error("Argument must be of type PartiqlAst.Expr or PartiqlAst.Type, found ${node::class.qualifiedName}")
    }

    /**
     * Convert a list of arguments to arg0, ...., argN
     */
    private fun args(nodes: List<PartiqlAst.Expr>) = args(*nodes.toTypedArray())

    /**
     * Convert arguments to arg0, ...., argN
     */
    private fun args(vararg nodes: PartiqlAst.PartiqlAstNode?) =
        nodes.filterNotNull().mapIndexed { i, arg -> arg("arg$i", arg) }

    /**
     * Convert keyword pairs of arguments
     */
    private fun args(vararg args: Pair<String, PartiqlAst.PartiqlAstNode>) = args.map { arg(it.first, it.second) }

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
    override fun walkMetas(node: MetaContainer, ctx: Ctx) = AstToPlan.unsupported(ctx.node)

    override fun walkExprMissing(node: PartiqlAst.Expr.Missing, ctx: Ctx) = visit(node) {
        Plan.rexLit(ionNull(), StaticType.MISSING)
    }

    override fun walkExprLit(node: PartiqlAst.Expr.Lit, ctx: Ctx) = visit(node) {
        val ionType = node.value.type.toIonType()
        Plan.rexLit(
            value = node.value,
            type = TypeConverter.convert(ionType)
        )
    }

    override fun walkExprSessionAttribute(node: PartiqlAst.Expr.SessionAttribute, accumulator: Ctx) = visit(node) {
        val functionName = when (node.value.text.uppercase(Locale.getDefault())) {
            EvaluationSession.Constants.CURRENT_USER_KEY -> ExprFunctionCurrentUser.FUNCTION_NAME
            else -> err(
                "Unsupported session attribute: ${node.value.text}",
                errorCode = ErrorCode.SEMANTIC_PROBLEM,
                errorContext = errorContextFrom(node.metas),
                internal = false
            )
        }
        Plan.rexCall(
            id = functionName,
            args = emptyList(),
            type = null
        )
    }

    override fun walkExprId(node: PartiqlAst.Expr.Id, ctx: Ctx) = visit(node) {
        Plan.rexId(
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
        Plan.rexPath(
            root = convert(node.root),
            steps = node.steps.map {
                when (it) {
                    is PartiqlAst.PathStep.PathExpr -> Plan.stepKey(
                        value = convert(it.index),
                        case = convertCase(it.case)
                    )
                    is PartiqlAst.PathStep.PathUnpivot -> Plan.stepUnpivot()
                    is PartiqlAst.PathStep.PathWildcard -> Plan.stepWildcard()
                }
            },
            type = null,
        )
    }

    override fun walkExprNot(node: PartiqlAst.Expr.Not, ctx: Ctx) = visit(node) {
        Plan.rexUnary(
            value = convert(node.expr),
            op = Rex.Unary.Op.NOT,
            type = StaticType.BOOL,
        )
    }

    override fun walkExprPos(node: PartiqlAst.Expr.Pos, ctx: Ctx) = visit(node) {
        Plan.rexUnary(
            value = convert(node.expr),
            op = Rex.Unary.Op.POS,
            type = StaticType.NUMERIC,
        )
    }

    override fun walkExprNeg(node: PartiqlAst.Expr.Neg, ctx: Ctx) = visit(node) {
        Plan.rexUnary(
            value = convert(node.expr),
            op = Rex.Unary.Op.NEG,
            type = StaticType.NUMERIC,
        )
    }

    override fun walkExprPlus(node: PartiqlAst.Expr.Plus, ctx: Ctx) = visit(node) {
        Plan.rexBinary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.PLUS,
            type = StaticType.NUMERIC,
        )
    }

    override fun walkExprMinus(node: PartiqlAst.Expr.Minus, ctx: Ctx) = visit(node) {
        Plan.rexBinary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.MINUS,
            type = StaticType.NUMERIC,
        )
    }

    override fun walkExprTimes(node: PartiqlAst.Expr.Times, ctx: Ctx) = visit(node) {
        Plan.rexBinary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.TIMES,
            type = StaticType.NUMERIC,
        )
    }

    override fun walkExprDivide(node: PartiqlAst.Expr.Divide, ctx: Ctx) = visit(node) {
        Plan.rexBinary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.DIV,
            type = StaticType.NUMERIC,
        )
    }

    override fun walkExprModulo(node: PartiqlAst.Expr.Modulo, ctx: Ctx) = visit(node) {
        Plan.rexBinary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.MODULO,
            type = StaticType.NUMERIC,
        )
    }

    override fun walkExprBitwiseAnd(node: PartiqlAst.Expr.BitwiseAnd, accumulator: Ctx) = visit(node) {
        Plan.rexBinary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.BITWISE_AND,
            type = StaticType.unionOf(
                StaticType.INT2, StaticType.INT4, StaticType.INT8, StaticType.INT
            )
        )
    }

    override fun walkExprConcat(node: PartiqlAst.Expr.Concat, ctx: Ctx) = visit(node) {
        Plan.rexBinary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.CONCAT,
            type = StaticType.TEXT,
        )
    }

    override fun walkExprAnd(node: PartiqlAst.Expr.And, ctx: Ctx) = visit(node) {
        Plan.rexBinary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.AND,
            type = StaticType.BOOL,
        )
    }

    override fun walkExprOr(node: PartiqlAst.Expr.Or, ctx: Ctx) = visit(node) {
        Plan.rexBinary(
            lhs = convert(node.operands[0]),
            rhs = convert(node.operands[1]),
            op = Rex.Binary.Op.OR,
            type = StaticType.BOOL,
        )
    }

    override fun walkExprEq(node: PartiqlAst.Expr.Eq, ctx: Ctx) = visit(node) {
        val (lhs, rhs) = walkComparisonOperands(node.operands)
        Plan.rexBinary(
            lhs = lhs,
            rhs = rhs,
            op = Rex.Binary.Op.EQ,
            type = StaticType.BOOL,
        )
    }

    override fun walkExprNe(node: PartiqlAst.Expr.Ne, ctx: Ctx) = visit(node) {
        val (lhs, rhs) = walkComparisonOperands(node.operands)
        Plan.rexBinary(
            lhs = lhs,
            rhs = rhs,
            op = Rex.Binary.Op.NEQ,
            type = StaticType.BOOL,
        )
    }

    override fun walkExprGt(node: PartiqlAst.Expr.Gt, ctx: Ctx) = visit(node) {
        val (lhs, rhs) = walkComparisonOperands(node.operands)
        Plan.rexBinary(
            lhs = lhs,
            rhs = rhs,
            op = Rex.Binary.Op.GT,
            type = StaticType.BOOL,
        )
    }

    override fun walkExprGte(node: PartiqlAst.Expr.Gte, ctx: Ctx) = visit(node) {
        val (lhs, rhs) = walkComparisonOperands(node.operands)
        Plan.rexBinary(
            lhs = lhs,
            rhs = rhs,
            op = Rex.Binary.Op.GTE,
            type = StaticType.BOOL,
        )
    }

    override fun walkExprLt(node: PartiqlAst.Expr.Lt, ctx: Ctx) = visit(node) {
        val (lhs, rhs) = walkComparisonOperands(node.operands)
        Plan.rexBinary(
            lhs = lhs,
            rhs = rhs,
            op = Rex.Binary.Op.LT,
            type = StaticType.BOOL,
        )
    }

    override fun walkExprLte(node: PartiqlAst.Expr.Lte, ctx: Ctx) = visit(node) {
        val (lhs, rhs) = walkComparisonOperands(node.operands)
        Plan.rexBinary(
            lhs = lhs,
            rhs = rhs,
            op = Rex.Binary.Op.LTE,
            type = StaticType.BOOL,
        )
    }

    /**
     * Converts Comparison Operands. Also coerces them if one of them is an array.
     */
    private fun walkComparisonOperands(operands: List<PartiqlAst.Expr>): Pair<Rex, Rex> {
        var lhs = convert(operands[0])
        var rhs = convert(operands[1])
        if (lhs is Rex.Collection.Array) { rhs = coercePotentialSubquery(rhs) }
        if (rhs is Rex.Collection.Array) { lhs = coercePotentialSubquery(lhs) }
        return lhs to rhs
    }

    /**
     * We convert the scalar subquery of a SFW into a scalar subquery of a SELECT VALUE.
     */
    private fun coercePotentialSubquery(rex: Rex): Rex {
        var rhs = rex
        if (rhs is Rex.Query.Scalar.Subquery) {
            val sfw = rhs.query as? Rex.Query.Collection ?: error("Malformed plan, all scalar subqueries should hold collection queries")
            val constructor = sfw.constructor ?: run {
                val relProject = sfw.rel as? Rel.Project ?: error("Malformed plan, the top of a plan should be a projection")
                getConstructorFromRelProject(relProject)
            }
            rhs = rhs.copy(
                query = rhs.query.copy(
                    constructor = constructor
                )
            )
        }
        return rhs
    }

    private fun getConstructorFromRelProject(relProject: Rel.Project): Rex {
        return when (relProject.bindings.size) {
            0 -> error("The Projection should not have held empty bindings.")
            1 -> {
                val binding = relProject.bindings.first()
                if (binding.value.isProjectAll()) {
                    error("Unimplemented feature: coercion of SELECT *.")
                }
                relProject.bindings.first().value
            }
            else -> {
                if (relProject.bindings.any { it.value.isProjectAll() }) {
                    error("Unimplemented feature: coercion of SELECT *.")
                }
                Plan.rexCollectionArray(
                    relProject.bindings.map { it.value },
                    type = StaticType.LIST
                )
            }
        }
    }

    override fun walkExprLike(node: PartiqlAst.Expr.Like, ctx: Ctx) = visit(node) {
        when (val escape = node.escape) {
            null -> Plan.rexCall(
                id = Constants.like,
                args = args(
                    "value" to node.value,
                    "pattern" to node.pattern,
                ),
                type = StaticType.BOOL,
            )
            else -> Plan.rexCall(
                id = Constants.likeEscape,
                args = args(
                    "value" to node.value,
                    "pattern" to node.pattern,
                    "escape" to escape,
                ),
                type = StaticType.BOOL,
            )
        }
    }

    override fun walkExprBetween(node: PartiqlAst.Expr.Between, ctx: Ctx) = visit(node) {
        Plan.rexCall(
            id = Constants.between,
            args = args("value" to node.value, "from" to node.from, "to" to node.to),
            type = StaticType.BOOL,
        )
    }

    /**
     * Here, we must visit the RHS. If it is a scalar subquery, we need to grab the underlying collection.
     */
    override fun walkExprInCollection(node: PartiqlAst.Expr.InCollection, ctx: Ctx) = visit(node) {
        val lhs = convert(node.operands[0])
        val potentialSubqueryRex = convert(node.operands[1])
        val potentialSubquery = coercePotentialSubquery(potentialSubqueryRex)
        val rhs = (potentialSubquery as? Rex.Query.Scalar.Subquery)?.query ?: potentialSubquery
        Plan.rexCall(
            id = Constants.inCollection,
            args = listOf(
                Plan.argValue("lhs", lhs),
                Plan.argValue("rhs", rhs),
            ),
            type = StaticType.BOOL,
        )
    }

    override fun walkExprStruct(node: PartiqlAst.Expr.Struct, ctx: Ctx) = visit(node) {
        Plan.rexTuple(
            fields = node.fields.map {
                Plan.field(
                    name = convert(it.first),
                    value = convert(it.second)
                )
            },
            type = StaticType.STRUCT,
        )
    }

    override fun walkExprBag(node: PartiqlAst.Expr.Bag, ctx: Ctx) = visit(node) {
        Plan.rexCollectionBag(
            values = convert(node.values),
            type = StaticType.BAG,
        )
    }

    override fun walkExprList(node: PartiqlAst.Expr.List, ctx: Ctx) = visit(node) {
        Plan.rexCollectionArray(
            values = convert(node.values),
            type = StaticType.LIST,
        )
    }

    override fun walkExprSexp(node: PartiqlAst.Expr.Sexp, accumulator: Ctx) = visit(node) {
        Plan.rexCollectionArray(
            values = convert(node.values),
            type = StaticType.LIST,
        )
    }

    override fun walkExprCall(node: PartiqlAst.Expr.Call, ctx: Ctx) = visit(node) {
        Plan.rexCall(
            id = node.funcName.text,
            args = args(*node.args.toTypedArray()),
            type = null,
        )
    }

    override fun walkExprCallAgg(node: PartiqlAst.Expr.CallAgg, ctx: Ctx) = visit(node) {
        Plan.rexAgg(
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
        Plan.rexCall(
            id = Constants.isType,
            args = args("value" to node.value, "type" to node.type),
            type = StaticType.BOOL,
        )
    }

    override fun walkExprSimpleCase(node: PartiqlAst.Expr.SimpleCase, ctx: Ctx) = visit(node) {
        Plan.rexSwitch(
            match = convert(node.expr),
            branches = node.cases.pairs.map {
                Plan.branch(
                    condition = convert(it.first),
                    value = convert(it.second),
                )
            },
            default = if (node.default != null) convert(node.default!!) else null,
            type = null
        )
    }

    override fun walkExprSearchedCase(node: PartiqlAst.Expr.SearchedCase, ctx: Ctx) = visit(node) {
        Plan.rexSwitch(
            match = null,
            branches = node.cases.pairs.map {
                Plan.branch(
                    condition = convert(it.first),
                    value = convert(it.second),
                )
            },
            default = if (node.default != null) convert(node.default!!) else null,
            type = null
        )
    }

    override fun walkExprDate(node: PartiqlAst.Expr.Date, ctx: Ctx): Ctx {
        error("Date class undetermined at the moment")
    }

    override fun walkExprLitTime(node: PartiqlAst.Expr.LitTime, ctx: Ctx): Ctx {
        error("Time class undetermined at the moment")
    }

    override fun walkExprBagOp(node: PartiqlAst.Expr.BagOp, ctx: Ctx) = visit(node) {
        // Hack for UNION / INTERSECT / EXCEPT because they are missing from the parser
        val op = when (node.quantifier) {
            is PartiqlAst.SetQuantifier.All -> when (node.op) {
                is PartiqlAst.BagOpType.Union,
                is PartiqlAst.BagOpType.OuterUnion -> Constants.outerBagUnion
                is PartiqlAst.BagOpType.Intersect,
                is PartiqlAst.BagOpType.OuterIntersect -> Constants.outerBagIntersect
                is PartiqlAst.BagOpType.Except,
                is PartiqlAst.BagOpType.OuterExcept -> Constants.outerBagExcept
            }
            is PartiqlAst.SetQuantifier.Distinct -> when (node.op) {
                is PartiqlAst.BagOpType.Union,
                is PartiqlAst.BagOpType.OuterUnion -> Constants.outerSetUnion
                is PartiqlAst.BagOpType.Intersect,
                is PartiqlAst.BagOpType.OuterIntersect -> Constants.outerSetIntersect
                is PartiqlAst.BagOpType.Except,
                is PartiqlAst.BagOpType.OuterExcept -> Constants.outerSetExcept
            }
        }
        Plan.rexCall(
            id = op,
            args = args("lhs" to node.operands[0], "rhs" to node.operands[1]),
            type = StaticType.BAG,
        )
    }

    override fun walkExprCast(node: PartiqlAst.Expr.Cast, ctx: Ctx) = visit(node) {
        Plan.rexCall(
            id = Constants.cast,
            args = args("value" to node.value, "type" to node.asType),
            type = TypeConverter.convert(node.asType),
        )
    }

    override fun walkExprCanCast(node: PartiqlAst.Expr.CanCast, ctx: Ctx) = visit(node) {
        Plan.rexCall(
            id = Constants.canCast,
            args = args("value" to node.value, "type" to node.asType),
            type = StaticType.BOOL,
        )
    }

    override fun walkExprCanLosslessCast(node: PartiqlAst.Expr.CanLosslessCast, ctx: Ctx) = visit(node) {
        Plan.rexCall(
            id = Constants.canLosslessCast,
            args = args("value" to node.value, "type" to node.asType),
            type = StaticType.BOOL,
        )
    }

    override fun walkExprNullIf(node: PartiqlAst.Expr.NullIf, ctx: Ctx) = visit(node) {
        Plan.rexCall(
            id = Constants.nullIf,
            args = args(node.expr1, node.expr2),
            type = StaticType.BOOL,
        )
    }

    override fun walkExprCoalesce(node: PartiqlAst.Expr.Coalesce, ctx: Ctx) = visit(node) {
        Plan.rexCall(
            id = Constants.coalesce,
            args = args(node.args),
            type = null,
        )
    }

    override fun walkExprSelect(node: PartiqlAst.Expr.Select, ctx: Ctx) = visit(node) {
        when (val query = RelConverter.convert(node)) {
            is Rex.Query.Collection -> Plan.rexQueryScalarSubquery(query, null)
            is Rex.Query.Scalar -> query
        }
    }

    internal fun convertCase(case: PartiqlAst.CaseSensitivity) = when (case) {
        is PartiqlAst.CaseSensitivity.CaseInsensitive -> Case.INSENSITIVE
        is PartiqlAst.CaseSensitivity.CaseSensitive -> Case.SENSITIVE
    }

    internal object Constants {

        // const val unaryNot = "unary_not"
        //
        // const val unaryPlus = "unary_plus"
        //
        // const val unaryMinus = "unary_minus"
        //
        // const val unaryNegate = "unary_negate"
        //
        // const val binaryAdd = "binary_add"
        //
        // const val binarySub = "binary_sb"
        //
        // const val binaryMult = "binary_mult"
        //
        // const val binaryDiv = "binary_div"
        //
        // const val binaryMod = "binary_mod"
        //
        // const val binaryConcat = "binary_concat"
        //
        // const val binaryAnd = "binary_and"
        //
        // const val binaryOr = "binary_or"
        //
        // const val binaryEq = "binary_eq"
        //
        // const val binaryNeq = "binary_neq"
        //
        // const val binaryGt = "binary_gt"
        //
        // const val binaryGte = "binary_gte"
        //
        // const val binaryLt = "binary_lt"
        //
        // const val binaryLte = "binary_lte"

        const val like = "like"

        const val likeEscape = "like_escape"

        const val between = "between"

        const val inCollection = "in_collection"

        const val isType = "is_type"

        const val outerBagUnion = "outer_bag_union"

        const val outerBagIntersect = "outer_bag_intersect"

        const val outerBagExcept = "outer_bag_except"

        const val outerSetUnion = "outer_set_union"

        const val outerSetIntersect = "outer_set_intersect"

        const val outerSetExcept = "outer_set_except"

        const val cast = "cast"

        const val canCast = "can_cast"

        const val canLosslessCast = "can_lossless_cast"

        const val nullIf = "null_if"

        const val coalesce = "coalesce"
    }
}
