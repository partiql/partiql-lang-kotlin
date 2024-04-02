package org.partiql.eval.internal.helpers

import org.partiql.value.ArrayType
import org.partiql.value.BagType
import org.partiql.value.BitType
import org.partiql.value.BitVaryingType
import org.partiql.value.BlobType
import org.partiql.value.BoolType
import org.partiql.value.ByteType
import org.partiql.value.CharType
import org.partiql.value.CharVarType
import org.partiql.value.CharVarUnboundedType
import org.partiql.value.ClobType
import org.partiql.value.ClobUnboundedType
import org.partiql.value.DateType
import org.partiql.value.DynamicType
import org.partiql.value.Int16Type
import org.partiql.value.Int32Type
import org.partiql.value.Int64Type
import org.partiql.value.Int8Type
import org.partiql.value.IntervalType
import org.partiql.value.MissingType
import org.partiql.value.NullType
import org.partiql.value.NumericType
import org.partiql.value.PartiQLType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.TimeType
import org.partiql.value.TimeWithTimeZoneType
import org.partiql.value.TimestampType
import org.partiql.value.TimestampWithTimeZoneType
import org.partiql.value.TupleType
import org.partiql.value.TypeDoublePrecision
import org.partiql.value.TypeIntBig
import org.partiql.value.TypeNumericUnbounded
import org.partiql.value.TypeReal
import org.partiql.value.bagValue
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
import org.partiql.value.stringValue
import org.partiql.value.structValue
import org.partiql.value.timeValue
import org.partiql.value.timestampValue

/**
 * Constructor for a typed null.
 */
@OptIn(PartiQLValueExperimental::class)
internal fun PartiQLType.toNull(): () -> PartiQLValue = when (this) {
    is DynamicType -> {
        { nullValue() }
    }
    is BoolType -> {
        { boolValue(null) }
    }
    is Int8Type -> {
        { int8Value(null) }
    }
    is Int16Type -> {
        { int16Value(null) }
    }
    is Int32Type -> {
        { int32Value(null) }
    }
    is Int64Type -> {
        { int64Value(null) }
    }
    is TypeIntBig -> {
        { intValue(null) }
    }
    is NumericType -> {
        { decimalValue(null, this.precision, this.scale) }
    }
    is TypeNumericUnbounded -> {
        { decimalValue(null) }
    }
    is TypeReal -> {
        { float32Value(null) }
    }
    is TypeDoublePrecision -> {
        { float64Value(null) }
    }
    is CharType -> {
        { charValue(null) }
    }
    is CharVarUnboundedType, is CharVarType -> {
        { stringValue(null) }
    }
    is BlobType -> {
        { blobValue(null) }
    }
    // TODO: BINARY?
//    PartiQLValueType.BLOB -> {
//        { blobValue(null) }
//    }
    is ByteType -> {
        { byteValue(null) }
    }
    is ClobType, is ClobUnboundedType -> {
        { clobValue(null) }
    }
    is DateType -> {
        { dateValue(null) }
    }
    is TimeType, is TimeWithTimeZoneType -> {
        { timeValue(null) }
    }
    is TimestampType, is TimestampWithTimeZoneType -> {
        { timestampValue(null) }
    }
    is IntervalType -> {
        { intervalValue(null) }
    }
    is BagType -> {
        { bagValue<PartiQLValue>(null) }
    }
    is ArrayType -> {
        { listValue<PartiQLValue>(null) }
    }
//    PartiQLValueType.SEXP -> {
//        { sexpValue<PartiQLValue>(null) }
//    }
    is TupleType -> {
        { structValue<PartiQLValue>(null) }
    }
    is NullType -> {
        { nullValue() }
    }
    is MissingType -> {
        { missingValue() }
    }
    is BitType, is BitVaryingType -> TODO("Not yet supported")
    is PartiQLType.Runtime.Custom -> TODO("Not yet supported")
}
