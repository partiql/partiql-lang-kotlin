/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval.visitors

import com.amazon.ionelement.api.emptyMetaContainer
import org.partiql.errors.ErrorCode
import org.partiql.errors.Problem
import org.partiql.errors.Property
import org.partiql.errors.UNKNOWN_PROBLEM_LOCATION
import org.partiql.lang.ast.passes.SemanticException
import org.partiql.lang.ast.passes.SemanticProblemDetails
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.string
import org.partiql.lang.domains.toBindingName
import org.partiql.lang.domains.toId
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.errorContextFrom
import org.partiql.lang.eval.extractColumnAlias
import org.partiql.lang.eval.sourceLocationMeta

/**
 * This VisitorTransform:
 * - adds unique aliases to each group key
 * - replaces group key references (ids) with their unique name
 * - replaces group key references (ids) with the expression that they represent (if within an aggregation function)
 * - handles scoping by keeping a context stack
 * - converts project star into the aggregation outputs (group keys and group as)
 * - throws errors on projection items that reference variables that are not part of the aggregation operator output
 *
 * This VisitorTransform is specifically used by the planner implementation.
 *
 * Turns:
 *
 * ```
 * SELECT
 *     k AS group_key,
 *     SUM(k) AS the_sum,
 *     g AS the_input,
 *     (
 *         SELECT k + SUM(k), SUM(h)
 *         FROM t2
 *         GROUP BY t2.a AS h
 *     ) AS projection_query
 * FROM t1
 * GROUP BY t1.a AS k, t1.b AS h
 * GROUP AS g
 * HAVING k + SUM(k) > 0
 * ORDER BY
 *     k + SUM(k)
 *     AND
 *     (SELECT k + SUM(k), SUM(h) FROM t2)
 * ```
 *
 * Into:
 *
 * ```
 * SELECT
 *     $__partiql__group_by_0_0 AS group_key,
 *     SUM(t1.a) AS the_sum,
 *     "g" AS the_input,
 *     (
 *         SELECT $__partiql__group_by_0_0 + SUM($__partiql__group_by_0_0), SUM($__partiql__group_by_1_0)
 *         FROM t2
 *         GROUP BY t2.a AS $__partiql__group_by_1_0
 *     ) AS projection_query
 * FROM t1
 * GROUP BY t1.a AS $__partiql__group_by_0_0, t1.b AS $__partiql__group_by_0_0
 * GROUP AS g
 * HAVING $__partiql__group_by_0_0 + SUM(t1.a) > 0
 * ORDER BY
 *     $__partiql__group_by_0_0 + SUM(t1.a)
 *     AND
 *     (SELECT $__partiql__group_by_0_0 + SUM($__partiql__group_by_0_0), SUM($__partiql__group_by_0_1) FROM t2)
 * ```
 *
 */
internal class AggregationVisitorTransform(
    private val contextStack: MutableList<VisitorContext> = mutableListOf()
) : VisitorTransformBase() {

    private val itemTransform = GroupKeyReferenceTransformer(contextStack)

    companion object {
        internal const val GROUP_PREFIX = "\$__partiql__group_by_"
        internal const val GROUP_DELIMITER = "_"
        internal fun uniqueAlias(level: Int, index: Int) = "$GROUP_PREFIX$level$GROUP_DELIMITER$index"
    }

    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
        // Every transformExprSelect implicitly adds to the contextStack during the nested transformExprSelect_group.
        // Therefore, after transforming the ExprSelect, we need to pop from the contextStack.
        return super.transformExprSelectEvaluationOrder(node).also { contextStack.removeLast() }
    }

    override fun transformExprSelect_group(node: PartiqlAst.Expr.Select): PartiqlAst.GroupBy? {
        // Return with Empty Context if without Group
        val containsAggregations = AggregationFinder().containsAggregations(node.project)
        if (node.group == null) {
            val context = VisitorContext(emptyList(), null, containsAggregations)
            contextStack.add(context)
            return null
        }

        // Add Unique Aliases to Keys and Create GroupKeyInformation
        val groupAsAlias = node.group!!.groupAsAlias
        val transformedKeys = mutableListOf<PartiqlAst.GroupKey>()
        val groupKeyInformation = node.group!!.keyList.keys.mapIndexed { index, key ->
            val publicAlias = key.asAlias ?: key.expr.extractColumnAlias(index)
            val uniqueAlias = uniqueAlias(this.contextStack.size, index)
            val represents = key.expr
            val transformedKey = PartiqlAst.build { groupKey(transformExpr(key.expr), defnid(uniqueAlias, delimited()), key.metas) }
            transformedKeys.add(transformedKey)
            val isPublicAliasUserDefined = key.asAlias != null
            GroupKeyInformation(groupKey = key, publicAlias = publicAlias, uniqueAlias = uniqueAlias, represents = represents, isPublicAliasUserDefined = isPublicAliasUserDefined)
        }

        // Add to Context Stack and return modified Group Keys
        val hasAggregateOperator = containsAggregations || groupKeyInformation.isNotEmpty() || groupAsAlias != null
        val ctx = VisitorContext(groupKeyInformation, groupAsAlias, hasAggregateOperator)
        contextStack.add(ctx)
        return PartiqlAst.build {
            groupBy(
                strategy = transformGroupBy_strategy(node.group!!),
                keyList = groupKeyList(transformedKeys),
                groupAsAlias = groupAsAlias,
                metas = node.group!!.metas
            )
        }
    }

    override fun transformExprSelect_having(node: PartiqlAst.Expr.Select): PartiqlAst.Expr? = node.having?.let { having ->
        itemTransform.transformExpr(having)
    }

    override fun transformSortSpec_expr(node: PartiqlAst.SortSpec) = itemTransform.transformSortSpec_expr(node)

    /**
     * Replaces [node] with [PartiqlAst.Projection.ProjectList] IF there are Group Keys and/or a Group Alias
     */
    override fun transformProjectionProjectStar(node: PartiqlAst.Projection.ProjectStar): PartiqlAst.Projection {
        if (contextStack.last().groupKeys.isNotEmpty() || contextStack.last().groupAsAlias != null) {
            return PartiqlAst.build {
                val projectionItems = contextStack.last().groupKeys.map { key ->
                    projectExpr(vr(id(key.uniqueAlias, delimited()), unqualified()), key.publicAlias)
                }.toMutableList()

                contextStack.last().groupAsAlias?.let { alias ->
                    // SQL-ids Prior code had adding delimited()/caseSensitive(),
                    // but now it comes with its own, so we keep what it has
                    val item = projectExpr(vr(alias.toId(), unqualified()), alias)
                    projectionItems.add(item)
                }
                projectList(projectionItems)
            }
        }
        return super.transformProjectionProjectStar(node)
    }

    override fun transformProjectionProjectValue(node: PartiqlAst.Projection.ProjectValue): PartiqlAst.Projection =
        PartiqlAst.build {
            projectValue(
                value = itemTransform.transformExpr(node.value),
                metas = node.metas
            )
        }

    override fun transformProjectionProjectList(node: PartiqlAst.Projection.ProjectList): PartiqlAst.Projection =
        PartiqlAst.build {
            projectList(
                projectItems = node.projectItems.map { item ->
                    when (item) {
                        is PartiqlAst.ProjectItem.ProjectExpr -> {
                            val projectionAlias = item.asAlias ?: throw SemanticException(
                                err = Problem(
                                    (item.metas.sourceLocationMeta?.toProblemLocation() ?: UNKNOWN_PROBLEM_LOCATION),
                                    details = SemanticProblemDetails.MissingAlias
                                )
                            )

                            projectExpr(
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

    /**
     * Recursively searches through a [PartiqlAst.Projection] to find [PartiqlAst.Expr.CallAgg]'s, but does NOT recurse
     * into [PartiqlAst.Expr.Select]. Designed to be called directly using [containsAggregations].
     */
    private class AggregationFinder : PartiqlAst.Visitor() {

        var hasAggregations: Boolean = false

        fun containsAggregations(node: PartiqlAst.Projection): Boolean {
            this.walkProjection(node)
            return this.hasAggregations.also { this.hasAggregations = false }
        }

        override fun visitExprCallAgg(node: PartiqlAst.Expr.CallAgg) {
            hasAggregations = true
        }

        override fun walkExprSelect(node: PartiqlAst.Expr.Select) { return }
    }

    /**
     * This VisitorTransform:
     * 1. transforms group key references (ids) to the unique name given to the group key
     * 2. transforms group key references (expressions) to the unique name given to the group key (if no explicit
     *  user-defined alias is given.
     * 3. transforms group key references (ids) to the expression that they group key represents (if the id is within
     * a [PartiqlAst.Expr.CallAgg] that is in scope of the current aggregate operator)
     * 4. throws exceptions when identifiers are seen within the projection list, having, or order by clauses that do
     *  not reference Group Keys (when the aggregate operator is defined)
     *
     * This also handles scoping by using the [ctxStack]. This class is designed to be used directly on the
     * [PartiqlAst.Projection], the [PartiqlAst.Expr.Select.having], and the [PartiqlAst.OrderBy].
     */
    private class GroupKeyReferenceTransformer(
        private val ctxStack: MutableList<VisitorContext>,
        private val isWithinCallAgg: Boolean = false
    ) : VisitorTransformBase() {

        override fun transformExprVr(node: PartiqlAst.Expr.Vr): PartiqlAst.Expr = when (this.isWithinCallAgg) {
            true -> getReplacementForIdInAggregationFunction(node)
            false -> getReplacementForIdOutsideOfAggregationFunction(node)
        }

        override fun transformExprCallAgg(node: PartiqlAst.Expr.CallAgg): PartiqlAst.Expr = PartiqlAst.build {
            val functionArgTransformer = GroupKeyReferenceTransformer(ctxStack, true)
            callAgg(
                setq = transformSetQuantifier(node.setq),
                funcName = node.funcName,
                arg = functionArgTransformer.transformExprCallAgg_arg(node),
                metas = transformMetas(node.metas)
            )
        }

        override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
            return AggregationVisitorTransform(ctxStack).transformExprSelect(node)
        }

        override fun transformExpr(node: PartiqlAst.Expr): PartiqlAst.Expr {
            return getReplacementExpression(node) ?: super.transformExpr(node)
        }

        private fun getReplacementExpression(node: PartiqlAst.Expr): PartiqlAst.Expr? {
            if (this.isWithinCallAgg) { return null }

            val ctxStackIter = ctxStack.listIterator(ctxStack.size)
            while (ctxStackIter.hasPrevious()) {
                val ctx = ctxStackIter.previous()
                ctx.groupKeys.firstOrNull { key ->
                    key.isPublicAliasUserDefined.not() && key.represents == node
                }?.let { key ->
                    return PartiqlAst.build {
                        vr(id(key.uniqueAlias, delimited()), unqualified(), emptyMetaContainer())
                    }
                }
            }
            return null
        }

        /**
         * IDs outside of aggregation functions should always be replaced with the Group Key unique aliases. If no
         * replacement is found, we throw an EvaluationException.
         */
        private fun getReplacementForIdOutsideOfAggregationFunction(node: PartiqlAst.Expr.Vr): PartiqlAst.Expr {
            val ctxStackIter = ctxStack.listIterator(ctxStack.size)
            while (ctxStackIter.hasPrevious()) {
                val ctx = ctxStackIter.previous()
                getReplacementInNormalContext(node, ctx)?.let { replacementId -> return replacementId }
            }

            when (ctxStack.last().hasLogicalAggregate) {
                false -> return node
                true -> throw EvaluationException(
                    "Variable not in GROUP BY or aggregation function: ${node.id.symb.text}",
                    ErrorCode.EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY,
                    errorContextFrom(node.metas).also {
                        it[Property.BINDING_NAME] = node.id.symb.text
                    },
                    internal = false
                )
            }
        }

        /**
         * Called from within a CallAgg -- and therefore, all IDs should be replaced with the current context's
         * [GroupKeyInformation.represents]. If not found, search for replacements within "normal" parent contexts.
         */
        private fun getReplacementForIdInAggregationFunction(node: PartiqlAst.Expr.Vr): PartiqlAst.Expr {
            getReplacementInAggregationContext(node, ctxStack.last())?.let { replacement -> return replacement }

            val ctxStackIter = ctxStack.listIterator(ctxStack.size)
            while (ctxStackIter.hasPrevious()) {
                val ctx = ctxStackIter.previous()
                getReplacementInNormalContext(node, ctx)?.let { replacementId -> return replacementId }
            }
            return node
        }

        /**
         * Gets replacement ID (the alias of the Group Key or Group Alias (ID))
         */
        private fun getReplacementInNormalContext(node: PartiqlAst.Expr.Vr, ctx: VisitorContext): PartiqlAst.Expr.Vr? {
            val bindingName = node.id.toBindingName()
            val replacementKey = ctx.groupKeys.firstOrNull { key ->
                bindingName.isEquivalentTo(key.publicAlias.string())
            }?.let { key -> PartiqlAst.build { vr(id(key.uniqueAlias, delimited()), node.qualifier) } }

            return when {
                replacementKey != null -> replacementKey
                bindingName.isEquivalentTo(ctx.groupAsAlias?.string()) -> PartiqlAst.build { vr(id(node.id.symb.text, delimited()), unqualified()) }
                else -> null
            }
        }

        /**
         * Gets replacement Expr (what the Group Key represents, or the Group As Alias)
         */
        private fun getReplacementInAggregationContext(node: PartiqlAst.Expr.Vr, ctx: VisitorContext): PartiqlAst.Expr? {
            val bindingName = node.id.toBindingName()
            val replacementExpression = ctx.groupKeys.firstOrNull { key ->
                bindingName.isEquivalentTo(key.publicAlias.string())
            }?.represents

            return when {
                replacementExpression != null -> replacementExpression
                bindingName.isEquivalentTo(ctx.groupAsAlias?.string()) -> PartiqlAst.build { vr(id(node.id.symb.text, delimited()), unqualified()) }
                else -> null
            }
        }
    }

    internal data class VisitorContext(
        val groupKeys: List<GroupKeyInformation>,
        val groupAsAlias: PartiqlAst.Defnid?,
        val hasLogicalAggregate: Boolean
    )

    internal data class GroupKeyInformation(
        val groupKey: PartiqlAst.GroupKey,
        val represents: PartiqlAst.Expr,
        val publicAlias: PartiqlAst.Defnid,
        val uniqueAlias: String,
        val isPublicAliasUserDefined: Boolean
    )
}
