package org.partiql.cli.plugin.localdb

import org.partiql.spi.BindingName
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.connector.ConnectorTableHandle
import org.partiql.spi.sources.TableSchema
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

class LocalConnectorMetadata : ConnectorMetadata {

    private val homeDir: Path = Paths.get(System.getProperty("user.home"))
    private val catalogDir = homeDir.resolve(".partiql/localdb")

    override fun getTableSchema(session: ConnectorSession, handle: ConnectorTableHandle): TableSchema {
        val jsonHandle = handle as LocalConnectorTableHandle
        return jsonHandle.getSchemaMetadata()
    }

    override fun getTableHandle(
        session: ConnectorSession,
        schema: BindingName?,
        table: BindingName
    ): ConnectorTableHandle? {
        if (schema == null) { return null }
        if (schemaExists(session, schema).not()) return null

        val schemaPaths = Files.list(catalogDir).toList()
        val schemaPath = schemaPaths.firstOrNull { path ->
            val filename = path.getName(path.nameCount - 1).toString()
            schema.isEquivalentTo(filename)
        } ?: return null

        val tablePaths = Files.list(schemaPath).toList()
        val tableDef = tablePaths.firstOrNull { path ->
            val filename = path.getName(path.nameCount - 1).toString().removeSuffix(".json")
            table.isEquivalentTo(filename)
        } ?: return null
        val tableDefString = String(Files.readAllBytes(tableDef))
        return LocalConnectorTableHandle(tableDefString)
    }

    override fun getObjectHandle(session: ConnectorSession, objectName: BindingName): ConnectorTableHandle? {
        TODO("Not yet implemented")
    }

    override fun schemaExists(session: ConnectorSession, name: BindingName): Boolean {
        if (Files.exists(catalogDir).not()) return false
        val schemaPaths = Files.list(catalogDir).toList()
        schemaPaths.firstOrNull { path ->
            val filename = path.getName(path.nameCount - 1).toString()
            name.isEquivalentTo(filename)
        } ?: return false
        return true
    }
}
