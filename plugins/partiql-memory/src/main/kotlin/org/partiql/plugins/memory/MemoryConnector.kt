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
import org.partiql.planner.catalog.Catalog
import org.partiql.planner.catalog.Identifier
import org.partiql.planner.catalog.Name
import org.partiql.planner.catalog.Session
import org.partiql.planner.catalog.Table
import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorBinding
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.types.PType
import org.partiql.types.StaticType

/**
 * This is a plugin used for testing and is not a versioned API per semver.
 */
public class MemoryConnector private constructor(
    private val name: String,
    private val tables: Map<Name, MemoryTable>,
) : Connector {

    override fun getBindings(): ConnectorBindings = bindings

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
    }

    public class Builder internal constructor() {

        private var name: String? = null
        private var tables: MutableMap<Name, MemoryTable> = mutableMapOf()

        public fun name(name: String): Builder = apply { this.name = name }

        // TODO REMOVE AFTER CREATE TABLE IS ADDED TO CATALOG
        public fun define(name: String, type: StaticType): Builder {
            val table = MemoryTable.empty(name, PType.fromStaticType(type))
            return define(table)
        }

        // TODO REMOVE AFTER CREATE TABLE IS ADDED TO CATALOG
        public fun define(table: MemoryTable): Builder = apply { tables[table.getName()] = table }

        public fun build(): MemoryConnector = MemoryConnector(name!!, tables)
    }

    /**
     * Implement [ConnectorBindings] over the tables map.
     */
    private val bindings = object : ConnectorBindings {
        override fun getBinding(name: Name): ConnectorBinding? = tables[name]
    }

    /**
     * Implement [Catalog] over the tables map.
     */
    private val catalog = object : Catalog {

        override fun getName(): String = name

        override fun getTable(session: Session, name: Name): Table? {
            if (name.hasNamespace()) {
                error("MemoryCatalog does not support namespaces")
            }
            return tables[name]
        }

        /**
         * TODO implement "longest match" on identifier searching.
         */
        override fun getTableHandle(session: Session, identifier: Identifier): Table.Handle? {
            // TODO memory connector does not handle qualified identifiers and longest match
            val first = identifier.first()
            for ((name, table) in tables) {
                val str = name.getName() // only use single identifiers for now
                if (first.matches(str)) {
                    // TODO emit errors on ambiguous table names
                    return Table.Handle(name, table)
                }
            }
            return super.getTableHandle(session, identifier)
        }

        override fun listTables(session: Session): Collection<Name> {
            return tables.keys.map { it }
        }
    }
}
