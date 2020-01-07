/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.lang.ast.passes

import org.partiql.lang.ast.*
import org.partiql.lang.eval.*


/**
 * Pre-calculates [GroupByItem] aliases, while not changing any that were previously specified, for example:
 *
 * `SELECT * FROM a, GROUP BY a.b AS foo, a.c, 2 + 3` becomes
 * `SELECT * FROM a, GROUP BY a.b AS foo, a.c AS c, 2 + 3 as _3`.
 *
 * Also adds [UniqueNameMeta] to [GroupBy.alias.metas].  In order for the generated unique names to be correct,
 * this rewrite must be presented with only the top-most node in a query.
 *
 * If provided with a query with all of the group by item aliases already specified, an exact clone is returned.
 */
class GroupByItemAliasRewriter(val nestLevel: Int = 0) : AstRewriterBase() {
    override fun rewriteGroupBy(groupBy: GroupBy): GroupBy =
        GroupBy(
            grouping = groupBy.grouping,
            groupByItems = groupBy.groupByItems.mapIndexed { index, it ->
                val alias = it.asName ?: SymbolicName(it.expr.extractColumnAlias(index),
                                                      it.expr.metas + metaContainerOf(IsSyntheticNameMeta.instance))

                GroupByItem(
                    rewriteExprNode(it.expr),
                    alias.copy(metas = alias.metas.add(UniqueNameMeta("\$__partiql__group_by_${nestLevel}_item_$index"))))
            },
            groupName = groupBy.groupName?.let { rewriteSymbolicName(it) }
        )

    override fun rewriteSelect(selectExpr: Select): ExprNode =
        GroupByItemAliasRewriter(nestLevel + 1)
            .innerRewriteSelect(selectExpr)
}

