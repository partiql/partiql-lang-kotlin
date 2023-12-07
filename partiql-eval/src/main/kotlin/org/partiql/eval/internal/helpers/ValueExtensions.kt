package org.partiql.eval.internal.helpers

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueType
import org.partiql.value.bagValue
import org.partiql.value.binaryValue
import org.partiql.value.blobValue
import org.partiql.value.boolValue
import org.partiql.value.byteValue
import org.partiql.value.charValue
import org.partiql.value.clobValue
import org.partiql.value.dateValue
import org.partiql.value.decimalValue
import org.partiql.value.float32Value
import org.partiql.value.float64Value
import org.partiql.value.int16Value
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.int8Value
import org.partiql.value.intValue
import org.partiql.value.intervalValue
import org.partiql.value.listValue
import org.partiql.value.missingValue
import org.partiql.value.nullValue
import org.partiql.value.sexpValue
import org.partiql.value.stringValue
import org.partiql.value.structValue
import org.partiql.value.symbolValue
import org.partiql.value.timeValue
import org.partiql.value.timestampValue

/**
 * Constructor for a typed null.
 */
internal fun PartiQLValueType.toNull(): () -> PartiQLValue = when (this) {
    PartiQLValueType.ANY -> {
        { nullValue() }
    }
    PartiQLValueType.BOOL -> {
        { boolValue(null) }
    }
    PartiQLValueType.INT8 -> {
        { int8Value(null) }
    }
    PartiQLValueType.INT16 -> {
        { int16Value(null) }
    }
    PartiQLValueType.INT32 -> {
        { int32Value(null) }
    }
    PartiQLValueType.INT64 -> {
        { int64Value(null) }
    }
    PartiQLValueType.INT -> {
        { intValue(null) }
    }
    PartiQLValueType.DECIMAL -> {
        { decimalValue(null) }
    }
    PartiQLValueType.FLOAT32 -> {
        { float32Value(null) }
    }
    PartiQLValueType.FLOAT64 -> {
        { float64Value(null) }
    }
    PartiQLValueType.CHAR -> {
        { charValue(null) }
    }
    PartiQLValueType.STRING -> {
        { stringValue(null) }
    }
    PartiQLValueType.SYMBOL -> {
        { symbolValue(null) }
    }
    PartiQLValueType.BINARY -> {
        { binaryValue(null) }
    }
    PartiQLValueType.BYTE -> {
        { byteValue(null) }
    }
    PartiQLValueType.BLOB -> {
        { blobValue(null) }
    }
    PartiQLValueType.CLOB -> {
        { clobValue(null) }
    }
    PartiQLValueType.DATE -> {
        { dateValue(null) }
    }
    PartiQLValueType.TIME -> {
        { timeValue(null) }
    }
    PartiQLValueType.TIMESTAMP -> {
        { timestampValue(null) }
    }
    PartiQLValueType.INTERVAL -> {
        { intervalValue(null) }
    }
    PartiQLValueType.BAG -> {
        { bagValue<PartiQLValue>(null) }
    }
    PartiQLValueType.LIST -> {
        { listValue<PartiQLValue>(null) }
    }
    PartiQLValueType.SEXP -> {
        { sexpValue<PartiQLValue>(null) }
    }
    PartiQLValueType.STRUCT -> {
        { structValue<PartiQLValue>(null) }
    }
    PartiQLValueType.NULL -> {
        { nullValue() }
    }
    PartiQLValueType.MISSING -> {
        { missingValue() }
    }
}
