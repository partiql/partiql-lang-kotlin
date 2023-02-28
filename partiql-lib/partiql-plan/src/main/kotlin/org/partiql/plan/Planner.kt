package org.partiql.plan

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.TypedOpBehavior
import org.partiql.lang.eval.visitors.AggregationVisitorTransform
import org.partiql.lang.eval.visitors.FromSourceAliasVisitorTransform
import org.partiql.lang.eval.visitors.OrderBySortSpecVisitorTransform
import org.partiql.lang.eval.visitors.PartiqlAstSanityValidator
import org.partiql.lang.eval.visitors.PipelinedVisitorTransform
import org.partiql.lang.eval.visitors.SelectListItemAliasVisitorTransform
import org.partiql.lang.eval.visitors.SelectStarVisitorTransform
import org.partiql.plan.ir.PartiQLPlan
import org.partiql.plan.ir.Rex

typealias Pass = (PartiQLPlan) -> PartiQLPlan

/**
 * Experimental [PartiqlAst.Statement] to PartiQL [Plan]
 */
class Planner(private val passes: List<Pass>) {

    companion object {

        /**
         * Stand-in for default impl
         */
        @JvmStatic
        val default = Planner(emptyList())

        /**
         * Common place to throw exceptions with access to the AST node.
         * Error handling pattern is undecided
         */
        internal fun unsupported(node: PartiqlAst.PartiqlAstNode): Nothing {
            throw UnsupportedOperationException("node: $node")
        }
    }

    /**
     * Raise an interface later
     */
    fun plan(statement: PartiqlAst.Statement): PartiQLPlan {
        // 1. Normalize
        val ast = statement.normalize()
        if (ast !is PartiqlAst.Statement.Query) {
            unsupported(ast)
        }
        // 2. Translate AST
        val root = convert(ast.expr)
        // 3. Initial plan
        val plan = PartiQLPlan(
            version = PartiQLPlan.Version.PARTIQL_V0,
            root = root,
        )
        // 4. Apply all passes
        return passes.fold(plan) { p, pass -> pass.invoke(p) }
    }

    // --- Internal ---------------------------------------------

    /**
     * Normalizes a statement AST node. Copied from EvaluatingCompiler, and include the validation.
     *
     * Notes:
     *  - AST normalization assumes operating on statement rather than a query statement, but the normalization
     *    only changes the SFW nodes. There's room to simplify here. Also, you have to enter the transform at
     *    `transformStatement` or nothing happens. I initially had `transformQuery` but that doesn't work because
     *    the pipelinedVisitorTransform traversal can only be entered on statement.
     */
    private fun PartiqlAst.Statement.normalize(): PartiqlAst.Statement {
        val transform = PipelinedVisitorTransform(
            SelectListItemAliasVisitorTransform(),
            FromSourceAliasVisitorTransform(),
            OrderBySortSpecVisitorTransform(),
            AggregationVisitorTransform(),
            SelectStarVisitorTransform()
        )
        // normalize
        val ast = transform.transformStatement(this)
        // validate
        val validatorCompileOptions = CompileOptions.build { typedOpBehavior(TypedOpBehavior.HONOR_PARAMETERS) }
        PartiqlAstSanityValidator().validate(this, validatorCompileOptions)
        return ast
    }

    /**
     * Convert Partiql.Ast.Expr to a Rex/Rel tree
     */
    private fun convert(query: PartiqlAst.Expr): Rex = when (query) {
        is PartiqlAst.Expr.Select -> {
            // <query-expression>
            val rex = RelConverter.convert(query)
            rex
        }
        else -> {
            // <value-expression>
            val rex = RexConverter.convert(query)
            rex
        }
    }
}
