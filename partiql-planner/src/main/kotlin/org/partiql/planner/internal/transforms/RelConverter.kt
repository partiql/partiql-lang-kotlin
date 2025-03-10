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

import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.AstNode
import org.partiql.ast.AstRewriter
import org.partiql.ast.AstVisitor
import org.partiql.ast.Exclude
import org.partiql.ast.ExcludeStep
import org.partiql.ast.From
import org.partiql.ast.FromExpr
import org.partiql.ast.FromJoin
import org.partiql.ast.FromType
import org.partiql.ast.GroupBy
import org.partiql.ast.GroupByStrategy
import org.partiql.ast.Identifier
import org.partiql.ast.JoinType
import org.partiql.ast.Literal.intNum
import org.partiql.ast.Nulls
import org.partiql.ast.Order
import org.partiql.ast.OrderBy
import org.partiql.ast.QueryBody
import org.partiql.ast.SelectItem
import org.partiql.ast.SelectList
import org.partiql.ast.SelectPivot
import org.partiql.ast.SelectStar
import org.partiql.ast.SelectValue
import org.partiql.ast.SetOpType
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Sort
import org.partiql.ast.WindowFunctionNullTreatment
import org.partiql.ast.WindowFunctionSimpleName
import org.partiql.ast.WindowFunctionType
import org.partiql.ast.WindowPartition
import org.partiql.ast.WindowReference
import org.partiql.ast.With
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprWindowFunction
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.PErrors
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
import org.partiql.planner.internal.ir.relOpWindow
import org.partiql.planner.internal.ir.relOpWindowWindowFunction
import org.partiql.planner.internal.ir.relOpWith
import org.partiql.planner.internal.ir.relOpWithWithListElement
import org.partiql.planner.internal.ir.relType
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpLit
import org.partiql.planner.internal.ir.rexOpPivot
import org.partiql.planner.internal.ir.rexOpSelect
import org.partiql.planner.internal.ir.rexOpStruct
import org.partiql.planner.internal.ir.rexOpStructField
import org.partiql.planner.internal.ir.rexOpVarLocal
import org.partiql.planner.internal.ir.rexOpVarUnresolved
import org.partiql.planner.internal.transforms.RelConverter.toRex
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.planner.internal.typer.PlanTyper.Companion.toCType
import org.partiql.planner.internal.util.BinderUtils.toBinder
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

/**
 * Lexically scoped state for use in translating an individual SELECT statement.
 */
internal object RelConverter {

    // IGNORE — so we don't have to non-null assert on operator inputs
    internal val nil = rel(relType(emptyList(), emptySet()), relOpErr("nil"))

    /**
     * Here we convert an SFW to composed [Rel]s, then apply the appropriate relation-value projection to get a [Rex].
     */
    internal fun apply(qSet: ExprQuerySet, env: Env): Rex {
        val newQSet = NormalizeSelect.normalize(qSet)
        val rex = when (val body = newQSet.body) {
            is QueryBody.SFW -> {
                val rel = newQSet.accept(ToRel(env), nil)
                when (val projection = body.select) {
                    // PIVOT ... FROM
                    is SelectPivot -> {
                        val key = projection.key.toRex(env)
                        val value = projection.value.toRex(env)
                        val type = (STRUCT)
                        val op = rexOpPivot(key, value, rel)
                        rex(type, op)
                    }
                    // SELECT VALUE ... FROM
                    is SelectValue -> {
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
                    is SelectStar -> {
                        throw IllegalArgumentException("AST not normalized")
                    }
                    // SELECT ... FROM
                    is SelectList -> {
                        throw IllegalArgumentException("AST not normalized")
                    }
                    else -> error("Unexpected Select type: $projection")
                }
            }
            is QueryBody.SetOp -> {
                val rel = newQSet.accept(ToRel(env), nil)
                val constructor = rex(ANY, rexOpVarLocal(0, 0))
                val op = rexOpSelect(constructor, rel)
                val type = when (rel.type.props.contains(Rel.Prop.ORDERED)) {
                    true -> (LIST)
                    else -> (BAG)
                }
                rex(type, op)
            }
            else -> error("Unexpected QueryBody type: ${newQSet.body}")
        }
        return rex
    }

    /**
     * Syntax sugar for converting an [Expr] tree to a [Rex] tree.
     */
    private fun Expr.toRex(env: Env): Rex = RexConverter.apply(this, env)

    @Suppress("LocalVariableName")
    internal class ToRel(private val env: Env) : AstVisitor<Rel, Rel>() {

        override fun defaultReturn(node: AstNode, input: Rel): Rel =
            throw IllegalArgumentException("unsupported rel $node")

        /**
         * Translate SFW AST node to a pipeline of [Rel] operators; skip any SELECT VALUE or PIVOT projection.
         */

        override fun visitExprQuerySet(node: ExprQuerySet, ctx: Rel): Rel {
            val body = node.body
            val orderBy = node.orderBy
            val limit = node.limit
            val offset = node.offset
            val with = node.with
            when (body) {
                is QueryBody.SFW -> {
                    var sel = body
                    var rel = visitFrom(sel.from, nil)
                    rel = convertWhere(rel, sel.where)
                    // kotlin does not have destructuring reassignment
                    val (_sel, _rel) = convertAgg(rel, sel, sel.groupBy)
                    sel = _sel
                    rel = _rel
                    // Plan.create (possibly rewritten) sel node
                    rel = convertHaving(rel, sel.having)
                    val (windowSel, windowRel) = convertWindow(rel, sel)
                    rel = windowRel
                    sel = windowSel
                    rel = convertOrderBy(rel, orderBy)
                    // offset should precede limit
                    rel = convertOffset(rel, offset)
                    rel = convertLimit(rel, limit)
                    rel = convertExclude(rel, sel.exclude)
                    // append SQL projection if present
                    rel = when (val projection = sel.select) {
                        is SelectValue -> {
                            val project = visitSelectValue(projection, rel)
                            visitSetQuantifier(projection.setq, project)
                        }
                        is SelectStar, is SelectList -> {
                            error("AST not normalized, found ${projection.javaClass.simpleName}")
                        }
                        is SelectPivot -> rel // Skip PIVOT
                        else -> error("Unexpected Select type: $projection")
                    }
                    rel = convertWith(rel, with)
                    return rel
                }
                is QueryBody.SetOp -> {
                    var rel = convertSetOp(body)
                    rel = convertOrderBy(rel, orderBy)
                    // offset should precede limit
                    rel = convertOffset(rel, offset)
                    rel = convertLimit(rel, limit)
                    rel = convertWith(rel, node.with)
                    return rel
                }
                else -> error("Unexpected QueryBody type: $body")
            }
        }

        /**
         * Given a [setQuantifier], this will return a [Rel] of [Rel.Op.Distinct] wrapping the [input].
         * If [setQuantifier] is null or ALL, this will return the [input].
         */
        private fun visitSetQuantifier(setQuantifier: SetQuantifier?, input: Rel): Rel {
            return when (setQuantifier?.code()) {
                SetQuantifier.DISTINCT -> rel(input.type, relOpDistinct(input))
                SetQuantifier.ALL, null -> input
                else -> error("Unexpected SetQuantifier type: $setQuantifier")
            }
        }

        override fun visitSelectList(node: SelectList, input: Rel): Rel {
            // this ignores aggregations
            val schema = mutableListOf<Rel.Binding>()
            val props = input.type.props
            val projections = mutableListOf<Rex>()
            node.items.forEach {
                val (binding, projection) = convertSelectItem(it)
                schema.add(binding)
                projections.add(projection)
            }
            val type = relType(schema, props)
            val op = relOpProject(input, projections)
            return rel(type, op)
        }

        override fun visitSelectValue(node: SelectValue, input: Rel): Rel {
            val name = node.constructor.toBinder(1).text
            val rex = RexConverter.apply(node.constructor, env)
            val schema = listOf(relBinding(name, rex.type))
            val props = input.type.props
            val type = relType(schema, props)
            val op = relOpProject(input, projections = listOf(rex))
            return rel(type, op)
        }

        override fun visitFrom(node: From, ctx: Rel): Rel {
            val tableRefs = node.tableRefs.map { visitFromTableRef(it, ctx) }
            return tableRefs.drop(1).fold(tableRefs.first()) { acc, tRef ->
                val joinType = Rel.Op.Join.Type.INNER
                val condition = rex(BOOL, rexOpLit(Datum.bool(true)))
                val schema = acc.type.schema + tRef.type.schema
                val props = emptySet<Rel.Prop>()
                val type = relType(schema, props)
                val op = relOpJoin(acc, tRef, condition, joinType)
                rel(type, op)
            }
        }

        override fun visitFromExpr(node: FromExpr, nil: Rel): Rel {
            val rex = RexConverter.applyRel(node.expr, env)
            val binding = when (val a = node.asAlias) {
                null -> error("AST not normalized, missing AS alias on $node")
                else -> relBinding(
                    name = a.text,
                    type = rex.type
                )
            }
            return when (node.fromType.code()) {
                FromType.SCAN -> {
                    when (val i = node.atAlias) {
                        null -> convertScan(rex, binding)
                        else -> {
                            val index = relBinding(
                                name = i.text,
                                type = (INT)
                            )
                            convertScanIndexed(rex, binding, index)
                        }
                    }
                }
                FromType.UNPIVOT -> {
                    val atAlias = when (val at = node.atAlias) {
                        null -> error("AST not normalized, missing AT alias on UNPIVOT $node")
                        else -> relBinding(
                            name = at.text,
                            type = (STRING)
                        )
                    }
                    convertUnpivot(rex, k = atAlias, v = binding)
                }
                else -> error("Unexpected FromType type: ${node.fromType}")
            }
        }

        /**
         * Appends [Rel.Op.Join] where the left and right sides are converted FROM sources
         *
         * TODO compute basic schema
         */
        override fun visitFromJoin(node: FromJoin, nil: Rel): Rel {
            val lhs = visitFromTableRef(node.lhs, nil)
            val rhs = visitFromTableRef(node.rhs, nil)
            val schema = lhs.type.schema + rhs.type.schema // Note: This gets more specific in PlanTyper. It is only used to find binding names here.
            val props = emptySet<Rel.Prop>()
            val condition = node.condition?.let { RexConverter.apply(it, env) } ?: rex(BOOL, rexOpLit(Datum.bool(true)))
            val joinType = when (node.joinType?.code()) {
                JoinType.LEFT_OUTER, JoinType.LEFT, JoinType.LEFT_CROSS -> Rel.Op.Join.Type.LEFT
                JoinType.RIGHT_OUTER, JoinType.RIGHT -> Rel.Op.Join.Type.RIGHT
                JoinType.FULL_OUTER, JoinType.FULL -> Rel.Op.Join.Type.FULL
                JoinType.INNER,
                JoinType.CROSS -> Rel.Op.Join.Type.INNER // Cross Joins are just INNER JOIN ON TRUE
                null -> Rel.Op.Join.Type.INNER // a JOIN b ON a.id = b.id <--> a INNER JOIN b ON a.id = b.id
                else -> error("Unexpected JoinType type: ${node.joinType}")
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

        private fun convertSelectItem(item: SelectItem) = when (item) {
            is SelectItem.Star -> convertSelectItemStar(item)
            is SelectItem.Expr -> convertSelectItemExpr(item)
            else -> error("Unexpected SelectItem type: $item")
        }

        private fun convertSelectItemStar(item: SelectItem.Star): Pair<Rel.Binding, Rex> {
            throw IllegalArgumentException("AST not normalized")
        }

        private fun convertSelectItemExpr(item: SelectItem.Expr): Pair<Rel.Binding, Rex> {
            val name = when (val a = item.asAlias) {
                null -> error("AST not normalized, missing AS alias on select item $item")
                else -> a.text
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
        private fun convertAgg(input: Rel, select: QueryBody.SFW, groupBy: GroupBy?): Pair<QueryBody.SFW, Rel> {
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
                val args = expr.args.map { arg -> arg.toRex(env) }
                val id = AstToPlan.convert(expr.function)
                if (id.hasQualifier()) {
                    error("Qualified aggregation calls are not supported.")
                }
                // lowercase normalize all calls
                val name = id.getIdentifier().getText().lowercase()
                if (name == "count" && expr.args.isEmpty()) {
                    relOpAggregateCallUnresolved(
                        name,
                        org.partiql.planner.internal.ir.SetQuantifier.ALL,
                        args = listOf(exprLit(intNum(1)).toRex(env))
                    )
                } else {
                    val setq = when (expr.setq?.code()) {
                        null -> org.partiql.planner.internal.ir.SetQuantifier.ALL
                        SetQuantifier.ALL -> org.partiql.planner.internal.ir.SetQuantifier.ALL
                        SetQuantifier.DISTINCT -> org.partiql.planner.internal.ir.SetQuantifier.DISTINCT
                        else -> error("Unexpected SetQuantifier type: ${expr.setq}")
                    }
                    relOpAggregateCallUnresolved(name, setq, args)
                }
            }.toMutableList()

            // Add GROUP_AS aggregation
            groupBy?.let { gb ->
                gb.asAlias?.let { groupAs ->
                    val binding = relBinding(groupAs.text, ANY)
                    schema.add(binding)
                    val fields = input.type.schema.mapIndexed { bindingIndex, currBinding ->
                        rexOpStructField(
                            k = rex(STRING, rexOpLit(Datum.string(currBinding.name))),
                            v = rex(ANY, rexOpVarLocal(0, bindingIndex))
                        )
                    }
                    val arg = listOf(rex(ANY, rexOpStruct(fields)))
                    calls.add(relOpAggregateCallUnresolved("group_as", org.partiql.planner.internal.ir.SetQuantifier.ALL, arg))
                }
            }
            var groups = emptyList<Rex>()
            if (groupBy != null) {
                groups = groupBy.keys.map {
                    if (it.asAlias == null) {
                        error("not normalized, group key $it missing unique name")
                    }
                    val binding = relBinding(
                        name = it.asAlias!!.text,
                        type = (ANY)
                    )
                    schema.add(binding)
                    it.expr.toRex(env)
                }
                strategy = when (groupBy.strategy.code()) {
                    GroupByStrategy.FULL -> Rel.Op.Aggregate.Strategy.FULL
                    GroupByStrategy.PARTIAL -> Rel.Op.Aggregate.Strategy.PARTIAL
                    else -> error("Unexpected GroupByStrategy type: ${groupBy.strategy}")
                }
            }
            val type = relType(schema, props)
            val op = relOpAggregate(input, strategy, calls, groups)
            val rel = rel(type, op)
            return Pair(sel, rel)
        }

        /**
         * Append [Rel.Op.Window] only if SELECT contains window functions.
         * TODO: Handle the window clause itself.
         */
        private fun convertWindow(input: Rel, select: QueryBody.SFW): Pair<QueryBody.SFW, Rel> {
            // Rewrite and extract all window functions in the SELECT clause
            val (sel, windowFunctions) = WindowTransform.apply(select)
            if (windowFunctions.isEmpty()) {
                return Pair(select, input)
            }

            // Convert nodes to plan nodes, fold to each be their own windows (currently), and return!
            // Schema = (input bindings... functions...)
            val props = emptySet<Rel.Prop>()
            val rel = windowFunctions.foldRight(input) { (bindingName, function), current ->
                val window = when (val ref = function.windowReference) {
                    is WindowReference.InLineSpecification -> ref
                    is WindowReference.Name -> error("Referencing window specifications by name is currently not supported") // TODO
                    else -> error("Window Reference ${ref.javaClass.simpleName} not supported.") // TODO
                }
                val orderBy = window.specification.orderClause?.sorts?.map { convertSort(it) } ?: emptyList()
                val partitions = window.specification.partitionClause?.partitions?.map {
                    if (it !is WindowPartition.Name) {
                        error("Window Partition ${it.javaClass.simpleName} not supported.")
                    }
                    val op = rexOpVarUnresolved(AstToPlan.convert(it.name), Rex.Op.Var.Scope.LOCAL)
                    rex(PType.dynamic().toCType(), op)
                } ?: emptyList()
                val functionNode = convertWindowFunction(function)
                val newSchema = current.type.schema + listOf(relBinding(bindingName, PType.dynamic().toCType())) // TODO: Dynamic?
                val type = relType(newSchema, props)
                val op = relOpWindow(current, listOf(functionNode), partitions, orderBy)
                rel(type, op)
            }
            return Pair(sel, rel)
        }

        private fun convertSort(it: Sort): Rel.Op.Sort.Spec {
            val rex = it.expr.toRex(env)
            val order = when (it.order?.code()) {
                Order.DESC -> when (it.nulls?.code()) {
                    Nulls.LAST -> Rel.Op.Sort.Order.DESC_NULLS_LAST
                    Nulls.FIRST, null -> Rel.Op.Sort.Order.DESC_NULLS_FIRST
                    else -> error("Unexpected Nulls type: ${it.nulls}")
                }
                else -> when (it.nulls?.code()) {
                    Nulls.FIRST -> Rel.Op.Sort.Order.ASC_NULLS_FIRST
                    Nulls.LAST, null -> Rel.Op.Sort.Order.ASC_NULLS_LAST
                    else -> error("Unexpected Nulls type: ${it.nulls}")
                }
            }
            return relOpSortSpec(rex, order)
        }

        private fun convertWindowFunction(node: ExprWindowFunction): Rel.Op.Window.WindowFunction {
            return when (val windowType = node.functionType) {
                is WindowFunctionType.NoArg -> {
                    val name = when (windowType.name.code()) {
                        WindowFunctionSimpleName.RANK -> "rank"
                        WindowFunctionSimpleName.ROW_NUMBER -> "row_number"
                        WindowFunctionSimpleName.DENSE_RANK -> "dense_rank"
                        else -> error("Unexpected WindowFunctionSimpleName type: ${windowType.name}")
                    }
                    // TODO: Check what the output type should be
                    // TODO: Check if isIgnoreNulls is true/false by default
                    relOpWindowWindowFunction(name, emptyList(), false, emptyList(), CompilerType(PType.bigint()))
                }
                is WindowFunctionType.LeadOrLag -> {
                    val name = when (windowType.isLag) {
                        true -> "lag"
                        false -> "lead"
                    }
                    val isIgnoreNulls = when (windowType.nullTreatment?.code()) {
                        WindowFunctionNullTreatment.RESPECT_NULLS, null -> false
                        WindowFunctionNullTreatment.IGNORE_NULLS -> true
                        else -> error("Unexpected WindowFunctionNullTreatment type: ${windowType.nullTreatment}")
                    }
                    val extent = windowType.extent.toRex(env)
                    val offset = windowType.offset?.let { rex(CompilerType(PType.bigint()), rexOpLit(Datum.bigint(it))) }
                    val default = windowType.defaultValue?.toRex(env)
                    val args = listOfNotNull(extent, offset, default) // TODO: Check if there are good defaults for these null values
                    relOpWindowWindowFunction(name, args, isIgnoreNulls, null, null)
                }
                else -> error("Unexpected WindowFunctionType type: $windowType")
            }
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

        private fun visitIfQuerySet(expr: Expr): Rel {
            return when (expr) {
                is ExprQuerySet -> visit(expr, nil)
                else -> {
                    val rex = RexConverter.applyRel(expr, env)
                    val op = relOpScan(rex)
                    val type = Rel.Type(listOf(Rel.Binding("_1", ANY)), props = emptySet())
                    return rel(type, op)
                }
            }
        }

        /**
         * Append SQL set operator if present
         */
        private fun convertSetOp(setExpr: QueryBody.SetOp): Rel {
            val lhs = visitIfQuerySet(setExpr.lhs)
            val rhs = visitIfQuerySet(setExpr.rhs)
            val type = Rel.Type(listOf(Rel.Binding("_0", ANY)), props = emptySet())
            val quantifier = when (setExpr.type.setq?.code()) {
                SetQuantifier.ALL -> org.partiql.planner.internal.ir.SetQuantifier.ALL
                null, SetQuantifier.DISTINCT -> org.partiql.planner.internal.ir.SetQuantifier.DISTINCT
                else -> error("Unexpected SetQuantifier type: ${setExpr.type.setq}")
            }
            val outer = setExpr.isOuter
            val op = when (setExpr.type.setOpType.code()) {
                SetOpType.UNION -> Rel.Op.Union(quantifier, outer, lhs, rhs)
                SetOpType.EXCEPT -> Rel.Op.Except(quantifier, outer, lhs, rhs)
                SetOpType.INTERSECT -> Rel.Op.Intersect(quantifier, outer, lhs, rhs)
                else -> error("Unexpected SetOpType type: ${setExpr.type.setOpType}")
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
                val order = when (it.order?.code()) {
                    Order.DESC -> when (it.nulls?.code()) {
                        Nulls.LAST -> Rel.Op.Sort.Order.DESC_NULLS_LAST
                        Nulls.FIRST, null -> Rel.Op.Sort.Order.DESC_NULLS_FIRST
                        else -> error("Unexpected Nulls type: ${it.nulls}")
                    }
                    else -> when (it.nulls?.code()) {
                        Nulls.FIRST -> Rel.Op.Sort.Order.ASC_NULLS_FIRST
                        Nulls.LAST, null -> Rel.Op.Sort.Order.ASC_NULLS_LAST
                        else -> error("Unexpected Nulls type: ${it.nulls}")
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

        /**
         * Append [Rel.Op.With] if there is a WITH clause
         */
        private fun convertWith(input: Rel, with: With?): Rel {
            if (with == null) {
                return input
            }
            if (with.isRecursive) {
                env.listener.report(PErrors.featureNotSupported("WITH RECURSIVE"))
            }
            val type = input.type
            val elements = with.elements.map { element ->
                if (element.columnList != null) {
                    env.listener.report(PErrors.featureNotSupported("WITH clause column list"))
                }
                val query = RexConverter.apply(element.asQuery, env)
                relOpWithWithListElement(element.queryName.text, query)
            }
            val op = relOpWith(input, elements)
            return rel(type, op)
        }

        private fun convertExclude(input: Rel, exclude: Exclude?): Rel {
            if (exclude == null) {
                return input
            }
            val type = input.type // PlanTyper handles typing the exclusion and removing redundant exclude paths
            val paths = exclude.excludePaths
                .groupBy(keySelector = { it.root }, valueTransform = { it.excludeSteps })
                .map { (root, exclusions) ->
                    val rootVar = (root.toRex(env)).op as Rex.Op.Var
                    val steps = exclusionsToSteps(exclusions)
                    relOpExcludePath(rootVar, steps)
                }
            val op = relOpExclude(input, paths)
            return rel(type, op)
        }

        private fun exclusionsToSteps(exclusions: List<List<ExcludeStep>>): List<Rel.Op.Exclude.Step> {
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

        private fun stepToExcludeType(step: ExcludeStep): Rel.Op.Exclude.Type {
            return when (step) {
                is ExcludeStep.StructField -> {
                    when (step.symbol.isRegular) {
                        true -> relOpExcludeTypeStructSymbol(step.symbol.text)
                        false -> relOpExcludeTypeStructKey(step.symbol.text)
                    }
                }
                is ExcludeStep.CollIndex -> relOpExcludeTypeCollIndex(step.index)
                is ExcludeStep.StructWildcard -> relOpExcludeTypeStructWildcard()
                is ExcludeStep.CollWildcard -> relOpExcludeTypeCollWildcard()
                else -> error("Unexpected ExcludeStep type: $step")
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
            val aggregations: MutableList<ExprCall>,
            val keys: List<GroupBy.Key>
        )

        fun apply(node: QueryBody.SFW): Pair<QueryBody.SFW, List<ExprCall>> {
            val aggs = mutableListOf<ExprCall>()
            val keys = node.groupBy?.keys ?: emptyList()
            val context = Context(aggs, keys)
            val select = super.visitQueryBodySFW(node, context) as QueryBody.SFW
            return Pair(select, aggs)
        }

        override fun visitSelectValue(node: SelectValue, ctx: Context): AstNode {
            val visited = super.visitSelectValue(node, ctx)
            val substitutions = ctx.keys.associate {
                it.expr to exprVarRef(Identifier.regular(it.asAlias!!.text), isQualified = false)
            }
            return SubstitutionVisitor.visit(visited, substitutions)
        }

        // only rewrite top-level SFW
        override fun visitQueryBodySFW(node: QueryBody.SFW, ctx: Context): AstNode = node

        override fun visitExprCall(node: ExprCall, ctx: Context) =
            // TODO replace w/ proper function resolution to determine whether a function call is a scalar or aggregate.
            //  may require further modification of SPI interfaces to support
            when (node.function.isAggregateCall()) {
                true -> {
                    val id = Identifier.delimited(syntheticAgg(ctx.aggregations.size))
                    ctx.aggregations += node
                    exprVarRef(id, isQualified = false)
                }
                else -> node
            }

        private fun String.isAggregateCall(): Boolean {
            return aggregates.contains(this)
        }

        private fun Identifier.isAggregateCall(): Boolean = identifier.text.lowercase().isAggregateCall()

        override fun defaultReturn(node: AstNode, context: Context) = node
    }

    private fun syntheticAgg(i: Int) = "\$agg_$i"

    private val ANY: CompilerType = CompilerType(PType.dynamic())
    private val BOOL: CompilerType = CompilerType(PType.bool())
    private val STRING: CompilerType = CompilerType(PType.string())
    private val STRUCT: CompilerType = CompilerType(PType.struct())
    private val BAG: CompilerType = CompilerType(PType.bag())
    private val LIST: CompilerType = CompilerType(PType.array())
    private val INT: CompilerType = CompilerType(PType.numeric(38, 0))
}
