/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.eval.plugin

import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.ExprValue

/**
 * Utility class for creating a MemoryCatalog.
 */
public class MemoryCatalogBuilder {

    private var name: String? = null
    private var bindings: Bindings<ExprValue> = Bindings.empty()

    public fun name(name: String): MemoryCatalogBuilder = this.apply { this.name = name }

    public fun binding(bindings: Bindings<ExprValue>) = this.apply { this.bindings = bindings }

    public fun build(): MemoryCatalog {
        val n = name ?: error("MemoryCatalog must have a name")
        return MemoryCatalog(n, bindings)
    }
}
