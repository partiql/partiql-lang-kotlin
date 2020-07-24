package org.partiql.lang.eval.visitors

import com.amazon.ionelement.api.emptyMetaContainer
import org.partiql.lang.ast.UniqueNameMeta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.errNoContext

class SelectStarTransform : PartiqlAst.VisitorTransform() {
    override fun transformExprSelect_project(node: PartiqlAst.Expr.Select): PartiqlAst.Projection =
        when (node.project) {
            // When the current select statement is *not* a `SELECT *`, call super to get the default deep clone.
            !is PartiqlAst.Projection.ProjectStar -> super.transformExprSelect_project(node)
            // Otherwise, transform out `SELECT *` to an equivalent project list.
            else -> when (node.group) {
                // No group by
                null -> {
                    val fromSourceAliases = node.from.extractFromSourceAliases()
                    PartiqlAst.build {
                        projectList(
                            projectItems =
                            fromSourceAliases.map { aliases ->
                                listOfNotNull(
                                    createSelectListItemProjectAll(aliases.asAlias),
                                    aliases.atAlias?.let { createSelectListItemExpr(it) },
                                    aliases.byAlias?.let { createSelectListItemExpr(it) })
                            }.flatten())
                    }
                }
                // With group by
                else -> {
                    val selectListItemsFromGroupBy = node.group.groupKeys.groupKeys.map {
                        val asName = it.asAlias
                            ?: errNoContext(
                                "GroupByItem has no AS-alias--GroupByItemAliasRewriter must be executed before SelectStarRewriter",
                                internal = true)

                        // We need to take the unique name of each grouping field key only because we need to handle
                        // the case when multiple grouping fields are assigned the same name (which is currently allowed)
                        val uniqueNameMeta = asName.metas[UniqueNameMeta.TAG] as? UniqueNameMeta?
                            ?: error("UniqueNameMeta not found--normally, this is added by GroupByItemAliasRewriter")

                        createSelectListItemExpr(uniqueNameMeta.uniqueName, asName.text)
                    }

                    val groupNameItem = node.group.groupAsAlias?.text?.let {
                        listOf(createSelectListItemExpr(it))
                    } ?: emptyList()

                    PartiqlAst.build {
                        PartiqlAst.Projection.ProjectList(selectListItemsFromGroupBy + groupNameItem)
                    }
                }
            }
        }
}


// TODO: rename these functions?
// TODO: should we de-encapsulate any of these?

private fun createSelectListItemProjectAll(name: String) = PartiqlAst.build {
    projectAll(id(name, caseSensitive(), unqualified(), emptyMetaContainer()))
}

private fun createSelectListItemExpr(variableName: String, asAlias: String = variableName) = PartiqlAst.build {
    projectExpr(
        id(variableName, caseSensitive(), unqualified()),
        asAlias)
}

private class FromSourceAliases(val asAlias: String, val atAlias: String?, val byAlias: String?)

private fun PartiqlAst.FromSource.extractFromSourceAliases() =
    object : PartiqlAst.VisitorFold<List<FromSourceAliases>>() {
        override fun visitFromSourceScan(
            node: PartiqlAst.FromSource.Scan,
            accumulator: List<FromSourceAliases>
        ): List<FromSourceAliases> =
            accumulator + FromSourceAliases(
                asAlias = node.asAlias?.text ?: error("FromSourceAliasRewriter must be executed before SelectStarRewriter"),
                atAlias = node.atAlias?.text,
                byAlias = node.byAlias?.text)

        /** Stops recursion into []PartiqlAst.Expr.Select] nodes. */
        override fun walkExprSelect(node: PartiqlAst.Expr.Select, accumulator: List<FromSourceAliases>): List<FromSourceAliases> {
            return accumulator
        }

    }.walkFromSource(this, emptyList())

