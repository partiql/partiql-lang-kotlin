// package org.partiql.planner.impl.transforms
//
// import com.amazon.ionelement.api.ionInt
// import com.amazon.ionelement.api.ionString
// import org.partiql.ast.Ast
// import org.partiql.ast.AstNode
// import org.partiql.ast.Expr
// import org.partiql.ast.From
// import org.partiql.ast.GroupBy
// import org.partiql.ast.Identifier
// import org.partiql.ast.OrderBy
// import org.partiql.ast.Select
// import org.partiql.ast.Sort
// import org.partiql.ast.builder.ast
// import org.partiql.ast.util.AstRewriter
// import org.partiql.plan.Binding
// import org.partiql.plan.Case
// import org.partiql.plan.Plan
// import org.partiql.plan.Rel
// import org.partiql.plan.Rex
// import org.partiql.plan.SortSpec
// import org.partiql.types.StaticType
// import org.partiql.value.TextValue
// import org.partiql.value.int64Value
//
// /**
//  * Lexically scoped state for use in translating an individual SELECT statement.
//  */
// internal class RelConverter {
//
//     // synthetic binding name counter
//     private var i = 0
//
//     // generate a synthetic binding name
//     private fun nextBindingName(): String = "\$__v${i++}"
//
//     /**
//      * As of now, the COMMON property of relation operators is under development, so just use empty for now
//      */
//     private val empty = Plan.common(
//         typeEnv = emptyList(),
//         properties = emptySet(),
//         metas = emptyMap()
//     )
//
//     companion object {
//
//         /**
//          * Converts a SELECT-FROM-WHERE AST node to a [Rex.Query]
//          */
//         @JvmStatic
//         fun convert(sfw: Expr.SFW): Rex.Query = with(RelConverter()) {
//             val rel = convertSelect(sfw)
//             val rex = when (val projection = sfw.select) {
//                 // PIVOT ... FROM
//                 is Select.Pivot -> {
//                     Plan.rexQueryScalarPivot(
//                         rel = rel,
//                         value = RexConverter.convert(projection.value),
//                         at = RexConverter.convert(projection.key),
//                         type = null
//                     )
//                 }
//                 // SELECT VALUE ... FROM
//                 is Select.Value -> {
//                     Plan.rexQueryCollection(
//                         rel = rel,
//                         constructor = RexConverter.convert(projection.constructor),
//                         type = null
//                     )
//                 }
//                 // SELECT ... FROM
//                 else -> {
//                     Plan.rexQueryCollection(
//                         rel = rel,
//                         constructor = null,
//                         type = null
//                     )
//                 }
//             }
//             rex
//         }
//     }
//
//     /**
//      * Translate SFW AST node to a pipeline of [Rel] operators; skip any SELECT VALUE or PIVOT projection.
//      */
//     private fun convertSelect(sfw: Expr.SFW): Rel {
//         var sel = sfw
//         var rel = convertFrom(sel.from)
//         rel = convertWhere(rel, sel.where)
//         // kotlin does not have destructuring assignment
//         val (_sel, _rel) = convertAgg(rel, sel, sel.groupBy)
//         sel = _sel
//         rel = _rel
//         // transform (possibly rewritten) sel node
//         rel = convertHaving(rel, sel.having)
//         rel = convertOrderBy(rel, sel.orderBy)
//         rel = convertFetch(rel, sel.limit, sel.offset)
//         // append SQL projection if present
//         rel = when (val projection = sel.select) {
//             is Select.Project -> convertProjectList(rel, projection)
//             is Select.Star -> error("AST not normalized, found project star")
//             else -> rel // skip PIVOT and SELECT VALUE
//         }
//         return rel
//     }
//
//     /**
//      * Appends the appropriate [Rel] operator for the given FROM source
//      */
//     private fun convertFrom(from: From): Rel = when (from) {
//         is From.Join -> convertJoin(from)
//         is From.Value -> when (from.type) {
//             From.Value.Type.SCAN -> convertScan(from)
//             From.Value.Type.UNPIVOT -> convertUnpivot(from)
//         }
//     }
//
//     /**
//      * Appends [Rel.Join] where the left and right sides are converted FROM sources
//      */
//     private fun convertJoin(join: From.Join): Rel {
//         val lhs = convertFrom(join.lhs)
//         val rhs = convertFrom(join.rhs)
//         val condition = if (join.condition != null) RexConverter.convert(join.condition!!) else null
//         return Plan.relJoin(
//             common = empty,
//             lhs = lhs,
//             rhs = rhs,
//             condition = condition,
//             type = when (join.type) {
//                 From.Join.Type.FULL -> Rel.Join.Type.FULL
//                 From.Join.Type.INNER -> Rel.Join.Type.INNER
//                 From.Join.Type.LEFT -> Rel.Join.Type.LEFT
//                 From.Join.Type.RIGHT -> Rel.Join.Type.RIGHT
//                 else -> Rel.Join.Type.INNER
//             }
//         )
//     }
//
//     /**
//      * Appends [Rel.Scan] which takes no input relational expression
//      */
//     private fun convertScan(scan: From.Value) = Plan.relScan(
//         common = empty,
//         value = when (val expr = scan.expr) {
//             is Expr.SFW -> convert(expr)
//             else -> RexConverter.convert(scan.expr)
//         },
//         alias = scan.asAlias,
//         at = scan.atAlias,
//         by = scan.byAlias,
//     )
//
//     /**
//      * Appends [Rel.Unpivot] to range over attribute value pairs
//      */
//     private fun convertUnpivot(scan: From.Value) = Plan.relUnpivot(
//         common = empty,
//         value = RexConverter.convert(scan.expr),
//         alias = scan.asAlias,
//         at = scan.atAlias,
//         by = scan.byAlias,
//     )
//
//     /**
//      * Append [Rel.Filter] only if a WHERE condition exists
//      */
//     private fun convertWhere(input: Rel, expr: Expr?): Rel = when (expr) {
//         null -> input
//         else -> Plan.relFilter(
//             common = empty,
//             input = input,
//             condition = RexConverter.convert(expr)
//         )
//     }
//
//     /**
//      * Append [Rel.Aggregate] only if SELECT contains aggregate expressions.
//      *
//      * @return Pair<Ast.Expr.SFW, Rel> is returned where
//      *         1. Ast.Expr.SFW has every Ast.Expr.CallAgg replaced by a synthetic Ast.Expr.Id
//      *         2. Rel which has the appropriate Rex.Agg calls and Rex groups
//      */
//     private fun convertAgg(
//         input: Rel,
//         select: Expr.SFW,
//         groupBy: GroupBy?
//     ): Pair<Expr.SFW, Rel> {
//         // Rewrite and extract all aggregations in the SELECT clause
//         val (sel, aggregations) = AggregationTransform.apply(select)
//
//         // No aggregation planning required for GROUP BY
//         if (aggregations.isEmpty()) {
//             if (groupBy != null) {
//                 // As of now, GROUP BY with no aggregations is considered an error.
//                 error("GROUP BY with no aggregations in SELECT clause")
//             }
//             return Pair(select, input)
//         }
//
//         val calls = aggregations.toMutableList()
//         var groups = emptyList<Binding>()
//         var strategy = Rel.Aggregate.Strategy.FULL
//
//         if (groupBy != null) {
//             // GROUP AS is implemented as an aggregation function
//             if (groupBy.asAlias != null) {
//                 calls.add(convertGroupAs(groupBy.asAlias!!, sel.from))
//             }
//             groups = groupBy.keys.map { convertGroupByKey(it) }
//             strategy = when (groupBy.strategy) {
//                 GroupBy.Strategy.FULL -> Rel.Aggregate.Strategy.FULL
//                 GroupBy.Strategy.PARTIAL -> Rel.Aggregate.Strategy.PARTIAL
//             }
//         }
//
//         val rel = Plan.relAggregate(
//             common = empty,
//             input = input,
//             calls = calls,
//             groups = groups,
//             strategy = strategy
//         )
//
//         return Pair(sel, rel)
//     }
//
//     /**
//      * Each GROUP BY becomes a binding available in the output tuples of [Rel.Aggregate]
//      */
//     private fun convertGroupByKey(groupKey: GroupBy.Key) = binding(
//         name = groupKey.asAlias ?: error("not normalized, group key $groupKey missing unique name"),
//         expr = groupKey.expr
//     )
//
//     /**
//      * Append [Rel.Filter] only if a HAVING condition exists
//      *
//      * Notes:
//      *  - This currently does not support aggregation expressions in the WHERE condition
//      */
//     private fun convertHaving(input: Rel, expr: Expr?): Rel = when (expr) {
//         null -> input
//         else -> Plan.relFilter(
//             common = empty,
//             input = input,
//             condition = RexConverter.convert(expr)
//         )
//     }
//
//     /**
//      * Append [Rel.Sort] only if an ORDER BY clause is present
//      */
//     private fun convertOrderBy(input: Rel, orderBy: OrderBy?) = when (orderBy) {
//         null -> input
//         else -> Plan.relSort(
//             common = empty,
//             input = input,
//             specs = orderBy.sorts.map { convertSort(it) }
//         )
//     }
//
//     /**
//      * Append [Rel.Fetch] if there is a LIMIT or LIMIT and OFFSET.
//      *
//      * Notes:
//      *  - It's unclear if OFFSET without LIMIT should be allowed in PartiQL, so err for now.
//      */
//     private fun convertFetch(
//         input: Rel,
//         limit: Expr?,
//         offset: Expr?
//     ): Rel {
//         if (limit == null) {
//             if (offset != null) error("offset without limit")
//             return input
//         }
//         return Plan.relFetch(
//             common = empty,
//             input = input,
//             limit = RexConverter.convert(limit),
//             offset = RexConverter.convert(offset ?: Ast.exprLiteral(int64Value(0)))
//         )
//     }
//
//     /**
//      * Appends a [Rel.Project] which projects the result of each binding rex into its binding name.
//      *
//      * @param input
//      * @param projection
//      * @return
//      */
//     private fun convertProjectList(input: Rel, projection: Select.Project) = Plan.relProject(
//         common = empty,
//         input = input,
//         bindings = projection.items.bindings()
//     )
//
//     /**
//      * Notes:
//      *  - ASC NULLS LAST   (default)
//      *  - DESC NULLS FIRST (default for DESC)
//      */
//     private fun convertSort(sort: Sort): SortSpec {
//         val value = RexConverter.convert(sort.expr)
//         val dir = when (sort.dir) {
//             Sort.Dir.DESC -> SortSpec.Dir.DESC
//             else -> SortSpec.Dir.ASC
//         }
//         val nulls = when (sort.nulls) {
//             Sort.Nulls.FIRST -> SortSpec.Nulls.FIRST
//             Sort.Nulls.LAST -> SortSpec.Nulls.LAST
//             null -> when (dir) {
//                 SortSpec.Dir.DESC -> SortSpec.Nulls.FIRST
//                 SortSpec.Dir.ASC -> SortSpec.Nulls.LAST
//             }
//         }
//         return Plan.sortSpec(value, dir, nulls)
//     }
//
//     /**
//      * Converts a GROUP AS X clause to a binding of the form:
//      * ```
//      * { 'X': group_as({ 'a_0': e_0, ..., 'a_n': e_n }) }
//      * ```
//      *
//      * Notes:
//      *  - This was included to be consistent with the existing PartiqlAst and PartiqlLogical representations,
//      *    but perhaps we don't want to represent GROUP AS with an agg function.
//      */
//     private fun convertGroupAs(name: String, from: From): Binding {
//         val fields = from.bindings().map { n ->
//             Plan.field(
//                 name = Plan.rexLit(ionString(n), StaticType.STRING),
//                 value = Plan.rexId(n, Case.SENSITIVE, Rex.Id.Qualifier.UNQUALIFIED, type = StaticType.STRUCT)
//             )
//         }
//         return Plan.binding(
//             name = name,
//             value = Plan.rexAgg(
//                 id = "group_as",
//                 args = listOf(Plan.rexTuple(fields, StaticType.STRUCT)),
//                 modifier = Rex.Agg.Modifier.ALL,
//                 type = StaticType.STRUCT
//             )
//         )
//     }
//
//     /**
//      * Helper to get all binding names in the FROM clause
//      */
//     private fun From.bindings(): List<String> = when (this) {
//         is From.Value -> {
//             if (asAlias == null) {
//                 error("not normalized, scan is missing an alias")
//             }
//             listOf(asAlias!!)
//         }
//         is From.Join -> lhs.bindings() + rhs.bindings()
//     }
//
//     /**
//      * Helper to convert ProjectItems to bindings
//      *
//      * As of now, bindings is just a list, not a tuple.
//      * Binding and Tuple/Struct will be consolidated.
//      */
//     private fun List<Select.Project.Item>.bindings() = map {
//         when (it) {
//             is Select.Project.Item.All -> {
//                 val path = Ast.exprPath(it.expr, listOf(Ast.exprPathStepWildcard()))
//                 val bindingName = when (val expr = it.expr) {
//                     is Expr.Var -> "VAR"
//                     is Expr.Literal -> TODO()
//                     else -> nextBindingName()
//                 }
//                 binding(bindingName, path)
//             }
//             is Select.Project.Item.Expression -> {
//                 binding(
//                     name = it.asAlias ?: error("not normalized"),
//                     expr = it.expr
//                 )
//             }
//         }
//     }
//
//     /**
//      * Rewrites a SFW node replacing all aggregations with a synthetic field name
//      *
//      * See AstToLogicalVisitorTransform.kt CallAggregationReplacer from org.partiql.lang.planner.transforms.
//      *
//      * ```
//      * SELECT g, h, SUM(t.b) AS sumB
//      * FROM t
//      * GROUP BY t.a AS g GROUP AS h
//      * ```
//      *
//      * into:
//      *
//      * ```
//      * SELECT g, h, $__v0 AS sumB
//      * FROM t
//      * GROUP BY t.a AS g GROUP AS h
//      * ```
//      *
//      * Where $__v0 is the binding name of SUM(t.b) in the aggregation output
//      *
//      * Inner object class to have access to current SELECT-FROM-WHERE converter state
//      */
//     @Suppress("PrivatePropertyName")
//     private val AggregationTransform = object : AstRewriter<Unit>() {
//
//         private var level = 0
//         private var aggregations = mutableListOf<Binding>()
//
//         fun apply(node: Expr.SFW): Pair<Expr.SFW, List<Binding>> {
//             level = 0
//             aggregations = mutableListOf()
//             val select = visitExprSFW(node, Unit) as Expr.SFW
//             return Pair(select, aggregations)
//         }
//
//         // only rewrite top-level SFW
//         override fun visitExprSFW(node: Expr.SFW, ctx: Unit): AstNode =
//             if (level++ == 0) super.visitExprSFW(node, ctx) else node
//
//         override fun visitExprAgg(node: Expr.Agg, ctx: Unit): AstNode {
//             val name = nextBindingName()
//             aggregations.add(binding(name, node))
//             return ast {
//                 exprVar {
//                     identifier = identifierSymbol(name, Identifier.CaseSensitivity.INSENSITIVE)
//                     scope = Expr.Var.Scope.DEFAULT
//                 }
//             }
//         }
//     }
//
//     /**
//      * Binding helper
//      */
//     private fun binding(name: String, expr: Expr) = Plan.binding(
//         name = name,
//         value = RexConverter.convert(expr)
//     )
// }
