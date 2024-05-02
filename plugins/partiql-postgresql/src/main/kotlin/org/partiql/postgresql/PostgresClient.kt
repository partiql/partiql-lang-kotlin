 package org.partiql.postgresql

 import org.partiql.plugins.BaseJdbcClient
 import org.partiql.plugins.jdbc.ColumnMapping
 import org.partiql.plugins.jdbc.ConnectionFactory
 import org.partiql.plugins.jdbc.JdbcType
 import org.partiql.plugins.jdbc.ReadFunction
 import org.partiql.spi.connector.ConnectorSession
 import org.partiql.value.PartiQLValueExperimental
 import org.partiql.value.PartiQLValueType
 import java.sql.Connection
 import java.sql.ResultSet
 import java.sql.Types

 public class PostgresClient(
    override val connectionFactory: ConnectionFactory
 ) : BaseJdbcClient(connectionFactory) {
    @OptIn(PartiQLValueExperimental::class)
    override fun toColumnMapping(session: ConnectorSession, connection: Connection, jdbcType: JdbcType): ColumnMapping {
        val jdbcTypeName: String = jdbcType.jdbcTypeName

        return when (jdbcType.jdbcType) {
            Types.SMALLINT -> ColumnMapping(
                PartiQLValueType.INT16,
                ReadFunction.of(
                    object : ReadFunction.ReadFunctionImplementation<Short> {
                        override fun read(resultSet: ResultSet?, columnIndex: Int): Short {
                            if (resultSet != null) {
                                return resultSet.getObject(columnIndex, Short::class.java)
                            } else {
                                error("result set null")
                            }
                        }
                    }
                )
            )

            Types.INTEGER -> ColumnMapping(
                PartiQLValueType.INT32,
                ReadFunction.of(
                    object : ReadFunction.ReadFunctionImplementation<Int> {
                        override fun read(resultSet: ResultSet?, columnIndex: Int): Int {
                            if (resultSet != null) {
                                // unbox???
                                return resultSet.getObject(columnIndex, Int::class.javaObjectType)
                            } else {
                                error("result set null")
                            }
                        }
                    }
                )
            )

            Types.BIGINT -> ColumnMapping(
                PartiQLValueType.INT64,
                ReadFunction.of(
                    object : ReadFunction.ReadFunctionImplementation<Long> {
                        override fun read(resultSet: ResultSet?, columnIndex: Int): Long {
                            if (resultSet != null) {
                                return resultSet.getObject(columnIndex, Long::class.java)
                            } else {
                                error("result set null")
                            }
                        }
                    }
                )
            )

            else -> TODO()
        }
    }
 }
