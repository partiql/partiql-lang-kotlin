package org.partiql.plugins
import org.partiql.plugins.jdbc.ConnectionFactory
import org.partiql.plugins.jdbc.JdbcClient
import org.partiql.plugins.jdbc.JdbcColumn
import org.partiql.plugins.jdbc.JdbcTable
import org.partiql.plugins.jdbc.JdbcType
import org.partiql.plugins.jdbc.ReadFunction
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.bagValue
import org.partiql.value.int64Value
import org.partiql.value.structValue
import java.sql.Connection
import java.sql.ResultSetMetaData
import java.sql.SQLException

public abstract class BaseJdbcClient(
    public open val connectionFactory: ConnectionFactory
) : JdbcClient {

    // Ideally we encode information like credential in ConnectorSession.
    override fun getJdbcSchema(session: ConnectorSession): Set<String> {
        // establish connection:
        val connection = connectionFactory.openConnection(session) ?: error("failed to establish connection")
        return listSchema(connection)
    }

    override fun getJdbcTable(
        session: ConnectorSession,
        catalog: String?,
        schema: String?,
        tableName: String?
    ): JdbcTable? {
        val connection = connectionFactory.openConnection(session) ?: error("Failed to establish connection")
        val query = buildString {
            this.append("SELECT * FROM ")
            catalog?.let { this.append("$catalog.") }
            schema?.let { this.append("$schema.") }
            this.append(tableName)
        }

        val resultSetMetaData = try {
            connection.prepareStatement(query).metaData
        } catch (e: SQLException) {
            return null
        }
        val columns = getJdbcColumns(session, connection, resultSetMetaData)
        return JdbcTable(catalog, schema, columns)


    }


    @OptIn(PartiQLValueExperimental::class)
    override fun getJdbcColumns(session: ConnectorSession, connection: Connection, metadata: ResultSetMetaData): List<JdbcColumn> =
        (1..metadata.columnCount).map {
            val name = metadata.getColumnName(it)
            val jdbcType = JdbcType(
                metadata.getColumnType(it),
                metadata.getColumnName(it),
                metadata.getPrecision(it),
                metadata.getScale(it)
            )
            val partiQLValueType = toColumnMapping(session, connection, jdbcType).partiQLValueType
            JdbcColumn(name, jdbcType, partiQLValueType, true)
        }

    @OptIn(PartiQLValueExperimental::class)
    // TODO: A way to cache
    override fun getBinding(session: ConnectorSession): ConnectorBindings {
        return object : ConnectorBindings {
            override fun getValue(path: ConnectorPath): PartiQLValue {
                val connection = connectionFactory.openConnection(session) ?: error("Failed to establish connection")
                val query = buildString {
                    this.append("SELECT * FROM ")
                    path.steps.forEach {
                        this.append("$it.")
                    }
                    this.setLength(this.length - 1)
                }

                val prepared = connection.prepareStatement(query)

                val resultSetMetaData = prepared.metaData

                val columns = getJdbcColumns(session, connection, resultSetMetaData)

                val resultSet = prepared.executeQuery()

                return bagValue(
                    sequence {
                        while (resultSet.next()) {
                            val pairs = columns.mapIndexed { idx, column ->
                                val readFunc = toColumnMapping(session, connection, column.columnJdbcType).readFunction as ReadFunction
                                column.columnName to int64Value((readFunc.read(resultSet, idx + 1) as Number).toLong())
                            }
                            yield(structValue(pairs))
                        }
                    }.toList()
                )
            }
        }
    }

    private fun listSchema(connection: Connection): Set<String> {
        //  Single catalog; multiple schema; multiple tables under each schema
        val catalog = connection.catalog
        val schemas = connection.metaData.getSchemas(catalog, null)
        return buildSet {
            while(schemas.next()) {
                val schemaName = schemas.getString("TABLE_SCHEM")
                this.add(schemaName)
            }
        }
    }
}
