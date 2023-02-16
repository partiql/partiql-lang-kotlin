package org.partiql.plan

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.visitors.AggregationVisitorTransform
import org.partiql.lang.eval.visitors.FromSourceAliasVisitorTransform
import org.partiql.lang.eval.visitors.OrderBySortSpecVisitorTransform
import org.partiql.lang.eval.visitors.PipelinedVisitorTransform
import org.partiql.lang.eval.visitors.SelectListItemAliasVisitorTransform
import org.partiql.lang.eval.visitors.SelectStarVisitorTransform
import org.partiql.lang.eval.visitors.VisitorTransformBase
import org.partiql.plan.ir.Binding
import org.partiql.plan.ir.Common
import org.partiql.plan.ir.Rel
import org.partiql.plan.ir.Rex
import org.partiql.plan.ir.SortSpec
import org.partiql.plan.ir.StructPart

/**
 * Experimental PartiqlAst.Statement to [Rel] transformation.
 */
object AstToRel {

    /**
     * Entry point for PartiqlAst to Rel translation
     *
     * @param statement
     * @return
     */
    fun convert(statement: PartiqlAst.Statement): Rel {
        if (statement !is PartiqlAst.Statement.Query) {
            unsupported(statement)
        }
        val plan = when (val query = normalize(statement).expr) {
            is PartiqlAst.Expr.Select -> RelConverter.convert(query)
            else -> {
                // This is incorrect for now, we don't want to coerce into a bag
                val rex = RexConverter.convert(query)
                Rel.Scan(COMMON, rex, alias = null, at = null, by = null)
            }
        }
        return plan
    }

    // --- Internal ---------------------------------------------

    /**
     * As of now, the COMMON property of relation operators is under development, so just use empty for now
     */
    private val COMMON = Common(
        schema = emptyMap(),
        properties = emptySet(),
        metas = emptyMap(),
    )

    /**
     * Common place to throw exceptions with access to the AST node
     */
    private fun unsupported(node: PartiqlAst.PartiqlAstNode): Nothing {
        throw UnsupportedOperationException("node: $node")
    }

    /**
     * Normalizes a query AST
     *
     * Notes:
     *  - AST normalization assumes operating on statement rather than a query statement, but the normalization
     *    only changes the SFW nodes. There's room to simplify here.
     */
    private fun normalize(query: PartiqlAst.Statement.Query): PartiqlAst.Statement.Query {
        val transform = PipelinedVisitorTransform(
            SelectListItemAliasVisitorTransform(),
            FromSourceAliasVisitorTransform(),
            OrderBySortSpecVisitorTransform(),
            AggregationVisitorTransform(),
            SelectStarVisitorTransform()
        )
        return transform.transformStatementQuery(query) as PartiqlAst.Statement.Query
    }

    /**
     * Lexically scoped state for use in translating an individual SELECT statement.
     * Calcite uses a similar object called Blackboard.
     */
    private class RelConverter {

        // static entry functions
        companion object {
            @JvmStatic
            fun convert(select: PartiqlAst.Expr.Select) = RelConverter().convertSelect(select)
        }

        // synthetic binding name counter
        private var i = 0

        // generate a synthetic binding name
        private fun nextBindingName(): String = "\$__v${i++}"

        /**
         * Translate SFW AST node to a pipeline of [Rel] operators
         *
         * Note:
         *  - The AST doesn't support set operators
         *  - The Parser doesn't have FETCH syntax
         */
        private fun convertSelect(node: PartiqlAst.Expr.Select): Rel {
            var sel = node
            var rel = convertFrom(sel.from)
            rel = convertWhere(rel, sel.where)
            // kotlin does not have destructuring assignment
            val (_sel, _rel) = convertAgg(rel, sel, sel.group)
            sel = _sel
            rel = _rel
            // transform (possibly rewritten) sel node
            rel = convertHaving(rel, sel.having)
            rel = convertOrderBy(rel, sel.order)
            rel = convertFetch(rel, sel.limit, sel.offset)
            rel = convertProject(rel, sel.project)
            return rel
        }

        /**
         * Appends the appropriate [Rel] operator for the given FROM source
         *
         * Notes:
         *  - TODO model PIVOT
         */
        private fun convertFrom(from: PartiqlAst.FromSource): Rel = when (from) {
            is PartiqlAst.FromSource.Join -> convertJoin(from)
            is PartiqlAst.FromSource.Scan -> convertScan(from)
            is PartiqlAst.FromSource.Unpivot -> unsupported(from)
        }

        /**
         * Appends [Rel.Join] where the left and right sides are converted FROM sources
         */
        private fun convertJoin(join: PartiqlAst.FromSource.Join): Rel {
            val lhs = convertFrom(join.left)
            val rhs = convertFrom(join.right)
            val condition = if (join.predicate != null) RexConverter.convert(join.predicate!!) else null
            return Rel.Join(
                common = COMMON,
                lhs = lhs,
                rhs = rhs,
                condition = condition,
                type = when (join.type) {
                    is PartiqlAst.JoinType.Full -> Rel.Join.Type.FULL
                    is PartiqlAst.JoinType.Inner -> Rel.Join.Type.INNER
                    is PartiqlAst.JoinType.Left -> Rel.Join.Type.LEFT
                    is PartiqlAst.JoinType.Right -> Rel.Join.Type.RIGHT
                }
            )
        }

        /**
         * Appends [Rel.Scan] which takes no input relational expression
         */
        private fun convertScan(scan: PartiqlAst.FromSource.Scan) = Rel.Scan(
            common = COMMON,
            rex = RexConverter.convert(scan.expr),
            alias = scan.asAlias?.text,
            at = scan.atAlias?.text,
            by = scan.byAlias?.text,
        )

        /**
         * Append [Rel.Filter] only if a WHERE condition exists
         */
        private fun convertWhere(input: Rel, expr: PartiqlAst.Expr?): Rel = when (expr) {
            null -> input
            else -> Rel.Filter(
                common = COMMON,
                input = input,
                condition = RexConverter.convert(expr),
            )
        }

        /**
         * Append [Rel.Aggregate] only if SELECT contains aggregate expressions.
         *
         * @return Pair<Ast.Expr.Select, Rel> is returned where
         *         1. Ast.Expr.Select has every Ast.Expr.CallAgg replaced by a synthetic Ast.Expr.Id
         *         2. Rel which has the appropriate Rex.Agg calls and Rex groups
         */
        private fun convertAgg(
            input: Rel,
            select: PartiqlAst.Expr.Select,
            groupBy: PartiqlAst.GroupBy?
        ): Pair<PartiqlAst.Expr.Select, Rel> {

            // Rewrite and extract all aggregations in the SELECT clause
            val (sel, aggregations) = AggregationTransform.apply(select)

            // No aggregation planning required for GROUP BY
            if (aggregations.isEmpty()) {
                if (groupBy != null) {
                    // As of now, GROUP BY with no aggregations is considered an error.
                    error("GROUP BY with no aggregations in SELECT clause")
                }
                return Pair(select, input)
            }

            val calls = aggregations.toMutableList()
            var groups = emptyList<Binding>()
            var strategy = Rel.Aggregate.Strategy.FULL

            if (groupBy != null) {
                // GROUP AS is implemented as an aggregation function
                if (groupBy.groupAsAlias != null) {
                    calls.add(convertGroupAs(groupBy.groupAsAlias!!.text, sel.from))
                }
                groups = groupBy.keyList.keys.map { convertGroupByKey(it) }
                strategy = when (groupBy.strategy) {
                    is PartiqlAst.GroupingStrategy.GroupFull -> Rel.Aggregate.Strategy.FULL
                    is PartiqlAst.GroupingStrategy.GroupPartial -> Rel.Aggregate.Strategy.PARTIAL
                }
            }

            val rel = Rel.Aggregate(
                common = COMMON,
                input = input,
                calls = calls,
                groups = groups,
                strategy = strategy,
            )

            return Pair(sel, rel)
        }

        /**
         * Each GROUP BY becomes a binding available in the output tuples of [Rel.Aggregate]
         */
        private fun convertGroupByKey(groupKey: PartiqlAst.GroupKey) = Binding(
            name = groupKey.asAlias?.text ?: error("not normalized, group key $groupKey missing unique name"),
            rex = RexConverter.convert(groupKey.expr),
        )

        /**
         * Append [Rel.Filter] only if a HAVING condition exists
         *
         * Notes:
         *  - This currently does not support aggregation expressions in the WHERE condition
         */
        private fun convertHaving(input: Rel, expr: PartiqlAst.Expr?): Rel = when (expr) {
            null -> input
            else -> Rel.Filter(
                common = COMMON,
                input = input,
                condition = RexConverter.convert(expr)
            )
        }

        /**
         * Append [Rel.Sort] only if an ORDER BY clause is present
         */
        private fun convertOrderBy(input: Rel, orderBy: PartiqlAst.OrderBy?) = when (orderBy) {
            null -> input
            else -> Rel.Sort(
                common = COMMON,
                input = input,
                specs = orderBy.sortSpecs.map { convertSortSpec(it) }
            )
        }

        /**
         * Append [Rel.Fetch] if there is a LIMIT or LIMIT and OFFSET.
         *
         * Notes:
         *  - It's unclear if OFFSET without LIMIT should be allowed in PartiQL, so err for now.
         */
        private fun convertFetch(
            input: Rel,
            limit: PartiqlAst.Expr?,
            offset: PartiqlAst.Expr?
        ): Rel {
            if (limit == null) {
                if (offset != null) error("offset without limit")
                return input
            }
            return Rel.Fetch(
                common = COMMON,
                input = input,
                limit = RexConverter.convert(limit),
                offset = RexConverter.convert(offset ?: PartiqlAst.Expr.Lit(ionInt(0).asAnyElement()))
            )
        }

        private fun convertProject(input: Rel, projection: PartiqlAst.Projection) = when (projection) {
            is PartiqlAst.Projection.ProjectList -> convertProjectList(input, projection)
            is PartiqlAst.Projection.ProjectPivot -> convertProjectPivot(input, projection)
            is PartiqlAst.Projection.ProjectStar -> error("AST not normalized, found project star")
            is PartiqlAst.Projection.ProjectValue -> TODO()
        }

        /**
         * Appends a [Rel.Project] which projects the result of each binding rex into its binding name.
         *
         * @param input
         * @param projection
         * @return
         */
        private fun convertProjectList(input: Rel, projection: PartiqlAst.Projection.ProjectList): Rel {
            unsupported(projection)
        }

        /**
         * TODO model PIVOT
         */
        private fun convertProjectPivot(input: Rel, projection: PartiqlAst.Projection.ProjectPivot): Rel {
            unsupported(projection)
        }

        /**
         * TODO model PROJECT VALUE
         */
        private fun convertProjectValue(input: Rel, projection: PartiqlAst.Projection.ProjectList): Rel {
            unsupported(projection)
        }

        /**
         * Converts Ast.SortSpec to SortSpec.
         *
         * Notes:
         *  - ASC NULLS LAST   (default)
         *  - DESC NULLS FIRST (default for DESC)
         */
        private fun convertSortSpec(sortSpec: PartiqlAst.SortSpec) = SortSpec(
            rex = RexConverter.convert(sortSpec.expr),
            dir = when (sortSpec.orderingSpec) {
                is PartiqlAst.OrderingSpec.Desc -> SortSpec.Dir.DESC
                is PartiqlAst.OrderingSpec.Asc -> SortSpec.Dir.ASC
                null -> SortSpec.Dir.ASC
            },
            nulls = when (sortSpec.nullsSpec) {
                is PartiqlAst.NullsSpec.NullsFirst -> SortSpec.Nulls.FIRST
                is PartiqlAst.NullsSpec.NullsLast -> SortSpec.Nulls.LAST
                null -> SortSpec.Nulls.LAST
            }
        )

        /**
         * Converts a GROUP AS X clause to a binding of the form:
         * ```
         * { 'X': group_as({ 'a_0': e_0, ..., 'a_n': e_n }) }
         * ```
         *
         * Notes:
         *  - This was included to be consistent with the existing PartiqlAst and PartiqlLogical representations,
         *    but perhaps we don't want to represent GROUP AS with an agg function.
         */
        private fun convertGroupAs(name: String, from: PartiqlAst.FromSource): Binding {
            val fields = from.bindings().map { a ->
                StructPart.Field(
                    name = Rex.Lit(ionString(a)),
                    rex = Rex.Id(a)
                )
            }
            return Binding(
                name = name,
                rex = Rex.Agg(
                    id = "group_as",
                    args = listOf(Rex.Struct(fields)),
                    modifier = Rex.Agg.Modifier.ALL,
                )
            )
        }

        /**
         * Helper to get all binding names in the FROM clause
         */
        private fun PartiqlAst.FromSource.bindings(): List<String> = when (this) {
            is PartiqlAst.FromSource.Scan -> {
                if (asAlias == null) {
                    error("not normalized, scan is missing an alias")
                }
                listOf(asAlias!!.text)
            }
            is PartiqlAst.FromSource.Join -> left.bindings() + right.bindings()
            is PartiqlAst.FromSource.Unpivot -> {
                if (asAlias == null) {
                    error("not normalized, scan is missing an alias")
                }
                listOf(asAlias!!.text)
            }
        }

        /**
         * Rewrites a SFW node replacing all aggregations with a synthetic field name
         *
         * See AstToLogicalVisitorTransform.kt CallAggregationReplacer from org.partiql.lang.planner.transforms.
         *
         * ```
         * SELECT g, h, SUM(t.b) AS sumB
         * FROM t
         * GROUP BY t.a AS g GROUP AS h
         * ```
         *
         * into:
         *
         * ```
         * SELECT g, h, $__v0 AS sumB
         * FROM t
         * GROUP BY t.a AS g GROUP AS h
         * ```
         *
         * Where $__v0 is the binding name of SUM(t.b) in the aggregation output
         *
         * Inner object class to have access to current SELECT-FROM-WHERE converter state
         */
        @Suppress("PrivatePropertyName")
        private val AggregationTransform = object : VisitorTransformBase() {

            private var level = 0
            private var aggregations = mutableListOf<Binding>()

            fun apply(node: PartiqlAst.Expr.Select): Pair<PartiqlAst.Expr.Select, List<Binding>> {
                level = 0
                aggregations = mutableListOf()
                val select = transformExprSelect(node) as PartiqlAst.Expr.Select
                return Pair(select, aggregations)
            }

            override fun transformProjectItemProjectExpr_expr(node: PartiqlAst.ProjectItem.ProjectExpr) =
                transformExpr(node.expr)

            override fun transformProjectionProjectValue_value(node: PartiqlAst.Projection.ProjectValue) =
                transformExpr(node.value)

            override fun transformExprSelect_having(node: PartiqlAst.Expr.Select): PartiqlAst.Expr? =
                when (node.having) {
                    null -> null
                    else -> transformExpr(node.having!!)
                }

            override fun transformSortSpec_expr(node: PartiqlAst.SortSpec) = transformExpr(node.expr)

            override fun transformExprSelect(node: PartiqlAst.Expr.Select) =
                if (level++ == 0) super.transformExprSelect(node) else node

            override fun transformExprCallAgg(node: PartiqlAst.Expr.CallAgg): PartiqlAst.Expr {
                val name = nextBindingName()
                val rex = RexConverter.convert(node)
                aggregations.add(Binding(name, rex))
                return PartiqlAst.build {
                    id(
                        name = name,
                        case = caseInsensitive(),
                        qualifier = unqualified(),
                        metas = node.metas
                    )
                }
            }
        }
    }

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
    private data class Ctx(
        val node: PartiqlAst.PartiqlAstNode,
        var rex: Rex? = null,
    )

    /**
     * Some workarounds for transforming a PIG tree without having to create another visitor:
     * - Using the VisitorFold with Ctx struct to create a parameterized return and scoped arguments/context
     * - Using walks to control traversal, also walks have generated if/else blocks for sum types so its more useful
     */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private object RexConverter : PartiqlAst.VisitorFold<Ctx>() {

        /**
         * Read as `val rex = node.accept(visitor = RexVisitor.INSTANCE, args = emptyList())`
         * Only works because RexConverter errs for all non Expr AST nodes, and Expr is one sum type.
         */
        fun convert(node: PartiqlAst.Expr) = RexConverter.walkExpr(node, Ctx(node)).rex!!

        /**
         * List version of hacked "accept"
         */
        fun convert(nodes: List<PartiqlAst.Expr>) = nodes.map { convert(it) }

        /**
         * Helper so the visitor "body" looks like it has Rex as the return value
         */
        fun visit(node: PartiqlAst.PartiqlAstNode, block: () -> Rex) = Ctx(node, block())

        /**
         * !! DEFAULT VISIT !!
         *
         * The PIG visitor doesn't give us control over the default "visit"
         * We can override walkMetas (which appears on every super.walk call) as if it were a default "visit"
         * MetaContainer isn't actually a domain node, and we don't have any context as to where the MetaContainer
         * is coming from which is why the current node is stuffed into Ctx
         */
        override fun walkMetas(node: MetaContainer, ctx: Ctx) = unsupported(ctx.node)

        override fun walkExprMissing(node: PartiqlAst.Expr.Missing, accumulator: Ctx): Ctx {
            TODO()
        }

        override fun walkExprLit(node: PartiqlAst.Expr.Lit, accumulator: Ctx) = visit(node) { Rex.Lit(node.value) }

        // add case sensitivity and scope
        override fun walkExprId(node: PartiqlAst.Expr.Id, accumulator: Ctx) = visit(node) { Rex.Id(node.name.text) }

        override fun walkExprNot(node: PartiqlAst.Expr.Not, accumulator: Ctx) = node.unary(node.expr, Rex.Unary.Op.NOT)

        override fun walkExprPos(node: PartiqlAst.Expr.Pos, accumulator: Ctx) = node.unary(node.expr, Rex.Unary.Op.POS)

        override fun walkExprNeg(node: PartiqlAst.Expr.Neg, accumulator: Ctx) = node.unary(node.expr, Rex.Unary.Op.NEG)

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
            Rex.Call(
                id = "in_collection",
                args = convert(node.operands),
            )
        }

        override fun walkExprStruct(node: PartiqlAst.Expr.Struct, accumulator: Ctx): Ctx {
            return super.walkExprStruct(node, accumulator)
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

        /**
         * Return a Ctx with a [Rex.Binary]
         */
        private fun PartiqlAst.Expr.unary(expr: PartiqlAst.Expr, op: Rex.Unary.Op) = Ctx(
            node = this,
            rex = Rex.Unary(
                rex = convert(expr),
                op = op,
            )
        )

        /**
         * Return a Ctx with a [Rex.Binary]
         */
        private fun PartiqlAst.Expr.binary(lhs: PartiqlAst.Expr, rhs: PartiqlAst.Expr, op: Rex.Binary.Op) = Ctx(
            node = this,
            rex = Rex.Binary(
                lhs = convert(lhs),
                rhs = convert(rhs),
                op = op,
            )
        )
    }
}
