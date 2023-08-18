package org.partiql.lang.planner.transforms

import org.partiql.lang.domains.PartiqlAst
import org.partiql.planner.ExperimentalPartiQLPlanner
import org.partiql.planner.transforms.AggregationVisitorTransform
import org.partiql.planner.transforms.FromSourceAliasVisitorTransform
import org.partiql.planner.transforms.OrderBySortSpecVisitorTransform
import org.partiql.planner.transforms.PipelinedVisitorTransform
import org.partiql.planner.transforms.SelectListItemAliasVisitorTransform
import org.partiql.planner.transforms.SelectStarVisitorTransform

/**
 * Executes several Visitor Transforms on the AST
 */
@OptIn(ExperimentalPartiQLPlanner::class)
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
