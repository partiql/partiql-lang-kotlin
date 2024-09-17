package org.partiql.spi.internal

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
    fun dynamic(): PType = PType.dynamic()

    //
    // BOOLEAN
    //

    @JvmStatic
    fun bool(): PType = PType.bool()

    //
    // NUMERIC
    //

    @JvmStatic
    fun tinyint(): PType = PType.tinyint()

    @JvmStatic
    fun smallint(): PType = PType.smallint()

    @JvmStatic
    fun int(): PType = PType.integer()

    @JvmStatic
    fun bigint(): PType = PType.bigint()

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
            precision != null && scale != null -> PType.decimal(precision, scale)
            precision != null -> PType.decimal(precision, 0)
            else -> PType.numeric()
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
            precision != null && scale != null -> PType.decimal(precision, scale)
            precision != null -> PType.decimal(precision, 0)
            else -> PType.decimal()
        }
    }

    @JvmStatic
    fun real(): PType = PType.real()

    @JvmStatic
    fun double(): PType = PType.doublePrecision()

    //
    // CHARACTER STRINGS
    //

    @JvmStatic
    fun char(length: Int? = null): PType = PType.character(length ?: 1)

    @JvmStatic
    fun varchar(length: Int? = null): PType = PType.varchar(length ?: MAX_SIZE)

    @JvmStatic
    fun string(): PType = PType.string()

    @JvmStatic
    fun clob(length: Int? = null) = PType.clob(length ?: MAX_SIZE)

    //
    // BIT STRINGS
    //

    @JvmStatic
    fun blob(length: Int? = null) = PType.blob(length ?: MAX_SIZE)

    //
    // DATETIME
    //

    @JvmStatic
    fun date(): PType = TODO()

    @JvmStatic
    fun time(precision: Int? = null): PType = PType.time(precision ?: 6)

    @JvmStatic
    fun timez(precision: Int? = null): PType = PType.timez(precision ?: 6)

    @JvmStatic
    fun timestamp(precision: Int? = null): PType = PType.time(precision ?: 6)

    @JvmStatic
    fun timestampz(precision: Int? = null): PType = PType.timestampz(precision ?: 6)

    //
    // COLLECTIONS
    //

    @JvmStatic
    fun array(element: PType? = null, size: Int? = null): PType {
        if (size != null) {
            error("Fixed-length ARRAY [N] is not supported.")
        }
        return when (element) {
            null -> PType.array()
            else -> PType.array(element)
        }
    }

    @JvmStatic
    fun bag(element: PType? = null, size: Int? = null): PType {
        if (size != null) {
            error("Fixed-length BAG [N] is not supported.")
        }
        return when (element) {
            null -> PType.bag()
            else -> PType.bag(element)
        }
    }

    //
    // STRUCTURAL
    //

    @JvmStatic
    fun struct(): PType = PType.struct()

    @JvmStatic
    fun row(fields: List<Field>): PType = PType.row(fields)

    // /**
    //  * Create PType from the AST type.
    //  */
    // @JvmStatic
    // fun from(type: Type): PType = when (type) {
    //     is Type.NullType -> error("Casting to NULL is not supported.")
    //     is Type.Missing -> error("Casting to MISSING is not supported.")
    //     is Type.Bool -> bool()
    //     is Type.Tinyint -> tinyint()
    //     is Type.Smallint, is Type.Int2 -> smallint()
    //     is Type.Int4, is Type.Int -> int()
    //     is Type.Bigint, is Type.Int8 -> bigint()
    //     is Type.Numeric -> numeric(type.precision, type.scale)
    //     is Type.Decimal -> decimal(type.precision, type.scale)
    //     is Type.Real -> real()
    //     is Type.Float32 -> real()
    //     is Type.Float64 -> double()
    //     is Type.Char -> char(type.length)
    //     is Type.Varchar -> varchar(type.length)
    //     is Type.String -> string()
    //     is Type.Symbol -> {
    //         // TODO will we continue supporting symbol?
    //         PType.typeSymbol()
    //     }
    //     is Type.Bit -> error("BIT is not supported yet.")
    //     is Type.BitVarying -> error("BIT VARYING is not supported yet.")
    //     is Type.ByteString -> error("BINARY is not supported yet.")
    //     is Type.Blob -> blob(type.length)
    //     is Type.Clob -> clob(type.length)
    //     is Type.Date -> date()
    //     is Type.Time -> time(type.precision)
    //     is Type.TimeWithTz -> timez(type.precision)
    //     is Type.Timestamp -> timestamp(type.precision)
    //     is Type.TimestampWithTz -> timestampz(type.precision)
    //     is Type.Interval -> error("INTERVAL is not supported yet.")
    //     is Type.Bag -> bag()
    //     is Type.Sexp -> {
    //         // TODO will we continue supporting s-expression?
    //         PType.typeSexp()
    //     }
    //     is Type.Any -> dynamic()
    //     is Type.List -> array()
    //     is Type.Tuple -> struct()
    //     is Type.Struct -> struct()
    //     is Type.Custom -> TODO("Custom type not supported ")
    // }

    @JvmStatic
    fun from(kind: PType.Kind): PType = when (kind) {
        PType.Kind.DYNAMIC -> dynamic()
        PType.Kind.BOOL -> bool()
        PType.Kind.TINYINT -> tinyint()
        PType.Kind.SMALLINT -> smallint()
        PType.Kind.INTEGER -> int()
        PType.Kind.BIGINT -> bigint()
        PType.Kind.NUMERIC -> numeric()
        PType.Kind.DECIMAL, PType.Kind.DECIMAL_ARBITRARY -> decimal()
        PType.Kind.REAL -> real()
        PType.Kind.DOUBLE -> double()
        PType.Kind.CHAR -> char()
        PType.Kind.VARCHAR -> varchar()
        PType.Kind.STRING -> string()
        PType.Kind.SYMBOL -> {
            // TODO will we continue supporting symbol?
            PType.symbol()
        }
        PType.Kind.BLOB -> blob()
        PType.Kind.CLOB -> clob()
        PType.Kind.DATE -> date()
        PType.Kind.TIMEZ -> timez()
        PType.Kind.TIME -> time()
        PType.Kind.TIMESTAMPZ -> timestampz()
        PType.Kind.TIMESTAMP -> timestamp()
        PType.Kind.BAG -> bag()
        PType.Kind.ARRAY -> array()
        PType.Kind.ROW -> error("Cannot create a ROW from Kind")
        PType.Kind.SEXP -> {
            // TODO will we continue supporting sexp?
            PType.sexp()
        }
        PType.Kind.STRUCT -> struct()
        PType.Kind.UNKNOWN -> PType.unknown()
    }
}
