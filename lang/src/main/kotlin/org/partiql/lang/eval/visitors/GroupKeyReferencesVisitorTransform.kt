/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */
package org.partiql.lang.eval.visitors

import org.partiql.lang.ast.IsGroupAttributeReferenceMeta
import org.partiql.lang.ast.UniqueNameMeta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.metaContainerOf
import org.partiql.lang.domains.toBindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.MapBindings
import org.partiql.lang.planner.transforms.errAstNotNormalized

/**
 * Specifies any previously unspecified select list item aliases.
 *
 * Turns:
 *
 * ```
 * SELECT groupKey AS projectionAlias
 * FROM source AS sourceTable
 * GROUP BY sourceTable.a AS groupKey(meta = UniqueNameMeta:someUniqueName)
 * ORDER BY groupKey
 * ```
 *
 * Into:
 *
 * ```
 * SELECT someUniqueName AS projectionAlias
 * FROM source AS sourceTable
 * GROUP BY sourceTable.a AS groupKey(meta = UniqueNameMeta:someUniqueName)
 * ORDER BY someUniqueName
 * ```
 *
 * If provided with a query with all of the select list aliases are already specified, an exact clone is returned.
 *
 * ```
 */
@Deprecated(
    "This class is subject to removal.",
    level = DeprecationLevel.WARNING
)
public class GroupKeyReferencesVisitorTransform(
    private val keys: Map<String, PartiqlAst.GroupKey> = emptyMap(),
    private val groupAliases: Set<String> = emptySet()
) : VisitorTransformBase() {

    private val itemTransform = GroupKeyReferencesToUniqueNameIdsVisitorTransform(this.keys, this.groupAliases)

    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
        val keys = getGroupByKeys(node) + this.keys
        val aliases = setOfNotNull(getGroupAsAlias(node)) + this.groupAliases
        return GroupKeyReferencesVisitorTransform(keys, aliases).transformExprSelectSupport(node)
    }

    private fun transformExprSelectSupport(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
        val transformedNode = transformExprSelectEvaluationOrder(node) as PartiqlAst.Expr.Select
        if (node.group == null) {
            return transformedNode
        }
        val projection = this.transformProjection(node.project)
        val order = node.order?.let { this.transformOrderBy(it) }
        return transformedNode.copy(project = projection, order = order)
    }

    override fun transformProjectionProjectValue(node: PartiqlAst.Projection.ProjectValue): PartiqlAst.Projection {
        return PartiqlAst.build {
            projectValue(
                value = itemTransform.transformExpr(node.value),
                metas = node.metas
            )
        }
    }

    override fun transformSortSpec_expr(node: PartiqlAst.SortSpec) = itemTransform.transformSortSpec_expr(node)

    override fun transformProjectionProjectList(node: PartiqlAst.Projection.ProjectList): PartiqlAst.Projection {
        return PartiqlAst.build {
            projectList(
                projectItems = node.projectItems.mapIndexed { _, item ->
                    when (item) {
                        is PartiqlAst.ProjectItem.ProjectExpr -> {
                            val projectionAlias = item.asAlias ?: errAstNotNormalized(
                                "All projection expressions should have an alias prior to arriving at the SelectListGroupKeysVisitorTransform."
                            )
                            projectExpr_(
                                expr = itemTransform.transformExpr(item.expr),
                                asAlias = projectionAlias
                            )
                        }
                        else -> item
                    }
                },
                metas = node.metas
            )
        }
    }

    private fun getGroupByKeys(node: PartiqlAst.Expr.Select): Map<String, PartiqlAst.GroupKey> {
        val groupByKeys = mutableMapOf<String, PartiqlAst.GroupKey>()
        node.group?.keyList?.keys?.reversed()?.forEach { key ->
            val keyAlias = key.asAlias?.text ?: errAstNotNormalized("Group By Keys should all have aliases.")
            groupByKeys[keyAlias] = key
        }
        return groupByKeys
    }

    private fun getGroupAsAlias(node: PartiqlAst.Expr.Select): String? = node.group?.groupAsAlias?.text
}

/**
 * Transforms identifiers that reference group keys into the Group Key's alias or unique name.
 */
private class GroupKeyReferencesToUniqueNameIdsVisitorTransform(val keys: Map<String, PartiqlAst.GroupKey>, val aliases: Set<String>) : VisitorTransformBase() {
    private val groupKeybindings = MapBindings(keys)
    private val groupAsBindings = MapBindings(aliases.associateWith { 1 })

    override fun transformExprId(node: PartiqlAst.Expr.Id): PartiqlAst.Expr {
        val bindingName = BindingName(node.name.text, node.case.toBindingCase())
        if (groupAsBindings[bindingName] != null) {
            val metas = node.metas + metaContainerOf(IsGroupAttributeReferenceMeta.instance)
            return node.copy(metas)
        }
        return when (val key = groupKeybindings[bindingName]) {
            null -> node
            else -> {
                val uniqueName = key.asAlias?.metas?.get(UniqueNameMeta.TAG) as? UniqueNameMeta
                    ?: errAstNotNormalized("All group keys should have an alias")
                val metas = key.metas + metaContainerOf(IsGroupAttributeReferenceMeta.instance)
                PartiqlAst.build {
                    id(uniqueName.uniqueName, caseSensitive(), unqualified(), metas = metas)
                }
            }
        }
    }

    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
        return GroupKeyReferencesVisitorTransform(keys, aliases).transformExprSelect(node)
    }
}
