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

import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorObjectHandle
import org.partiql.spi.connector.ConnectorObjectPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.sources.ValueDescriptor
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

/**
 * This mock implementation of [ConnectorMetadata] searches for JSON files from its [root] to
 * resolve requests for any [BindingPath].
 */
class LocalConnectorMetadata(val name: String, private val root: Path) : ConnectorMetadata {

    override fun getObjectDescriptor(session: ConnectorSession, handle: ConnectorObjectHandle): ValueDescriptor {
        val jsonHandle = handle.value as LocalConnectorObject
        return jsonHandle.getDescriptor()
    }

    override fun getObjectHandle(
        session: ConnectorSession,
        path: BindingPath
    ): ConnectorObjectHandle? {
        val resolvedObject = resolveObject(root, path.steps) ?: return null
        return ConnectorObjectHandle(
            absolutePath = ConnectorObjectPath(resolvedObject.names),
            value = LocalConnectorObject(resolvedObject.json)
        )
    }

    //
    //
    // HELPER METHODS
    //
    //

    private class NamespaceMetadata(
        val names: List<String>,
        val json: String
    )

    private fun resolveObject(root: Path, names: List<BindingName>): NamespaceMetadata? {
        var current = root
        val fileNames = mutableListOf<String>()
        names.forEach { name ->
            current = resolveDirectory(current, name) ?: return@forEach
            fileNames.add(current.fileName.toString())
        }
        if (fileNames.lastIndex == names.lastIndex) {
            return null
        }
        val table = names[fileNames.size]
        val tablePaths = Files.list(current).toList()
        var filename = ""
        val tableDef = tablePaths.firstOrNull { file ->
            filename = file.getName(file.nameCount - 1).toString().removeSuffix(".json")
            table.isEquivalentTo(filename)
        } ?: return null
        val tableDefString = String(Files.readAllBytes(tableDef))
        return NamespaceMetadata(
            names = fileNames + listOf(filename),
            json = tableDefString
        )
    }

    private fun resolveDirectory(root: Path, name: BindingName): Path? {
        val schemaPaths = Files.list(root).toList()
        return schemaPaths.firstOrNull { directory ->
            val filename = directory.getName(directory.nameCount - 1).toString()
            name.isEquivalentTo(filename)
        }
    }
}
