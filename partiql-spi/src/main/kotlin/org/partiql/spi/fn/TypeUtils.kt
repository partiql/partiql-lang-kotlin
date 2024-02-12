package org.partiql.spi.fn

import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

internal object TypeUtils {
    @OptIn(PartiQLValueExperimental::class)
    fun PartiQLValueType.toNonNullStaticType(): StaticType = when (this) {
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
