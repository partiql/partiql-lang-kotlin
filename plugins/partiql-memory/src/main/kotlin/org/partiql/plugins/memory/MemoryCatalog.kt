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

package org.partiql.plugins.memory

import org.partiql.eval.bindings.Binding
import org.partiql.eval.bindings.Bindings
import org.partiql.planner.metadata.Metadata
import org.partiql.planner.metadata.Namespace

/**
 * A basic connector implementation used in testing.
 *
 * TODO incorporate the nested namespaces possible in the current v1 memory plugin.
 *      I'm keeping this simple for now before APIs stabilize.
 *
 * https://github.com/partiql/partiql-lang-kotlin/commit/f8cbeb83a8d4ba8f5218b9db016e0661d778441e
 */
public class MemoryCatalog private constructor(
    private val name: String,
    private val tables: Map<String, MemoryTable>,
) {

    // these could be anonymous, but I thought it was cleaner to separate.
    private val bindings = MBindings()
    private val metadata = MMetadata()

    public fun getBindings(): Bindings = bindings
    public fun getMetadata(): Metadata = metadata

    public class Builder internal constructor() {

        private var name: String? = null
        private var tables: MutableMap<String, MemoryTable> = mutableMapOf()

        public fun name(name: String): Builder = apply { this.name = name }

        public fun defineTable(name: String, table: MemoryTable): Builder = apply { tables[name] = table }

        public fun build(): MemoryCatalog = MemoryCatalog(name!!, tables)
    }

    public companion object {

        @JvmStatic
        public fun builder(): Builder = Builder()
    }

    private inner class MBindings : Bindings {
        override fun getBindings(name: String): Bindings? = null
        override fun getBinding(name: String): Binding? = tables[name]
    }

    private inner class MMetadata : Metadata {

        /**
         * Build a root namespace.
         */
        private val root = MemoryNamespace.builder()
            .name(name)
            .apply {
                for (table in tables) {
                    val n = table.key
                    val t = table.value
                    defineTable(n, t)
                }
            }
            .build()

        override fun getNamespace(): Namespace = root
    }
}
