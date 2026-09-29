package org.partiql.spi.utils

import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

/**
 * Reads this numeric [Datum] as its backing [Number]. This is a pure value extractor: callers must
 * check [Datum.isNull] first.
 *
 * @throws NullPointerException if the value is null.
 * @throws IllegalStateException if the type is not a number type.
 */
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
