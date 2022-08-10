package org.partiql.lang.types

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.ots.plugins.standard.types.CompileTimeCharType
import org.partiql.lang.ots.plugins.standard.types.CompileTimeTimeType
import org.partiql.lang.ots.plugins.standard.types.CompileTimeVarcharType
import org.partiql.lang.ots.plugins.standard.types.DecimalType
import org.partiql.lang.ots.plugins.standard.types.TimeType
import org.partiql.lang.util.BuiltInScalarTypeId

/**
 * Helper to convert [PartiqlPhysical.Type] in AST to a [TypedOpParameter].
 */
fun PartiqlPhysical.Type.toTypedOpParameter(customTypedOpParameters: Map<String, TypedOpParameter>): TypedOpParameter = when (this) {
    is PartiqlPhysical.Type.MissingType -> TypedOpParameter(StaticType.MISSING)
    is PartiqlPhysical.Type.NullType -> TypedOpParameter(StaticType.NULL)
    is PartiqlPhysical.Type.ScalarType -> when (id.text) {
        BuiltInScalarTypeId.BOOLEAN -> TypedOpParameter(StaticType.BOOL)
        BuiltInScalarTypeId.SMALLINT -> TypedOpParameter(StaticType.INT2)
        BuiltInScalarTypeId.INTEGER4 -> TypedOpParameter(StaticType.INT4)
        BuiltInScalarTypeId.INTEGER8 -> TypedOpParameter(StaticType.INT8)
        BuiltInScalarTypeId.INTEGER -> TypedOpParameter(StaticType.INT)
        BuiltInScalarTypeId.FLOAT,
        BuiltInScalarTypeId.REAL,
        BuiltInScalarTypeId.DOUBLE_PRECISION -> TypedOpParameter(StaticType.FLOAT)
        BuiltInScalarTypeId.DECIMAL,
        BuiltInScalarTypeId.NUMERIC -> when (parameters.size) {
            0 -> TypedOpParameter(StaticType.DECIMAL)
            1,
            2 -> TypedOpParameter(
                StaticScalarType(
                    DecimalType.createType(parameters.map { it.value.toInt() })
                )
            )
            else -> error("Internal Error: DECIMAL type must have at most 2 parameters during compiling")
        }
        BuiltInScalarTypeId.TIMESTAMP -> TypedOpParameter(StaticType.TIMESTAMP)
        BuiltInScalarTypeId.CHARACTER -> when (parameters.size) {
            0 -> TypedOpParameter(StaticScalarType(CompileTimeCharType(1))) // TODO: See if we need to use unconstrained string instead
            1 -> TypedOpParameter(StaticScalarType(CompileTimeCharType(parameters[0].value.toInt())))
            else -> error("Internal Error: CHARACTER type must have 1 parameters during compiling")
        }
        BuiltInScalarTypeId.CHARACTER_VARYING -> when (parameters.size) {
            0 -> TypedOpParameter(StaticType.STRING)
            1 -> TypedOpParameter(StaticScalarType(CompileTimeVarcharType(parameters[0].value.toInt())))
            else -> error("Internal Error: CHARACTER_VARYING type must have 1 parameters during compiling")
        }
        BuiltInScalarTypeId.STRING -> TypedOpParameter(StaticType.STRING)
        BuiltInScalarTypeId.SYMBOL -> TypedOpParameter(StaticType.SYMBOL)
        BuiltInScalarTypeId.CLOB -> TypedOpParameter(StaticType.CLOB)
        BuiltInScalarTypeId.BLOB -> TypedOpParameter(StaticType.BLOB)
        BuiltInScalarTypeId.DATE -> TypedOpParameter(StaticType.DATE)
        BuiltInScalarTypeId.TIME -> when (parameters.size) {
            0 -> TypedOpParameter(StaticScalarType(TimeType.createType(emptyList())))
            1 -> TypedOpParameter(StaticScalarType(TimeType.createType(parameters.map { it.value.toInt() })))
            else -> error("\"Internal Error: TIME type must have at most 1 parameters during compiling\"")
        }
        BuiltInScalarTypeId.TIME_WITH_TIME_ZONE -> when (parameters.size) {
            0 -> TypedOpParameter(
                StaticScalarType(
                    CompileTimeTimeType(
                        withTimeZone = true
                    )
                )
            )
            1 -> TypedOpParameter(
                StaticScalarType(
                    CompileTimeTimeType(
                        precision = parameters[0].value.toInt(),
                        withTimeZone = true
                    )
                )
            )
            else -> error("Internal Error: TIME_WITH_TIME_ZONE type must have 1 parameters during compiling")
        }
        else -> error("Unrecognized scalar type ID")
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
