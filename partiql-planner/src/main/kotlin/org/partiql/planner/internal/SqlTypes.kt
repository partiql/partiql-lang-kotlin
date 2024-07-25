package org.partiql.planner.internal

import org.partiql.ast.Type
import org.partiql.types.Field
import org.partiql.types.PType

/**
 * A factory for single-source of truth for type creations — DO NOT CREATE PTYPE DIRECTLY.
 *
 * This allows us to raise an interface if we need custom type factories; for now just use defaults with static methods.
 */
internal object SqlTypes {

    private const val MAX_SIZE = Int.MAX_VALUE

    //
    // DYNAMIC
    //

    @JvmStatic
    fun dynamic(): PType = PType.typeDynamic()

    //
    // BOOLEAN
    //

    @JvmStatic
    fun bool(): PType = PType.typeBool()

    //
    // NUMERIC
    //

    @JvmStatic
    fun tinyint(): PType = PType.typeTinyInt()

    @JvmStatic
    fun smallint(): PType = PType.typeSmallInt()

    @JvmStatic
    fun int(): PType = PType.typeInt()

    @JvmStatic
    fun bigint(): PType = PType.typeBigInt()

    /**
     * NUMERIC represents an integer with arbitrary precision. It is equivalent to Ion’s integer type, and is conformant to SQL-99s rules for the NUMERIC type. In SQL-99, if a scale is omitted then we choose zero — and if a precision is omitted then the precision is implementation defined. For PartiQL, we define this precision to be inf — aka arbitrary precision.
     *
     * @param precision     Defaults to inf.
     * @param scale         Defaults to 0.
     * @return
     */
    @JvmStatic
    fun numeric(precision: Int? = null, scale: Int? = null): PType {
        if (scale != null && precision == null) {
            error("Precision can never be null while scale is specified.")
        }
        return when {
            precision != null && scale != null -> PType.typeDecimal(precision, scale)
            precision != null -> PType.typeDecimal(precision, 0)
            else -> PType.typeIntArbitrary()
        }
    }

    /**
     * DECIMAL represents an exact numeric type with arbitrary precision and arbitrary scale. It is equivalent to Ion’s decimal type. For a DECIMAL with no given scale we choose inf rather than the SQL prescribed 0 (zero). Here we diverge from SQL-99 for Ion compatibility. Finally, SQL defines decimals as having precision equal to or greater than the given precision. Like other systems, we truncate extraneous precision so that NUMERIC(p,s) is equivalent to DECIMAL(p,s). The only difference between them is the default scale when it’s not specified — we follow SQL-99 for NUMERIC, and we follow Postgres for DECIMAL.
     *
     * @param precision     Defaults to inf.
     * @param scale         Defaults to 0 when precision is given, otherwise inf.
     * @return
     */
    @JvmStatic
    fun decimal(precision: Int? = null, scale: Int? = null): PType {
        if (scale != null && precision == null) {
            error("Precision can never be null while scale is specified.")
        }
        return when {
            precision != null && scale != null -> PType.typeDecimal(precision, scale)
            precision != null -> PType.typeDecimal(precision, 0)
            else -> PType.typeDecimalArbitrary()
        }
    }

    @JvmStatic
    fun real(): PType = PType.typeReal()

    @JvmStatic
    fun double(): PType = PType.typeDoublePrecision()

    //
    // CHARACTER STRINGS
    //

    @JvmStatic
    fun char(length: Int? = null): PType = PType.typeChar(length ?: 1)

    @JvmStatic
    fun varchar(length: Int? = null): PType = PType.typeVarChar(length ?: MAX_SIZE)

    @JvmStatic
    fun string(): PType = PType.typeString()

    @JvmStatic
    fun clob(length: Int? = null) = PType.typeClob(length ?: MAX_SIZE)

    //
    // BIT STRINGS
    //

    @JvmStatic
    fun blob(length: Int? = null) = PType.typeBlob(length ?: MAX_SIZE)

    //
    // DATETIME
    //

    @JvmStatic
    fun date(): PType = PType.typeDate()

    @JvmStatic
    fun time(precision: Int? = null): PType = PType.typeTimeWithoutTZ(precision ?: 6)

    @JvmStatic
    fun timez(precision: Int? = null): PType = PType.typeTimeWithTZ(precision ?: 6)

    @JvmStatic
    fun timestamp(precision: Int? = null): PType = PType.typeTimeWithoutTZ(precision ?: 6)

    @JvmStatic
    fun timestampz(precision: Int? = null): PType = PType.typeTimestampWithTZ(precision ?: 6)

    //
    // COLLECTIONS
    //

    @JvmStatic
    fun array(element: PType? = null, size: Int? = null): PType {
        if (size != null) {
            error("Fixed-length ARRAY [N] is not supported.")
        }
        return when (element) {
            null -> PType.typeList()
            else -> PType.typeList(element)
        }
    }

    @JvmStatic
    fun bag(element: PType? = null, size: Int? = null): PType {
        if (size != null) {
            error("Fixed-length BAG [N] is not supported.")
        }
        return when (element) {
            null -> PType.typeBag()
            else -> PType.typeBag(element)
        }
    }

    //
    // STRUCTURAL
    //

    @JvmStatic
    fun struct(): PType = PType.typeStruct()

    @JvmStatic
    fun row(fields: List<Field>): PType = PType.typeRow(fields)

    /**
     * Create PType from the AST type.
     */
    @JvmStatic
    fun from(type: Type): PType = when (type) {
        is Type.NullType -> error("Casting to NULL is not supported.")
        is Type.Missing -> error("Casting to MISSING is not supported.")
        is Type.Bool -> bool()
        is Type.Tinyint -> tinyint()
        is Type.Smallint, is Type.Int2 -> smallint()
        is Type.Int4, is Type.Int -> int()
        is Type.Bigint, is Type.Int8 -> bigint()
        is Type.Numeric -> numeric(type.precision, type.scale)
        is Type.Decimal -> decimal(type.precision, type.scale)
        is Type.Real -> real()
        is Type.Float32 -> real()
        is Type.Float64 -> double()
        is Type.Char -> char(type.length)
        is Type.Varchar -> varchar(type.length)
        is Type.String -> string()
        is Type.Symbol -> {
            // TODO will we continue supporting symbol?
            PType.typeSymbol()
        }
        is Type.Bit -> error("BIT is not supported yet.")
        is Type.BitVarying -> error("BIT VARYING is not supported yet.")
        is Type.ByteString -> error("BINARY is not supported yet.")
        is Type.Blob -> blob(type.length)
        is Type.Clob -> clob(type.length)
        is Type.Date -> date()
        is Type.Time -> time(type.precision)
        is Type.TimeWithTz -> timez(type.precision)
        is Type.Timestamp -> timestamp(type.precision)
        is Type.TimestampWithTz -> timestampz(type.precision)
        is Type.Interval -> error("INTERVAL is not supported yet.")
        is Type.Bag -> bag()
        is Type.Sexp -> {
            // TODO will we continue supporting s-expression?
            PType.typeSexp()
        }
        is Type.Any -> dynamic()
        is Type.List -> array()
        is Type.Array -> array(type.type?.let { from(it) })
        is Type.Tuple -> struct()
        is Type.Struct -> struct()
        is Type.Custom -> TODO("Custom type not supported ")
    }
}
