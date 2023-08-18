package org.partiql.lang.planner.transforms

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.planner.transforms.plan.RelConverter
import org.partiql.lang.planner.transforms.plan.RexConverter
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.Plan
import org.partiql.plan.Rex
import org.partiql.planner.ExperimentalPartiQLPlanner
import org.partiql.planner.transforms.AggregationVisitorTransform
import org.partiql.planner.transforms.FromSourceAliasVisitorTransform
import org.partiql.planner.transforms.OrderBySortSpecVisitorTransform
import org.partiql.planner.transforms.PipelinedVisitorTransform
import org.partiql.planner.transforms.SelectListItemAliasVisitorTransform
import org.partiql.planner.transforms.SelectStarVisitorTransform
import org.partiql.planner.validators.PartiqlAstSanityValidator

/**
 * Translate the PIG AST to an implementation of the PartiQL Plan Representation.
 */
object AstToPlan {

    /**
     * Converts a PartiqlAst.Statement to a [PartiQLPlan]
     */
    fun transform(statement: PartiqlAst.Statement): PartiQLPlan {
        val ast = statement.normalize()
        if (ast !is PartiqlAst.Statement.Query) {
            unsupported(ast)
        }
        val root = transform(ast.expr)
        return Plan.partiQLPlan(
            version = PartiQLPlan.Version.PARTIQL_V0,
            root = root,
        )
    }

    // --- Internal ---------------------------------------------

    /**
     * Common place to throw exceptions with access to the AST node.
     * Error handling pattern is undecided
     */
    internal fun unsupported(node: PartiqlAst.PartiqlAstNode): Nothing {
        throw UnsupportedOperationException("node: $node")
    }

    /**
     * Normalizes a statement AST node. Copied from EvaluatingCompiler, and include the validation.
     *
     * Notes:
     *  - AST normalization assumes operating on statement rather than a query statement, but the normalization
     *    only changes the SFW nodes. There's room to simplify here. Also, you have to enter the transform at
     *    `transformStatement` or nothing happens. I initially had `transformQuery` but that doesn't work because
     *    the pipelinedVisitorTransform traversal can only be entered on statement.
     */
    @OptIn(ExperimentalPartiQLPlanner::class)
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
        PartiqlAstSanityValidator().validate(this)
        return ast
    }

    /**
     * Convert Partiql.Ast.Expr to a Rex/Rel tree
     */
    private fun transform(query: PartiqlAst.Expr): Rex = when (query) {
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
