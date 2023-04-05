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

package org.partiql.lang.planner.transforms.impl

import com.amazon.ionelement.api.StructElement
import org.partiql.lang.planner.transforms.ObjectHandle
import org.partiql.lang.planner.transforms.PlannerSession
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.Plugin
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.connector.Constants
import org.partiql.spi.sources.ValueDescriptor

/**
 * Acts to consolidate multiple [org.partiql.spi.connector.ConnectorMetadata]'s.
 */
internal class Metadata(
    private val plugins: List<Plugin>,
    private val catalogMap: Map<String, StructElement>
) {

    private val connectorFactories = plugins.flatMap { it.getConnectorFactories() }
    private val connectorMap = catalogMap.toList().associate {
        val (catalogName, catalogConfig) = it
        catalogName to connectorFactories.first { factory ->
            val connectorName = catalogConfig[Constants.CONFIG_KEY_CONNECTOR_NAME].stringValue
            factory.getName() == connectorName
        }.create(catalogName, catalogConfig)
    }

    public fun getObjectHandle(session: PlannerSession, catalog: BindingName, path: BindingPath): ObjectHandle? {
        val connectorSession = session.toConnectorSession()
        val metadataInfo = getMetadata(session.toConnectorSession(), catalog) ?: return null
        return metadataInfo.metadata.getObjectHandle(connectorSession, path)?.let {
            ObjectHandle(
                connectorHandle = it,
                catalogName = metadataInfo.catalogName
            )
        }
    }

    public fun getObjectDescriptor(session: PlannerSession, handle: ObjectHandle): ValueDescriptor {
        val connectorSession = session.toConnectorSession()
        val metadata = getMetadata(session.toConnectorSession(), BindingName(handle.catalogName, BindingCase.SENSITIVE))!!.metadata
        return metadata.getObjectDescriptor(connectorSession, handle.connectorHandle)!!
    }

    private fun getMetadata(connectorSession: ConnectorSession, catalogName: BindingName): MetadataInformation? {
        val catalogKey = catalogMap.keys.firstOrNull { catalogName.isEquivalentTo(it) } ?: return null
        val connector = connectorMap[catalogKey] ?: return null
        return MetadataInformation(catalogKey, connector.getMetadata(session = connectorSession))
    }

    private class MetadataInformation(
        internal val catalogName: String,
        internal val metadata: ConnectorMetadata
    )
}
