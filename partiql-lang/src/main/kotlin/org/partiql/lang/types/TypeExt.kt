package org.partiql.lang.types

import org.partiql.lang.domains.PartiqlAst
import org.partiql.types.DecimalType
import org.partiql.types.IntType
import org.partiql.types.NumberConstraint
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.TimeType

/**
 * Helper to convert [PartiqlAst.Type] in AST to a [TypedOpParameter].
 */
internal fun PartiqlAst.Type.toTypedOpParameter(customTypedOpParameters: Map<String, TypedOpParameter>): TypedOpParameter =
    when (this) {
        is PartiqlAst.Type.MissingType -> TypedOpParameter(StaticType.MISSING)
        is PartiqlAst.Type.NullType -> TypedOpParameter(StaticType.NULL)
        is PartiqlAst.Type.BooleanType -> TypedOpParameter(StaticType.BOOL)
        is PartiqlAst.Type.SmallintType -> TypedOpParameter(IntType(IntType.IntRangeConstraint.SHORT))
        is PartiqlAst.Type.Integer4Type -> TypedOpParameter(IntType(IntType.IntRangeConstraint.INT4))
        is PartiqlAst.Type.Integer8Type -> TypedOpParameter(IntType(IntType.IntRangeConstraint.LONG))
        is PartiqlAst.Type.IntegerType -> TypedOpParameter(IntType(IntType.IntRangeConstraint.LONG))
        is PartiqlAst.Type.FloatType, is PartiqlAst.Type.RealType, is PartiqlAst.Type.DoublePrecisionType -> TypedOpParameter(
            StaticType.FLOAT
        )
        is PartiqlAst.Type.DecimalType -> when {
            this.precision == null && this.scale == null -> TypedOpParameter(StaticType.DECIMAL)
            this.precision != null && this.scale == null -> TypedOpParameter(
                DecimalType(
                    DecimalType.PrecisionScaleConstraint.Constrained(
                        this.precision!!.value.toInt()
                    )
                )
            )
            else -> TypedOpParameter(
                DecimalType(
                    DecimalType.PrecisionScaleConstraint.Constrained(
                        this.precision!!.value.toInt(),
                        this.scale!!.value.toInt()
                    )
                )
            )
        }
        is PartiqlAst.Type.NumericType -> when {
            this.precision == null && this.scale == null -> TypedOpParameter(StaticType.DECIMAL)
            this.precision != null && this.scale == null -> TypedOpParameter(
                DecimalType(
                    DecimalType.PrecisionScaleConstraint.Constrained(
                        this.precision!!.value.toInt()
                    )
                )
            )
            else -> TypedOpParameter(
                DecimalType(
                    DecimalType.PrecisionScaleConstraint.Constrained(
                        this.precision!!.value.toInt(),
                        this.scale!!.value.toInt()
                    )
                )
            )
        }
        is PartiqlAst.Type.TimestampType -> TypedOpParameter(StaticType.TIMESTAMP)
        is PartiqlAst.Type.CharacterType -> when {
            this.length == null -> TypedOpParameter(
                StringType(
                    StringType.StringLengthConstraint.Constrained(
                        NumberConstraint.Equals(1)
                    )
                )
            )
            else -> TypedOpParameter(
                StringType(
                    StringType.StringLengthConstraint.Constrained(
                        NumberConstraint.Equals(this.length!!.value.toInt())
                    )
                )
            )
        }
        is PartiqlAst.Type.CharacterVaryingType -> when (val length = this.length) {
            null -> TypedOpParameter(StringType(StringType.StringLengthConstraint.Unconstrained))
            else -> TypedOpParameter(
                StringType(
                    StringType.StringLengthConstraint.Constrained(
                        NumberConstraint.UpTo(
                            length.value.toInt()
                        )
                    )
                )
            )
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
            customTypedOpParameters.mapKeys { (k, _) ->
                k.lowercase()
            }[this.name.text.lowercase()] ?: error("Could not find parameter for $this")
        is PartiqlAst.Type.DateType -> TypedOpParameter(StaticType.DATE)
        is PartiqlAst.Type.TimeType -> TypedOpParameter(
            TimeType(this.precision?.value?.toInt(), withTimeZone = false)
        )
        is PartiqlAst.Type.TimeWithTimeZoneType -> TypedOpParameter(
            TimeType(this.precision?.value?.toInt(), withTimeZone = true)
        )

        is PartiqlAst.Type.TimestampWithTimeZoneType -> TODO()
    }
