package org.partiql.cli.plugin.localdb2

import org.partiql.catalog.Name
import org.partiql.cli.plugin.localdb.LocalConnectorTableHandle
import org.partiql.spi.BindingName
import org.partiql.catalog.Catalog
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorMetadata2
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.connector.ConnectorTableHandle
import org.partiql.spi.sources.TableSchema
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

class LocalConnectorMetadata2 : ConnectorMetadata {

    private val homeDir: Path = Paths.get(System.getProperty("user.home"))
    private val catalogDir = homeDir.resolve(".partiql/localdb")
    private val catalog = initCatalog()

    fun getCatalog(session: ConnectorSession): Catalog {
        return catalog
    }

    private fun initCatalog(): Catalog {
        var catalog = Catalog()
        var name = Name("localdb", listOf(Name("house")))
        catalog.addObject("plant", name)
        return catalog
    }

    override fun getObjectHandle(session: ConnectorSession, objectName: BindingName): ConnectorTableHandle? {
        var qualifiedName = ""
        println("1")
        catalog.objects.forEach {(k, v) ->
            println("v=${v.id}")
            qualifiedName = v.id
            v.children.forEach { c ->
                println("c=$c")
                qualifiedName = "$qualifiedName/${c.id}"
            }
            qualifiedName = "$qualifiedName/$k"
            println("path=$qualifiedName")
        }

        println("$qualifiedName.json")
        val objectPath = homeDir.resolve(".partiql/$qualifiedName.json")
        val tableDefString = String(Files.readAllBytes(objectPath))
        return LocalConnectorTableHandle(tableDefString)
    }

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
