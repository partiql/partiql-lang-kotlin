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

package org.partiql.plugins.fs.connector

import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.StructElement
import org.partiql.plugins.fs.FsDB
import org.partiql.plugins.fs.getAngry
import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.connector.sql.SqlConnector
import org.partiql.spi.connector.sql.SqlMetadata
import java.nio.file.Paths

/**
 * Fs connector implementation wraps a database.
 */
internal class FsConnector(private val database: FsDB) : SqlConnector() {

    private val bindings = FsBindings(database.index)

    /**
     * Access to the <obj>.shape.ion files.
     */
    override fun getMetadata(session: ConnectorSession): SqlMetadata = FsMetadata(database.index, info)

    /**
     * Access to the <obj>.ion files.
     */
    override fun getBindings(): ConnectorBindings = bindings

    // we need a way to reload the connector upon write op
    // observer?

    /**
     * Load a FsDB from the given root.
     */
    internal class Factory : Connector.Factory {

        override val name: String = "Fs"

        override fun create(catalogName: String, config: StructElement?): Connector {
            assert(config != null) { "Fs plugin requires non-null config" }
            val root = config!!.getAngry<StringElement>("root").let { Paths.get(it.textValue) }
            val db = FsDB.load(root)
            val listener = Thread(db, "listener thread")
            listener.start()
            return FsConnector(db)
        }
    }
}
