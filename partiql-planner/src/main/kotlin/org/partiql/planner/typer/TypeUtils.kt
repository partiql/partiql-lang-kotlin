package org.partiql.planner.typer

import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.BlobType
import org.partiql.types.BoolType
import org.partiql.types.ClobType
import org.partiql.types.DateType
import org.partiql.types.DecimalType
import org.partiql.types.FloatType
import org.partiql.types.GraphType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.NumberConstraint
import org.partiql.types.PartiQLValueType
import org.partiql.types.SexpType
import org.partiql.types.SingleType
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.SymbolType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType
import org.partiql.value.PartiQLValueExperimental

internal fun StaticType.isNullOrMissing(): Boolean = (this is NullType || this is MissingType)

internal fun StaticType.isNumeric(): Boolean = (this is IntType || this is FloatType || this is DecimalType)

internal fun StaticType.isText(): Boolean = (this is SymbolType || this is StringType)

internal fun StaticType.isUnknown(): Boolean = (this.isNullOrMissing() || this == StaticType.NULL_OR_MISSING)

internal fun StaticType.isOptional(): Boolean = when (this) {
    is AnyType, MissingType -> true // Any includes Missing type
    is AnyOfType -> types.any { it.isOptional() }
    else -> false
}

internal fun PartiQLValueType.toStaticType(): StaticType = when (this) {
    PartiQLValueType.BOOL -> StaticType.BOOL
    PartiQLValueType.INT8 -> StaticType.INT2 // TODO
    PartiQLValueType.INT16 -> StaticType.INT2
    PartiQLValueType.INT32 -> StaticType.INT4
    PartiQLValueType.INT64 -> StaticType.INT8
    PartiQLValueType.INT -> StaticType.INT
    PartiQLValueType.DECIMAL -> StaticType.DECIMAL
    PartiQLValueType.FLOAT32 -> StaticType.FLOAT
    PartiQLValueType.FLOAT64 -> StaticType.FLOAT
    PartiQLValueType.CHAR -> char()
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
    PartiQLValueType.GRAPH -> StaticType.GRAPH
    PartiQLValueType.NULL -> StaticType.NULL
    PartiQLValueType.MISSING -> StaticType.MISSING
    PartiQLValueType.NULLABLE_BOOL -> StaticType.unionOf(StaticType.BOOL, StaticType.NULL)
    PartiQLValueType.NULLABLE_INT8 -> StaticType.unionOf(StaticType.INT2, StaticType.NULL)
    PartiQLValueType.NULLABLE_INT16 -> StaticType.unionOf(StaticType.INT2, StaticType.NULL)
    PartiQLValueType.NULLABLE_INT32 -> StaticType.unionOf(StaticType.INT4, StaticType.NULL)
    PartiQLValueType.NULLABLE_INT64 -> StaticType.unionOf(StaticType.INT8, StaticType.NULL)
    PartiQLValueType.NULLABLE_INT -> StaticType.unionOf(StaticType.INT, StaticType.NULL)
    PartiQLValueType.NULLABLE_DECIMAL -> StaticType.unionOf(StaticType.DECIMAL, StaticType.NULL)
    PartiQLValueType.NULLABLE_FLOAT32 -> StaticType.unionOf(StaticType.FLOAT, StaticType.NULL)
    PartiQLValueType.NULLABLE_FLOAT64 -> StaticType.unionOf(StaticType.FLOAT, StaticType.NULL)
    PartiQLValueType.NULLABLE_CHAR -> StaticType.unionOf(char(), StaticType.NULL)
    PartiQLValueType.NULLABLE_STRING -> StaticType.unionOf(StaticType.STRING, StaticType.NULL)
    PartiQLValueType.NULLABLE_SYMBOL -> StaticType.unionOf(StaticType.SYMBOL, StaticType.NULL)
    PartiQLValueType.NULLABLE_BINARY -> TODO()
    PartiQLValueType.NULLABLE_BYTE -> TODO()
    PartiQLValueType.NULLABLE_BLOB -> StaticType.unionOf(StaticType.BLOB, StaticType.NULL)
    PartiQLValueType.NULLABLE_CLOB -> StaticType.unionOf(StaticType.CLOB, StaticType.NULL)
    PartiQLValueType.NULLABLE_DATE -> StaticType.unionOf(StaticType.DATE, StaticType.NULL)
    PartiQLValueType.NULLABLE_TIME -> StaticType.unionOf(StaticType.TIME, StaticType.NULL)
    PartiQLValueType.NULLABLE_TIMESTAMP -> StaticType.unionOf(StaticType.TIMESTAMP, StaticType.NULL)
    PartiQLValueType.NULLABLE_INTERVAL -> TODO()
    PartiQLValueType.NULLABLE_BAG -> StaticType.unionOf(StaticType.BAG, StaticType.NULL)
    PartiQLValueType.NULLABLE_LIST -> StaticType.unionOf(StaticType.LIST, StaticType.NULL)
    PartiQLValueType.NULLABLE_SEXP -> StaticType.unionOf(StaticType.SEXP, StaticType.NULL)
    PartiQLValueType.NULLABLE_STRUCT -> StaticType.unionOf(StaticType.STRUCT, StaticType.NULL)
}

internal fun char(): StaticType {
    val constraint = StringType.StringLengthConstraint.Constrained(NumberConstraint.Equals(1))
    return StringType(constraint)
}

/**
 * TypeUtils is like
 */
@OptIn(PartiQLValueExperimental::class)
internal object TypeUtils {

    @JvmStatic
    public fun getTypeDomain(type: StaticType): Set<PartiQLValueType> = when (type) {
        is AnyType -> enumValues<PartiQLValueType>().toSet()
        is SingleType -> setOf(getRuntimeType(type))
        is AnyOfType -> type.types.flatMap { getTypeDomain(it) }.toSet()
    }

    @JvmStatic
    public fun getRuntimeType(type: SingleType): PartiQLValueType = when (type) {
        is BlobType -> PartiQLValueType.BLOB
        is BoolType -> PartiQLValueType.BOOL
        is ClobType -> PartiQLValueType.CLOB
        is BagType -> PartiQLValueType.BAG
        is ListType -> PartiQLValueType.LIST
        is SexpType -> PartiQLValueType.SEXP
        is DateType -> PartiQLValueType.DATE
        is DecimalType -> PartiQLValueType.DECIMAL
        is FloatType -> PartiQLValueType.FLOAT64
        is IntType -> PartiQLValueType.INT
        MissingType -> PartiQLValueType.MISSING
        is NullType -> PartiQLValueType.NULL
        is StringType -> PartiQLValueType.STRING
        is StructType -> PartiQLValueType.STRUCT
        is SymbolType -> PartiQLValueType.SYMBOL
        is TimeType -> PartiQLValueType.TIME
        is TimestampType -> PartiQLValueType.TIMESTAMP
        is GraphType -> PartiQLValueType.GRAPH
    }
}
