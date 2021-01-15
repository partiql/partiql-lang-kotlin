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

import org.partiql.lang.ast.DataType
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.HasMetas
import org.partiql.lang.ast.MetaContainer
import org.partiql.lang.ast.metaContainerOf

@Deprecated("Will be removed after existing tests no longer require stripping metas")
class MetaStrippingRewriter : AstRewriterBase() {
    companion object {
        private val emptyMetas = metaContainerOf()
        fun stripMetas(expr: ExprNode): ExprNode {
            val rewriter = MetaStrippingRewriter()
            return rewriter.rewriteExprNode(expr)
        }
    }

    override fun rewriteMetas(itemWithMetas: HasMetas): MetaContainer = emptyMetas
    override fun rewriteDataType(dataType: DataType): DataType =
        DataType(dataType.sqlDataType, dataType.args, emptyMetas)
}
