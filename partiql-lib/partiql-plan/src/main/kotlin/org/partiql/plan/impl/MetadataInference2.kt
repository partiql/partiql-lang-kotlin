package org.partiql.plan.impl

import org.partiql.catalog.Catalog
import org.partiql.catalog.Name
import org.partiql.plan.PlannerSession
import org.partiql.plan.PlannerSession2
import org.partiql.plan.TableHandle
import org.partiql.plan.passes.TableHandle2
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.Plugin
import org.partiql.spi.Plugin2
import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.Connector2
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorMetadata2
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.sources.TableSchema

internal class MetadataInference2(
    private val plugins: List<Plugin>,
    private val connector: String,
) : Metadata2 {
    override fun getCatalog(session: PlannerSession2): Catalog {
        TODO()
    }

    override fun getTableHandle(session: PlannerSession2, name: org.partiql.lang.eval.BindingName): TableHandle2? {
        val connectorSession = session.toConnectorSession()
        val metadata = getMetadata(session.toConnectorSession(), session.connector)
        val objectName = convertBindingName(name!!)
        return metadata.getObjectHandle(connectorSession, objectName)?.let {
            TableHandle2(
                connectorHandle = it,
            )
        }
    }

    override fun getTableSchema(session: PlannerSession2, handle: TableHandle2): TableSchema {
        val connectorSession = session.toConnectorSession()
        val metadata = getMetadata(connectorSession, session.connector)
        return metadata.getTableSchema(connectorSession, handle.connectorHandle)!!
    }

    private fun getMetadata(connectorSession: ConnectorSession, connectorName: String): ConnectorMetadata {
        val connectorFactory = plugins.flatMap { it.getConnectorFactories() }.first { it.getName() == connectorName }
        val connector = connectorFactory.create()
        return connector.getMetadata(session = connectorSession)
    }

    private fun getConnector(session: PlannerSession2, connectorName: String): Connector {
        val connectorSession = session.toConnectorSession()
        val connectorFactory = plugins.flatMap { it.getConnectorFactories() }.first { it.getName() == connectorName }
        return connectorFactory.create()
    }

    private fun convertBindingName(name: org.partiql.lang.eval.BindingName): BindingName {
        return BindingName(
            name.name,
            convertBindingCase(name.bindingCase)
        )
    }

    private fun convertBindingCase(case: org.partiql.lang.eval.BindingCase): BindingCase = when (case) {
        org.partiql.lang.eval.BindingCase.INSENSITIVE -> BindingCase.INSENSITIVE
        org.partiql.lang.eval.BindingCase.SENSITIVE -> BindingCase.SENSITIVE
    }
}
