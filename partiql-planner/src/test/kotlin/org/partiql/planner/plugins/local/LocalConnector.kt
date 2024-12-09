/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.planner.plugins.local

import org.partiql.spi.Connector
import org.partiql.spi.catalog.Catalog
import java.nio.file.Path

/**
 * An implementation of a PartiQL [Connector] backed by a catalog in a local directory.
 *
 * Example:
 * ```
 * val connector = LocalConnector()
 * val context = LocalConnector.Context(Paths.get("/path/to/catalog"))
 * val catalog = connector.getCatalog("my_catalog", context)
 * // use catalog
 * ```
 */
public class LocalConnector : Connector {

    /**
     * Context arguments for instantiating a [LocalConnector] catalog.
     *
     * @property root   The catalog root directory.
     */
    public class Context(@JvmField public val root: Path) : Connector.Context

    override fun getCatalog(name: String): Catalog {
        throw IllegalArgumentException("LocalConnector cannot instantiate a catalog with no context")
    }

    override fun getCatalog(name: String, context: Connector.Context): Catalog {
        if (context !is Context) {
            throw IllegalArgumentException("LocalConnector context must be of type ${Context::class.java}, found: ${context::class.java}")
        }
        return getCatalog(name, context)
    }

    private fun getCatalog(name: String, context: Context): Catalog = LocalCatalog(name, context.root)
}
