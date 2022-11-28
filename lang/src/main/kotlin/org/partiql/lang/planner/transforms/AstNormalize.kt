package org.partiql.lang.planner.transforms

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.visitors.AggregationVisitorTransform
import org.partiql.lang.eval.visitors.FromSourceAliasVisitorTransform
import org.partiql.lang.eval.visitors.OrderBySortSpecVisitorTransform
import org.partiql.lang.eval.visitors.PipelinedVisitorTransform
import org.partiql.lang.eval.visitors.SelectListItemAliasVisitorTransform
import org.partiql.lang.eval.visitors.SelectStarVisitorTransform

/**
 * Executes several Visitor Transforms on the AST
 */
fun PartiqlAst.Statement.normalize(): PartiqlAst.Statement {
    val transforms = PipelinedVisitorTransform(
        SelectListItemAliasVisitorTransform(),
        FromSourceAliasVisitorTransform(),
        OrderBySortSpecVisitorTransform(),
        AggregationVisitorTransform(),
        SelectStarVisitorTransform()
    )
    return transforms.transformStatement(this)
}
