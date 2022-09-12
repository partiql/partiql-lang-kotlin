package org.partiql.lang.types

// TODO: remove this file once we use OTS to define aliases & arity for a scalar type

enum class BuiltInScalarType(
    val typeAliases: List<String>,
    val arity: IntRange
) {
    BLOB(listOf("blob"), 0..0),
    BOOLEAN(listOf("bool", "boolean"), 0..0),
    CHARACTER(listOf("char", "character"), 0..1),
    CHARACTER_VARYING(listOf("varchar", "character_varying"), 0..1),
    CLOB(listOf("clob"), 0..0),
    DATE(listOf("date"), 0..0),
    DECIMAL(listOf("dec", "decimal"), 0..2),
    DOUBLE_PRECISION(listOf("double_precision"), 0..0),
    FLOAT(listOf("float"), 0..1),
    INTEGER4(listOf("int4", "integer4"), 0..0),
    INTEGER8(listOf("int8", "bigint", "integer8"), 0..0),
    INTEGER(listOf("int", "integer"), 0..0),
    NUMERIC(listOf("numeric"), 0..2),
    REAL(listOf("real"), 0..0),
    SMALLINT(listOf("int2", "smallint", "integer2"), 0..0),
    STRING(listOf("string"), 0..0),
    SYMBOL(listOf("symbol"), 0..0),
    TIME(listOf("time"), 0..1),
    TIME_WITH_TIME_ZONE(listOf("time_with_time_zone"), 0..1),
    TIMESTAMP(listOf("timestamp"), 0..0);
}

@JvmField internal val TYPE_ALIAS_TO_SCALAR_TYPE = BuiltInScalarType.values().flatMap { scalarType ->
    scalarType.typeAliases.map { typeAlias -> typeAlias to scalarType }
}.associate { it.first to it.second }
