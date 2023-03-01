package org.partiql.plan.impl

import org.partiql.catalog.Catalog
import org.partiql.catalog.Name
import org.partiql.plan.PlannerSession
import org.partiql.plan.PlannerSession2
import org.partiql.plan.TableHandle
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.Plugin
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.sources.TableSchema

internal class MetadataInference2(
    private val plugins: List<Plugin>,
    private val connector: String,
) : Metadata2 {
    override fun getCatalog(session: PlannerSession2): Catalog {
        var catalog = Catalog()
        var name = Name("localdb")
        name.addChildren(listOf(Name("house")))
        catalog.addObject("plant", name)
        return catalog
    }

    override fun schemaExists(session: PlannerSession, catalogName: String, schemaName: String): Boolean {
//        val connectorSession = session.toConnectorSession()
//        val metadata = getMetadata(connectorSession, catalogName)
//        return metadata.schemaExists(connectorSession, BindingName(schemaName, BindingCase.SENSITIVE))
        TODO()
    }

    override fun getTableHandle(session: PlannerSession, tableName: QualifiedObjectName): TableHandle? {
//        val connectorSession = session.toConnectorSession()
//        val catalogName = tableName.catalogName?.name ?: return null
//        val metadata = getMetadata(session.toConnectorSession(), catalogName)
//        val schemaName = convertBindingName(tableName.schemaName!!)
//        val objectName = convertBindingName(tableName.objectName!!)
//        return metadata.getTableHandle(connectorSession, schemaName, objectName)?.let {
//            TableHandle(
//                connectorHandle = it,
//                catalogName = catalogName
//            )
//        }
        TODO()
    }

    override fun getTableSchema(session: PlannerSession, handle: TableHandle): TableSchema {
        val connectorSession = session.toConnectorSession()
//        val metadata = getMetadata(session.toConnectorSession(), handle.catalogName)
//        return metadata.getTableSchema(connectorSession, handle.connectorHandle)!!
        TODO()
    }

//    private fun getMetadata(connectorSession: ConnectorSession, catalogName: String): ConnectorMetadata {
//        val connectorName = catalogMap[catalogName] ?: error(
//            "Unknown catalog: $catalogName"
//        )
//        val connectorFactory = plugins.flatMap { it.getConnectorFactories() }.first { it.getName() == connectorName }
//        val connector = connectorFactory.create()
//        return connector.getMetadata(session = connectorSession)
//    }

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
