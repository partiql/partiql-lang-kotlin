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
        // Once https://github.com/partiql/partiql-ir-generator/issues/52, adding the .copy function is released,
        // we can remove this code and call .copy instead
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
        val transformedExpr = super.transformExprSelect(node) as PartiqlAst.Expr.Select

        val projection = transformedExpr.project

        // Check if SELECT * is being used.
        if (projection is PartiqlAst.Projection.ProjectStar) {
            when (transformedExpr.group) {    // No group by
                null -> {
                    val fromSourceAliases = extractAliases(transformedExpr.from)

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
                    return copyProjectionToSelect(transformedExpr, newProjection)
                }
                else -> {               // With group by
                    val selectListItemsFromGroupBy = transformedExpr.group.keyList.keys.map {
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

                    val groupNameItem = transformedExpr.group.groupAsAlias?.text.let {
                        if (it != null) listOf(createProjectExpr(it)) else emptyList()
                    }

                    val newProjection = PartiqlAst.build { projectList(selectListItemsFromGroupBy + groupNameItem) }

                    return copyProjectionToSelect(transformedExpr, newProjection)
                }
            }
        }
        return transformedExpr
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

    /** Extracts all the FROM source/unpivot aliases without recursing into any nested queries */
    private fun extractAliases(fromSource: PartiqlAst.FromSource): List<FromSourceAliases> {
        val aliases = mutableListOf<FromSourceAliases>()
        val visitor = object : PartiqlAst.Visitor() {
            override fun visitFromSourceScan(node: PartiqlAst.FromSource.Scan) {
                aliases.add(
                    FromSourceAliases(
                        node.asAlias?.text
                            ?: error("FromSourceAliasVisitorTransform must be executed before SelectStarVisitorTransform"),
                        node.atAlias?.text,
                        node.byAlias?.text))
            }

            override fun visitFromSourceUnpivot(node: PartiqlAst.FromSource.Unpivot) {
                aliases.add(
                    FromSourceAliases(
                        node.asAlias?.text
                            ?: error("FromSourceAliasVisitorTransform must be executed before SelectStarVisitorTransform"),
                        node.atAlias?.text,
                        node.byAlias?.text))
            }

            /** We do not want to recurse into the nested select query */
            override fun walkExprSelect(node: PartiqlAst.Expr.Select) {
                return
            }
        }
        visitor.walkFromSource(fromSource)
        return aliases
    }
}
