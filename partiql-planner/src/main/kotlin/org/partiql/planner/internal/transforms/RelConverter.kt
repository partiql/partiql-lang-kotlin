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

package org.partiql.planner.internal.transforms

import org.partiql.ast.AstNode
import org.partiql.ast.Exclude
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.GroupBy
import org.partiql.ast.Identifier
import org.partiql.ast.OrderBy
import org.partiql.ast.Select
import org.partiql.ast.SetOp
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Sort
import org.partiql.ast.builder.ast
import org.partiql.ast.exprLit
import org.partiql.ast.exprVar
import org.partiql.ast.helpers.toBinder
import org.partiql.ast.identifierSymbol
import org.partiql.ast.util.AstRewriter
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.rel
import org.partiql.planner.internal.ir.relBinding
import org.partiql.planner.internal.ir.relOpAggregate
import org.partiql.planner.internal.ir.relOpAggregateCallUnresolved
import org.partiql.planner.internal.ir.relOpDistinct
import org.partiql.planner.internal.ir.relOpErr
import org.partiql.planner.internal.ir.relOpExclude
import org.partiql.planner.internal.ir.relOpExcludePath
import org.partiql.planner.internal.ir.relOpExcludeStep
import org.partiql.planner.internal.ir.relOpExcludeTypeCollIndex
import org.partiql.planner.internal.ir.relOpExcludeTypeCollWildcard
import org.partiql.planner.internal.ir.relOpExcludeTypeStructKey
import org.partiql.planner.internal.ir.relOpExcludeTypeStructSymbol
import org.partiql.planner.internal.ir.relOpExcludeTypeStructWildcard
import org.partiql.planner.internal.ir.relOpFilter
import org.partiql.planner.internal.ir.relOpJoin
import org.partiql.planner.internal.ir.relOpLimit
import org.partiql.planner.internal.ir.relOpOffset
import org.partiql.planner.internal.ir.relOpProject
import org.partiql.planner.internal.ir.relOpScan
import org.partiql.planner.internal.ir.relOpScanIndexed
import org.partiql.planner.internal.ir.relOpSort
import org.partiql.planner.internal.ir.relOpSortSpec
import org.partiql.planner.internal.ir.relOpUnpivot
import org.partiql.planner.internal.ir.relType
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpLit
import org.partiql.planner.internal.ir.rexOpPivot
import org.partiql.planner.internal.ir.rexOpSelect
import org.partiql.planner.internal.ir.rexOpStruct
import org.partiql.planner.internal.ir.rexOpStructField
import org.partiql.planner.internal.ir.rexOpVarLocal
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.types.PType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.boolValue
import org.partiql.value.int32Value
import org.partiql.value.stringValue
import org.partiql.planner.internal.ir.Identifier as InternalId

/**
 * Lexically scoped state for use in translating an individual SELECT statement.
 */
internal object RelConverter {

    // IGNORE — so we don't have to non-null assert on operator inputs
    private val nil = rel(relType(emptyList(), emptySet()), relOpErr("nil"))

    /**
     * Here we convert an SFW to composed [Rel]s, then apply the appropriate relation-value projection to get a [Rex].
     */
    internal fun apply(sfw: Expr.SFW): Rex {
        val normalizedSfw = NormalizeSelect.normalize(sfw)
        val rel = normalizedSfw.accept(ToRel(), nil)
        val rex = when (val projection = normalizedSfw.select) {
            // PIVOT ... FROM
            is Select.Pivot -> {
                val key = projection.key.toRex()
                val value = projection.value.toRex()
                val type = (STRUCT)
                val op = rexOpPivot(key, value, rel)
                rex(type, op)
            }
            // SELECT VALUE ... FROM
            is Select.Value -> {
                assert(rel.type.schema.size == 1) {
                    "Expected SELECT VALUE's input to have a single binding. " +
                        "However, it contained: ${rel.type.schema.map { it.name }}."
                }
                val constructor = rex(ANY, rexOpVarLocal(0, 0))
                val op = rexOpSelect(constructor, rel)
                val type = when (rel.type.props.contains(Rel.Prop.ORDERED)) {
                    true -> (LIST)
                    else -> (BAG)
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
    private fun Expr.toRex(): Rex = RexConverter.apply(this)

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE", "LocalVariableName")
    private class ToRel : AstBaseVisitor<Rel, Rel>() {

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
            // offset should precede limit
            rel = convertOffset(rel, sel.offset)
            rel = convertLimit(rel, sel.limit)
            rel = convertExclude(rel, sel.exclude)
            // append SQL projection if present
            rel = when (val projection = sel.select) {
                is Select.Value -> {
                    val project = visitSelectValue(projection, rel)
                    visitSetQuantifier(projection.setq, project)
                }
                is Select.Star, is Select.Project -> {
                    error("AST not normalized, found ${projection.javaClass.simpleName}")
                }
                is Select.Pivot -> rel // Skip PIVOT
            }
            return rel
        }

        /**
         * Given a non-null [setQuantifier], this will return a [Rel] of [Rel.Op.Distinct] wrapping the [input].
         * If [setQuantifier] is null or ALL, this will return the [input].
         */
        private fun visitSetQuantifier(setQuantifier: SetQuantifier?, input: Rel): Rel {
            return when (setQuantifier) {
                SetQuantifier.DISTINCT -> rel(input.type, relOpDistinct(input))
                SetQuantifier.ALL, null -> input
            }
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
            val rex = RexConverter.apply(node.constructor)
            val schema = listOf(relBinding(name, rex.type))
            val props = input.type.props
            val type = relType(schema, props)
            val op = relOpProject(input, projections = listOf(rex))
            return rel(type, op)
        }

        override fun visitFromValue(node: From.Value, nil: Rel): Rel {
            val rex = RexConverter.applyRel(node.expr)
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
                                type = (INT)
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
                            type = (STRING)
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
            val schema = lhs.type.schema + rhs.type.schema // Note: This gets more specific in PlanTyper. It is only used to find binding names here.
            val props = emptySet<Rel.Prop>()
            val condition = node.condition?.let { RexConverter.apply(it) } ?: rex(BOOL, rexOpLit(boolValue(true)))
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
            val props = emptySet<Rel.Prop>()
            val type = relType(schema, props)
            val op = relOpScanIndexed(rex)
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
            val rex = RexConverter.apply(item.expr)
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
            val predicate = expr.toRex()
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
        @OptIn(PartiQLValueExperimental::class)
        private fun convertAgg(input: Rel, select: Expr.SFW, groupBy: GroupBy?): Pair<Expr.SFW, Rel> {
            // Rewrite and extract all aggregations in the SELECT clause
            val (sel, aggregations) = AggregationTransform.apply(select)

            // No aggregation planning required for GROUP BY
            if (aggregations.isEmpty() && groupBy == null) {
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
                    type = (ANY),
                )
                schema.add(binding)
                val args = expr.args.map { arg -> arg.toRex() }
                val id = AstToPlan.convert(expr.function)
                val name = when (id) {
                    is InternalId.Qualified -> error("Qualified aggregation calls are not supported.")
                    is InternalId.Symbol -> id.symbol.lowercase()
                }
                if (name == "count" && expr.args.isEmpty()) {
                    relOpAggregateCallUnresolved(
                        name,
                        Rel.Op.Aggregate.SetQuantifier.ALL,
                        args = listOf(exprLit(int32Value(1)).toRex())
                    )
                } else {
                    val setq = when (expr.setq) {
                        null -> Rel.Op.Aggregate.SetQuantifier.ALL
                        SetQuantifier.ALL -> Rel.Op.Aggregate.SetQuantifier.ALL
                        SetQuantifier.DISTINCT -> Rel.Op.Aggregate.SetQuantifier.DISTINCT
                    }
                    relOpAggregateCallUnresolved(name, setq, args)
                }
            }.toMutableList()

            // Add GROUP_AS aggregation
            groupBy?.let { gb ->
                gb.asAlias?.let { groupAs ->
                    val binding = relBinding(groupAs.symbol, ANY)
                    schema.add(binding)
                    val fields = input.type.schema.mapIndexed { bindingIndex, currBinding ->
                        rexOpStructField(
                            k = rex(STRING, rexOpLit(stringValue(currBinding.name))),
                            v = rex(ANY, rexOpVarLocal(0, bindingIndex))
                        )
                    }
                    val arg = listOf(rex(ANY, rexOpStruct(fields)))
                    calls.add(relOpAggregateCallUnresolved("group_as", Rel.Op.Aggregate.SetQuantifier.ALL, arg))
                }
            }
            var groups = emptyList<Rex>()
            if (groupBy != null) {
                groups = groupBy.keys.map {
                    if (it.asAlias == null) {
                        error("not normalized, group key $it missing unique name")
                    }
                    val binding = relBinding(
                        name = it.asAlias!!.symbol,
                        type = (ANY)
                    )
                    schema.add(binding)
                    it.expr.toRex()
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
            val predicate = expr.toRex()
            val op = relOpFilter(input, predicate)
            return rel(type, op)
        }

        /**
         * Append SQL set operator if present
         */
        private fun convertSetOp(input: Rel, setOp: Expr.SFW.SetOp?): Rel {
            if (setOp == null) {
                return input
            }
            val type = input.type.copy(props = emptySet())
            val lhs = input
            val rhs = visitExprSFW(setOp.operand, nil)
            val quantifier = when (setOp.type.setq) {
                SetQuantifier.ALL -> Rel.Op.Set.Quantifier.ALL
                null, SetQuantifier.DISTINCT -> Rel.Op.Set.Quantifier.DISTINCT
            }
            val op = when (setOp.type.type) {
                SetOp.Type.UNION -> Rel.Op.Set.Union(quantifier, lhs, rhs, false)
                SetOp.Type.EXCEPT -> Rel.Op.Set.Except(quantifier, lhs, rhs, false)
                SetOp.Type.INTERSECT -> Rel.Op.Set.Intersect(quantifier, lhs, rhs, false)
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
                val rex = it.expr.toRex()
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
            val rex = RexConverter.apply(limit)
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
            val rex = RexConverter.apply(offset)
            val op = relOpOffset(input, rex)
            return rel(type, op)
        }

        private fun convertExclude(input: Rel, exclude: Exclude?): Rel {
            if (exclude == null) {
                return input
            }
            val type = input.type // PlanTyper handles typing the exclusion and removing redundant exclude paths
            val paths = exclude.items
                .groupBy(keySelector = { it.root }, valueTransform = { it.steps })
                .map { (root, exclusions) ->
                    val rootVar = (root.toRex()).op as Rex.Op.Var
                    val steps = exclusionsToSteps(exclusions)
                    relOpExcludePath(rootVar, steps)
                }
            val op = relOpExclude(input, paths)
            return rel(type, op)
        }

        private fun exclusionsToSteps(exclusions: List<List<Exclude.Step>>): List<Rel.Op.Exclude.Step> {
            if (exclusions.any { it.isEmpty() }) {
                // if there exists a path with no further steps, can remove the longer paths
                // e.g. t.a.b, t.a.b.c, t.a.b.d[*].*.e -> can keep just t.a.b
                return emptyList()
            }
            return exclusions
                .groupBy(keySelector = { it.first() }, valueTransform = { it.drop(1) })
                .map { (head, steps) ->
                    val type = stepToExcludeType(head)
                    val substeps = exclusionsToSteps(steps)
                    relOpExcludeStep(type, substeps)
                }
        }

        private fun stepToExcludeType(step: Exclude.Step): Rel.Op.Exclude.Type {
            return when (step) {
                is Exclude.Step.StructField -> {
                    when (step.symbol.caseSensitivity) {
                        Identifier.CaseSensitivity.INSENSITIVE -> relOpExcludeTypeStructSymbol(step.symbol.symbol)
                        Identifier.CaseSensitivity.SENSITIVE -> relOpExcludeTypeStructKey(step.symbol.symbol)
                    }
                }
                is Exclude.Step.CollIndex -> relOpExcludeTypeCollIndex(step.index)
                is Exclude.Step.StructWildcard -> relOpExcludeTypeStructWildcard()
                is Exclude.Step.CollWildcard -> relOpExcludeTypeCollWildcard()
            }
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
        //             name = Plan.rexLit(ionString(n), STRING),
        //             value = Plan.rexId(n, Case.SENSITIVE, Rex.Id.Qualifier.UNQUALIFIED, type = STRUCT)
        //         )
        //     }
        //     return Plan.binding(
        //         name = name,
        //         value = Plan.rexAgg(
        //             id = "group_as",
        //             args = listOf(Plan.rexTuple(fields, STRUCT)),
        //             modifier = Rex.Agg.Modifier.ALL,
        //             type = STRUCT
        //         )
        //     )
        // }
    }

    /**
     * Rewrites a SELECT node replacing (and extracting) each aggregation `i` with a synthetic field name `$agg_i`.
     */
    private object AggregationTransform : AstRewriter<AggregationTransform.Context>() {
        // currently hard-coded
        @JvmStatic
        private val aggregates = setOf("count", "avg", "sum", "min", "max", "any", "some", "every")

        private data class Context(
            val aggregations: MutableList<Expr.Call>,
            val keys: List<GroupBy.Key>
        )

        fun apply(node: Expr.SFW): Pair<Expr.SFW, List<Expr.Call>> {
            val aggs = mutableListOf<Expr.Call>()
            val keys = node.groupBy?.keys ?: emptyList()
            val context = Context(aggs, keys)
            val select = super.visitExprSFW(node, context) as Expr.SFW
            return Pair(select, aggs)
        }

        override fun visitSelectValue(node: Select.Value, ctx: Context): AstNode {
            val visited = super.visitSelectValue(node, ctx)
            val substitutions = ctx.keys.associate {
                it.expr to exprVar(identifierSymbol(it.asAlias!!.symbol, Identifier.CaseSensitivity.SENSITIVE), Expr.Var.Scope.DEFAULT)
            }
            return SubstitutionVisitor.visit(visited, substitutions)
        }

        // only rewrite top-level SFW
        override fun visitExprSFW(node: Expr.SFW, ctx: Context): AstNode = node

        override fun visitExprCall(node: Expr.Call, ctx: Context) = ast {
            // TODO replace w/ proper function resolution to determine whether a function call is a scalar or aggregate.
            //  may require further modification of SPI interfaces to support
            when (node.function.isAggregateCall()) {
                true -> {
                    val id = identifierSymbol {
                        symbol = syntheticAgg(ctx.aggregations.size)
                        caseSensitivity = org.partiql.ast.Identifier.CaseSensitivity.INSENSITIVE
                    }
                    ctx.aggregations += node
                    exprVar(id, Expr.Var.Scope.DEFAULT)
                }
                else -> node
            }
        }

        private fun String.isAggregateCall(): Boolean {
            return aggregates.contains(this)
        }

        private fun Identifier.isAggregateCall(): Boolean {
            return when (this) {
                is Identifier.Symbol -> this.symbol.lowercase().isAggregateCall()
                is Identifier.Qualified -> this.steps.last().symbol.lowercase().isAggregateCall()
            }
        }
    }

    private fun syntheticAgg(i: Int) = "\$agg_$i"

    private val ANY: CompilerType = CompilerType(PType.typeDynamic())
    private val BOOL: CompilerType = CompilerType(PType.typeBool())
    private val STRING: CompilerType = CompilerType(PType.typeString())
    private val STRUCT: CompilerType = CompilerType(PType.typeStruct())
    private val BAG: CompilerType = CompilerType(PType.typeBag())
    private val LIST: CompilerType = CompilerType(PType.typeList())
    private val INT: CompilerType = CompilerType(PType.typeIntArbitrary())
}
