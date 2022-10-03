package org.partiql.lang.types

import OTS.ITF.org.partiql.ots.type.ScalarType
import org.partiql.lang.domains.PartiqlPhysical

/**
 * Helper to convert [PartiqlPhysical.Type] in AST to a [TypedOpParameter].
 */
fun PartiqlPhysical.Type.toTypedOpParameter(customTypedOpParameters: Map<String, TypedOpParameter>, aliasToScalarType: Map<String, ScalarType>): TypedOpParameter = when (this) {
    is PartiqlPhysical.Type.MissingType -> TypedOpParameter(StaticType.MISSING)
    is PartiqlPhysical.Type.NullType -> TypedOpParameter(StaticType.NULL)
    is PartiqlPhysical.Type.ScalarType -> TypedOpParameter(
        StaticScalarType(
            aliasToScalarType[alias.text] ?: error("No such type alias: ${alias.text}"),
            parameters.map { it.value.toInt() }
        )
    )
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
