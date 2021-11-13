package org.partiql.lang.types

import org.partiql.lang.ast.sourceLocation
import org.partiql.lang.domains.PartiqlAst

/**
 * Helper to convert [PartiqlAst.Type] in AST to a [StaticType].
 */
internal fun PartiqlAst.Type.toStaticType(customTypeFunctions: Map<String, CustomTypeFunction>): StaticType = when (this) {
    is PartiqlAst.Type.MissingType -> StaticType.MISSING
    is PartiqlAst.Type.NullType -> StaticType.NULL
    is PartiqlAst.Type.BooleanType -> StaticType.BOOL
    is PartiqlAst.Type.SmallintType -> IntType(IntType.IntRangeConstraint.SHORT)
    is PartiqlAst.Type.Integer4Type -> IntType(IntType.IntRangeConstraint.INT4)
    is PartiqlAst.Type.Integer8Type -> IntType(IntType.IntRangeConstraint.LONG)
    is PartiqlAst.Type.IntegerType -> IntType(IntType.IntRangeConstraint.LONG)
    is PartiqlAst.Type.FloatType, is PartiqlAst.Type.RealType, is PartiqlAst.Type.DoublePrecisionType -> StaticType.FLOAT
    is PartiqlAst.Type.DecimalType -> when {
        this.precision == null && this.scale == null -> StaticType.DECIMAL
        this.precision != null && this.scale == null -> DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(this.precision.value.toInt()))
        else -> DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(this.precision!!.value.toInt(), this.scale!!.value.toInt()))
    }
    is PartiqlAst.Type.NumericType -> when {
        this.precision == null && this.scale == null -> StaticType.DECIMAL
        this.precision != null && this.scale == null -> DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(this.precision.value.toInt()))
        else -> DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(this.precision!!.value.toInt(), this.scale!!.value.toInt()))
    }
    is PartiqlAst.Type.TimestampType -> StaticType.TIMESTAMP
    is PartiqlAst.Type.CharacterType -> when {
        this.length == null -> StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.Equals(1)))
        else -> StringType(StringType.StringLengthConstraint.Constrained(
            NumberConstraint.Equals(this.length.value.toInt())))
    }
    is PartiqlAst.Type.CharacterVaryingType -> when (this.length) {
        null -> StringType(StringType.StringLengthConstraint.Unconstrained)
        else -> StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.UpTo(this.length.value.toInt())))
    }
    is PartiqlAst.Type.StringType -> StaticType.STRING
    is PartiqlAst.Type.SymbolType -> StaticType.SYMBOL
    is PartiqlAst.Type.ClobType -> StaticType.CLOB
    is PartiqlAst.Type.BlobType -> StaticType.BLOB
    is PartiqlAst.Type.StructType -> StaticType.STRUCT
    is PartiqlAst.Type.TupleType -> StaticType.STRUCT
    is PartiqlAst.Type.ListType -> StaticType.LIST
    is PartiqlAst.Type.SexpType -> StaticType.SEXP
    is PartiqlAst.Type.BagType -> StaticType.BAG
    is PartiqlAst.Type.AnyType -> StaticType.ANY
    is PartiqlAst.Type.CustomType -> {
        // Case-insensitive lookup
        val args = this.args.map { TypeParameter.IntParameter(it.value.toInt(), metas.sourceLocation) }
        customTypeFunctions.mapKeys { (k,_) -> k.toLowerCase() }[this.name.text.toLowerCase()]?.constructStaticType(args)
            ?: error("Could not find parameter for $this")
    }
    is PartiqlAst.Type.DateType -> StaticType.DATE
    is PartiqlAst.Type.TimeType -> TimeType(this.precision?.value?.toInt(), withTimeZone = false)
    is PartiqlAst.Type.TimeWithTimeZoneType -> TimeType(this.precision?.value?.toInt(), withTimeZone = true)
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
