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

import com.amazon.ionelement.api.StructElement
import org.partiql.eval.bindings.Binding
import org.partiql.eval.bindings.Bindings
import org.partiql.planner.catalog.Catalog
import org.partiql.planner.catalog.Name
import org.partiql.planner.catalog.Table
import org.partiql.spi.connector.Connector

/**
 * This is a plugin used for testing and is not a versioned API per semver.
 */
public class MemoryConnector private constructor(
    private val name: String,
    private val tables: Map<String, MemoryTable>,
) : Connector {

    override fun getBindings(): Bindings = bindings

    override fun getCatalog(): Catalog = catalog

    /**
     * For use with ServiceLoader to instantiate a connector from an Ion config.
     */
    internal class Factory : Connector.Factory {

        override val name: String = "memory"

        override fun create(config: StructElement): Connector {
            TODO("Instantiation of a MemoryConnector via the factory is currently not supported")
        }
    }

    public companion object {

        @JvmStatic
        public fun builder(): Builder = Builder()

        public class Builder internal constructor() {

            private var name: String? = null
            private var tables: MutableMap<String, MemoryTable> = mutableMapOf()

            public fun name(name: String): Builder = apply { this.name = name }

            public fun createTable(table: MemoryTable): Builder = apply { tables[table.getName()] = table }

            public fun build(): MemoryConnector = MemoryConnector(name!!, tables)
        }
    }

    /**
     * Implement [Bindings] over the tables map.
     */
    private val bindings = object : Bindings {
        override fun getBindings(name: String): Bindings? = null
        override fun getBinding(name: String): Binding? = tables[name]
    }

    /**
     * Implement [Catalog] over the tables map.
     */
    private val catalog = object : Catalog {

        override fun getName(): String = name

        override fun getTable(name: Name): Table? {
            if (name.hasNamespace()) {
                error("MemoryCatalog does not support namespaces")
            }
            return tables[name.getName()]
        }

        override fun listTables(): Collection<Name> {
            return tables.keys.map { Name.of(it) }
        }
    }
}
