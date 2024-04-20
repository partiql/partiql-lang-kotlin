package org.partiql.plugins

import org.partiql.plugins.jdbc.JdbcClient
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorObject
import org.partiql.spi.connector.ConnectorPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.connector.sql.SqlMetadata
import org.partiql.spi.connector.sql.info.InfoSchema
import org.partiql.spi.fn.FnExperimental
import org.partiql.types.BagType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

public class BaseJdbcMetadata(
    public val jdbcClient: JdbcClient,
    public val session: ConnectorSession,
    info: InfoSchema,
    ) : SqlMetadata(session, info) {


    /**
     * For the default implementation, we only support path steps up to three:
     */
    @OptIn(PartiQLValueExperimental::class)
    override fun getObject(path: BindingPath): ConnectorHandle.Obj? {
        val normalizedPath = path.normalized
        // TODO: Have a way to allow mapping of PartiQL Identifier to Remote Data source Identifier
        //  I.e., normalized PartiQL will be upper cased, other DB may use lower case
        val (catalog, schema, tableName) = when(path.steps.size) {
            1 -> Triple(null, null, normalizedPath.first())
            2 -> Triple(null, normalizedPath[0], normalizedPath[1])
            3 -> Triple(normalizedPath[0], normalizedPath[1], normalizedPath[2])
            else -> error("Path > 3 not yet supported")
        }

        val table = jdbcClient.getJdbcTable(session, catalog, schema, tableName) ?: return null
        return ConnectorHandle.Obj(
            ConnectorPath(path.normalized),
            object : ConnectorObject {
                override fun getType(): StaticType =
                    BagType(
                        StructType(
                            listOf(
                                StructType.Field(
                                    path.normalized.last(),
                                    StructType(
                                        table.columns.map {
                                            StructType.Field(
                                                it.columnName,
                                                it.partiQLValueType.toStaticType()
                                            )
                                        },
                                        true,
                                        emptyList(),
                                        setOf(
                                            TupleConstraint.Open(false)
                                        )
                                    )
                                )
                            ),
                            true,
                            emptyList(),
                            setOf(
                                TupleConstraint.Open(false)
                            )

                        )
                    )
            }
        )
    }

    @FnExperimental
    override fun getFunction(path: BindingPath): ConnectorHandle.Fn? =
        super.getFunction(path)

    @FnExperimental
    override fun getAggregation(path: BindingPath): ConnectorHandle.Agg? =
        super.getAggregation(path)

    @OptIn(PartiQLValueExperimental::class)
    internal fun PartiQLValueType.toStaticType(): StaticType = when (this) {
        PartiQLValueType.NULL -> StaticType.NULL
        PartiQLValueType.MISSING -> StaticType.MISSING
        else -> toNonNullStaticType().asNullable()
    }

    @OptIn(PartiQLValueExperimental::class)
    internal fun PartiQLValueType.toNonNullStaticType(): StaticType = when (this) {
        PartiQLValueType.ANY -> StaticType.ANY
        PartiQLValueType.BOOL -> StaticType.BOOL
        PartiQLValueType.INT8 -> StaticType.INT2
        PartiQLValueType.INT16 -> StaticType.INT2
        PartiQLValueType.INT32 -> StaticType.INT4
        PartiQLValueType.INT64 -> StaticType.INT8
        PartiQLValueType.INT -> StaticType.INT
        PartiQLValueType.DECIMAL_ARBITRARY -> StaticType.DECIMAL
        PartiQLValueType.DECIMAL -> StaticType.DECIMAL
        PartiQLValueType.FLOAT32 -> StaticType.FLOAT
        PartiQLValueType.FLOAT64 -> StaticType.FLOAT
        PartiQLValueType.CHAR -> StaticType.CHAR
        PartiQLValueType.STRING -> StaticType.STRING
        PartiQLValueType.SYMBOL -> StaticType.SYMBOL
        PartiQLValueType.BINARY -> TODO()
        PartiQLValueType.BYTE -> TODO()
        PartiQLValueType.BLOB -> StaticType.BLOB
        PartiQLValueType.CLOB -> StaticType.CLOB
        PartiQLValueType.DATE -> StaticType.DATE
        PartiQLValueType.TIME -> StaticType.TIME
        PartiQLValueType.TIMESTAMP -> StaticType.TIMESTAMP
        PartiQLValueType.INTERVAL -> TODO()
        PartiQLValueType.BAG -> StaticType.BAG
        PartiQLValueType.LIST -> StaticType.LIST
        PartiQLValueType.SEXP -> StaticType.SEXP
        PartiQLValueType.STRUCT -> StaticType.STRUCT
        PartiQLValueType.NULL -> StaticType.NULL
        PartiQLValueType.MISSING -> StaticType.MISSING
    }
}
