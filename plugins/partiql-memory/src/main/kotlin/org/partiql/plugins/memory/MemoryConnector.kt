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
import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.connector.sql.SqlConnector
import org.partiql.spi.connector.sql.SqlMetadata

/**
 * This is a plugin used for testing and is not a versioned API per semver.
 */
public class MemoryConnector(private val catalog: MemoryCatalog) : SqlConnector() {

    private val bindings = MemoryBindings(catalog)

    override fun getBindings(): ConnectorBindings = bindings

    override fun getMetadata(session: ConnectorSession): SqlMetadata = MemoryMetadata(catalog, session, info)

    internal class Factory(private val catalogs: List<MemoryCatalog>) : Connector.Factory {

        override val name: String = "memory"

        override fun create(catalogName: String, config: StructElement?): MemoryConnector {
            val catalog = catalogs.firstOrNull { it.name == catalogName }
                ?: error("Catalog $catalogName is not registered in the MemoryPlugin")
            return MemoryConnector(catalog)
        }
    }

    public companion object {

        @JvmStatic
        public fun empty(name: String = "default"): MemoryConnector = MemoryConnector(MemoryCatalog(name))
    }
}
