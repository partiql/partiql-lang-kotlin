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

package org.partiql.plugins.local

import com.amazon.ionelement.api.StructElement
import org.partiql.planner.catalog.Catalog
import org.partiql.spi.connector.Connector
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists

/**
 * An implementation of a PartiQL [Connector] backed by a catalog in a local directory.
 *
 * Set to the "root" key to specify the root of the local database.
 *
 * ```ion
 * {
 *   connector_name: "local",
 *   root: "/Users/me/some/root/directory"
 * }
 * ```
 */
public class LocalConnector private constructor(
    private val name: String,
    private val root: Path,
) : Connector {

    private val catalog = LocalCatalog(name, root)

    public companion object {

        public const val CONNECTOR_NAME: String = "local"

        public const val ROOT_KEY: String = "root"

        @JvmStatic
        public fun builder(): Builder = Builder()

        public class Builder internal constructor() {

            private var name: String? = null

            private var root: Path? = null

            public fun name(name: String): Builder = apply { this.name = name }

            public fun root(root: Path): Builder = apply { this.root = root }

            public fun build(): LocalConnector = LocalConnector(name!!, root!!)
        }
    }

    override fun getCatalog(): Catalog = catalog

    internal class Factory : Connector.Factory {

        override val name: String = CONNECTOR_NAME

        override fun create(config: StructElement): Connector {
            val root = config.getOptional(ROOT_KEY)?.stringValueOrNull?.let { Paths.get(it) }
            if (root == null) {
                error("Root cannot be null")
            }
            if (root.notExists() || !root.isDirectory()) {
                error("Invalid catalog `$root`")
            }
            return LocalConnector("default", root)
        }
    }
}
