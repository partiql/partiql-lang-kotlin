package org.partiql.lang.types

import OTS.IMP.org.partiql.ots.legacy.types.CharType
import OTS.IMP.org.partiql.ots.legacy.types.DecimalType
import OTS.IMP.org.partiql.ots.legacy.types.TimeType
import OTS.IMP.org.partiql.ots.legacy.types.VarcharType
import org.partiql.lang.domains.PartiqlPhysical

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
            0 -> TypedOpParameter(StaticScalarType(TimeType()))
            1 -> TypedOpParameter(StaticScalarType(TimeType(), parameters.map { it.value.toInt() }))
            else -> error("\"Internal Error: TIME type must have at most 1 parameters during compiling\"")
        }
        BuiltInScalarType.TIME_WITH_TIME_ZONE -> when (parameters.size) {
            0 -> TypedOpParameter(
                StaticScalarType(
                    TimeType(true)
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
        else -> error("Unrecognized scalar type ID: ${alias.text}")
    }
    is PartiqlPhysical.Type.StructType -> TypedOpParameter(StaticType.STRUCT)
    is PartiqlPhysical.Type.TupleType -> TypedOpParameter(StaticType.STRUCT)
    is PartiqlPhysical.Type.ListType -> TypedOpParameter(StaticType.LIST)
    is PartiqlPhysical.Type.SexpType -> TypedOpParameter(StaticType.SEXP)
    is PartiqlPhysical.Type.BagType -> TypedOpParameter(StaticType.BAG)
    is PartiqlPhysical.Type.AnyType -> TypedOpParameter(StaticType.ANY)
    is PartiqlPhysical.Type.CustomType ->
        customTypedOpParameters.mapKeys {
            (k, _) ->
            k.toLowerCase()
        }[this.name.text.toLowerCase()] ?: error("Could not find parameter for $this")
    is PartiqlPhysical.Type.EsAny,
    is PartiqlPhysical.Type.EsBoolean,
    is PartiqlPhysical.Type.EsFloat,
    is PartiqlPhysical.Type.EsInteger,
    is PartiqlPhysical.Type.EsText,
    is PartiqlPhysical.Type.RsBigint,
    is PartiqlPhysical.Type.RsBoolean,
    is PartiqlPhysical.Type.RsDoublePrecision,
    is PartiqlPhysical.Type.RsInteger,
    is PartiqlPhysical.Type.RsReal,
    is PartiqlPhysical.Type.RsVarcharMax,
    is PartiqlPhysical.Type.SparkBoolean,
    is PartiqlPhysical.Type.SparkDouble,
    is PartiqlPhysical.Type.SparkFloat,
    is PartiqlPhysical.Type.SparkInteger,
    is PartiqlPhysical.Type.SparkLong,
    is PartiqlPhysical.Type.SparkShort -> error("$this node should not be present in PartiqlPhysical. Consider transforming the AST using CustomTypeVisitorTransform.")
}
