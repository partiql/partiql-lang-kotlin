package org.partiql.lang.ast.passes

import org.partiql.lang.ast.CaseSensitivity
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.FromSource
import org.partiql.lang.ast.FromSourceJoin
import org.partiql.lang.ast.FromSourceLet
import org.partiql.lang.ast.ScopeQualifier
import org.partiql.lang.ast.Select
import org.partiql.lang.ast.SelectListItemExpr
import org.partiql.lang.ast.SelectListItemProjectAll
import org.partiql.lang.ast.SelectListItemStar
import org.partiql.lang.ast.SelectProjectionList
import org.partiql.lang.ast.SymbolicName
import org.partiql.lang.ast.UniqueNameMeta
import org.partiql.lang.ast.VariableReference
import org.partiql.lang.ast.emptyMetaContainer
import org.partiql.lang.eval.errNoContext

class SelectStarRewriter : AstRewriterBase() {
    override fun rewriteSelect(selectExpr: Select): ExprNode {
        val rewritten = super.innerRewriteSelect(selectExpr)

        val projection = rewritten.projection

        // Check if SELECT * is being used.
        if(projection is SelectProjectionList && projection.items.any { it is SelectListItemStar }) {
            if(projection.items.count() > 1) {
                // We throw an error here in this case because otherwise this rewriter will eat any select list items
                // that might exist in addition to '*'.  Better to barf than cause confusing behavior.
                errNoContext("'*' can only exist in the select list by itself", internal = true)
            }
            when {
                // No group by
                rewritten.groupBy == null -> {
                    val fromSourceAliases = extractAliases(rewritten.from)

                    val newProjection =
                        SelectProjectionList(
                            fromSourceAliases.map { aliases ->
                                // We are concatenating 3 lists here
                                listOf(createSelectListItemProjectAll(aliases.asAlias)) +
                                (aliases.atAlias?.let { listOf(createSelectListItemExpr(it)) } ?: emptyList()) +
                                (aliases.byAlias?.let { listOf(createSelectListItemExpr(it)) } ?: emptyList())
                            }.flatten()
                        )

                    return rewritten.copy(projection = newProjection)
                }
                // With group by
                else -> {
                    val selectListItemsFromGroupBy = rewritten.groupBy.groupByItems.map {
                        val asName = it.asName
                                     ?: errNoContext(
                                         "GroupByItem has no AS-alias--GroupByItemAliasVisitorTransform must be executed before SelectStarRewriter",
                                         internal = true)

                        // We need to take the unique name of each grouping field key only because we need to handle
                        // the case when multiple grouping fields are assigned the same name (which is currently allowed)
                        val uniqueNameMeta = asName.metas[UniqueNameMeta.TAG] as? UniqueNameMeta?
                                             ?: error("UniqueNameMeta not found--normally, this is added by GroupByItemAliasVisitorTransform")

                        createSelectListItemExpr(uniqueNameMeta.uniqueName, asName.name)
                    }

                    val groupNameItem = rewritten.groupBy.groupName?.name?.let {
                        listOf(createSelectListItemExpr(it))
                    } ?: emptyList()

                    return rewritten.copy(projection = SelectProjectionList(selectListItemsFromGroupBy + groupNameItem))
                }
            }
        }
        return rewritten
    }
}

private fun createSelectListItemProjectAll(name: String) =
    SelectListItemProjectAll(
        VariableReference(name, CaseSensitivity.SENSITIVE, ScopeQualifier.UNQUALIFIED, emptyMetaContainer))

private fun createSelectListItemExpr(variableName: String, asAlias: String = variableName) =
    SelectListItemExpr(
        VariableReference(variableName, CaseSensitivity.SENSITIVE, ScopeQualifier.UNQUALIFIED, emptyMetaContainer),
        SymbolicName(asAlias, emptyMetaContainer))

private class FromSourceAliases(val asAlias: String, val atAlias: String?, val byAlias: String?)
private fun extractAliases(fromSource: FromSource): List<FromSourceAliases> =
    when (fromSource) {
        is FromSourceLet -> {
            listOf(
                FromSourceAliases(
                    fromSource.variables.asName?.name
                        ?: error("FromSourceAliasRewriter must be executed before SelectStarRewriter"),
                    fromSource.variables.atName?.name,
                    fromSource.variables.byName?.name))
        }
        is FromSourceJoin -> {
            extractAliases(fromSource.leftRef) + extractAliases(fromSource.rightRef)
        }
    }

