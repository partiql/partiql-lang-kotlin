package org.partiql.lang.eval.visitors

import com.amazon.ionelement.api.SymbolElement
import org.partiql.lang.ast.SelectListItemExpr
import org.partiql.lang.ast.SelectListItemProjectAll
import org.partiql.lang.ast.SelectListItemStar
import org.partiql.lang.ast.SelectProjection
import org.partiql.lang.ast.SelectProjectionList
import org.partiql.lang.ast.SymbolicName
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.extractColumnAlias
import org.partiql.pig.runtime.SymbolPrimitive


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
class SelectListItemAliasVisitorTransform : PartiqlAst.VisitorTransform() {
    override fun transformExprSelect_project(node: PartiqlAst.Expr.Select): PartiqlAst.Projection {
        return super.transformExprSelect_project(node)
    }

    override fun transformProjectionProjectList(node: PartiqlAst.Projection.ProjectList): PartiqlAst.Projection {
        return PartiqlAst.build {
            projectList(
                projectItems = node.projectItems.mapIndexed { idx, it ->
                    when(it) {
                        is PartiqlAst.ProjectItem.ProjectExpr ->
                            when (it.asAlias) {
                                //  Synthesize a column name if one was not specified in the query.
                                null -> projectExpr(it.expr, it.expr.extractColumnAlias(idx))
                                else -> it
                            }
                        else -> it
                    }
                }
            )
        }
    }
}

