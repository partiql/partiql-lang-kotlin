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
package org.partiql.lang.ast

/**
 * This is used because the IN Predicate requires that the RHS cannot be a SELECT expression or a VALUES expression.
 * Therefore, attaching this to a VALUES expression (which is just a BAG in the AST) allows the PartiQLVisitor to
 * differentiate between bags (specifically for the IN Predicate).
 */
class IsValuesExprMeta private constructor() : Meta {
    override val tag = TAG

    companion object {
        const val TAG = "\$is_values_expr"

        val instance = IsValuesExprMeta()
    }
}
