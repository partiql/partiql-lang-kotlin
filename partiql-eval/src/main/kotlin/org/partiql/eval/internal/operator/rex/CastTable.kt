package org.partiql.eval.internal.operator.rex

import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.IonElementException
import com.amazon.ionelement.api.createIonElementLoader
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.types.PType
import org.partiql.spi.types.PType.ARRAY
import org.partiql.spi.types.PType.BAG
import org.partiql.spi.types.PType.BIGINT
import org.partiql.spi.types.PType.BOOL
import org.partiql.spi.types.PType.CHAR
import org.partiql.spi.types.PType.CLOB
import org.partiql.spi.types.PType.DATE
import org.partiql.spi.types.PType.DECIMAL
import org.partiql.spi.types.PType.DOUBLE
import org.partiql.spi.types.PType.DYNAMIC
import org.partiql.spi.types.PType.INTEGER
import org.partiql.spi.types.PType.INTERVAL_DT
import org.partiql.spi.types.PType.INTERVAL_YM
import org.partiql.spi.types.PType.NUMERIC
import org.partiql.spi.types.PType.REAL
import org.partiql.spi.types.PType.ROW
import org.partiql.spi.types.PType.SMALLINT
import org.partiql.spi.types.PType.STRING
import org.partiql.spi.types.PType.STRUCT
import org.partiql.spi.types.PType.TIME
import org.partiql.spi.types.PType.TIMESTAMP
import org.partiql.spi.types.PType.TIMESTAMPZ
import org.partiql.spi.types.PType.TIMEZ
import org.partiql.spi.types.PType.TINYINT
import org.partiql.spi.types.PType.VARCHAR
import org.partiql.spi.types.PType.VARIANT
import org.partiql.spi.value.Datum
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Represent the cast operation. This casts an input [Datum] to the target [PType] (returning a potentially new [Datum]).
 */
private typealias Cast = (Datum, PType) -> Datum

/**
 * A two-dimensional array to look up casts by an input [PType.code] and target [PType.code]. If a [Cast] is found
 * (aka not null), then the cast is valid and may proceed. If the cast is null, then the cast is not supported between the
 * two types.
 *
 * The cast table is made fast by using the [PType.code] for indexing. It is up to the [Cast] to provide
 * additional logic regarding a type's parameters (aka [PType]).
 *
 * @see PType
 * @see PType.code
 */
private typealias CastLookupTable = Array<Array<Cast?>>

internal object CastTable {

    /**
     * Casts the [source] to the [target].
     * @throws PRuntimeException if the cast is not supported or if the cast fails.
     */
    @Throws(PRuntimeException::class)
    public fun cast(source: Datum, target: PType): Datum {
        if (source.isNull) {
            return Datum.nullValue(target)
        }
        if (source.isMissing) {
            return Datum.missing(target)
        }
        if (target.code() == DYNAMIC) {
            return source
        }
        val cast = _table[source.type.code()][target.code()] ?: throw PErrors.castUndefinedException(source.type, target)
        return try {
            cast.invoke(source, target)
        } catch (e: PRuntimeException) {
            throw e
        } catch (t: Throwable) {
            throw PErrors.internalErrorException(t)
        }
    }

    private val TYPES = PType.codes()
    private val SIZE = TYPES.size
    private val TYPE_NAME_MAX_LENGTH = TYPES.maxOf { it.toString().length }
    private val _table: CastLookupTable = Array(PType.codes().size) {
        Array(PType.codes().size) {
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
        registerReal()
        registerDoublePrecision()
        registerStruct()
        registerRow()
        registerString()
        registerBag()
        registerList()
        registerDate()
        registerTime()
        registerTimestamp()
        registerVariant()
        registerInterval()
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
                    val typeName = TYPES[targetTypeIndex].toString()
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
                append(TYPES[sourceTypeIndex].toString().pad())
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
     */
    private fun registerBool() {
        register(BOOL, BOOL) { x, _ -> x }
        register(BOOL, TINYINT) { x, _ -> Datum.tinyint(if (x.boolean) 1 else 0) }
        register(BOOL, SMALLINT) { x, _ -> Datum.smallint(if (x.boolean) 1 else 0) }
        register(BOOL, INTEGER) { x, _ -> Datum.integer(if (x.boolean) 1 else 0) }
        register(BOOL, BIGINT) { x, _ -> Datum.bigint(if (x.boolean) 1 else 0) }
        register(
            BOOL,
            NUMERIC
        ) { x, t -> Datum.numeric(if (x.boolean) BigDecimal.ONE else BigDecimal.ZERO, t.precision, t.scale) }
        register(
            BOOL,
            DECIMAL
        ) { x, t -> Datum.decimal(if (x.boolean) BigDecimal.ONE else BigDecimal.ZERO, t.precision, t.scale) }
        register(BOOL, REAL) { x, _ -> Datum.real(if (x.boolean) 1F else 0F) }
        register(
            BOOL,
            DOUBLE
        ) { x, _ -> Datum.doublePrecision(if (x.boolean) 1.0 else 0.0) }
        register(BOOL, STRING) { x, _ -> Datum.string(if (x.boolean) "true" else "false") }
        register(BOOL, VARCHAR) { x, t -> Datum.varchar(if (x.boolean) "true" else "false", t.length) }
        register(BOOL, CHAR) { x, t -> Datum.character(if (x.boolean) "true" else "false", t.length) }
        register(BOOL, CLOB) { x, t -> Datum.clob((if (x.boolean) "true" else "false").toByteArray(), t.length) }
    }

    /**
     * CAST(<tinyint> AS <type>)
     */
    private fun registerTinyInt() {
        register(TINYINT, BOOL) { x, _ -> Datum.bool(x.byte.toInt() != 0) }
        register(TINYINT, TINYINT) { x, _ -> x }
        register(TINYINT, SMALLINT) { x, _ -> Datum.smallint(x.byte.toShort()) }
        register(TINYINT, INTEGER) { x, _ -> Datum.integer(x.byte.toInt()) }
        register(TINYINT, BIGINT) { x, _ -> Datum.bigint(x.byte.toLong()) }
        register(TINYINT, NUMERIC) { x, t ->
            Datum.numeric(
                x.byte.toInt().toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(TINYINT, DECIMAL) { x, t ->
            Datum.decimal(
                x.byte.toInt().toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(TINYINT, REAL) { x, _ -> Datum.real(x.byte.toFloat()) }
        register(TINYINT, DOUBLE) { x, _ -> Datum.doublePrecision(x.byte.toDouble()) }
        register(TINYINT, STRING) { x, _ -> Datum.string(x.byte.toString()) }
        register(TINYINT, VARCHAR) { x, t -> Datum.varchar(x.byte.toString(), t.length) }
        register(TINYINT, CHAR) { x, t -> Datum.character(x.byte.toString(), t.length) }
        register(TINYINT, CLOB) { x, t -> Datum.clob(x.byte.toString().toByteArray(), t.length) }
    }

    /**
     * CAST(<smallint> AS <target>)
     */
    private fun registerSmallInt() {
        register(SMALLINT, BOOL) { x, _ -> Datum.bool(x.short.toInt() != 0) }
        register(SMALLINT, TINYINT) { x, _ -> datumTinyInt(x.short) }
        register(SMALLINT, SMALLINT) { x, _ -> x }
        register(SMALLINT, INTEGER) { x, _ -> Datum.integer(x.short.toInt()) }
        register(SMALLINT, BIGINT) { x, _ -> Datum.bigint(x.short.toLong()) }
        register(SMALLINT, NUMERIC) { x, t ->
            Datum.numeric(
                x.short.toInt().toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(SMALLINT, DECIMAL) { x, t ->
            Datum.decimal(
                x.short.toInt().toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(SMALLINT, REAL) { x, _ -> Datum.real(x.short.toFloat()) }
        register(SMALLINT, DOUBLE) { x, _ -> Datum.doublePrecision(x.short.toDouble()) }
        register(SMALLINT, STRING) { x, _ -> Datum.string(x.short.toString()) }
        register(SMALLINT, VARCHAR) { x, t -> Datum.varchar(x.short.toString(), t.length) }
        register(SMALLINT, CHAR) { x, t -> Datum.character(x.short.toString(), t.length) }
        register(SMALLINT, CLOB) { x, t -> Datum.clob(x.short.toString().toByteArray(), t.length) }
    }

    /**
     * CAST(<int> AS <target>)
     */
    private fun registerInt() {
        register(INTEGER, BOOL) { x, _ -> Datum.bool(x.int != 0) }
        register(INTEGER, TINYINT) { x, _ -> datumTinyInt(x.int) }
        register(INTEGER, SMALLINT) { x, _ -> datumSmallInt(x.int) }
        register(INTEGER, INTEGER) { x, _ -> x }
        register(INTEGER, BIGINT) { x, _ -> Datum.bigint(x.int.toLong()) }
        register(INTEGER, NUMERIC) { x, t -> Datum.numeric(x.int.toBigDecimal(), t.precision, t.scale) }
        register(INTEGER, DECIMAL) { x, t ->
            Datum.decimal(
                x.int.toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(INTEGER, REAL) { x, _ -> Datum.real(x.int.toFloat()) }
        register(INTEGER, DOUBLE) { x, _ -> Datum.doublePrecision(x.int.toDouble()) }
        register(INTEGER, STRING) { x, _ -> Datum.string(x.int.toString()) }
        register(INTEGER, VARCHAR) { x, t -> Datum.varchar(x.int.toString(), t.length) }
        register(INTEGER, CHAR) { x, t -> Datum.character(x.int.toString(), t.length) }
        register(INTEGER, CLOB) { x, t -> Datum.clob(x.int.toString().toByteArray(), t.length) }
    }

    /**
     * CAST(<bigint> AS <target>)
     */
    private fun registerBigInt() {
        register(BIGINT, BOOL) { x, _ -> Datum.bool(x.long != 0L) }
        register(BIGINT, TINYINT) { x, _ -> datumTinyInt(x.long) }
        register(BIGINT, SMALLINT) { x, _ -> datumSmallInt(x.long) }
        register(BIGINT, INTEGER) { x, _ -> datumInt(x.long) }
        register(BIGINT, BIGINT) { x, _ -> x }
        register(BIGINT, NUMERIC) { x, t -> Datum.numeric(x.long.toBigDecimal(), t.precision, t.scale) }
        register(BIGINT, DECIMAL) { x, t ->
            Datum.decimal(
                x.long.toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(BIGINT, REAL) { x, _ -> Datum.real(x.long.toFloat()) }
        register(BIGINT, DOUBLE) { x, _ -> Datum.doublePrecision(x.long.toDouble()) }
        register(BIGINT, STRING) { x, _ -> Datum.string(x.long.toString()) }
        register(BIGINT, VARCHAR) { x, t -> Datum.varchar(x.long.toString(), t.length) }
        register(BIGINT, CHAR) { x, t -> Datum.character(x.long.toString(), t.length) }
        register(BIGINT, CLOB) { x, t -> Datum.clob(x.long.toString().toByteArray(), t.length) }
    }

    /**
     * CAST(<int arbitrary> AS <target>)
     */
    private fun registerIntArbitrary() {
        register(NUMERIC, BOOL) { x, _ -> Datum.bool(x.bigDecimal != BigDecimal.ZERO) }
        register(NUMERIC, TINYINT) { x, _ -> datumTinyInt(x.bigDecimal) }
        register(NUMERIC, SMALLINT) { x, _ -> datumSmallInt(x.bigDecimal) }
        register(NUMERIC, INTEGER) { x, _ -> datumInt(x.bigDecimal) }
        register(NUMERIC, BIGINT) { x, _ -> datumBigInt(x.bigDecimal) }
        register(NUMERIC, NUMERIC) { x, t -> Datum.numeric(x.bigDecimal, t.precision, t.scale) }
        register(NUMERIC, DECIMAL) { x, t ->
            Datum.decimal(
                x.bigDecimal,
                t.precision,
                t.scale
            )
        }
        register(NUMERIC, REAL) { x, _ -> datumReal(x.bigDecimal) }
        register(
            NUMERIC,
            DOUBLE
        ) { x, _ -> datumDoublePrecision(x.bigDecimal) }
        register(NUMERIC, STRING) { x, _ -> Datum.string(x.bigDecimal.toString()) }
        register(NUMERIC, VARCHAR) { x, t -> Datum.varchar(x.bigDecimal.toString(), t.length) }
        register(NUMERIC, CHAR) { x, t -> Datum.character(x.bigDecimal.toString(), t.length) }
        register(NUMERIC, CLOB) { x, t -> Datum.clob(x.bigDecimal.toString().toByteArray(), t.length) }
    }

    /**
     * CAST(<decimal> AS <target>)
     */
    private fun registerDecimal() {
        register(DECIMAL, BOOL) { x, _ -> Datum.bool(x.bigDecimal != BigDecimal.ZERO) }
        register(DECIMAL, TINYINT) { x, _ -> datumTinyInt(x.bigDecimal) }
        register(DECIMAL, SMALLINT) { x, _ -> datumSmallInt(x.bigDecimal) }
        register(DECIMAL, INTEGER) { x, _ -> datumInt(x.bigDecimal) }
        register(DECIMAL, BIGINT) { x, _ -> datumBigInt(x.bigDecimal) }
        register(
            DECIMAL,
            NUMERIC
        ) { x, t -> Datum.numeric(x.bigDecimal, t.precision, t.scale) }
        register(DECIMAL, DECIMAL) { x, t -> Datum.decimal(x.bigDecimal, t.precision, t.scale) }
        register(DECIMAL, REAL) { x, _ -> datumReal(x.bigDecimal) }
        register(
            DECIMAL,
            DOUBLE
        ) { x, _ -> datumDoublePrecision(x.bigDecimal) }
        register(DECIMAL, STRING) { x, _ -> Datum.string(x.bigDecimal.toString()) }
        register(DECIMAL, VARCHAR) { x, t -> Datum.varchar(x.bigDecimal.toString(), t.length) }
        register(DECIMAL, CHAR) { x, t -> Datum.character(x.bigDecimal.toString(), t.length) }
        register(DECIMAL, CLOB) { x, t -> Datum.clob(x.bigDecimal.toString().toByteArray(), t.length) }
    }

    /**
     * CAST(<real> AS <target>)
     */
    private fun registerReal() {
        register(REAL, BOOL) { x, _ -> Datum.bool(x.float != 0F) }
        register(REAL, TINYINT) { x, _ -> datumTinyInt(x.float) }
        register(REAL, SMALLINT) { x, _ -> datumSmallInt(x.float) }
        register(REAL, INTEGER) { x, _ -> datumInt(x.float) }
        register(REAL, BIGINT) { x, _ -> datumBigInt(x.float) }
        register(REAL, NUMERIC) { x, t ->
            Datum.numeric(
                x.float.toInt().toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(REAL, DECIMAL) { x, t ->
            Datum.decimal(
                x.float.toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(REAL, REAL) { x, _ -> x }
        register(REAL, DOUBLE) { x, _ -> Datum.doublePrecision(x.float.toDouble()) }
        register(REAL, STRING) { x, _ -> Datum.string(x.float.toString()) }
        register(REAL, VARCHAR) { x, t -> Datum.varchar(x.float.toString(), t.length) }
        register(REAL, CHAR) { x, t -> Datum.character(x.float.toString(), t.length) }
        register(REAL, CLOB) { x, t -> Datum.clob(x.float.toString().toByteArray(), t.length) }
    }

    /**
     * CAST(<double precision> AS <target>)
     */
    private fun registerDoublePrecision() {
        register(DOUBLE, BOOL) { x, _ -> Datum.bool(x.double != 0.0) }
        register(DOUBLE, TINYINT) { x, _ -> datumTinyInt(x.double) }
        register(DOUBLE, SMALLINT) { x, _ ->
            Datum.smallint(
                x.double.toInt().toShort()
            )
        }
        register(DOUBLE, INTEGER) { x, _ -> datumInt(x.double) }
        register(DOUBLE, BIGINT) { x, _ -> datumBigInt(x.double) }
        register(DOUBLE, NUMERIC) { x, t ->
            Datum.numeric(
                x.double.toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(DOUBLE, DECIMAL) { x, t ->
            Datum.decimal(
                x.double.toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(DOUBLE, REAL) { x, _ -> datumReal(x.double) }
        register(DOUBLE, DOUBLE) { x, _ -> x }
        register(DOUBLE, STRING) { x, _ -> Datum.string(x.double.toString()) }
        register(DOUBLE, VARCHAR) { x, t -> Datum.varchar(x.double.toString(), t.length) }
        register(DOUBLE, CHAR) { x, t -> Datum.character(x.double.toString(), t.length) }
        register(DOUBLE, CLOB) { x, t -> Datum.clob(x.double.toString().toByteArray(), t.length) }
    }

    /**
     * CAST(<struct> AS <target>)
     */
    private fun registerStruct() {
        register(STRUCT, STRUCT) { x, _ -> x }
        register(STRUCT, ROW) { x, _ -> Datum.row(x.fields.asSequence().toList()) }
    }

    /**
     * CAST(<row> AS <target>)
     */
    private fun registerRow() {
        register(ROW, STRUCT) { x, _ -> Datum.struct(x.fields.asSequence().asIterable()) }
        register(ROW, ROW) { x, _ -> x }
    }

    /**
     * CAST(<string> AS <target>)
     */
    private fun registerString() {
        register(STRING, BOOL) { x, _ ->
            val str = x.string.lowercase().trim()
            when (str) {
                "true" -> Datum.bool(true)
                "false" -> Datum.bool(false)
                else -> throw PErrors.internalErrorException(IllegalArgumentException("Expected TRUE/FALSE"))
            }
        }
        register(STRING, TINYINT) { x, t -> cast(numberFromString(x.string), t) }
        register(STRING, SMALLINT) { x, t -> cast(numberFromString(x.string), t) }
        register(STRING, INTEGER) { x, t -> cast(numberFromString(x.string), t) }
        register(STRING, BIGINT) { x, t -> cast(numberFromString(x.string), t) }
        register(STRING, NUMERIC) { x, t -> cast(numberFromString(x.string), t) }
        register(STRING, DECIMAL) { x, t -> cast(numberFromString(x.string), t) }
        register(STRING, REAL) { x, t -> cast(numberFromString(x.string), t) }
        register(STRING, DOUBLE) { x, t -> cast(numberFromString(x.string), t) }
        register(STRING, STRING) { x, _ -> x }
        register(STRING, VARCHAR) { x, t -> Datum.varchar(x.string, t.length) }
        register(STRING, CHAR) { x, t -> Datum.character(x.string, t.length) }
        register(STRING, CLOB) { x, t -> Datum.clob(x.string.toByteArray(), t.length) }
    }

    private fun registerBag() {
        register(BAG, BAG) { x, _ -> x }
        register(BAG, ARRAY) { x, _ -> Datum.array(x) }
    }

    private fun registerList() {
        register(ARRAY, BAG) { x, _ -> Datum.bag(x) }
        register(ARRAY, ARRAY) { x, _ -> x }
    }

    private fun registerDate() {
        register(DATE, STRING) { x, _ -> Datum.string(x.localDate.toString()) }
        register(DATE, DATE) { x, _ -> Datum.date(x.localDate) }
        register(DATE, TIMESTAMP) { x, _ -> Datum.timestamp(x.localDate.atTime(LocalTime.MIN), 0) }
        register(DATE, TIMESTAMPZ) { x, _ -> Datum.timestampz(x.localDate.atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC), 0) }
    }

    private fun registerTime() {
        // WITHOUT TZ
        register(TIME, STRING) { x, _ -> Datum.string(x.localTime.toString()) }
        register(TIME, TIME) { x, t -> Datum.time(x.localTime, t.precision) }
        register(TIME, TIMEZ) { x, t -> Datum.timez(x.localTime.atOffset(ZoneOffset.UTC), t.precision) }
        register(TIME, TIMESTAMP) { x, t -> Datum.timestamp(x.localTime.atDate(LocalDate.now()), t.precision) }
        register(TIME, TIMESTAMPZ) { x, t -> Datum.timestampz(x.localTime.atDate(LocalDate.now()).atOffset(ZoneOffset.UTC), t.precision) }
        // WITH TZ
        register(TIMEZ, STRING) { x, _ -> Datum.string(x.offsetTime.toString()) }
        register(TIMEZ, TIME) { x, t -> Datum.time(x.localTime, t.precision) }
        register(TIMEZ, TIMEZ) { x, t -> Datum.timez(x.offsetTime, t.precision) }
        register(TIMEZ, TIMESTAMP) { x, t -> Datum.timestamp(x.localTime.atDate(LocalDate.now()), t.precision) }
        register(TIMEZ, TIMESTAMPZ) { x, t -> Datum.timestampz(x.offsetTime.atDate(LocalDate.now()), t.precision) }
    }

    private fun registerTimestamp() {
        // WITHOUT TZ
        register(TIMESTAMP, STRING) { x, _ -> Datum.string(x.localDateTime.toString()) }
        register(TIMESTAMP, TIMESTAMP) { x, t -> Datum.timestamp(x.localDateTime, t.precision) }
        register(TIMESTAMP, TIMESTAMPZ) { x, t -> Datum.timestampz(x.localDateTime.atOffset(ZoneOffset.UTC), t.precision) }
        register(TIMESTAMP, TIME) { x, t -> Datum.time(x.localTime, t.precision) }
        register(TIMESTAMP, DATE) { x, _ -> Datum.date(x.localDate) }
        // WITH TZ
        register(TIMESTAMPZ, STRING) { x, _ -> Datum.string(x.localDateTime.toString()) }
        register(TIMESTAMPZ, TIMESTAMP) { x, t -> Datum.timestamp(x.localDateTime, t.precision) }
        register(TIMESTAMPZ, TIMESTAMPZ) { x, t -> Datum.timestampz(x.offsetDateTime, t.precision) }
        register(TIMESTAMPZ, TIME) { x, t -> Datum.time(x.localTime, t.precision) }
        register(TIMESTAMPZ, DATE) { x, _ -> Datum.date(x.localDate) }
    }

    private fun registerInterval() {
        register(INTERVAL_YM, INTERVAL_YM) { v, t -> IntervalUtils.getIntervalYearMonth(v.years, v.months, t.precision, t.intervalCode) }
        register(INTERVAL_DT, INTERVAL_DT) { v, t -> IntervalUtils.getIntervalDateTime(v.days, v.hours, v.minutes, v.seconds, v.nanos, t.precision, t.fractionalPrecision, t.intervalCode) }
    }

    private fun registerVariant() {
        PType.codes().forEach { pType ->
            register(VARIANT, pType) { x, t -> cast(x.lower(), t) }
        }
        register(VARIANT, VARIANT) { x, _ -> x }
    }

    private fun register(source: Int, target: Int, cast: (Datum, PType) -> Datum) {
        _table[source][target] = cast
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
            throw PErrors.invalidStringCastException(str)
        }
        if (ion.isNull) {
            return Datum.nullValue()
        }
        return when (ion.type) {
            ElementType.INT -> Datum.numeric(ion.bigIntegerValue.toBigDecimal())
            ElementType.FLOAT -> Datum.doublePrecision(ion.doubleValue)
            ElementType.DECIMAL -> Datum.decimal(ion.decimalValue)
            else -> throw PErrors.invalidStringCastException(str)
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
            throw PErrors.numericOutOfRangeException(value.toString(), PType.integer())
        }
        return Datum.integer(value.toInt())
    }

    private fun datumInt(value: BigDecimal): Datum {
        val int = try {
            value.setScale(0, RoundingMode.HALF_EVEN).intValueExact()
        } catch (e: ArithmeticException) {
            throw PErrors.numericOutOfRangeException(value.toString(), PType.integer())
        }
        return Datum.integer(int)
    }

    private fun datumInt(value: Float): Datum {
        if (value > Int.MAX_VALUE || value < Int.MIN_VALUE) {
            throw PErrors.numericOutOfRangeException(value.toString(), PType.integer())
        }
        return Datum.integer(value.toInt())
    }

    private fun datumInt(value: Double): Datum {
        if (value > Int.MAX_VALUE || value < Int.MIN_VALUE) {
            throw PErrors.numericOutOfRangeException(value.toString(), PType.integer())
        }
        return Datum.integer(value.toInt())
    }

    private fun datumTinyInt(value: Long): Datum {
        if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
            throw PErrors.numericOutOfRangeException(value.toString(), PType.tinyint())
        }
        return Datum.tinyint(value.toByte())
    }
    private fun datumTinyInt(value: Int): Datum {
        if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
            throw PErrors.numericOutOfRangeException(value.toString(), PType.tinyint())
        }
        return Datum.tinyint(value.toByte())
    }

    private fun datumTinyInt(value: BigDecimal): Datum {
        val byte = try {
            value.setScale(0, RoundingMode.HALF_EVEN).byteValueExact()
        } catch (e: ArithmeticException) {
            throw PErrors.numericOutOfRangeException(value.toString(), PType.tinyint())
        }
        return Datum.tinyint(byte)
    }
    private fun datumTinyInt(value: Short): Datum {
        if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
            throw PErrors.numericOutOfRangeException(value.toString(), PType.tinyint())
        }
        return Datum.tinyint(value.toByte())
    }

    private fun datumTinyInt(value: Float): Datum {
        if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
            throw PErrors.numericOutOfRangeException(value.toString(), PType.tinyint())
        }
        return Datum.tinyint(value.toInt().toByte())
    }

    private fun datumTinyInt(value: Double): Datum {
        if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
            throw PErrors.numericOutOfRangeException(value.toString(), PType.tinyint())
        }
        return Datum.tinyint(value.toInt().toByte())
    }

    private fun datumSmallInt(value: Long): Datum {
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw PErrors.numericOutOfRangeException(value.toString(), PType.smallint())
        }
        return Datum.smallint(value.toShort())
    }
    private fun datumSmallInt(value: BigDecimal): Datum {
        val short = try {
            value.setScale(0, RoundingMode.HALF_EVEN).shortValueExact()
        } catch (e: ArithmeticException) {
            throw PErrors.numericOutOfRangeException(value.toString(), PType.smallint())
        }
        return Datum.smallint(short)
    }

    private fun datumSmallInt(value: Float): Datum {
        if (value > Short.MAX_VALUE || value < Short.MIN_VALUE) {
            throw PErrors.numericOutOfRangeException(value.toString(), PType.smallint())
        }
        return Datum.smallint(value.toInt().toShort())
    }

    private fun datumSmallInt(value: Int): Datum {
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw PErrors.numericOutOfRangeException(value.toString(), PType.smallint())
        }
        return Datum.smallint(value.toShort())
    }

    private fun datumBigInt(value: BigDecimal): Datum {
        return Datum.bigint(value.setScale(0, RoundingMode.HALF_EVEN).longValueExact())
    }

    private fun datumBigInt(value: Double): Datum {
        if (value > Long.MAX_VALUE || value < Long.MIN_VALUE) {
            throw PErrors.numericOutOfRangeException(value.toString(), PType.bigint())
        }
        return Datum.bigint(value.toLong())
    }

    private fun datumBigInt(value: Float): Datum {
        if (value > Long.MAX_VALUE || value < Long.MIN_VALUE) {
            throw PErrors.numericOutOfRangeException(value.toString(), PType.bigint())
        }
        return Datum.bigint(value.toLong())
    }

    private fun datumDoublePrecision(value: BigDecimal): Datum {
        return Datum.doublePrecision(value.toDouble())
    }

    private fun datumReal(value: Double): Datum {
        if (value > Float.MAX_VALUE || value < Float.MIN_VALUE) {
            throw PErrors.numericOutOfRangeException(value.toString(), PType.real())
        }
        return Datum.real(value.toFloat())
    }

    private fun datumReal(value: BigDecimal): Datum {
        val float = value.toFloat()
        if (float == Float.NEGATIVE_INFINITY || float == Float.POSITIVE_INFINITY) {
            throw PErrors.numericOutOfRangeException(value.toString(), PType.real())
        }
        return Datum.real(float)
    }
}
