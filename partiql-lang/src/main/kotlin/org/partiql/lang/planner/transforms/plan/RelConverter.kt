package org.partiql.lang.planner.transforms.plan

import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.visitors.VisitorTransformBase
import org.partiql.plan.Binding
import org.partiql.plan.Case
import org.partiql.plan.Common
import org.partiql.plan.Field
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.SortSpec
import org.partiql.types.StaticType

/**
 * Lexically scoped state for use in translating an individual SELECT statement.
 */
internal class RelConverter {

    /**
     * As of now, the COMMON property of relation operators is under development, so just use empty for now
     */
    private val empty = Common(
        schema = emptyMap(),
        properties = emptySet(),
        metas = emptyMap(),
    )

    companion object {

        /**
         * Converts a SELECT-FROM-WHERE AST node to a [Rex.Query]
         */
        @JvmStatic
        fun convert(select: PartiqlAst.Expr.Select): Rex.Query = with(RelConverter()) {
            val rel = convertSelect(select)
            val rex = when (val projection = select.project) {
                // PIVOT ... FROM
                is PartiqlAst.Projection.ProjectPivot -> {
                    Rex.Query.Scalar.Pivot(
                        rel = rel,
                        value = RexConverter.convert(projection.value),
                        at = RexConverter.convert(projection.key),
                        type = null,
                    )
                }
                // SELECT VALUE ... FROM
                is PartiqlAst.Projection.ProjectValue -> {
                    Rex.Query.Collection(
                        rel = rel,
                        constructor = RexConverter.convert(projection.value),
                    )
                }
                // SELECT ... FROM
                else -> {
                    Rex.Query.Collection(
                        rel = rel,
                        constructor = null,
                    )
                }
            }
            rex
        }
    }

    // synthetic binding name counter
    private var i = 0

    // generate a synthetic binding name
    private fun nextBindingName(): String = "\$__v${i++}"

    /**
     * Translate SFW AST node to a pipeline of [Rel] operators; this skips the final projection.
     *
     * Note:
     *  - This does not append the final projection
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
        // append SQL projection if present
        rel = when (val projection = sel.project) {
            is PartiqlAst.Projection.ProjectList -> convertProjectList(rel, projection)
            is PartiqlAst.Projection.ProjectStar -> error("AST not normalized, found project star")
            else -> rel // skip
        }
        return rel
    }

    /**
     * Appends the appropriate [Rel] operator for the given FROM source
     */
    private fun convertFrom(from: PartiqlAst.FromSource): Rel = when (from) {
        is PartiqlAst.FromSource.Join -> convertJoin(from)
        is PartiqlAst.FromSource.Scan -> convertScan(from)
        is PartiqlAst.FromSource.Unpivot -> convertUnpivot(from)
    }

    /**
     * Appends [Rel.Join] where the left and right sides are converted FROM sources
     */
    private fun convertJoin(join: PartiqlAst.FromSource.Join): Rel {
        val lhs = convertFrom(join.left)
        val rhs = convertFrom(join.right)
        val condition = if (join.predicate != null) RexConverter.convert(join.predicate!!) else null
        return Rel.Join(
            common = empty,
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
        common = empty,
        value = RexConverter.convert(scan.expr),
        alias = scan.asAlias?.text,
        at = scan.atAlias?.text,
        by = scan.byAlias?.text,
    )

    /**
     * Appends [Rel.Unpivot] to range over attribute value pairs
     */
    private fun convertUnpivot(scan: PartiqlAst.FromSource.Unpivot) = Rel.Unpivot(
        common = empty,
        value = RexConverter.convert(scan.expr),
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
            common = empty,
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
            common = empty,
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
    private fun convertGroupByKey(groupKey: PartiqlAst.GroupKey) = binding(
        name = groupKey.asAlias?.text ?: error("not normalized, group key $groupKey missing unique name"),
        expr = groupKey.expr,
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
            common = empty,
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
            common = empty,
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
            common = empty,
            input = input,
            limit = RexConverter.convert(limit),
            offset = RexConverter.convert(offset ?: PartiqlAst.Expr.Lit(ionInt(0).asAnyElement()))
        )
    }

    /**
     * Appends a [Rel.Project] which projects the result of each binding rex into its binding name.
     *
     * @param input
     * @param projection
     * @return
     */
    private fun convertProjectList(input: Rel, projection: PartiqlAst.Projection.ProjectList) = Rel.Project(
        common = empty,
        input = input,
        bindings = projection.projectItems.bindings()
    )

    /**
     * Converts Ast.SortSpec to SortSpec.
     *
     * Notes:
     *  - ASC NULLS LAST   (default)
     *  - DESC NULLS FIRST (default for DESC)
     */
    private fun convertSortSpec(sortSpec: PartiqlAst.SortSpec) = SortSpec(
        value = RexConverter.convert(sortSpec.expr),
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
        val fields = from.bindings().map { n ->
            Field(
                name = Rex.Lit(ionString(n), StaticType.STRING),
                value = Rex.Id(n, Case.SENSITIVE, Rex.Id.Qualifier.UNQUALIFIED, type = StaticType.STRUCT),
            )
        }
        return Binding(
            name = name,
            value = Rex.Agg(
                id = "group_as",
                args = listOf(Rex.Tuple(fields, StaticType.STRUCT)),
                modifier = Rex.Agg.Modifier.ALL,
                type = StaticType.STRUCT,
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
     * Helper to convert ProjectItems to bindings
     *
     * As of now, bindings is just a list, not a tuple.
     * Binding and Tuple/Struct will be consolidated.
     */
    private fun List<PartiqlAst.ProjectItem>.bindings() = map {
        when (it) {
            is PartiqlAst.ProjectItem.ProjectAll -> binding("*", it.expr)
            is PartiqlAst.ProjectItem.ProjectExpr -> binding(
                name = it.asAlias?.text ?: error("not normalized"),
                expr = it.expr,
            )
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
            aggregations.add(binding(name, node))
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

    /**
     * Binding helper
     */
    private fun binding(name: String, expr: PartiqlAst.Expr) = Binding(
        name = name,
        value = RexConverter.convert(expr)
    )
}
