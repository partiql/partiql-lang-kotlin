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
 *      baz + 1,
 *      zoo.*
 *  FROM bar AS b
 * ```
 *
 * Into:
 *
 * ```
 *  SELECT
 *      foo AS foo,
 *      b.bat AS bat,
 *      baz + 1 AS _3,
 *      zoo.*
 *  FROM bar AS b
 * ```
 *
 * If provided with a query with all of the select list aliases are already specified, an exact clone is returned.
 *
 * ```
 */
class SelectListItemAliasVisitorTransform : VisitorTransformBase() {

    override fun transformProjectionProjectList(node: PartiqlAst.Projection.ProjectList): PartiqlAst.Projection {
        return PartiqlAst.build {
            projectList(
                projectItems = node.projectItems.mapIndexed { idx, it ->
                    when (it) {
                        is PartiqlAst.ProjectItem.ProjectExpr ->
                            when (it.asAlias) {
                                //  Synthesize a column name if one was not specified in the query.
                                null -> projectExpr(
                                    expr = transformExpr(it.expr),
                                    asAlias = it.expr.extractColumnAlias(idx),
                                )
                                else -> projectExpr(
                                    expr = transformExpr(it.expr),
                                    asAlias = it.asAlias
                                )
                            }
                        else -> it
                    }
                },
                metas = node.metas
            )
        }
    }
}
