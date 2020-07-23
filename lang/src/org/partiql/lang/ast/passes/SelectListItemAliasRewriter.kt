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

import org.partiql.lang.ast.SelectListItemExpr
import org.partiql.lang.ast.SelectListItemProjectAll
import org.partiql.lang.ast.SelectListItemStar
import org.partiql.lang.ast.SelectProjection
import org.partiql.lang.ast.SelectProjectionList
import org.partiql.lang.ast.SymbolicName
import org.partiql.lang.eval.extractColumnAlias

// DL TODO: delete this file.

/**
 * Specifies any previously unspecified select list item aliases.
 *
 * Turns:
 *
 * ```
 *  SELECT
 *      foo,
 *      b.bat,
 *      baz + 1
 * FROM bar AS b
 * ```
 *
 * Into:
 *
 * ```
 *  SELECT
 *      foo AS foo,
 *      b.bat AS bat,
 *      baz + 1 AS _3
 * FROM bar AS b
 * ```
 *
 * If provided with a query with all of the select list aliases are already specified, an exact clone is returned.
 *
 * ```
 */
class SelectListItemAliasRewriter : AstRewriterBase() {

    override fun rewriteSelectProjectionList(projection: SelectProjectionList): SelectProjection =
        SelectProjectionList(
            items = projection.items.mapIndexed { index, item ->
                when (item) {
                    is SelectListItemExpr                              ->
                        SelectListItemExpr(
                            expr = rewriteExprNode(item.expr),
                            asName = item.asName ?: SymbolicName(item.expr.extractColumnAlias(index),
                                                                 item.expr.metas))

                    is SelectListItemProjectAll, is SelectListItemStar ->
                        super.rewriteSelectListItem(item)
                }
            }
        )
}

