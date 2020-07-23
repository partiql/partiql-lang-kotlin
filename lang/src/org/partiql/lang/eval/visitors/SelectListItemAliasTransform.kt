package org.partiql.lang.eval.visitors

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.extractColumnAlias

/**
 * Specifies any previously unspecified select list item aliases.
 *
 * Turns:
 *
 * ```
 *  SELECT
 *      foo,
 *      b.bat,
 *      baz + 1
 * FROM bar AS b
 * ```
 *
 * Into:
 *
 * ```
 *  SELECT
 *      foo AS foo,
 *      b.bat AS bat,
 *      baz + 1 AS _3
 * FROM bar AS b
 * ```
 *
 * If provided with a query with all of the select list aliases are already specified, an exact clone is returned.
 *
 * ```
 */
class SelectListItemAliasTransform : PartiqlAst.VisitorTransform() {
    override fun transformProjectionProjectList(node: PartiqlAst.Projection.ProjectList): PartiqlAst.Projection.ProjectList =
        PartiqlAst.build {
            projectList(node.projectItems.mapIndexed { index, item ->
                when (item) {
                    is PartiqlAst.ProjectItem.ProjectExpr -> {
                        projectExpr_(
                            this@SelectListItemAliasTransform.transformExpr(item.expr),
                            asAlias = item.asAlias ?: item.expr.extractColumnAlias(index)
                        )
                    }
                    is PartiqlAst.ProjectItem.ProjectAll -> super.transformProjectItemProjectAll(item)
                }
            },
            node.metas)
        }
}