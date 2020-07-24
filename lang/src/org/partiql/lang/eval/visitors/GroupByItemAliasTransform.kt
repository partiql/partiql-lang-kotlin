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

package org.partiql.lang.eval.visitors

import org.partiql.lang.ast.GroupBy
import org.partiql.lang.ast.GroupByItem
import org.partiql.lang.ast.IsSyntheticNameMeta
import org.partiql.lang.ast.UniqueNameMeta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.extractColumnAlias


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
class GroupByItemAliasTransform : PartiqlAst.VisitorTransform() {
    private var nestLevel = 0

    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr.Select {
        nestLevel++
        return super.transformExprSelect(node).also { nestLevel-- }
    }

    override fun transformGroupBy_groupKeys(node: PartiqlAst.GroupBy): PartiqlAst.GroupKeyList =
        PartiqlAst.build {
            groupKeyList(
                node.groupKeys.groupKeys.mapIndexed { index, it ->
                    val alias1 = it.asAlias
                        ?: it.expr.extractColumnAlias(index).withMeta(IsSyntheticNameMeta.TAG, IsSyntheticNameMeta.instance)
                    val newAlias = alias1.withMeta(UniqueNameMeta.TAG, UniqueNameMeta("\$__partiql__group_by_${nestLevel}_item_$index"))
                    groupKey_(
                        expr = super.transformExpr(it.expr),
                        asAlias = newAlias)
                })
        }

}




