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

package org.partiql.cli.puglin.localdb

import com.amazon.ionelement.api.StructElement
import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorSession
import java.nio.file.Path
import java.nio.file.Paths

class LocalConnector(private val catalogName: String, private val config: StructElement) : Connector {

    private val default: Path = Paths.get(System.getProperty("user.home")).resolve(".partiql/localdb")
    private val root = config.getOptional("localdb_root")?.stringValueOrNull?.let {
        Paths.get(it)
    } ?: default
    private val metadata = LocalConnectorMetadata(catalogName, root)

    override fun getMetadata(session: ConnectorSession): ConnectorMetadata = metadata

    class Factory : Connector.Factory {

        override fun getName(): String = "localdb"

        override fun create(catalogName: String, config: StructElement): Connector = LocalConnector(catalogName, config)
    }
}
