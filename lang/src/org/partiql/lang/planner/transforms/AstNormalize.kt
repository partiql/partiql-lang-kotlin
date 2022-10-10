package org.partiql.lang.planner.transforms

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.visitors.FromSourceAliasVisitorTransform
import org.partiql.lang.eval.visitors.GroupByItemAliasVisitorTransform
import org.partiql.lang.eval.visitors.GroupByPathExpressionVisitorTransform
import org.partiql.lang.eval.visitors.GroupKeyReferencesVisitorTransform
import org.partiql.lang.eval.visitors.OrderBySortSpecVisitorTransform
import org.partiql.lang.eval.visitors.PipelinedVisitorTransform
import org.partiql.lang.eval.visitors.SelectListItemAliasVisitorTransform
import org.partiql.lang.eval.visitors.SelectStarVisitorTransform

/**
 * Executes several Visitor Transforms on the AST
 */
fun PartiqlAst.Statement.normalize(): PartiqlAst.Statement {
    // Since these passes all work on PartiqlAst, we can use a PipelinedVisitorTransform which executes each
    // specified VisitorTransform in sequence.
    val transforms = PipelinedVisitorTransform(
        // Synthesizes unspecified `SELECT <expr> AS ...` aliases
        SelectListItemAliasVisitorTransform(),
        // Synthesizes unspecified `FROM <expr> AS ...` aliases
        FromSourceAliasVisitorTransform(),
        OrderBySortSpecVisitorTransform(),
        GroupByItemAliasVisitorTransform(),
        GroupByPathExpressionVisitorTransform(),
        GroupKeyReferencesVisitorTransform(),
        // Changes `SELECT * FROM a, b` to SELECT a.*, b.* FROM a, b`
        SelectStarVisitorTransform()
    )
    return transforms.transformStatement(this)
}
