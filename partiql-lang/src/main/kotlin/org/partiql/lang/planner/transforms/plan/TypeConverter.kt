package org.partiql.lang.planner.transforms.plan

import com.amazon.ion.IonType
import org.partiql.lang.domains.PartiqlAst
import org.partiql.types.DecimalType
import org.partiql.types.NumberConstraint
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.TimeType

/**
 * Converts a [PartiqlAst.Type] to [StaticType]
 *
 * Similar PIG workarounds to RexConverter; we could do a massive if-else like PartiqlPhysicalTypeExtensions.
 * but VisitorFold already has the big if-else.
 *
 * StaticType could use some helper functions.
 * The conversions (afaik)
 *  - PartiqlAst.Type to StaticType
 *  - IonType to StaticType
 * both require converting to ExprValueType then StaticType, or converting to Ion Sexp then parsing as PartiQL physical
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
internal object TypeConverter : PartiqlAst.VisitorFold<StaticType>() {

    // Something to appease the input accumulator, we only want the return value
    @Suppress("ObjectPropertyName")
    private val _ignore = StaticType.NULL

    fun convert(type: PartiqlAst.Type): StaticType = TypeConverter.walkType(type, _ignore)

    fun convert(type: IonType): StaticType = when (type) {
        IonType.NULL -> StaticType.NULL
        IonType.BOOL -> StaticType.BOOL
        IonType.INT -> StaticType.INT
        IonType.FLOAT -> StaticType.FLOAT
        IonType.DECIMAL -> StaticType.DECIMAL
        IonType.TIMESTAMP -> StaticType.TIMESTAMP
        IonType.SYMBOL -> StaticType.SYMBOL
        IonType.STRING -> StaticType.STRING
        IonType.CLOB -> StaticType.CLOB
        IonType.BLOB -> StaticType.BLOB
        IonType.LIST -> StaticType.LIST
        IonType.SEXP -> StaticType.SEXP
        IonType.STRUCT -> StaticType.STRUCT
        // datagram
        else -> error("unexpected Ion type $type")
    }

    override fun walkTypeNullType(node: PartiqlAst.Type.NullType, _ignore: StaticType) = StaticType.NULL

    override fun walkTypeBooleanType(node: PartiqlAst.Type.BooleanType, _ignore: StaticType) = StaticType.BOOL

    override fun walkTypeSmallintType(node: PartiqlAst.Type.SmallintType, _ignore: StaticType) = StaticType.INT2

    override fun walkTypeInteger4Type(node: PartiqlAst.Type.Integer4Type, _ignore: StaticType) = StaticType.INT4

    override fun walkTypeInteger8Type(node: PartiqlAst.Type.Integer8Type, _ignore: StaticType) = StaticType.INT8

    override fun walkTypeIntegerType(node: PartiqlAst.Type.IntegerType, _ignore: StaticType) = StaticType.INT

    override fun walkTypeFloatType(node: PartiqlAst.Type.FloatType, _ignore: StaticType) = StaticType.FLOAT

    override fun walkTypeRealType(node: PartiqlAst.Type.RealType, _ignore: StaticType) = StaticType.FLOAT

    override fun walkTypeDoublePrecisionType(
        node: PartiqlAst.Type.DoublePrecisionType,
        _ignore: StaticType
    ) = StaticType.FLOAT

    override fun walkTypeDecimalType(node: PartiqlAst.Type.DecimalType, _ignore: StaticType) = when {
        node.precision == null && node.scale == null -> StaticType.DECIMAL
        else -> DecimalType(
            DecimalType.PrecisionScaleConstraint.Constrained(
                precision = node.precision!!.value.toInt(),
                scale = node.scale?.value?.toInt() ?: 0
            )
        )
    }

    override fun walkTypeNumericType(node: PartiqlAst.Type.NumericType, _ignore: StaticType) = when {
        node.precision == null && node.scale == null -> StaticType.DECIMAL
        else -> DecimalType(
            DecimalType.PrecisionScaleConstraint.Constrained(
                precision = node.precision!!.value.toInt(),
                scale = node.scale?.value?.toInt() ?: 0
            )
        )
    }

    override fun walkTypeTimestampType(node: PartiqlAst.Type.TimestampType, _ignore: StaticType) = StaticType.TIMESTAMP

    override fun walkTypeCharacterType(node: PartiqlAst.Type.CharacterType, _ignore: StaticType) = StringType(
        StringType.StringLengthConstraint.Constrained(
            NumberConstraint.Equals(node.length?.value?.toInt() ?: 1)
        )
    )

    override fun walkTypeCharacterVaryingType(node: PartiqlAst.Type.CharacterVaryingType, _ignore: StaticType) = when (node.length) {
        null -> StringType(StringType.StringLengthConstraint.Unconstrained)
        else -> StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.UpTo(node.length.value.toInt())))
    }

    override fun walkTypeMissingType(node: PartiqlAst.Type.MissingType, _ignore: StaticType) = StaticType.MISSING

    override fun walkTypeStringType(node: PartiqlAst.Type.StringType, _ignore: StaticType) = StaticType.STRING

    override fun walkTypeSymbolType(node: PartiqlAst.Type.SymbolType, _ignore: StaticType) = StaticType.SYMBOL

    override fun walkTypeBlobType(node: PartiqlAst.Type.BlobType, _ignore: StaticType) = StaticType.BLOB

    override fun walkTypeClobType(node: PartiqlAst.Type.ClobType, _ignore: StaticType) = StaticType.CLOB

    override fun walkTypeDateType(node: PartiqlAst.Type.DateType, _ignore: StaticType) = StaticType.DATE

    override fun walkTypeTimeType(node: PartiqlAst.Type.TimeType, _ignore: StaticType) = TimeType(node.precision?.value?.toInt(), withTimeZone = false)

    override fun walkTypeTimeWithTimeZoneType(node: PartiqlAst.Type.TimeWithTimeZoneType, _ignore: StaticType) = TimeType(node.precision?.value?.toInt(), withTimeZone = false)

    override fun walkTypeStructType(node: PartiqlAst.Type.StructType, _ignore: StaticType) = StaticType.STRUCT

    override fun walkTypeTupleType(node: PartiqlAst.Type.TupleType, _ignore: StaticType) = StaticType.STRUCT

    override fun walkTypeListType(node: PartiqlAst.Type.ListType, _ignore: StaticType) = StaticType.LIST

    override fun walkTypeSexpType(node: PartiqlAst.Type.SexpType, _ignore: StaticType) = StaticType.SEXP

    override fun walkTypeBagType(node: PartiqlAst.Type.BagType, _ignore: StaticType) = StaticType.BAG

    override fun walkTypeAnyType(node: PartiqlAst.Type.AnyType, _ignore: StaticType) = StaticType.ANY

    override fun walkTypeCustomType(node: PartiqlAst.Type.CustomType, _ignore: StaticType): StaticType {
        error("custom type not supported in current representation")
    }
}
