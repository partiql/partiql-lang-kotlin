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

import org.partiql.spi.Connector
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Name

/**
 * This is the standard connector implementation.
 *
 * TODO move into partiql-spi package before 1.0!
 */
public class MemoryConnector : Connector {

    /**
     * TODO
     *
     * @property tables
     */
    public class Context(
        @JvmField public val tables: Map<Name, MemoryTable>,
    ) : Connector.Context

    override fun getCatalog(name: String): Catalog = getCatalog(name, Context(emptyMap()))

    override fun getCatalog(name: String, context: Connector.Context): Catalog {
        if (context !is Context) {
            throw IllegalArgumentException("MemoryConnector context must be of type ${Context::class.java}, found: ${context::class.java}")
        }
        return getCatalog(name, context)
    }

    private fun getCatalog(name: String, context: Context): Catalog {
        return MemoryCatalog(name, context.tables)
    }
}
