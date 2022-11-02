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

package org.partiql.shell

import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.MapBindings
import org.partiql.lang.eval.StructOrdering
import org.partiql.lang.eval.delegate
import org.partiql.lang.eval.structExprValue

class ShellGlobalBinding {
    private val knownNames = mutableSetOf<String>()
    var bindings = Bindings.empty<ExprValue>()
        private set

    fun add(bindings: Bindings<ExprValue>): ShellGlobalBinding {
        when (bindings) {
            is MapBindings -> {
                knownNames.addAll(bindings.originalCaseMap.keys)
                this.bindings = bindings.delegate(this.bindings)
            }
            Bindings.empty<ExprValue>() -> {
            } // nothing to do
            else -> throw IllegalArgumentException("Invalid binding type for global environment: $bindings")
        }

        return this
    }

    fun asExprValue(): ExprValue {
        val values: Sequence<ExprValue> = knownNames.map {
            bindings[BindingName(it, BindingCase.SENSITIVE)]
        }.filterNotNull().asSequence()

        return structExprValue(values, StructOrdering.UNORDERED)
    }
}
