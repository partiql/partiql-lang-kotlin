package org.partiql.planner.impl.transforms

import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.GroupBy
import org.partiql.ast.OrderBy
import org.partiql.ast.Select
import org.partiql.ast.SetOp
import org.partiql.ast.Sort
import org.partiql.ast.builder.ast
import org.partiql.ast.util.AstRewriter
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.plan.Plan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.builder.PlanFactory
import org.partiql.plan.builder.plan
import org.partiql.planner.impl.PartiQLPlannerEnv
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.nullValue
import org.partiql.value.stringValue

/**
 * Lexically scoped state for use in translating an individual SELECT statement.
 */
internal object RelConverter {

    private val factory: PlanFactory = Plan

    // IGNORE — so we don't have to non-null assert on operators
    @OptIn(PartiQLValueExperimental::class)
    private val nil = plan(factory) {
        rel {
            op = relOpScan {
                rex = rex(typeRef("null", 0), rexOpLit(nullValue()))
            }
        }
    }

    /**
     * Here we convert an SFW to composed [Rel]s, then apply the appropriate relation-value projection to get a [Rex].
     */
    internal fun apply(sfw: Expr.SFW, env: PartiQLPlannerEnv): Rex = with(factory) {
        val rel = sfw.accept(ToRel(env), nil)
        val rex = when (val projection = sfw.select) {
            // PIVOT ... FROM
            is Select.Pivot -> {
                val key = projection.key.toRex(env)
                val value = projection.value.toRex(env)
                val type = env.type(StaticType.STRUCT)
                val op = rexOpPivot(key, value, rel)
                rex(type, op)
            }
            // SELECT VALUE ... FROM
            is Select.Value -> {
                val constructor = projection.constructor.toRex(env)
                val op = rexOpSelect(constructor, rel)
                val type = when (rel.props.contains(Rel.Prop.ORDERED)) {
                    true -> env.type(StaticType.LIST)
                    else -> env.type(StaticType.BAG)
                }
                rex(type, op)
            }
            // SELECT ... FROM
            else -> {
                val constructor = defaultConstructor(env, rel.schema)
                val op = rexOpSelect(constructor, rel)
                val type = when (rel.props.contains(Rel.Prop.ORDERED)) {
                    true -> env.type(StaticType.LIST)
                    else -> env.type(StaticType.BAG)
                }
                rex(type, op)
            }
        }
        return rex
    }

    /**
     * Syntax sugar for converting an [Expr] tree to a [Rex] tree.
     */
    private fun Expr.toRex(env: PartiQLPlannerEnv): Rex = RexConverter.apply(this, env)

    private fun PartiQLPlannerEnv.type(type: StaticType) = resolveType(AstToPlan.convert(type))

    /**
     * Produces the default constructor to generalize a SQL SELECT to a SELECT VALUE.
     *  - https://partiql.org/dql/select.html#sql-select
     */
    @OptIn(PartiQLValueExperimental::class)
    private fun defaultConstructor(env: PartiQLPlannerEnv, schema: List<Rel.Binding>): Rex = with(factory) {
        val str = env.type(StaticType.STRING)
        val type = env.type(StaticType.STRUCT)
        val fields = schema.mapIndexed { i, b ->
            val k = rex(str, rexOpLit(stringValue(b.name)))
            val v = rex(b.type, rexOpVarResolved(i))
            rexOpStructField(k, v)
        }
        val op = rexOpStruct(fields)
        rex(type, op)
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE", "LocalVariableName")
    private class ToRel(
        private val env: PartiQLPlannerEnv,
    ) : AstBaseVisitor<Rel, Rel>() {

        private inline fun <T : PlanNode> transform(block: PlanFactory.() -> T): T = factory.block()

        override fun defaultReturn(node: AstNode, input: Rel): Rel =
            throw IllegalArgumentException("unsupported rel $node")

        /**
         * Translate SFW AST node to a pipeline of [Rel] operators; skip any SELECT VALUE or PIVOT projection.
         */

        override fun visitExprSFW(node: Expr.SFW, input: Rel): Rel {
            var sel = node
            var rel = visitFrom(sel.from, nil)
            rel = convertWhere(rel, sel.where)
            // kotlin does not have destructuring reassignment
            val (_sel, _rel) = convertAgg(rel, sel, sel.groupBy)
            sel = _sel
            rel = _rel
            // transform (possibly rewritten) sel node
            rel = convertHaving(rel, sel.having)
            rel = convertSetOp(rel, sel.setOp)
            rel = convertOrderBy(rel, sel.orderBy)
            rel = convertLimit(rel, sel.limit)
            rel = convertOffset(rel, sel.offset)
            // append SQL projection if present
            rel = when (val projection = sel.select) {
                is Select.Project -> visitSelectProject(projection, rel)
                is Select.Star -> error("AST not normalized, found project star")
                else -> rel // skip PIVOT and SELECT VALUE
            }
            return rel
        }

        override fun visitSelectProject(node: Select.Project, input: Rel) = transform {
            // this ignores aggregations
            val schema = mutableListOf<Rel.Binding>()
            val projections = mutableListOf<Rex>()
            val props = emptySet<Rel.Prop>()
            node.items.forEach {
                val (binding, projection) = convertProjectionItem(it)
                schema.add(binding)
                projections.add(projection)
            }
            val op = relOpProject(input, projections)
            rel(schema, props, op)
        }

        override fun visitFromValue(node: From.Value, nil: Rel) = transform {
            val rex = RexConverter.apply(node.expr, env)
            val binding = when (val a = node.asAlias) {
                null -> error("AST not normalized, missing AS alias on $node")
                else -> relBinding(
                    name = a.symbol,
                    type = rex.type
                )
            }
            when (node.type) {
                From.Value.Type.SCAN -> {
                    when (val i = node.atAlias) {
                        null -> convertScan(rex, binding)
                        else -> {
                            val index = relBinding(
                                name = i.symbol,
                                type = env.type(StaticType.INT)
                            )
                            convertScanIndexed(rex, binding, index)
                        }
                    }
                }
                From.Value.Type.UNPIVOT -> {
                    val atAlias = when (val at = node.atAlias) {
                        null -> error("AST not normalized, missing AT alias on UNPIVOT $node")
                        else -> relBinding(
                            name = at.symbol,
                            type = env.type(StaticType.STRING)
                        )
                    }
                    convertUnpivot(rex, k = atAlias, v = binding)
                }
            }
        }

        /**
         * Appends [Rel.Op.Join] where the left and right sides are converted FROM sources
         *
         * TODO compute basic schema
         */
        override fun visitFromJoin(node: From.Join, nil: Rel) = transform {
            val lhs = visitFrom(node.lhs, nil)
            val rhs = visitFrom(node.rhs, nil)
            val schema = listOf<Rel.Binding>()
            val props = emptySet<Rel.Prop>()
            val type = convertJoinType(node)
            val op = relOpJoin(lhs, rhs, type)
            rel(schema, props, op)
        }

        // Helpers

        private fun convertScan(rex: Rex, binding: Rel.Binding) = transform {
            val schema = listOf(binding)
            val props = emptySet<Rel.Prop>()
            val op = relOpScan(rex)
            rel(schema, props, op)
        }

        private fun convertScanIndexed(rex: Rex, binding: Rel.Binding, index: Rel.Binding) = transform {
            val schema = listOf(binding, index)
            val props = setOf(Rel.Prop.ORDERED)
            val op = relOpScan(rex)
            rel(schema, props, op)
        }

        private fun convertUnpivot(rex: Rex, k: Rel.Binding, v: Rel.Binding) = transform {
            val schema = listOf(k, v)
            val props = emptySet<Rel.Prop>()
            val op = relOpUnpivot(rex)
            rel(schema, props, op)
        }

        private fun convertProjectionItem(item: Select.Project.Item) = when (item) {
            is Select.Project.Item.All -> convertProjectItemAll(item)
            is Select.Project.Item.Expression -> convertProjectItemRex(item)
        }

        private fun convertProjectItemAll(item: Select.Project.Item.All): Pair<Rel.Binding, Rex> {
            TODO("Remove Project All in favor of project unpivot")
        }

        private fun convertProjectItemRex(item: Select.Project.Item.Expression): Pair<Rel.Binding, Rex> {
            val name = when (val a = item.asAlias) {
                null -> error("AST not normalized, missing AS alias on projection item $item")
                else -> a.symbol
            }
            val rex = RexConverter.apply(item.expr, env)
            val binding = factory.relBinding(name, rex.type)
            return binding to rex
        }

        /**
         * Append [Rel.Op.Filter] only if a WHERE condition exists
         */
        private fun convertWhere(input: Rel, expr: Expr?): Rel = transform {
            if (expr == null) {
                return input
            }
            val schema = input.schema
            val props = input.props
            val predicate = expr.toRex(env)
            val op = relOpFilter(input, predicate)
            rel(schema, props, op)
        }

        private fun convertJoinType(join: From.Join): Rel.Op.Join.Type = transform {
            val capture = when (join.type) {
                From.Join.Type.INNER -> Rel.Op.Join.Capture.INNER
                From.Join.Type.LEFT -> Rel.Op.Join.Capture.LEFT
                From.Join.Type.LEFT_OUTER -> Rel.Op.Join.Capture.LEFT_OUTER
                From.Join.Type.RIGHT -> Rel.Op.Join.Capture.RIGHT
                From.Join.Type.RIGHT_OUTER -> Rel.Op.Join.Capture.RIGHT_OUTER
                From.Join.Type.FULL -> Rel.Op.Join.Capture.FULL
                From.Join.Type.FULL_OUTER -> Rel.Op.Join.Capture.FULL_OUTER
                From.Join.Type.CROSS -> return relOpJoinTypeCross()
                From.Join.Type.COMMA -> return relOpJoinTypeCross()
                null -> Rel.Op.Join.Capture.INNER
            }
            when (join.condition) {
                null -> relOpJoinTypeEqui(capture)
                else -> relOpJoinTypeTheta(capture, join.condition!!.toRex(env))
            }
        }

        /**
         * Append [Rel.Op.Aggregate] only if SELECT contains aggregate expressions.
         *
         * TODO Set quantifiers
         * TODO Group As
         *
         * @return Pair<Ast.Expr.SFW, Rel> is returned where
         *         1. Ast.Expr.SFW has every Ast.Expr.CallAgg replaced by a synthetic Ast.Expr.Var
         *         2. Rel which has the appropriate Rex.Agg calls and groups
         */
        private fun convertAgg(input: Rel, select: Expr.SFW, groupBy: GroupBy?): Pair<Expr.SFW, Rel> {
            // Rewrite and extract all aggregations in the SELECT clause
            val (sel, aggregations) = AggregationTransform.apply(select)

            // No aggregation planning required for GROUP BY
            if (aggregations.isEmpty()) {
                if (groupBy != null) {
                    // GROUP BY with no aggregations is considered an error.
                    error("GROUP BY with no aggregations in SELECT clause")
                }
                return Pair(select, input)
            }

            // Build the schema -> (aggs... groups...)
            val schema = mutableListOf<Rel.Binding>()
            val props = emptySet<Rel.Prop>()

            // Build the rel operator
            var strategy = Rel.Op.Aggregate.Strategy.FULL
            val aggs = aggregations.mapIndexed { i, agg ->
                val binding = Plan.relBinding(
                    name = syntheticAgg(i),
                    type = env.type(StaticType.ANY),
                )
                schema.add(binding)
                val args = agg.args.map { arg ->
                    val rex = arg.toRex(env)
                    Plan.rexOpCallArgValue(rex)
                }
                val id = AstToPlan.convert(agg.function)
                val fn = Plan.fnRefUnresolved(id)
                Plan.relOpAggregateAgg(fn, args)
            }
            var groups = emptyList<Rex>()
            if (groupBy != null) {
                groups = groupBy.keys.map {
                    if (it.asAlias == null) {
                        error("not normalized, group key $it missing unique name")
                    }
                    val binding = Plan.relBinding(
                        name = it.asAlias!!.symbol,
                        type = env.type(StaticType.ANY)
                    )
                    schema.add(binding)
                    it.expr.toRex(env)
                }
                strategy = when (groupBy.strategy) {
                    GroupBy.Strategy.FULL -> Rel.Op.Aggregate.Strategy.FULL
                    GroupBy.Strategy.PARTIAL -> Rel.Op.Aggregate.Strategy.PARTIAL
                }
            }
            val op = Plan.relOpAggregate(input, strategy, aggs, groups)
            val rel = Plan.rel(schema, props, op)
            return Pair(sel, rel)
        }

        /**
         * Append [Rel.Op.Filter] only if a HAVING condition exists
         *
         * Notes:
         *  - This currently does not support aggregation expressions in the WHERE condition
         */
        private fun convertHaving(input: Rel, expr: Expr?): Rel = transform {
            if (expr == null) {
                return input
            }
            val schema = input.schema
            val props = input.props
            val predicate = expr.toRex(env)
            val op = relOpFilter(input, predicate)
            rel(schema, props, op)
        }

        /**
         * Append SQL set operator if present
         *
         * TODO combine/compare schemas
         * TODO set quantifier
         */
        private fun convertSetOp(input: Rel, setOp: Expr.SFW.SetOp?): Rel = transform {
            if (setOp == null) {
                return@transform input
            }
            val schema = input.schema
            val props = emptySet<Rel.Prop>()
            val lhs = input
            val rhs = visitExprSFW(setOp.operand, nil)
            val op = when (setOp.type.type) {
                SetOp.Type.UNION -> relOpUnion(lhs, rhs)
                SetOp.Type.INTERSECT -> relOpIntersect(lhs, rhs)
                SetOp.Type.EXCEPT -> relOpIntersect(lhs, rhs)
            }
            rel(schema, props, op)
        }

        /**
         * Append [Rel.Op.Sort] only if an ORDER BY clause is present
         */
        private fun convertOrderBy(input: Rel, orderBy: OrderBy?) = transform {
            if (orderBy == null) {
                return@transform input
            }
            val schema = input.schema
            val props = setOf(Rel.Prop.ORDERED)
            val specs = orderBy.sorts.map {
                val rex = it.expr.toRex(env)
                val order = when (it.dir) {
                    Sort.Dir.DESC -> when (it.nulls) {
                        Sort.Nulls.LAST -> Rel.Op.Sort.Order.DESC_NULLS_LAST
                        else -> Rel.Op.Sort.Order.DESC_NULLS_FIRST
                    }
                    else -> when (it.nulls) {
                        Sort.Nulls.FIRST -> Rel.Op.Sort.Order.ASC_NULLS_FIRST
                        else -> Rel.Op.Sort.Order.ASC_NULLS_LAST
                    }
                }
                relOpSortSpec(rex, order)
            }
            val op = relOpSort(input, specs)
            rel(schema, props, op)
        }

        /**
         * Append [Rel.Op.Limit] if there is a LIMIT
         */
        private fun convertLimit(input: Rel, limit: Expr?): Rel = transform {
            if (limit == null) {
                return@transform input
            }
            val schema = input.schema
            val props = input.props
            val rex = RexConverter.apply(limit, env)
            val op = relOpLimit(input, rex)
            rel(schema, props, op)
        }

        /**
         * Append [Rel.Op.Offset] if there is an OFFSET
         */
        private fun convertOffset(input: Rel, offset: Expr?): Rel = transform {
            if (offset == null) {
                return@transform input
            }
            val schema = input.schema
            val props = input.props
            val rex = RexConverter.apply(offset, env)
            val op = relOpOffset(input, rex)
            rel(schema, props, op)
        }

        // /**
        //  * Converts a GROUP AS X clause to a binding of the form:
        //  * ```
        //  * { 'X': group_as({ 'a_0': e_0, ..., 'a_n': e_n }) }
        //  * ```
        //  *
        //  * Notes:
        //  *  - This was included to be consistent with the existing PartiqlAst and PartiqlLogical representations,
        //  *    but perhaps we don't want to represent GROUP AS with an agg function.
        //  */
        // private fun convertGroupAs(name: String, from: From): Binding {
        //     val fields = from.bindings().map { n ->
        //         Plan.field(
        //             name = Plan.rexLit(ionString(n), StaticType.STRING),
        //             value = Plan.rexId(n, Case.SENSITIVE, Rex.Id.Qualifier.UNQUALIFIED, type = StaticType.STRUCT)
        //         )
        //     }
        //     return Plan.binding(
        //         name = name,
        //         value = Plan.rexAgg(
        //             id = "group_as",
        //             args = listOf(Plan.rexTuple(fields, StaticType.STRUCT)),
        //             modifier = Rex.Agg.Modifier.ALL,
        //             type = StaticType.STRUCT
        //         )
        //     )
        // }

    }

    /**
     * Rewrites a SELECT node replacing (and extracting) each aggregation `i` with a synthetic field name `$agg_i`.
     */
    private object AggregationTransform : AstRewriter<MutableList<Expr.Agg>>() {

        fun apply(node: Expr.SFW): Pair<Expr.SFW, List<Expr.Agg>> {
            val aggs = mutableListOf<Expr.Agg>()
            val select = super.visitExprSFW(node, aggs) as Expr.SFW
            return Pair(select, aggs)
        }

        // only rewrite top-level SFW
        override fun visitExprSFW(node: Expr.SFW, ctx: MutableList<Expr.Agg>): AstNode = node

        override fun visitExprAgg(node: Expr.Agg, ctx: MutableList<Expr.Agg>) = ast {
            val id = identifierSymbol {
                symbol = syntheticAgg(ctx.size)
                caseSensitivity = org.partiql.ast.Identifier.CaseSensitivity.INSENSITIVE
            }
            ctx += node
            exprVar(id, Expr.Var.Scope.DEFAULT)
        }
    }

    private fun syntheticAgg(i: Int) = "\$agg_$i"
}
