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
class SelectListGroupKeysVisitorTransform(
    val keys: Map<String, PartiqlAst.GroupKey> = emptyMap(),
    private val groupAliases: Set<String> = emptySet()
) : VisitorTransformBase() {

    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
        val keyVisitor = GroupKeyGathererVisitor()
        keyVisitor.walkExprSelect(node)
        val keys = keyVisitor.groupByKeys + this.keys
        val aliases = keyVisitor.groupAliases + this.groupAliases
        return SelectListGroupKeysVisitorTransform(keys, aliases).transformExprSelectSupport(node)
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
        val itemTransform = GroupKeyReferencesToUniqueNameIdsVisitorTransform(this.keys, this.groupAliases)
        return PartiqlAst.build {
            projectValue(
                value = itemTransform.transformExpr(node.value),
                metas = node.metas
            )
        }
    }

    override fun transformSortSpec_expr(node: PartiqlAst.SortSpec): PartiqlAst.Expr {
        val itemTransform = GroupKeyReferencesToUniqueNameIdsVisitorTransform(this.keys, this.groupAliases)
        return itemTransform.transformSortSpec_expr(node)
    }

    override fun transformProjectionProjectList(node: PartiqlAst.Projection.ProjectList): PartiqlAst.Projection {
        val itemTransform = GroupKeyReferencesToUniqueNameIdsVisitorTransform(this.keys, this.groupAliases)
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
}

private class GroupKeyReferencesToUniqueNameIdsVisitorTransform(val keys: Map<String, PartiqlAst.GroupKey>, val aliases: Set<String>) : VisitorTransformBase() {
    val groupKeybindings = MapBindings(keys)
    val groupAsBindings = MapBindings(aliases.associateWith { 1 })

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
        return SelectListGroupKeysVisitorTransform(keys, aliases).transformExprSelect(node)
    }
}

private class GroupKeyGathererVisitor : PartiqlAst.Visitor() {
    val groupByKeys = mutableMapOf<String, PartiqlAst.GroupKey>()
    val groupAliases = mutableSetOf<String>()
    override fun walkExprSelect(node: PartiqlAst.Expr.Select) {
        // Note: We use reversed() here so that we access the keys in LIFO order
        node.group?.keyList?.keys?.reversed()?.forEach { key ->
            val keyAlias = key.asAlias?.text ?: errAstNotNormalized("Group By Keys should all have aliases.")
            groupByKeys[keyAlias] = key
        }
        node.group?.groupAsAlias?.text?.let { text -> groupAliases.add(text) }
    }
}
