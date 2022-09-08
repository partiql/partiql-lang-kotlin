package org.partiql.lang.types

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.ots.plugins.standard.types.CharType
import org.partiql.lang.ots.plugins.standard.types.DecimalType
import org.partiql.lang.ots.plugins.standard.types.TimeType
import org.partiql.lang.ots.plugins.standard.types.VarcharType

/**
 * Helper to convert [PartiqlPhysical.Type] in AST to a [TypedOpParameter].
 */
fun PartiqlPhysical.Type.toTypedOpParameter(customTypedOpParameters: Map<String, TypedOpParameter>): TypedOpParameter = when (this) {
    is PartiqlPhysical.Type.MissingType -> TypedOpParameter(StaticType.MISSING)
    is PartiqlPhysical.Type.NullType -> TypedOpParameter(StaticType.NULL)
    is PartiqlPhysical.Type.ScalarType -> when (TYPE_ALIAS_TO_SCALAR_TYPE[alias.text]) {
        BuiltInScalarType.BOOLEAN -> TypedOpParameter(StaticType.BOOL)
        BuiltInScalarType.SMALLINT -> TypedOpParameter(StaticType.INT2)
        BuiltInScalarType.INTEGER4 -> TypedOpParameter(StaticType.INT4)
        BuiltInScalarType.INTEGER8 -> TypedOpParameter(StaticType.INT8)
        BuiltInScalarType.INTEGER -> TypedOpParameter(StaticType.INT)
        BuiltInScalarType.FLOAT,
        BuiltInScalarType.REAL,
        BuiltInScalarType.DOUBLE_PRECISION -> TypedOpParameter(StaticType.FLOAT)
        BuiltInScalarType.DECIMAL,
        BuiltInScalarType.NUMERIC -> when (parameters.size) {
            0 -> TypedOpParameter(StaticType.DECIMAL)
            1 -> TypedOpParameter(StaticScalarType(DecimalType, listOf(parameters.first().value.toInt(), 0)))
            2 -> TypedOpParameter(
                StaticScalarType(
                    DecimalType,
                    parameters.map { it.value.toInt() }
                )
            )
            else -> error("Internal Error: DECIMAL type must have at most 2 parameters during compiling")
        }
        BuiltInScalarType.TIMESTAMP -> TypedOpParameter(StaticType.TIMESTAMP)
        BuiltInScalarType.CHARACTER -> when (parameters.size) {
            0 -> TypedOpParameter(StaticScalarType(CharType, listOf(1))) // TODO: See if we need to use unconstrained string instead
            1 -> TypedOpParameter(StaticScalarType(CharType, listOf(parameters[0].value.toInt())))
            else -> error("Internal Error: CHARACTER type must have 1 parameters during compiling")
        }
        BuiltInScalarType.CHARACTER_VARYING -> when (parameters.size) {
            0 -> TypedOpParameter(StaticType.STRING)
            1 -> TypedOpParameter(StaticScalarType(VarcharType, listOf(parameters[0].value.toInt())))
            else -> error("Internal Error: CHARACTER_VARYING type must have 1 parameters during compiling")
        }
        BuiltInScalarType.STRING -> TypedOpParameter(StaticType.STRING)
        BuiltInScalarType.SYMBOL -> TypedOpParameter(StaticType.SYMBOL)
        BuiltInScalarType.CLOB -> TypedOpParameter(StaticType.CLOB)
        BuiltInScalarType.BLOB -> TypedOpParameter(StaticType.BLOB)
        BuiltInScalarType.DATE -> TypedOpParameter(StaticType.DATE)
        BuiltInScalarType.TIME -> when (parameters.size) {
            0 -> TypedOpParameter(StaticScalarType(TimeType(), listOf(null)))
            1 -> TypedOpParameter(StaticScalarType(TimeType(), parameters.map { it.value.toInt() }))
            else -> error("\"Internal Error: TIME type must have at most 1 parameters during compiling\"")
        }
        BuiltInScalarType.TIME_WITH_TIME_ZONE -> when (parameters.size) {
            0 -> TypedOpParameter(
                StaticScalarType(
                    TimeType(true),
                    listOf(null)
                )
            )
            1 -> TypedOpParameter(
                StaticScalarType(
                    TimeType(true),
                    listOf(parameters[0].value.toInt())
                )
            )
            else -> error("Internal Error: TIME_WITH_TIME_ZONE type must have 1 parameters during compiling")
        }
        else -> customTypedOpParameters.mapKeys { (k, _) -> k.toLowerCase() }[alias.text.toLowerCase()] ?: error("No such scalar type: ${alias.text.toLowerCase()}")
    }
    is PartiqlPhysical.Type.StructType -> TypedOpParameter(StaticType.STRUCT)
    is PartiqlPhysical.Type.TupleType -> TypedOpParameter(StaticType.STRUCT)
    is PartiqlPhysical.Type.ListType -> TypedOpParameter(StaticType.LIST)
    is PartiqlPhysical.Type.SexpType -> TypedOpParameter(StaticType.SEXP)
    is PartiqlPhysical.Type.BagType -> TypedOpParameter(StaticType.BAG)
    is PartiqlPhysical.Type.AnyType -> TypedOpParameter(StaticType.ANY)
    // TODO: consider using a more proper way to model non-scalar custom type
    is PartiqlPhysical.Type.EsAnyType -> customTypedOpParameters.mapKeys { (k, _) -> k.toLowerCase() }["es_any"] ?: error("`es_amy` is not injected as a custom type")
}
