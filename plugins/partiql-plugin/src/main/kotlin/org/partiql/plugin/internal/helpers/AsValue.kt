package org.partiql.plugin.internal.helpers

import org.partiql.errors.DataException
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int8Value
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int16Value
import org.partiql.value.int32Value
import org.partiql.value.int8Value

@OptIn(PartiQLValueExperimental::class)
internal fun Int.asInt8(): Int8Value {
    if (this < Byte.MIN_VALUE || Byte.MAX_VALUE < this) {
        throw DataException("INT2 out of range: $this")
    }
    return int8Value(this.toByte())
}

@OptIn(PartiQLValueExperimental::class)
internal fun Int.asInt16(): Int16Value {
    if (this < Short.MIN_VALUE || Short.MAX_VALUE < this) {
        throw DataException("INT4 out of range: $this")
    }
    return int16Value(this.toShort())
}

@OptIn(PartiQLValueExperimental::class)
internal fun Long.asInt32(): Int32Value {
    if (this < Int.MIN_VALUE || Int.MAX_VALUE < this) {
        throw DataException("INT8 out of range: $this")
    }
    return int32Value(this.toInt())
}
