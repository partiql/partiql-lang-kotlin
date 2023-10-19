/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */

package org.partiql.planner.transforms

import org.partiql.ast.AstNode
import org.partiql.ast.Exclude
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.GroupBy
import org.partiql.ast.OrderBy
import org.partiql.ast.Select
import org.partiql.ast.SetOp
import org.partiql.ast.Sort
import org.partiql.ast.builder.ast
import org.partiql.ast.helpers.toBinder
import org.partiql.ast.util.AstRewriter
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.aggUnresolved
import org.partiql.plan.rel
import org.partiql.plan.relBinding
import org.partiql.plan.relOpAggregate
import org.partiql.plan.relOpAggregateCall
import org.partiql.plan.relOpErr
import org.partiql.plan.relOpExcept
import org.partiql.plan.relOpExclude
import org.partiql.plan.relOpExcludeItem
import org.partiql.plan.relOpExcludeStepAttr
import org.partiql.plan.relOpExcludeStepCollectionWildcard
import org.partiql.plan.relOpExcludeStepPos
import org.partiql.plan.relOpExcludeStepStructWildcard
import org.partiql.plan.relOpFilter
import org.partiql.plan.relOpIntersect
import org.partiql.plan.relOpJoin
import org.partiql.plan.relOpLimit
import org.partiql.plan.relOpOffset
import org.partiql.plan.relOpProject
import org.partiql.plan.relOpScan
import org.partiql.plan.relOpSort
import org.partiql.plan.relOpSortSpec
import org.partiql.plan.relOpUnion
import org.partiql.plan.relOpUnpivot
import org.partiql.plan.relType
import org.partiql.plan.rex
import org.partiql.plan.rexOpLit
import org.partiql.plan.rexOpPivot
import org.partiql.plan.rexOpSelect
import org.partiql.plan.rexOpVarResolved
import org.partiql.planner.Env
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.boolValue

/**
 * Lexically scoped state for use in translating an individual SELECT statement.
 */
internal object RelConverter {

    // IGNORE — so we don't have to non-null assert on operator inputs
    private val nil = rel(relType(emptyList(), emptySet()), relOpErr("nil"))

    /**
     * Here we convert an SFW to composed [Rel]s, then apply the appropriate relation-value projection to get a [Rex].
     */
    internal fun apply(sfw: Expr.SFW, env: Env): Rex {
        val rel = sfw.accept(ToRel(env), nil)
        val rex = when (val projection = sfw.select) {
            // PIVOT ... FROM
            is Select.Pivot -> {
                val key = projection.key.toRex(env)
                val value = projection.value.toRex(env)
                val type = (StaticType.STRUCT)
                val op = rexOpPivot(key, value, rel)
                rex(type, op)
            }
            // SELECT VALUE ... FROM
            is Select.Value -> {
                assert(rel.type.schema.size == 1) {
                    "Expected SELECT VALUE's input to have a single binding. " +
                        "However, it contained: ${rel.type.schema.map { it.name }}."
                }
                val constructor = rex(StaticType.ANY, rexOpVarResolved(0))
                val op = rexOpSelect(constructor, rel)
                val type = when (rel.type.props.contains(Rel.Prop.ORDERED)) {
                    true -> (StaticType.LIST)
                    else -> (StaticType.BAG)
                }
                rex(type, op)
            }
            // SELECT * FROM
            is Select.Star -> {
                throw IllegalArgumentException("AST not normalized")
            }
            // SELECT ... FROM
            is Select.Project -> {
                throw IllegalArgumentException("AST not normalized")
            }
        }
        return rex
    }

    /**
     * Syntax sugar for converting an [Expr] tree to a [Rex] tree.
     */
    private fun Expr.toRex(env: Env): Rex = RexConverter.apply(this, env)

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE", "LocalVariableName")
    private class ToRel(private val env: Env) : AstBaseVisitor<Rel, Rel>() {

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
            // Plan.create (possibly rewritten) sel node
            rel = convertHaving(rel, sel.having)
            rel = convertSetOp(rel, sel.setOp)
            rel = convertOrderBy(rel, sel.orderBy)
            rel = convertLimit(rel, sel.limit)
            rel = convertOffset(rel, sel.offset)
            rel = convertExclude(rel, sel.exclude)
            // append SQL projection if present
            rel = when (val projection = sel.select) {
                is Select.Project -> visitSelectProject(projection, rel)
                is Select.Value -> visitSelectValue(projection, rel)
                is Select.Star -> error("AST not normalized, found project star")
                else -> rel // skip PIVOT and SELECT VALUE
            }
            return rel
        }

        override fun visitSelectProject(node: Select.Project, input: Rel): Rel {
            // this ignores aggregations
            val schema = mutableListOf<Rel.Binding>()
            val props = input.type.props
            val projections = mutableListOf<Rex>()
            node.items.forEach {
                val (binding, projection) = convertProjectionItem(it)
                schema.add(binding)
                projections.add(projection)
            }
            val type = relType(schema, props)
            val op = relOpProject(input, projections)
            return rel(type, op)
        }

        override fun visitSelectValue(node: Select.Value, input: Rel): Rel {
            val name = node.constructor.toBinder(1).symbol
            val rex = RexConverter.apply(node.constructor, env)
            val schema = listOf(relBinding(name, rex.type))
            val props = input.type.props
            val type = relType(schema, props)
            val op = relOpProject(input, projections = listOf(rex))
            return rel(type, op)
        }

        override fun visitFromValue(node: From.Value, nil: Rel): Rel {
            val rex = RexConverter.apply(node.expr, env)
            val binding = when (val a = node.asAlias) {
                null -> error("AST not normalized, missing AS alias on $node")
                else -> relBinding(
                    name = a.symbol,
                    type = rex.type
                )
            }
            return when (node.type) {
                From.Value.Type.SCAN -> {
                    when (val i = node.atAlias) {
                        null -> convertScan(rex, binding)
                        else -> {
                            val index = relBinding(
                                name = i.symbol,
                                type = (StaticType.INT)
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
                            type = (StaticType.STRING)
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
        @OptIn(PartiQLValueExperimental::class)
        override fun visitFromJoin(node: From.Join, nil: Rel): Rel {
            val lhs = visitFrom(node.lhs, nil)
            val rhs = visitFrom(node.rhs, nil)
            val schema = listOf<Rel.Binding>()
            val props = emptySet<Rel.Prop>()
            val condition = node.condition?.let { RexConverter.apply(it, env) } ?: rex(StaticType.BOOL, rexOpLit(boolValue(true)))
            val joinType = when (node.type) {
                From.Join.Type.LEFT_OUTER, From.Join.Type.LEFT -> Rel.Op.Join.Type.LEFT
                From.Join.Type.RIGHT_OUTER, From.Join.Type.RIGHT -> Rel.Op.Join.Type.RIGHT
                From.Join.Type.FULL_OUTER, From.Join.Type.FULL -> Rel.Op.Join.Type.FULL
                From.Join.Type.COMMA,
                From.Join.Type.INNER,
                From.Join.Type.CROSS -> Rel.Op.Join.Type.INNER // Cross Joins are just INNER JOIN ON TRUE
                null -> Rel.Op.Join.Type.INNER // a JOIN b ON a.id = b.id <--> a INNER JOIN b ON a.id = b.id
            }
            val type = relType(schema, props)
            val op = relOpJoin(lhs, rhs, condition, joinType)
            return rel(type, op)
        }

        // Helpers

        private fun convertScan(rex: Rex, binding: Rel.Binding): Rel {
            val schema = listOf(binding)
            val props = emptySet<Rel.Prop>()
            val type = relType(schema, props)
            val op = relOpScan(rex)
            return rel(type, op)
        }

        private fun convertScanIndexed(rex: Rex, binding: Rel.Binding, index: Rel.Binding): Rel {
            val schema = listOf(binding, index)
            val props = setOf(Rel.Prop.ORDERED)
            val type = relType(schema, props)
            val op = relOpScan(rex)
            return rel(type, op)
        }

        /**
         * Output schema of an UNPIVOT is < k, v >
         *
         * @param rex
         * @param k
         * @param v
         */
        private fun convertUnpivot(rex: Rex, k: Rel.Binding, v: Rel.Binding): Rel {
            val schema = listOf(k, v)
            val props = emptySet<Rel.Prop>()
            val type = relType(schema, props)
            val op = relOpUnpivot(rex)
            return rel(type, op)
        }

        private fun convertProjectionItem(item: Select.Project.Item) = when (item) {
            is Select.Project.Item.All -> convertProjectItemAll(item)
            is Select.Project.Item.Expression -> convertProjectItemRex(item)
        }

        private fun convertProjectItemAll(item: Select.Project.Item.All): Pair<Rel.Binding, Rex> {
            throw IllegalArgumentException("AST not normalized")
        }

        private fun convertProjectItemRex(item: Select.Project.Item.Expression): Pair<Rel.Binding, Rex> {
            val name = when (val a = item.asAlias) {
                null -> error("AST not normalized, missing AS alias on projection item $item")
                else -> a.symbol
            }
            val rex = RexConverter.apply(item.expr, env)
            val binding = relBinding(name, rex.type)
            return binding to rex
        }

        /**
         * Append [Rel.Op.Filter] only if a WHERE condition exists
         */
        private fun convertWhere(input: Rel, expr: Expr?): Rel {
            if (expr == null) {
                return input
            }
            val type = input.type
            val predicate = expr.toRex(env)
            val op = relOpFilter(input, predicate)
            return rel(type, op)
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

            // Build the schema -> (calls... groups...)
            val schema = mutableListOf<Rel.Binding>()
            val props = emptySet<Rel.Prop>()

            // Build the rel operator
            var strategy = Rel.Op.Aggregate.Strategy.FULL
            val calls = aggregations.mapIndexed { i, expr ->
                val binding = relBinding(
                    name = syntheticAgg(i),
                    type = (StaticType.ANY),
                )
                schema.add(binding)
                val args = expr.args.map { arg -> arg.toRex(env) }
                val id = AstToPlan.convert(expr.function)
                val fn = aggUnresolved(id)
                relOpAggregateCall(fn, args)
            }
            var groups = emptyList<Rex>()
            if (groupBy != null) {
                groups = groupBy.keys.map {
                    if (it.asAlias == null) {
                        error("not normalized, group key $it missing unique name")
                    }
                    val binding = relBinding(
                        name = it.asAlias!!.symbol,
                        type = (StaticType.ANY)
                    )
                    schema.add(binding)
                    it.expr.toRex(env)
                }
                strategy = when (groupBy.strategy) {
                    GroupBy.Strategy.FULL -> Rel.Op.Aggregate.Strategy.FULL
                    GroupBy.Strategy.PARTIAL -> Rel.Op.Aggregate.Strategy.PARTIAL
                }
            }
            val type = relType(schema, props)
            val op = relOpAggregate(input, strategy, calls, groups)
            val rel = rel(type, op)
            return Pair(sel, rel)
        }

        /**
         * Append [Rel.Op.Filter] only if a HAVING condition exists
         *
         * Notes:
         *  - This currently does not support aggregation expressions in the WHERE condition
         */
        private fun convertHaving(input: Rel, expr: Expr?): Rel {
            if (expr == null) {
                return input
            }
            val type = input.type
            val predicate = expr.toRex(env)
            val op = relOpFilter(input, predicate)
            return rel(type, op)
        }

        /**
         * Append SQL set operator if present
         *
         * TODO combine/compare schemas
         * TODO set quantifier
         */
        private fun convertSetOp(input: Rel, setOp: Expr.SFW.SetOp?): Rel {
            if (setOp == null) {
                return input
            }
            val type = input.type.copy(props = emptySet())
            val lhs = input
            val rhs = visitExprSFW(setOp.operand, nil)
            val op = when (setOp.type.type) {
                SetOp.Type.UNION -> relOpUnion(lhs, rhs)
                SetOp.Type.INTERSECT -> relOpIntersect(lhs, rhs)
                SetOp.Type.EXCEPT -> relOpExcept(lhs, rhs)
            }
            return rel(type, op)
        }

        /**
         * Append [Rel.Op.Sort] only if an ORDER BY clause is present
         */
        private fun convertOrderBy(input: Rel, orderBy: OrderBy?): Rel {
            if (orderBy == null) {
                return input
            }
            val type = input.type.copy(props = setOf(Rel.Prop.ORDERED))
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
            return rel(type, op)
        }

        /**
         * Append [Rel.Op.Limit] if there is a LIMIT
         */
        private fun convertLimit(input: Rel, limit: Expr?): Rel {
            if (limit == null) {
                return input
            }
            val type = input.type
            val rex = RexConverter.apply(limit, env)
            val op = relOpLimit(input, rex)
            return rel(type, op)
        }

        /**
         * Append [Rel.Op.Offset] if there is an OFFSET
         */
        private fun convertOffset(input: Rel, offset: Expr?): Rel {
            if (offset == null) {
                return input
            }
            val type = input.type
            val rex = RexConverter.apply(offset, env)
            val op = relOpOffset(input, rex)
            return rel(type, op)
        }

        private fun convertExclude(input: Rel, exclude: Exclude?): Rel {
            if (exclude == null) {
                return input
            }
            val type = input.type // PlanTyper handles typing the exclusion
            val items = exclude.exprs.map { convertExcludeItem(it) }
            val op = relOpExclude(input, items)
            return rel(type, op)
        }

        private fun convertExcludeItem(expr: Exclude.ExcludeExpr): Rel.Op.Exclude.Item {
            val root = AstToPlan.convert(expr.root)
            val steps = expr.steps.map { convertExcludeStep(it) }
            return relOpExcludeItem(root, steps)
        }

        private fun convertExcludeStep(step: Exclude.Step): Rel.Op.Exclude.Step = when (step) {
            is Exclude.Step.ExcludeTupleAttr -> relOpExcludeStepAttr(AstToPlan.convert(step.symbol))
            is Exclude.Step.ExcludeCollectionIndex -> relOpExcludeStepPos(step.index)
            is Exclude.Step.ExcludeCollectionWildcard -> relOpExcludeStepCollectionWildcard()
            is Exclude.Step.ExcludeTupleWildcard -> relOpExcludeStepStructWildcard()
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
