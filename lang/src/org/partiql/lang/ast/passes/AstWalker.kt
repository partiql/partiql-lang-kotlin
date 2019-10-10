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
import org.partiql.lang.util.*

/**
 * Contains the logic necessary to walk every node in the AST and invokes methods of [AstVisitor] along the way.
 */
@Deprecated("Use AstNode#iterator() or AstNode#children()")
class AstWalker(private val visitor: AstVisitor) {

    fun walk(exprNode: ExprNode) {
        exprNode.forEach { node -> 
            when(node) {
                is ExprNode -> visitor.visitExprNode(node)
                is DataType -> visitor.visitDataType(node)
                is PathComponent -> visitor.visitPathComponent(node)
                is FromSource -> visitor.visitFromSource(node)
                is SelectListItem -> visitor.visitSelectListItem(node)
                is SelectProjection -> visitor.visitSelectProjection(node)
                    
            }
        }
    }
}