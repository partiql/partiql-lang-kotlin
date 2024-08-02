package org.partiql.eval.internal.operator.rex

import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.IonElementException
import com.amazon.ionelement.api.createIonElementLoader
import org.partiql.errors.DataException
import org.partiql.errors.TypeCheckException
import org.partiql.eval.value.Datum
import org.partiql.types.PType
import org.partiql.types.PType.Kind.BAG
import org.partiql.types.PType.Kind.BIGINT
import org.partiql.types.PType.Kind.BOOL
import org.partiql.types.PType.Kind.DECIMAL
import org.partiql.types.PType.Kind.DECIMAL_ARBITRARY
import org.partiql.types.PType.Kind.DOUBLE_PRECISION
import org.partiql.types.PType.Kind.DYNAMIC
import org.partiql.types.PType.Kind.INT
import org.partiql.types.PType.Kind.INT_ARBITRARY
import org.partiql.types.PType.Kind.LIST
import org.partiql.types.PType.Kind.REAL
import org.partiql.types.PType.Kind.SEXP
import org.partiql.types.PType.Kind.SMALLINT
import org.partiql.types.PType.Kind.STRING
import org.partiql.types.PType.Kind.STRUCT
import org.partiql.types.PType.Kind.SYMBOL
import org.partiql.types.PType.Kind.TINYINT
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Represent the cast operation. This casts an input [Datum] to the target [PType] (returning a potentially new [Datum]).
 */
private typealias Cast = (Datum, PType) -> Datum

/**
 * A two-dimensional array to look up casts by an input [PType.Kind] and target [PType.Kind]. If a [Cast] is found
 * (aka not null), then the cast is valid and may proceed. If the cast is null, then the cast is not supported between the
 * two types.
 *
 * The cast table is made fast by using the [ordinal] for indexing. It is up to the [Cast] to provide
 * additional logic regarding a type's parameters (aka [PType]).
 *
 * @see PType
 * @see PType.Kind
 */
private typealias CastLookupTable = Array<Array<Cast?>>

@Suppress("DEPRECATION")
internal object CastTable {

    /**
     * Casts the [source] to the [target].
     * @throws TypeCheckException if the cast is not supported or if the cast fails.
     */
    @Throws(TypeCheckException::class)
    public fun cast(source: Datum, target: PType): Datum {
        if (source.isNull) {
            return Datum.nullValue(target)
        }
        if (source.isMissing) {
            return Datum.missingValue(target)
        }
        if (target.kind == DYNAMIC) {
            return source
        }
        val cast = _table[source.type.kind.ordinal][target.kind.ordinal]
            ?: throw TypeCheckException("CAST(${source.type} AS $target) is not supported.")
        return try {
            cast.invoke(source, target)
        } catch (t: Throwable) {
            throw TypeCheckException("Failed to cast $source to $target")
        }
    }

    private val TYPES = PType.Kind.values()
    private val SIZE = TYPES.size
    private val TYPE_NAME_MAX_LENGTH = TYPES.maxOf { it.name.length }
    private val _table: CastLookupTable = Array(PType.Kind.values().size) {
        Array(PType.Kind.values().size) {
            null
        }
    }

    init {
        registerBool()
        registerTinyInt()
        registerSmallInt()
        registerInt()
        registerBigInt()
        registerIntArbitrary()
        registerDecimal()
        registerDecimalArbitrary()
        registerReal()
        registerDoublePrecision()
        registerStruct()
        registerString()
        registerSymbol()
        registerBag()
        registerList()
        registerSexp()
    }

    private fun PType.Kind.pad(): String {
        return this.name.pad()
    }

    private fun String.pad(): String {
        return this.padEnd(TYPE_NAME_MAX_LENGTH, ' ').plus("|")
    }

    override fun toString(): String {
        return buildString {
            // Print top (target type) row
            for (typeStringIndex in 0 until TYPE_NAME_MAX_LENGTH) {
                when (typeStringIndex) {
                    TYPE_NAME_MAX_LENGTH - 1 -> append("INPUT \\ TARGET ".padStart(TYPE_NAME_MAX_LENGTH, ' ') + "|")
                    else -> append("".pad())
                }
                for (targetTypeIndex in 0 until SIZE) {
                    val typeName = TYPES[targetTypeIndex].name
                    val charIndex = typeStringIndex - (TYPE_NAME_MAX_LENGTH - typeName.length)
                    val char = typeName.getOrElse(charIndex) { ' ' }
                    append(' ')
                    append(char)
                    append("  ")
                }
                appendLine()
            }
            // Print separator line
            val numberOfSpaces = (TYPE_NAME_MAX_LENGTH + 1) + (SIZE * 4)
            appendLine("_".repeat(numberOfSpaces))
            // Print content with source type on left side
            for (sourceTypeIndex in 0 until SIZE) {
                append(TYPES[sourceTypeIndex].pad())
                val row = _table[sourceTypeIndex]
                row.forEach { cell ->
                    if (cell != null) {
                        append(" X |")
                    } else {
                        append("   |")
                    }
                }
                appendLine()
            }
        }
    }

    /**
     * CAST(<bool> AS <type>)
     * TODO: CHAR, VARCHAR, SYMBOL
     */
    private fun registerBool() {
        register(BOOL, BOOL) { x, _ -> x }
        register(BOOL, TINYINT) { x, _ -> Datum.tinyInt(if (x.boolean) 1 else 0) }
        register(BOOL, SMALLINT) { x, _ -> Datum.smallInt(if (x.boolean) 1 else 0) }
        register(BOOL, INT) { x, _ -> Datum.int32Value(if (x.boolean) 1 else 0) }
        register(BOOL, BIGINT) { x, _ -> Datum.int64Value(if (x.boolean) 1 else 0) }
        register(
            BOOL,
            INT_ARBITRARY
        ) { x, _ -> Datum.intArbitrary(if (x.boolean) BigInteger.ONE else BigInteger.ZERO) }
        register(
            BOOL,
            DECIMAL
        ) { x, t -> Datum.decimal(if (x.boolean) BigDecimal.ONE else BigDecimal.ZERO, t.precision, t.scale) }
        register(
            BOOL,
            DECIMAL_ARBITRARY
        ) { x, _ -> Datum.decimalArbitrary(if (x.boolean) BigDecimal.ONE else BigDecimal.ZERO) }
        register(BOOL, REAL) { x, _ -> Datum.real(if (x.boolean) 1F else 0F) }
        register(
            BOOL,
            DOUBLE_PRECISION
        ) { x, _ -> Datum.doublePrecision(if (x.boolean) 1.0 else 0.0) }
        register(BOOL, STRING) { x, _ -> Datum.stringValue(if (x.boolean) "true" else "false") }
        register(BOOL, SYMBOL) { x, _ -> Datum.stringValue(if (x.boolean) "true" else "false") }
    }

    /**
     * CAST(<tinyint> AS <type>)
     * TODO: CHAR, VARCHAR, SYMBOL
     */
    private fun registerTinyInt() {
        register(TINYINT, BOOL) { x, _ -> Datum.boolValue(x.byte.toInt() != 0) }
        register(TINYINT, TINYINT) { x, _ -> x }
        register(TINYINT, SMALLINT) { x, _ -> Datum.smallInt(x.byte.toShort()) }
        register(TINYINT, INT) { x, _ -> Datum.int32Value(x.byte.toInt()) }
        register(TINYINT, BIGINT) { x, _ -> Datum.int64Value(x.byte.toLong()) }
        register(TINYINT, INT_ARBITRARY) { x, _ ->
            Datum.intArbitrary(
                x.byte.toInt().toBigInteger()
            )
        }
        register(TINYINT, DECIMAL) { x, t ->
            Datum.decimal(
                x.byte.toInt().toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(TINYINT, DECIMAL_ARBITRARY) { x, _ ->
            Datum.decimalArbitrary(
                x.byte.toInt().toBigDecimal()
            )
        }
        register(TINYINT, REAL) { x, _ -> Datum.real(x.byte.toFloat()) }
        register(TINYINT, DOUBLE_PRECISION) { x, _ -> Datum.doublePrecision(x.byte.toDouble()) }
        register(TINYINT, STRING) { x, _ -> Datum.stringValue(x.byte.toString()) }
        register(TINYINT, SYMBOL) { x, _ -> Datum.stringValue(x.byte.toString()) }
    }

    /**
     * CAST(<smallint> AS <target>)
     * TODO: CHAR, VARCHAR, SYMBOL
     */
    private fun registerSmallInt() {
        register(SMALLINT, BOOL) { x, _ -> Datum.boolValue(x.short.toInt() != 0) }
        register(SMALLINT, TINYINT) { x, _ -> datumTinyInt(x.short) }
        register(SMALLINT, SMALLINT) { x, _ -> x }
        register(SMALLINT, INT) { x, _ -> Datum.int32Value(x.short.toInt()) }
        register(SMALLINT, BIGINT) { x, _ -> Datum.int64Value(x.short.toLong()) }
        register(SMALLINT, INT_ARBITRARY) { x, _ ->
            Datum.intArbitrary(
                x.short.toInt().toBigInteger()
            )
        }
        register(SMALLINT, DECIMAL) { x, t ->
            Datum.decimal(
                x.short.toInt().toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(SMALLINT, DECIMAL_ARBITRARY) { x, _ ->
            Datum.decimalArbitrary(
                x.short.toInt().toBigDecimal()
            )
        }
        register(SMALLINT, REAL) { x, _ -> Datum.real(x.short.toFloat()) }
        register(SMALLINT, DOUBLE_PRECISION) { x, _ -> Datum.doublePrecision(x.short.toDouble()) }
        register(SMALLINT, STRING) { x, _ -> Datum.stringValue(x.short.toString()) }
        register(SMALLINT, SYMBOL) { x, _ -> Datum.stringValue(x.short.toString()) }
    }

    /**
     * CAST(<int> AS <target>)
     * TODO: CHAR, VARCHAR, SYMBOL
     */
    private fun registerInt() {
        register(INT, BOOL) { x, _ -> Datum.boolValue(x.int != 0) }
        register(INT, TINYINT) { x, _ -> datumTinyInt(x.int) }
        register(INT, SMALLINT) { x, _ -> datumSmallInt(x.int) }
        register(INT, INT) { x, _ -> x }
        register(INT, BIGINT) { x, _ -> Datum.int64Value(x.int.toLong()) }
        register(INT, INT_ARBITRARY) { x, _ -> Datum.intArbitrary(x.int.toBigInteger()) }
        register(INT, DECIMAL) { x, t ->
            Datum.decimal(
                x.int.toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(INT, DECIMAL_ARBITRARY) { x, _ -> Datum.decimalArbitrary(x.int.toBigDecimal()) }
        register(INT, REAL) { x, _ -> Datum.real(x.int.toFloat()) }
        register(INT, DOUBLE_PRECISION) { x, _ -> Datum.doublePrecision(x.int.toDouble()) }
        register(INT, STRING) { x, _ -> Datum.stringValue(x.int.toString()) }
        register(INT, SYMBOL) { x, _ -> Datum.stringValue(x.int.toString()) }
    }

    /**
     * CAST(<bigint> AS <target>)
     * TODO: CHAR, VARCHAR, SYMBOL
     */
    private fun registerBigInt() {
        register(BIGINT, BOOL) { x, _ -> Datum.boolValue(x.long != 0L) }
        register(BIGINT, TINYINT) { x, _ -> datumTinyInt(x.long) }
        register(BIGINT, SMALLINT) { x, _ -> datumSmallInt(x.long) }
        register(BIGINT, INT) { x, _ -> datumInt(x.long) }
        register(BIGINT, BIGINT) { x, _ -> x }
        register(BIGINT, INT_ARBITRARY) { x, _ -> Datum.intArbitrary(x.long.toBigInteger()) }
        register(BIGINT, DECIMAL) { x, t ->
            Datum.decimal(
                x.long.toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(
            BIGINT,
            DECIMAL_ARBITRARY
        ) { x, _ -> Datum.decimalArbitrary(x.long.toBigDecimal()) }
        register(BIGINT, REAL) { x, _ -> Datum.real(x.long.toFloat()) }
        register(BIGINT, DOUBLE_PRECISION) { x, _ -> Datum.doublePrecision(x.long.toDouble()) }
        register(BIGINT, STRING) { x, _ -> Datum.stringValue(x.long.toString()) }
        register(BIGINT, SYMBOL) { x, _ -> Datum.stringValue(x.long.toString()) }
    }

    /**
     * CAST(<int arbitrary> AS <target>)
     * TODO: CHAR, VARCHAR, SYMBOL
     */
    private fun registerIntArbitrary() {
        register(INT_ARBITRARY, BOOL) { x, _ -> Datum.boolValue(x.bigInteger != BigInteger.ZERO) }
        register(INT_ARBITRARY, TINYINT) { x, _ -> datumTinyInt(x.bigInteger) }
        register(INT_ARBITRARY, SMALLINT) { x, _ -> datumSmallInt(x.bigInteger) }
        register(INT_ARBITRARY, INT) { x, _ -> datumInt(x.bigInteger) }
        register(INT_ARBITRARY, BIGINT) { x, _ -> datumBigInt(x.bigInteger) }
        register(INT_ARBITRARY, INT_ARBITRARY) { x, _ -> x }
        register(INT_ARBITRARY, DECIMAL) { x, t ->
            Datum.decimal(
                x.bigInteger.toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(
            INT_ARBITRARY,
            DECIMAL_ARBITRARY
        ) { x, _ -> Datum.decimalArbitrary(x.bigInteger.toBigDecimal()) }
        register(INT_ARBITRARY, REAL) { x, _ -> datumReal(x.bigInteger) }
        register(
            INT_ARBITRARY,
            DOUBLE_PRECISION
        ) { x, _ -> datumDoublePrecision(x.bigInteger) }
        register(INT_ARBITRARY, STRING) { x, _ -> Datum.stringValue(x.bigInteger.toString()) }
        register(INT_ARBITRARY, SYMBOL) { x, _ -> Datum.stringValue(x.bigInteger.toString()) }
    }

    /**
     * CAST(<decimal> AS <target>)
     * TODO: CHAR, VARCHAR, SYMBOL
     */
    private fun registerDecimal() {
        register(DECIMAL, BOOL) { x, _ -> Datum.boolValue(x.bigDecimal != BigDecimal.ZERO) }
        register(DECIMAL, TINYINT) { x, _ -> datumTinyInt(x.bigDecimal) }
        register(DECIMAL, SMALLINT) { x, _ -> datumSmallInt(x.bigDecimal) }
        register(DECIMAL, INT) { x, _ -> datumInt(x.bigDecimal) }
        register(DECIMAL, BIGINT) { x, _ -> datumBigInt(x.bigDecimal) }
        register(
            DECIMAL,
            INT_ARBITRARY
        ) { x, _ -> datumIntArbitrary(x.bigDecimal) }
        register(DECIMAL, DECIMAL) { x, _ -> x }
        register(DECIMAL, DECIMAL_ARBITRARY) { x, _ -> Datum.decimalArbitrary(x.bigDecimal) }
        register(DECIMAL, REAL) { x, _ -> datumReal(x.bigDecimal) }
        register(
            DECIMAL,
            DOUBLE_PRECISION
        ) { x, _ -> datumDoublePrecision(x.bigDecimal) }
        register(DECIMAL, STRING) { x, _ -> Datum.stringValue(x.bigDecimal.toString()) }
        register(DECIMAL, SYMBOL) { x, _ -> Datum.stringValue(x.bigDecimal.toString()) }
    }

    /**
     * CAST(<decimal arbitrary> AS <target>)
     * TODO: CHAR, VARCHAR, SYMBOL
     */
    private fun registerDecimalArbitrary() {
        register(
            DECIMAL_ARBITRARY,
            BOOL
        ) { x, _ -> Datum.boolValue(x.bigDecimal != BigDecimal.ZERO) }
        register(DECIMAL_ARBITRARY, TINYINT) { x, _ -> datumTinyInt(x.bigDecimal) }
        register(DECIMAL_ARBITRARY, SMALLINT) { x, _ -> datumSmallInt(x.bigDecimal) }
        register(DECIMAL_ARBITRARY, INT) { x, _ -> datumInt(x.bigDecimal) }
        register(DECIMAL_ARBITRARY, BIGINT) { x, _ -> datumBigInt(x.bigDecimal) }
        register(
            DECIMAL_ARBITRARY,
            INT_ARBITRARY
        ) { x, _ -> datumIntArbitrary(x.bigDecimal) }
        register(DECIMAL_ARBITRARY, DECIMAL) { x, t ->
            Datum.decimal(
                x.bigDecimal,
                t.precision,
                t.scale
            )
        }
        register(DECIMAL_ARBITRARY, DECIMAL_ARBITRARY) { x, _ -> x }
        register(DECIMAL_ARBITRARY, REAL) { x, _ -> datumReal(x.bigDecimal) }
        register(
            DECIMAL_ARBITRARY,
            DOUBLE_PRECISION
        ) { x, _ -> datumDoublePrecision(x.bigDecimal) }
        register(DECIMAL_ARBITRARY, STRING) { x, _ -> Datum.stringValue(x.bigDecimal.toString()) }
        register(DECIMAL_ARBITRARY, SYMBOL) { x, _ -> Datum.stringValue(x.bigDecimal.toString()) }
    }

    /**
     * CAST(<real> AS <target>)
     * TODO: CHAR, VARCHAR, SYMBOL
     */
    private fun registerReal() {
        register(REAL, BOOL) { x, _ -> Datum.boolValue(x.float != 0F) }
        register(REAL, TINYINT) { x, _ -> datumTinyInt(x.float) }
        register(REAL, SMALLINT) { x, _ -> datumSmallInt(x.float) }
        register(REAL, INT) { x, _ -> datumInt(x.float) }
        register(REAL, BIGINT) { x, _ -> datumBigInt(x.float) }
        register(REAL, INT_ARBITRARY) { x, _ ->
            Datum.intArbitrary(
                x.float.toInt().toBigInteger()
            )
        }
        register(REAL, DECIMAL) { x, t ->
            Datum.decimal(
                x.float.toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(
            REAL,
            DECIMAL_ARBITRARY
        ) { x, _ -> Datum.decimalArbitrary(x.float.toBigDecimal()) }
        register(REAL, REAL) { x, _ -> x }
        register(REAL, DOUBLE_PRECISION) { x, _ -> Datum.doublePrecision(x.float.toDouble()) }
        register(REAL, STRING) { x, _ -> Datum.stringValue(x.float.toString()) }
        register(REAL, SYMBOL) { x, _ -> Datum.stringValue(x.float.toString()) }
    }

    /**
     * CAST(<double precision> AS <target>)
     * TODO: CHAR, VARCHAR, SYMBOL
     */
    private fun registerDoublePrecision() {
        register(DOUBLE_PRECISION, BOOL) { x, _ -> Datum.boolValue(x.double != 0.0) }
        register(DOUBLE_PRECISION, TINYINT) { x, _ -> datumTinyInt(x.double) }
        register(DOUBLE_PRECISION, SMALLINT) { x, _ ->
            Datum.smallInt(
                x.double.toInt().toShort()
            )
        }
        register(DOUBLE_PRECISION, INT) { x, _ -> datumInt(x.double) }
        register(DOUBLE_PRECISION, BIGINT) { x, _ -> datumBigInt(x.double) }
        register(DOUBLE_PRECISION, INT_ARBITRARY) { x, _ ->
            datumIntArbitrary(x.double)
        }
        register(DOUBLE_PRECISION, DECIMAL) { x, t ->
            Datum.decimal(
                x.double.toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(
            DOUBLE_PRECISION,
            DECIMAL_ARBITRARY
        ) { x, _ -> Datum.decimalArbitrary(x.double.toBigDecimal()) }
        register(DOUBLE_PRECISION, REAL) { x, _ -> datumReal(x.double) }
        register(DOUBLE_PRECISION, DOUBLE_PRECISION) { x, _ -> x }
        register(DOUBLE_PRECISION, STRING) { x, _ -> Datum.stringValue(x.double.toString()) }
        register(DOUBLE_PRECISION, SYMBOL) { x, _ -> Datum.stringValue(x.double.toString()) }
    }

    /**
     * CAST(<struct> AS <target>)
     */
    private fun registerStruct() {
        register(STRUCT, STRUCT) { x, _ -> x }
    }

    /**
     * CAST(<string> AS <target>)
     */
    private fun registerString() {
        register(STRING, BOOL) { x, _ ->
            val str = x.string.lowercase()
            when (str) {
                "true " -> Datum.boolValue(true)
                "false" -> Datum.boolValue(false)
                else -> throw TypeCheckException()
            }
        }
        register(STRING, TINYINT) { x, t -> cast(numberFromString(x.string), t) }
        register(STRING, SMALLINT) { x, t -> cast(numberFromString(x.string), t) }
        register(STRING, INT) { x, t -> cast(numberFromString(x.string), t) }
        register(STRING, BIGINT) { x, t -> cast(numberFromString(x.string), t) }
        register(STRING, INT_ARBITRARY) { x, t -> cast(numberFromString(x.string), t) }
        register(STRING, DECIMAL) { x, t -> cast(numberFromString(x.string), t) }
        register(STRING, DECIMAL_ARBITRARY) { x, t -> cast(numberFromString(x.string), t) }
        register(STRING, REAL) { x, t -> cast(numberFromString(x.string), t) }
        register(STRING, DOUBLE_PRECISION) { x, t -> cast(numberFromString(x.string), t) }
        register(STRING, STRING) { x, _ -> x }
        register(STRING, SYMBOL) { x, _ -> Datum.stringValue(x.string) }
    }
    /**
     * CAST(<string> AS <target>)
     */
    private fun registerSymbol() {
        register(SYMBOL, BOOL) { x, _ ->
            val str = x.string.lowercase()
            when (str) {
                "true " -> Datum.boolValue(true)
                "false" -> Datum.boolValue(false)
                else -> throw TypeCheckException()
            }
        }
        register(SYMBOL, TINYINT) { x, t -> cast(numberFromString(x.string), t) }
        register(SYMBOL, SMALLINT) { x, t -> cast(numberFromString(x.string), t) }
        register(SYMBOL, INT) { x, t -> cast(numberFromString(x.string), t) }
        register(SYMBOL, BIGINT) { x, t -> cast(numberFromString(x.string), t) }
        register(SYMBOL, INT_ARBITRARY) { x, t -> cast(numberFromString(x.string), t) }
        register(SYMBOL, DECIMAL) { x, t -> cast(numberFromString(x.string), t) }
        register(SYMBOL, DECIMAL_ARBITRARY) { x, t -> cast(numberFromString(x.string), t) }
        register(SYMBOL, REAL) { x, t -> cast(numberFromString(x.string), t) }
        register(SYMBOL, DOUBLE_PRECISION) { x, t -> cast(numberFromString(x.string), t) }
        register(SYMBOL, STRING) { x, _ -> Datum.stringValue(x.string) }
        register(SYMBOL, SYMBOL) { x, _ -> x }
    }

    private fun registerBag() {
        register(BAG, BAG) { x, _ -> x }
        register(BAG, LIST) { x, _ -> Datum.listValue(x) }
        register(BAG, SEXP) { x, _ -> Datum.sexpValue(x) }
    }

    private fun registerList() {
        register(LIST, BAG) { x, _ -> Datum.bagValue(x) }
        register(LIST, LIST) { x, _ -> x }
        register(LIST, SEXP) { x, _ -> Datum.sexpValue(x) }
    }

    private fun registerSexp() {
        register(SEXP, BAG) { x, _ -> Datum.bagValue(x) }
        register(SEXP, LIST) { x, _ -> Datum.listValue(x) }
        register(SEXP, SEXP) { x, _ -> x }
    }

    private fun register(source: PType.Kind, target: PType.Kind, cast: (Datum, PType) -> Datum) {
        _table[source.ordinal][target.ordinal] = cast
    }

    /**
     * Converts a string to a Datum number.
     *
     * For now, utilize ion to parse string such as 0b10, etc.
     */
    private fun numberFromString(str: String): Datum {
        val ion = try {
            str.let { createIonElementLoader().loadSingleElement(it.normalizeForCastToInt()) }
        } catch (e: IonElementException) {
            throw TypeCheckException()
        }
        if (ion.isNull) {
            return Datum.nullValue()
        }
        return when (ion.type) {
            ElementType.INT -> Datum.intArbitrary(ion.bigIntegerValue)
            ElementType.FLOAT -> Datum.doublePrecision(ion.doubleValue)
            ElementType.DECIMAL -> Datum.decimalArbitrary(ion.decimalValue)
            else -> throw TypeCheckException()
        }
    }

    private fun String.normalizeForCastToInt(): String {
        fun Char.isSign() = this == '-' || this == '+'
        fun Char.isHexOrBase2Marker(): Boolean {
            val c = this.lowercaseChar()

            return c == 'x' || c == 'b'
        }

        fun String.possiblyHexOrBase2() = (length >= 2 && this[1].isHexOrBase2Marker()) || (length >= 3 && this[0].isSign() && this[2].isHexOrBase2Marker())

        return when {
            length == 0 -> this
            possiblyHexOrBase2() -> {
                if (this[0] == '+') {
                    this.drop(1)
                } else {
                    this
                }
            }

            else -> {
                val (isNegative, startIndex) = when (this[0]) {
                    '-' -> Pair(true, 1)
                    '+' -> Pair(false, 1)
                    else -> Pair(false, 0)
                }

                var toDrop = startIndex
                while (toDrop < length && this[toDrop] == '0') {
                    toDrop += 1
                }

                when {
                    toDrop == length -> "0" // string is all zeros
                    toDrop == 0 -> this
                    toDrop == 1 && isNegative -> this
                    toDrop > 1 && isNegative -> '-' + this.drop(toDrop)
                    else -> this.drop(toDrop)
                }
            }
        }
    }

    private fun datumInt(value: Long): Datum {
        if (value < Int.MIN_VALUE || value > Int.MAX_VALUE) {
            throw DataException("Overflow when casting $value to INT")
        }
        return Datum.int32Value(value.toInt())
    }

    private fun datumInt(value: BigDecimal): Datum {
        val int = try {
            value.setScale(0, RoundingMode.DOWN).intValueExact()
        } catch (e: ArithmeticException) {
            throw DataException("Overflow when casting $value to INT")
        }
        return Datum.int32Value(int)
    }

    private fun datumInt(value: BigInteger): Datum {
        val int = try {
            value.intValueExact()
        } catch (e: ArithmeticException) {
            throw DataException("Overflow when casting $value to INT")
        }
        return Datum.int32Value(int)
    }

    private fun datumInt(value: Float): Datum {
        if (value > Int.MAX_VALUE || value < Int.MIN_VALUE) {
            throw DataException("Overflow when casting $value to INT")
        }
        return Datum.int32Value(value.toInt())
    }

    private fun datumInt(value: Double): Datum {
        if (value > Int.MAX_VALUE || value < Int.MIN_VALUE) {
            throw DataException("Overflow when casting $value to INT")
        }
        return Datum.int32Value(value.toInt())
    }

    private fun datumTinyInt(value: Long): Datum {
        if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
            throw DataException("Overflow when casting $value to TINYINT")
        }
        return Datum.tinyInt(value.toByte())
    }
    private fun datumTinyInt(value: Int): Datum {
        if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
            throw DataException("Overflow when casting $value to TINYINT")
        }
        return Datum.tinyInt(value.toByte())
    }

    private fun datumTinyInt(value: BigDecimal): Datum {
        val byte = try {
            value.setScale(0, RoundingMode.DOWN).byteValueExact()
        } catch (e: ArithmeticException) {
            throw DataException("Overflow when casting $value to TINYINT")
        }
        return Datum.tinyInt(byte)
    }
    private fun datumTinyInt(value: Short): Datum {
        if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
            throw DataException("Overflow when casting $value to TINYINT")
        }
        return Datum.tinyInt(value.toByte())
    }

    private fun datumTinyInt(value: BigInteger): Datum {
        val byte = try {
            value.byteValueExact()
        } catch (e: ArithmeticException) {
            throw DataException("Overflow when casting $value to TINYINT")
        }
        return Datum.tinyInt(byte)
    }

    private fun datumTinyInt(value: Float): Datum {
        if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
            throw DataException("Overflow when casting $value to TINYINT")
        }
        return Datum.tinyInt(value.toInt().toByte())
    }

    private fun datumTinyInt(value: Double): Datum {
        if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
            throw DataException("Overflow when casting $value to TINYINT")
        }
        return Datum.tinyInt(value.toInt().toByte())
    }

    private fun datumSmallInt(value: Long): Datum {
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw DataException("Overflow when casting $value to SMALLINT")
        }
        return Datum.smallInt(value.toShort())
    }
    private fun datumSmallInt(value: BigDecimal): Datum {
        val short = try {
            value.setScale(0, RoundingMode.DOWN).shortValueExact()
        } catch (e: ArithmeticException) {
            throw DataException("Overflow when casting $value to SMALLINT")
        }
        return Datum.smallInt(short)
    }
    private fun datumSmallInt(value: BigInteger): Datum {
        val short = try {
            value.shortValueExact()
        } catch (e: ArithmeticException) {
            throw DataException("Overflow when casting $value to SMALLINT")
        }
        return Datum.smallInt(short)
    }
    private fun datumSmallInt(value: Float): Datum {
        if (value > Short.MAX_VALUE || value < Short.MIN_VALUE) {
            throw DataException("Overflow when casting $value to SMALLINT")
        }
        return Datum.smallInt(value.toInt().toShort())
    }
    private fun datumSmallInt(value: Double): Datum {
        if (value > Double.MAX_VALUE || value < Double.MIN_VALUE) {
            throw DataException("Overflow when casting $value to SMALLINT")
        }
        return Datum.smallInt(value.toInt().toShort())
    }
    private fun datumSmallInt(value: Int): Datum {
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw DataException("Overflow when casting $value to SMALLINT")
        }
        return Datum.smallInt(value.toShort())
    }

    private fun datumReal(value: Long): Datum {
        return Datum.real(value.toFloat())
    }

    private fun datumIntArbitrary(value: BigDecimal): Datum {
        return Datum.intArbitrary(value.setScale(0, RoundingMode.DOWN).toBigInteger())
    }

    private fun datumIntArbitrary(value: Double): Datum {
        return Datum.intArbitrary(value.toBigDecimal().setScale(0, RoundingMode.DOWN).toBigInteger())
    }

    private fun datumBigInt(value: BigInteger): Datum {
        return Datum.int64Value(value.longValueExact())
    }

    private fun datumBigInt(value: BigDecimal): Datum {
        return Datum.int64Value(value.setScale(0, RoundingMode.DOWN).longValueExact())
    }

    private fun datumBigInt(value: Double): Datum {
        if (value > Long.MAX_VALUE || value < Long.MIN_VALUE) {
            throw DataException("Overflow when casting $value to BIGINT")
        }
        return Datum.int64Value(value.toLong())
    }

    private fun datumBigInt(value: Float): Datum {
        if (value > Long.MAX_VALUE || value < Long.MIN_VALUE) {
            throw DataException("Overflow when casting $value to BIGINT")
        }
        return Datum.int64Value(value.toLong())
    }

    private fun datumDoublePrecision(value: BigDecimal): Datum {
        return Datum.doublePrecision(value.toDouble())
    }

    private fun datumDoublePrecision(value: BigInteger): Datum {
        return Datum.doublePrecision(value.toDouble())
    }

    private fun datumReal(value: Double): Datum {
        if (value > Float.MAX_VALUE || value < Float.MIN_VALUE) {
            throw DataException("Overflow when casting $value to REAL")
        }
        return Datum.real(value.toFloat())
    }

    private fun datumReal(value: BigDecimal): Datum {
        val float = value.toFloat()
        if (float == Float.NEGATIVE_INFINITY || float == Float.POSITIVE_INFINITY) {
            throw DataException("Overflow when casting $value to REAL")
        }
        return Datum.real(float)
    }

    private fun datumReal(value: BigInteger): Datum {
        return Datum.real(value.toFloat())
    }
}
