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
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorObjectHandle
import org.partiql.spi.connector.ConnectorObjectPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.StaticType
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
 * @property root       Catalog root path
 * @property catalog    Catalog name
 * @property format     Catalog format
 * @property config     Catalog configuration
 */
class LocalConnector(
    private val root: Path,
    private val catalog: String,
    private val format: LocalFormat,
    private val config: StructElement,
) : Connector {

    companion object {
        const val CONNECTOR_NAME = "local"
        const val ROOT_KEY = "root"
        const val FORMAT_KEY = "format"
    }

    private val metadata = Metadata(root)

    private val bindings = LocalBindings(root, format)

    // not yet defined in SPI
    public fun listObjects(): List<BindingPath> = metadata.listObjects()

    override fun getMetadata(session: ConnectorSession): ConnectorMetadata = metadata

    override fun getBindings(): ConnectorBindings {
        TODO("Not yet implemented")
    }

    class Factory : Connector.Factory {

        private val default: Path = Paths.get(System.getProperty("user.home")).resolve(".partiql/local")

        override val name: String = CONNECTOR_NAME

        override fun create(catalogName: String, config: StructElement): Connector {
            val root = config.getOptional(ROOT_KEY)?.stringValueOrNull?.let { Paths.get(it) }
            val catalogRoot = root ?: default
            if (catalogRoot.notExists()) {
                error("Invalid catalog `$catalogRoot`")
            }
            var catalogFormat = LocalFormat.ION
            val format = config.getOptional(FORMAT_KEY)?.stringValueOrNull
            if (format != null) {
                catalogFormat = LocalFormat.safeValueOf(format) ?: error("Invalid format $format")
            }
            return LocalConnector(catalogRoot, catalogName, catalogFormat, config)
        }
    }

    class Metadata(private val root: Path) : ConnectorMetadata {

        /**
         * TODO watch root for changes and rebuild catalog if needed.
         */
        // private val watcher = FileSystems.getDefault().newWatchService()

        /**
         * Cached catalog
         */
        private var catalog = LocalCatalog.load(root)

        override fun getObjectType(session: ConnectorSession, handle: ConnectorObjectHandle): StaticType {
            val obj = handle.value as LocalObject
            return obj.getDescriptor()
        }

        override fun getObjectHandle(session: ConnectorSession, path: BindingPath): ConnectorObjectHandle? {
            val value = catalog.lookup(path) ?: return null
            return ConnectorObjectHandle(
                absolutePath = ConnectorObjectPath(value.path),
                value = value,
            )
        }

        internal fun listObjects(): List<BindingPath> = catalog.listObjects()
    }
}
