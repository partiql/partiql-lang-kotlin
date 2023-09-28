package org.partiql.lang.planner.transforms

import org.partiql.errors.ProblemHandler
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
import org.partiql.lang.planner.transforms.plan.RelConverter
import org.partiql.lang.planner.transforms.plan.RexConverter
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.Plan
import org.partiql.plan.Rex

/**
 * Translate the PIG AST to an implementation of the PartiQL Plan Representation.
 */
object AstToPlan {

    /**
     * Converts a PartiqlAst.Statement to a [PartiQLPlan]
     */
    fun transform(statement: PartiqlAst.Statement, problemHandler: ProblemHandler): PartiQLPlan {
        val ast = statement.normalize()
        if (ast !is PartiqlAst.Statement.Query) {
            unsupported(ast)
        }
        val root = transform(ast.expr, problemHandler)
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
    private fun transform(query: PartiqlAst.Expr, problemHandler: ProblemHandler): Rex = when (query) {
        is PartiqlAst.Expr.Select -> {
            // <query-expression>
            val rex = RelConverter.convert(query, problemHandler)
            rex
        }
        else -> {
            // <value-expression>
            val rex = RexConverter.convert(query, problemHandler)
            rex
        }
    }
}
