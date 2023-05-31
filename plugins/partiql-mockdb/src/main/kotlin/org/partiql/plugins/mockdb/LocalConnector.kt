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

package org.partiql.plugins.mockdb

import com.amazon.ionelement.api.StructElement
import org.partiql.plugins.mockdb.LocalConnector.Companion.ROOT_KEY
import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorSession
import java.nio.file.Path
import java.nio.file.Paths

/**
 * A mock implementation of [Connector] that provides access to data in a specified [root] [Path]. To configure the root,
 * please specify the corresponding value of [ROOT_KEY] in the [config].
 *
 * For example, to specify the root of the filesystem, your [config] might look like:
 * ```ion
 * {
 *     "connector_name": "localdb",
 *     "localdb_root": "/Users/me/some/root/directory"
 * }
 * ```
 */
class LocalConnector(private val catalogName: String, private val config: StructElement) : Connector {

    companion object {
        const val CONNECTOR_NAME = "localdb"
        const val ROOT_KEY = "localdb_root"
    }

    private val default: Path = Paths.get(System.getProperty("user.home")).resolve(".partiql/localdb")
    private val root = config.getOptional(ROOT_KEY)?.stringValueOrNull?.let {
        Paths.get(it)
    } ?: default
    private val metadata = LocalConnectorMetadata(catalogName, root)

    override fun getMetadata(session: ConnectorSession): ConnectorMetadata = metadata

    class Factory : Connector.Factory {

        override fun getName(): String = CONNECTOR_NAME

        override fun create(catalogName: String, config: StructElement): Connector = LocalConnector(catalogName, config)
    }
}
