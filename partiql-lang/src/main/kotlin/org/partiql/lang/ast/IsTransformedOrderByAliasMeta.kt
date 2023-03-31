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

import org.partiql.lang.eval.visitors.OrderBySortSpecVisitorTransform

/**
 * A [Meta] to help the [OrderBySortSpecVisitorTransform] to know when the OrderBy SortSpec has already been transformed. It
 * essentially helps to turn
 *
 * ```SELECT a + 1 AS b FROM c ORDER BY b```
 *
 * into
 *
 * ```SELECT a + 1 AS b FROM c ORDER BY a + 1```
 *
 * even when there are multiple transforms over the AST.
 */
class IsTransformedOrderByAliasMeta private constructor() : Meta {
    override val tag = TAG
    companion object {
        const val TAG = "\$is_transformed_order_by_alias"

        val instance = IsTransformedOrderByAliasMeta()
    }
}
