package org.partiql.plan

import com.amazon.ionelement.api.MetaContainer
import org.partiql.ir.rel.Common
import org.partiql.ir.rel.Rel
import org.partiql.ir.rex.Rex
import org.partiql.lang.domains.PartiqlAst

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
    fun translate(statement: PartiqlAst.Statement): Rel {
        // read as `val rel = statement.accept(visitor = Visitor, args = emptyList())
        val (_, rel) = RelVisitor.walkStatement(statement, statement.ctx())
        return rel!!
    }

    //--- Internal ---------------------------------------------

    /**
     * Workaround for PIG visitor where:
     *  - Args != null when Ctx is the accumulator IN
     *  - Rel  != null when Ctx is the accumulator OUT
     *
     * Destructuring ordering chosen for val (in, out) = ...
     *
     * @param R         Return type
     * @property node   Node to invoke the behavior on
     * @property r      Return value
     */
    private data class Ctx<R>(
        val node: PartiqlAst.PartiqlAstNode,
        var r: R? = null,
    )

    private fun <T> PartiqlAst.PartiqlAstNode.ctx() = Ctx<T>(this)

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
     * TODO change to not use the visitor as SELECT is only a single node so it just complicates things
     *
     * See notes on RexVisitor
     */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private object RelVisitor : PartiqlAst.VisitorFold<Ctx<Rel>>() {

        /**
         * TODO don't use visitor as there's not enough traversal to justify the complexity compared to recursion
         *
         * See notes on RexVisitor
         */
        override fun walkMetas(node: MetaContainer, ctx: Ctx<Rel>) = unsupported(ctx.node)

        /**
         * Plan the SFW, else transform the expression into a Rex and wrap in a scan
         */
        override fun walkStatementQuery(node: PartiqlAst.Statement.Query, ctx: Ctx<Rel>): Ctx<Rel> {
            val rel = when (val n = node.expr) {
                is PartiqlAst.Expr.Select -> walkExprSelect(n, n.ctx()).r!!
                else -> {
                    val rex = RexVisitor.walkExpr(n, n.ctx()).r!!
                    Rel.Scan(COMMON, rex, alias = null, at = null, by = null)
                }
            }
            return ctx.copy(r = rel)
        }

        /**
         * Translate SFW AST node to a pipeline of [Rel] operators
         */
        override fun walkExprSelect(node: PartiqlAst.Expr.Select, ctx: Ctx<Rel>): Ctx<Rel> {
            // Each
            val scope = Scope()
            var rel = convertFrom(node.from)

            //
            rel = scope.convertWhere(rel, node.where)

            // Let e_0 , ... , e_n be the projection of SELECT
            // If e_i is an aggregation, replace it
            rel = scope.convertAgg(rel, node.project, node.group)

            return ctx.copy(r = rel)
        }
    }

    /**
     * Some workarounds for transforming a PIG tree without having to create another visitor.
     *
     * - Using the VisitorFold with Pair<T, C> to create a parameterized return and scoped arguments/context
     * - Overriding walks rather than visits to control traversal
     *      - Walks needn't exist, but since they do, the walk functions as a normal visitor "accept"
     *      - Visit methods are not implemented as they don't need to exist become of walk
     * - Walks have if/else blocks generated for sum types so in the absence of OO style, we can use it as if it were "accept"
     */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private object RexVisitor : PartiqlAst.VisitorFold<Ctx<Rex>>() {

        /**
         * !! DEFAULT VISIT !!
         *
         * The PIG visitor doesn't give us control over the default "visit"
         * We can override walkMetas (which appears on every super.walk call) as if it were a default "visit"
         * MetaContainer isn't actually a domain node, and we don't have any context as to where the MetaContainer
         * is coming from which is why the current node is stuffed into Ctx
         */
        override fun walkMetas(node: MetaContainer, ctx: Ctx<Rex>) = unsupported(ctx.node)
    }

    /**
     * A place for stateful, lexical scope helpers.
     * Calcite uses a similar scoped mutable object called Blackboard.
     */
    private class Scope {

        // synthetic field counter
        private var i = 0;
        private val aliased: MutableMap<Rex, Rex> = mutableMapOf()

        // generate a synthetic field name for the given Rex
        fun alias(rex: Rex): String {
            val name = "\$v${i++}"
            aliased[rex] = Rex.Id(name)
            return name
        }

         // return the alias of a given expression, or itself if no alias exists
        fun resolve(rex: Rex) = aliased[rex] ?: rex

        fun convertFrom(from: PartiqlAst.FromSource): Rel = when (from) {
            is PartiqlAst.FromSource.Join -> unsupported(from)
            is PartiqlAst.FromSource.Scan -> unsupported(from)
            is PartiqlAst.FromSource.Unpivot -> unsupported(from)
        }

        fun convertWhere(input: Rel, expr: PartiqlAst.Expr?): Rel = when (expr) {
            null -> input
            else -> {
                val condition = RexVisitor.walkExpr(expr, expr.ctx()).r!!
                Rel.Filter(COMMON, input, condition)
            }
        }

        fun convertAgg(input: Rel, select: PartiqlAst.Projection, groupBy: PartiqlAst.GroupBy?): Rel {

        }
    }
}
