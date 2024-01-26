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
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorFnProvider
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.fn.FnExperimental
import java.nio.file.Path
import java.nio.file.Paths
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
 *
 * @property catalogRoot    Catalog root path
 * @property catalogName    Catalog name
 * @property config         Catalog configuration
 */
public class LocalConnector(
    private val catalogRoot: Path,
    private val catalogName: String,
    private val config: StructElement,
) : Connector {

    public companion object {
        public const val CONNECTOR_NAME: String = "local"
        public const val ROOT_KEY: String = "root"
    }

    private val metadata = Metadata(catalogRoot)

    // not yet defined in SPI
    public fun listObjects(): List<BindingPath> = metadata.listObjects()

    override fun getMetadata(session: ConnectorSession): ConnectorMetadata = metadata

    override fun getBindings(): ConnectorBindings {
        TODO("Not yet implemented")
    }

    @FnExperimental
    override fun getFunctions(): ConnectorFnProvider {
        TODO("Not yet implemented")
    }

    internal class Factory : Connector.Factory {

        private val default: Path = Paths.get(System.getProperty("user.home")).resolve(".partiql/local")

        override val name: String = CONNECTOR_NAME

        override fun create(catalogName: String, config: StructElement?): Connector {
            assert(config != null) { "Local plugin requires non-null config" }
            val root = config!!.getOptional(ROOT_KEY)?.stringValueOrNull?.let { Paths.get(it) }
            val catalogRoot = root ?: default
            if (catalogRoot.notExists()) {
                error("Invalid catalog `$catalogRoot`")
            }
            return LocalConnector(catalogRoot, catalogName, config)
        }
    }

    public class Metadata(root: Path) : ConnectorMetadata {

        /**
         * TODO watch root for changes and rebuild catalog if needed.
         */
        // private val watcher = FileSystems.getDefault().newWatchService()

        /**
         * Cached catalog
         */
        private var catalog = LocalCatalog.load(root)

        override fun getObject(path: BindingPath): ConnectorHandle.Obj? {
            val value = catalog.lookup(path) ?: return null
            return ConnectorHandle.Obj(
                path = value.path,
                entity = value,
            )
        }

        @FnExperimental
        override fun getFunction(path: BindingPath): ConnectorHandle.Fn? {
            TODO("Not yet implemented")
        }

        internal fun listObjects(): List<BindingPath> = catalog.listObjects()
    }
}
