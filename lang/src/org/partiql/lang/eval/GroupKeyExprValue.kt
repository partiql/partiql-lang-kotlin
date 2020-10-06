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

package org.partiql.lang.eval

import com.amazon.ion.*
import org.partiql.lang.eval.visitors.GroupByItemAliasVisitorTransform


/**
 * This is a special [ExprValue] just for group keys.
 *
 * It derives from [StructExprValue] and adds a second set of bindings for the "unique name" assigned to
 * group by expressions.  See [GroupByItemAliasVisitorTransform] and other uses of
 * [org.partiql.lang.ast.UniqueNameMeta].
 */
internal class GroupKeyExprValue(ion: IonSystem, sequence: Sequence<ExprValue>, private val uniqueNames: Map<String, ExprValue>)
    : StructExprValue(ion, StructOrdering.UNORDERED, sequence) {

    private val keyBindings by lazy {
        when {
            uniqueNames.any() -> Bindings.ofMap(uniqueNames).delegate(super.bindings)
            else              -> super.bindings
        }
    }

    override val bindings: Bindings<ExprValue>
        get() = keyBindings
}