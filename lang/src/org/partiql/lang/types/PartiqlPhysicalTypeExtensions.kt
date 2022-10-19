package org.partiql.lang.types

import org.partiql.lang.domains.PartiqlPhysical

/**
 * Helper to convert [PartiqlPhysical.Type] in AST to a [TypedOpParameter].
 */
internal fun PartiqlPhysical.Type.toTypedOpParameter(customTypedOpParameters: Map<String, TypedOpParameter>): TypedOpParameter = when (this) {
    is PartiqlPhysical.Type.MissingType -> TypedOpParameter(StaticType.MISSING)
    is PartiqlPhysical.Type.NullType -> TypedOpParameter(StaticType.NULL)
    is PartiqlPhysical.Type.BooleanType -> TypedOpParameter(StaticType.BOOL)
    is PartiqlPhysical.Type.SmallintType -> TypedOpParameter(IntType(IntType.IntRangeConstraint.SHORT))
    is PartiqlPhysical.Type.Integer4Type -> TypedOpParameter(IntType(IntType.IntRangeConstraint.INT4))
    is PartiqlPhysical.Type.Integer8Type -> TypedOpParameter(IntType(IntType.IntRangeConstraint.LONG))
    is PartiqlPhysical.Type.IntegerType -> TypedOpParameter(IntType(IntType.IntRangeConstraint.LONG))
    is PartiqlPhysical.Type.FloatType, is PartiqlPhysical.Type.RealType, is PartiqlPhysical.Type.DoublePrecisionType -> TypedOpParameter(StaticType.FLOAT)
    is PartiqlPhysical.Type.DecimalType -> when {
        this.precision == null && this.scale == null -> TypedOpParameter(StaticType.DECIMAL)
        this.precision != null && this.scale == null -> TypedOpParameter(DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(this.precision.value.toInt())))
        else -> TypedOpParameter(
            DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(this.precision!!.value.toInt(), this.scale!!.value.toInt()))
        )
    }
    is PartiqlPhysical.Type.NumericType -> when {
        this.precision == null && this.scale == null -> TypedOpParameter(StaticType.DECIMAL)
        this.precision != null && this.scale == null -> TypedOpParameter(DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(this.precision.value.toInt())))
        else -> TypedOpParameter(
            DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(this.precision!!.value.toInt(), this.scale!!.value.toInt()))
        )
    }
    is PartiqlPhysical.Type.TimestampType -> TypedOpParameter(StaticType.TIMESTAMP)
    is PartiqlPhysical.Type.CharacterType -> when {
        this.length == null -> TypedOpParameter(StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.Equals(1))))
        else -> TypedOpParameter(
            StringType(
                StringType.StringLengthConstraint.Constrained(
                    NumberConstraint.Equals(this.length.value.toInt())
                )
            )
        )
    }
    is PartiqlPhysical.Type.CharacterVaryingType -> when (this.length) {
        null -> TypedOpParameter(StringType(StringType.StringLengthConstraint.Unconstrained))
        else -> TypedOpParameter(StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.UpTo(this.length.value.toInt()))))
    }
    is PartiqlPhysical.Type.StringType -> TypedOpParameter(StaticType.STRING)
    is PartiqlPhysical.Type.SymbolType -> TypedOpParameter(StaticType.SYMBOL)
    is PartiqlPhysical.Type.ClobType -> TypedOpParameter(StaticType.CLOB)
    is PartiqlPhysical.Type.BlobType -> TypedOpParameter(StaticType.BLOB)
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
    is PartiqlPhysical.Type.DateType -> TypedOpParameter(StaticType.DATE)
    is PartiqlPhysical.Type.TimeType -> TypedOpParameter(
        TimeType(this.precision?.value?.toInt(), withTimeZone = false)
    )
    is PartiqlPhysical.Type.TimeWithTimeZoneType -> TypedOpParameter(
        TimeType(this.precision?.value?.toInt(), withTimeZone = true)
    )
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
