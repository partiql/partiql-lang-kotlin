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

package org.partiql.plugins.fs

import org.partiql.plugins.fs.connector.FsConnector
import org.partiql.spi.Plugin
import org.partiql.spi.connector.Connector
import java.nio.file.Path

/**
 * LocalPlugin is a PartiQL plugin that provides schemas written in PartiQL Value Schema.
 *
 * Backed by a memoized catalog tree from the given root dir; global bindings are files.
 */
public class FsPlugin : Plugin {

    override val factory: Connector.Factory = FsConnector.Factory()

    public companion object {

        // workaround for simplicity
        public fun create(root: Path): Connector {
            val db = FsDB.load(root)
            val listener = Thread(db, "listener thread")
            listener.start()
            return FsConnector(db)
        }
    }
}
