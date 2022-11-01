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
 * Within the [PartiqlAst.Projection.ProjectList], the [PartiqlAst.Expr.Select.having], and the [PartiqlAst.Expr.Select.order]
 * clauses, this VisitorTransform replaces all references to Group Keys with their unique name OR replaces them with the
 * expression that they represent. This is specifically used for the planner.
 *
 * Note: The reason we need to conditionally replace with the unique name or with the expression they represent is due to
 * the design of the logically resolved plan. The aggregate operator outputs the variable declarations of the aggregate
 * functions and the group keys. Therefore, to allow the syntactic sugar of an aggregate function in the projection list
 * whose argument is a group key, we shall rewrite it to be the expression the group key represents. In contrast,
 * aggregate functions within sub-queries within the projection list (and other clauses) do not have access to what the
 * group key represents and therefore needs the unique name output.
 *
 * Turns:
 *
 * ```
 * SELECT
 *     k AS group_key,
 *     SUM(k) AS the_sum,
 *     (SELECT k + SUM(k), SUM(h) FROM t2 GROUP BY t2.a AS h) AS projection_query
 * FROM t1
 * GROUP BY t1.a AS k(meta = UniqueNameMeta:someUniqueName)
 * HAVING k + SUM(k) > 0
 * ORDER BY
 *     k + SUM(k)
 *     AND
 *     (SELECT k + SUM(k), SUM(h) FROM t2 GROUP BY t2.a AS h)
 * ```
 *
 * Into:
 *
 * ```
 * SELECT
 *     someUniqueName AS group_key,
 *     SUM(t1.a) AS the_sum,
 *     (SELECT someUniqueName + SUM(someUniqueName), SUM(t2.a) FROM t2 GROUP BY t2.a AS h) AS projection_query
 * FROM t1
 * GROUP BY t1.a AS k(meta = UniqueNameMeta:someUniqueName)
 * HAVING someUniqueName + SUM(t1.a) > 0
 * ORDER BY
 *     someUniqueName + SUM(t1.a)
 *     AND
 *     (SELECT someUniqueName + SUM(someUniqueName), SUM(t2.a) FROM t2 GROUP BY t2.a AS h)
 * ```
 *
 */
public class GroupKeyReferencesVisitorTransform(
    private val keys: Map<String, PartiqlAst.GroupKey> = mutableMapOf(),
    private val groupAliases: Set<String> = mutableSetOf(),
    private val nestedKeys: Map<String, PartiqlAst.GroupKey> = mutableMapOf(),
    private val nestedGroupAliases: Set<String> = mutableSetOf(),
) : VisitorTransformBase() {

    private val itemTransform = GroupKeyReferenceTransformer(
        this.keys,
        this.groupAliases,
        this.nestedKeys,
        this.nestedGroupAliases
    )

    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr = GroupKeyReferencesVisitorTransform(
        keys = getGroupByKeys(node) + this.keys,
        groupAliases = setOfNotNull(getGroupAsAlias(node)) + this.groupAliases,
        nestedKeys = this.nestedKeys,
        nestedGroupAliases = this.nestedGroupAliases
    ).transformExprSelectSupport(node)

    private fun transformExprSelectSupport(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
        val transformedNode = transformExprSelectEvaluationOrder(node) as PartiqlAst.Expr.Select
        if (node.group == null) {
            return transformedNode
        }
        val projection = this.transformProjection(node.project)
        val order = node.order?.let { this.transformOrderBy(it) }
        val having = node.having?.let { this.transformHaving(it) }
        return transformedNode.copy(project = projection, order = order, having = having)
    }

    override fun transformProjectionProjectValue(node: PartiqlAst.Projection.ProjectValue): PartiqlAst.Projection =
        PartiqlAst.build {
            projectValue(
                value = itemTransform.transformExpr(node.value),
                metas = node.metas
            )
        }

    private fun transformHaving(node: PartiqlAst.Expr): PartiqlAst.Expr = itemTransform.transformExpr(node)

    override fun transformSortSpec_expr(node: PartiqlAst.SortSpec) = itemTransform.transformSortSpec_expr(node)

    override fun transformProjectionProjectList(node: PartiqlAst.Projection.ProjectList): PartiqlAst.Projection =
        PartiqlAst.build {
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

    private fun getGroupByKeys(node: PartiqlAst.Expr.Select): LinkedHashMap<String, PartiqlAst.GroupKey> {
        val groupByKeys = mutableMapOf<String, PartiqlAst.GroupKey>()
        node.group?.keyList?.keys?.reversed()?.forEach { key ->
            val keyAlias = key.asAlias?.text ?: errAstNotNormalized("Group By Keys should all have aliases.")
            groupByKeys[keyAlias] = key
        }
        return groupByKeys as LinkedHashMap
    }

    private fun getGroupAsAlias(node: PartiqlAst.Expr.Select): String? = node.group?.groupAsAlias?.text

    /**
     * This transforms group key references (identifiers) to either:
     * 1. the unique name given to the group key (the output of the logical aggregate operator)
     * 2. or, the expression that the group key represents (the input to the logical aggregate operator)
     *
     * This also handles scoping by internally using LinkedHashMaps and LinkedHashSets to maintain the order in
     * which keys/aliases are added to [keyBindings] and [aliases]. The [keyBindings] and [aliases] represent the
     * GROUP BY <key> and GROUP AS <alias> of the current SFW query. The [parentKeyBindings] and [parentAliasBindings]
     * represent all of the outer SFW queries: SELECT (<inner query>) FROM t GROUP BY <parentKey> GROUP AS <parentAlias>.
     */
    private class GroupKeyReferenceTransformer(
        val keys: Map<String, PartiqlAst.GroupKey> = emptyMap(),
        val aliases: Set<String> = emptySet(),
        val parentKeys: Map<String, PartiqlAst.GroupKey> = emptyMap(),
        val parentAliases: Set<String> = emptySet(),
        val isInAggregateFunction: Boolean = false
    ) : VisitorTransformBase() {

        private val keyBindings = MapBindings(keys)
        private val parentKeyBindings = MapBindings(parentKeys)
        private val aliasBindings = MapBindings(aliases.associateWith { 1 })
        private val parentAliasBindings = MapBindings(parentAliases.associateWith { 1 })

        override fun transformExprId(node: PartiqlAst.Expr.Id): PartiqlAst.Expr {
            val bindingName = BindingName(node.name.text, node.case.toBindingCase())
            val childLevelKey = keyBindings[bindingName]
            val childLevelAlias = aliasBindings[bindingName]
            val parentLevelKey = parentKeyBindings[bindingName]
            val parentLevelAlias = parentAliasBindings[bindingName]
            return when {
                childLevelKey != null -> {
                    when (this.isInAggregateFunction) {
                        true -> childLevelKey.expr
                        false -> replaceKeyReferenceWithId(childLevelKey)
                    }
                }
                childLevelAlias != null -> addGroupingMeta(node)
                parentLevelKey != null -> replaceKeyReferenceWithId(parentLevelKey)
                parentLevelAlias != null -> addGroupingMeta(node)
                else -> node
            }
        }

        override fun transformExprCallAgg(node: PartiqlAst.Expr.CallAgg): PartiqlAst.Expr = PartiqlAst.build {
            val functionArgTransformer = GroupKeyReferenceTransformer(
                keys, aliases, parentKeys, parentAliases, true
            )
            callAgg_(
                setq = transformSetQuantifier(node.setq),
                funcName = node.funcName,
                arg = functionArgTransformer.transformExprCallAgg_arg(node),
                metas = transformMetas(node.metas)
            )
        }

        override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
            return GroupKeyReferencesVisitorTransform(
                nestedKeys = keys + parentKeys,
                nestedGroupAliases = aliases + parentAliases
            ).transformExprSelect(node)
        }

        private fun replaceKeyReferenceWithId(key: PartiqlAst.GroupKey): PartiqlAst.Expr.Id = PartiqlAst.build {
            val uniqueName = key.asAlias?.metas?.get(UniqueNameMeta.TAG) as? UniqueNameMeta
                ?: errAstNotNormalized("All group keys should have an alias")
            val metas = key.metas + metaContainerOf(IsGroupAttributeReferenceMeta.instance)
            id(uniqueName.uniqueName, caseSensitive(), unqualified(), metas = metas)
        }

        private fun addGroupingMeta(node: PartiqlAst.Expr.Id): PartiqlAst.Expr.Id {
            val metas = node.metas + metaContainerOf(IsGroupAttributeReferenceMeta.instance)
            return node.copy(metas)
        }
    }
}
