package org.partiql.spi.utils

import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal fun Datum.getNumber(): Number {
    return when (this.type.code()) {
        PType.TINYINT -> this.byte
        PType.INTEGER -> this.int
        PType.SMALLINT -> this.short
        PType.BIGINT -> this.long
        PType.REAL -> this.float
        PType.DOUBLE -> this.double
        PType.DECIMAL -> this.bigDecimal
        PType.NUMERIC -> this.bigDecimal
        else -> error("Unexpected type: ${this.type}")
    }
}
