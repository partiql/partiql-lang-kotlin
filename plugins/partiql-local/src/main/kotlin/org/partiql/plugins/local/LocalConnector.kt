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
import org.partiql.eval.bindings.Bindings
import org.partiql.planner.metadata.Metadata
import org.partiql.planner.metadata.Namespace
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
public class LocalConnector(private val root: Path) : Connector {

    public companion object {
        public const val CONNECTOR_NAME: String = "local"
        public const val ROOT_KEY: String = "root"
    }

    override fun getBindings(): Bindings = LocalBindings

    override fun getMetadata(): Metadata = object : Metadata {
        override fun getNamespace(): Namespace = LocalNamespace(root)
    }

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
            return LocalConnector(root)
        }
    }
}
