package org.partiql.lang.eval.visitors

import com.amazon.ionelement.api.emptyMetaContainer
import org.partiql.lang.ast.UniqueNameMeta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.errNoContext

class SelectStarVisitorTransform : PartiqlAst.VisitorTransform() {

    /**
     * Copies all parts of [PartiqlAst.Expr.Select] except [newProjection] for [PartiqlAst.Projection].
     */
    private fun copyProjectionToSelect(node: PartiqlAst.Expr.Select, newProjection: PartiqlAst.Projection): PartiqlAst.Expr {
        return PartiqlAst.build {
            select(
                setq = node.setq,
                project = newProjection,
                from = node.from,
                fromLet = node.fromLet,
                where = node.where,
                group = node.group,
                having = node.having,
                limit = node.limit,
                metas = node.metas)
        }
    }

    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
        val rewritten = super.transformExprSelect(node) as PartiqlAst.Expr.Select

        val projection = rewritten.project

        // Check if SELECT * is being used.
        if (projection is PartiqlAst.Projection.ProjectStar) {
            when (rewritten.group) {    // No group by
                null -> {
                    val fromSourceAliases = extractAliases(rewritten.from)

                    val newProjection =
                        PartiqlAst.build {
                            projectList(
                                fromSourceAliases.map { aliases ->
                                    // We are concatenating 3 lists here
                                    listOf(createProjectAll(aliases.asAlias)) +
                                        (aliases.atAlias?.let { listOf(createProjectExpr(it)) } ?: emptyList()) +
                                        (aliases.byAlias?.let { listOf(createProjectExpr(it)) } ?: emptyList())
                                }.flatten()
                            )
                        }
                    return copyProjectionToSelect(rewritten, newProjection)
                }
                else -> {               // With group by
                    val selectListItemsFromGroupBy = rewritten.group.keyList.keys.map {
                        val asName = it.asAlias
                            ?: errNoContext(
                                "GroupByItem has no AS-alias--GroupByItemAliasVisitorTransform must be executed before SelectStarVisitorTransform",
                                internal = true)

                        // We need to take the unique name of each grouping field key only because we need to handle
                        // the case when multiple grouping fields are assigned the same name (which is currently allowed)
                        val uniqueNameMeta = asName.metas[UniqueNameMeta.TAG] as? UniqueNameMeta?
                            ?: error("UniqueNameMeta not found--normally, this is added by GroupByItemAliasVisitorTransform")

                        createProjectExpr(uniqueNameMeta.uniqueName, asName.text)
                    }

                    val groupNameItem = rewritten.group.groupAsAlias?.text.let {
                        if (it != null) listOf(createProjectExpr(it)) else emptyList()
                    }

                    val newProjection = PartiqlAst.build { projectList(selectListItemsFromGroupBy + groupNameItem) }

                    return copyProjectionToSelect(rewritten, newProjection)
                }
            }
        }
        return rewritten
    }

    private fun createProjectAll(name: String) =
        PartiqlAst.build {
            projectAll(id(name, caseSensitive(), unqualified(), emptyMetaContainer()))
        }

    private fun createProjectExpr(variableName: String, asAlias: String = variableName) =
        PartiqlAst.build {
            projectExpr(id(variableName, caseSensitive(), unqualified(), emptyMetaContainer()), asAlias)
        }

    private class FromSourceAliases(val asAlias: String, val atAlias: String?, val byAlias: String?)

    private fun extractAliases(fromSource: PartiqlAst.FromSource): List<FromSourceAliases> =
        when (fromSource) {
            is PartiqlAst.FromSource.Scan -> {
                listOf(
                    FromSourceAliases(
                        fromSource.asAlias?.text
                            ?: error("FromSourceAliasVisitorTransform must be executed before SelectStarVisitorTransform"),
                        fromSource.atAlias?.text,
                        fromSource.byAlias?.text)
                )
            }
            is PartiqlAst.FromSource.Unpivot -> {
                listOf(
                    FromSourceAliases(
                        fromSource.asAlias?.text
                            ?: error("FromSourceAliasVisitorTransform must be executed before SelectStarVisitorTransform"),
                        fromSource.atAlias?.text,
                        fromSource.byAlias?.text)
                )
            }
            is PartiqlAst.FromSource.Join -> {
                extractAliases(fromSource.left) + extractAliases(fromSource.right)
            }
        }

}
