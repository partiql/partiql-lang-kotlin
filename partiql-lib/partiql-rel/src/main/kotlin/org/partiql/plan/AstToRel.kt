package org.partiql.plan

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import org.partiql.ir.rel.Binding
import org.partiql.ir.rel.Common
import org.partiql.ir.rel.Rel
import org.partiql.ir.rel.SortSpec
import org.partiql.ir.rex.Rex
import org.partiql.ir.rex.StructPart
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.visitors.VisitorTransformBase

/**
 * Experimental PartiqlAst.Statement to [Rel] transformation.
 */
internal object AstToRel {

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
        return when (val node = statement.expr) {
            is PartiqlAst.Expr.Select -> RelConverter.convert(node)
            else -> {
                // This is incorrect for now
                val rex = RexConverter.convert(node)
                Rel.Scan(COMMON, rex, alias = null, at = null, by = null)
            }
        }
    }

    //--- Internal ---------------------------------------------

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
        private var i = 0;

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
            return rel
        }

        /**
         * TODO
         */
        private fun convertFrom(from: PartiqlAst.FromSource): Rel = when (from) {
            is PartiqlAst.FromSource.Join -> unsupported(from)
            is PartiqlAst.FromSource.Scan -> unsupported(from)
            is PartiqlAst.FromSource.Unpivot -> unsupported(from)
        }

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
            val (sel, aggregations) = AggregationTransform().apply(select)

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
         * { 'X': Rex.Agg.group_as({ 'a_0': e_0, ..., 'a_n': e_n }) }
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
         */
        private inner class AggregationTransform : VisitorTransformBase() {

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
     * - Overriding walks rather than visits to control traversal
     * - Visit methods are not implemented because the walk methods are effectively the same
     * - Walks have if/else blocks generated for sum types so in the absence of OO style, we can use it as if it were "accept"
     */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private object RexConverter : PartiqlAst.VisitorFold<Ctx>() {

        /**
         * read as `val rex = node.accept(visitor = RexVisitor.INSTANCE, args = emptyList())`
         */
        fun convert(node: PartiqlAst.Expr) = RexConverter.walkExpr(node, Ctx(node)).rex!!

        /**
         * !! DEFAULT VISIT !!
         *
         * The PIG visitor doesn't give us control over the default "visit"
         * We can override walkMetas (which appears on every super.walk call) as if it were a default "visit"
         * MetaContainer isn't actually a domain node, and we don't have any context as to where the MetaContainer
         * is coming from which is why the current node is stuffed into Ctx
         */
        override fun walkMetas(node: MetaContainer, ctx: Ctx) = unsupported(ctx.node)
    }
}
