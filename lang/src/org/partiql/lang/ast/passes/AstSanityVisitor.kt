
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

/**
 * Provides rules for basic AST sanity checks that should be performed before any attempt at further AST processing.
 * This is provided as a distinct visitor so that all other passes may assume that the AST at least passed the
 * checking performed here.
 *
 * Any exception thrown by this class should always be considered an indication of a bug in one of the following places:
 *
 * - [org.partiql.lang.syntax.SqlParser]
 * - A rewrite pass (internal or external)
 *
 * At the time of this writing there are only 3 checks performed, with the idea that more may be added later.  Checks
 * that:
 *
 * - [NAry] arity is correct
 * - [DataType] arity is correct.
 * - `*` is not used in conjunction with other select list items.
 */
@Deprecated("Use AstSanityValidator instead")
class AstSanityVisitor : AstVisitor {

    override fun visitExprNode(expr: ExprNode) {
        AstSanityValidator.validate(expr)
    }

    override fun visitSelectProjection(projection: SelectProjection) {
        AstSanityValidator.validate(projection)
    }

    override fun visitDataType(dataType: DataType) {
        AstSanityValidator.validate(dataType)
    }
}
