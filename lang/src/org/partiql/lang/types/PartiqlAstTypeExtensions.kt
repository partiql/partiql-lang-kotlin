package org.partiql.lang.types

import org.partiql.lang.domains.PartiqlAst

/**
 * Helper to convert [PartiqlAst.Type] in AST to a [TypedOpParameter].
 */
fun PartiqlAst.Type.toTypedOpParameter(customTypedOpParameters: Map<String, TypedOpParameter>): TypedOpParameter = when (this) {
    is PartiqlAst.Type.MissingType -> TypedOpParameter(StaticType.MISSING)
    is PartiqlAst.Type.NullType -> TypedOpParameter(StaticType.NULL)
    is PartiqlAst.Type.BooleanType -> TypedOpParameter(StaticType.BOOL)
    is PartiqlAst.Type.SmallintType -> TypedOpParameter(IntType(IntType.IntRangeConstraint.SHORT))
    is PartiqlAst.Type.Integer4Type -> TypedOpParameter(IntType(IntType.IntRangeConstraint.INT4))
    is PartiqlAst.Type.Integer8Type -> TypedOpParameter(IntType(IntType.IntRangeConstraint.LONG))
    is PartiqlAst.Type.IntegerType -> TypedOpParameter(IntType(IntType.IntRangeConstraint.LONG))
    is PartiqlAst.Type.FloatType, is PartiqlAst.Type.RealType, is PartiqlAst.Type.DoublePrecisionType -> TypedOpParameter(StaticType.FLOAT)
    is PartiqlAst.Type.DecimalType -> when {
        this.precision == null && this.scale == null -> TypedOpParameter(StaticType.DECIMAL)
        this.precision != null && this.scale == null -> TypedOpParameter(DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(this.precision.value.toInt())))
        else -> TypedOpParameter(
            DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(this.precision!!.value.toInt(), this.scale!!.value.toInt()))
        )
    }
    is PartiqlAst.Type.NumericType -> when {
        this.precision == null && this.scale == null -> TypedOpParameter(StaticType.DECIMAL)
        this.precision != null && this.scale == null -> TypedOpParameter(DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(this.precision.value.toInt())))
        else -> TypedOpParameter(
            DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(this.precision!!.value.toInt(), this.scale!!.value.toInt()))
        )
    }
    is PartiqlAst.Type.TimestampType -> TypedOpParameter(StaticType.TIMESTAMP)
    is PartiqlAst.Type.CharacterType -> when {
        this.length == null -> TypedOpParameter(StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.Equals(1))))
        else -> TypedOpParameter(
            StringType(
                StringType.StringLengthConstraint.Constrained(
                    NumberConstraint.Equals(this.length.value.toInt())
                )
            )
        )
    }
    is PartiqlAst.Type.CharacterVaryingType -> when (this.length) {
        null -> TypedOpParameter(StringType(StringType.StringLengthConstraint.Unconstrained))
        else -> TypedOpParameter(StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.UpTo(this.length.value.toInt()))))
    }
    is PartiqlAst.Type.StringType -> TypedOpParameter(StaticType.STRING)
    is PartiqlAst.Type.SymbolType -> TypedOpParameter(StaticType.SYMBOL)
    is PartiqlAst.Type.ClobType -> TypedOpParameter(StaticType.CLOB)
    is PartiqlAst.Type.BlobType -> TypedOpParameter(StaticType.BLOB)
    is PartiqlAst.Type.StructType -> TypedOpParameter(StaticType.STRUCT)
    is PartiqlAst.Type.TupleType -> TypedOpParameter(StaticType.STRUCT)
    is PartiqlAst.Type.ListType -> TypedOpParameter(StaticType.LIST)
    is PartiqlAst.Type.SexpType -> TypedOpParameter(StaticType.SEXP)
    is PartiqlAst.Type.BagType -> TypedOpParameter(StaticType.BAG)
    is PartiqlAst.Type.AnyType -> TypedOpParameter(StaticType.ANY)
    is PartiqlAst.Type.CustomType ->
        customTypedOpParameters.mapKeys {
            (k, _) ->
            k.toLowerCase()
        }[this.name.text.toLowerCase()] ?: error("Could not find parameter for $this")
    is PartiqlAst.Type.DateType -> TypedOpParameter(StaticType.DATE)
    is PartiqlAst.Type.TimeType -> TypedOpParameter(
        TimeType(this.precision?.value?.toInt(), withTimeZone = false)
    )
    is PartiqlAst.Type.TimeWithTimeZoneType -> TypedOpParameter(
        TimeType(this.precision?.value?.toInt(), withTimeZone = true)
    )
    is PartiqlAst.Type.EsAny,
    is PartiqlAst.Type.EsBoolean,
    is PartiqlAst.Type.EsFloat,
    is PartiqlAst.Type.EsInteger,
    is PartiqlAst.Type.EsText,
    is PartiqlAst.Type.RsBigint,
    is PartiqlAst.Type.RsBoolean,
    is PartiqlAst.Type.RsDoublePrecision,
    is PartiqlAst.Type.RsInteger,
    is PartiqlAst.Type.RsReal,
    is PartiqlAst.Type.RsVarcharMax,
    is PartiqlAst.Type.SparkBoolean,
    is PartiqlAst.Type.SparkDouble,
    is PartiqlAst.Type.SparkFloat,
    is PartiqlAst.Type.SparkInteger,
    is PartiqlAst.Type.SparkLong,
    is PartiqlAst.Type.SparkShort -> error("$this node should not be present in PartiQLAST. Consider transforming the AST using CustomTypeVisitorTransform.")
}
