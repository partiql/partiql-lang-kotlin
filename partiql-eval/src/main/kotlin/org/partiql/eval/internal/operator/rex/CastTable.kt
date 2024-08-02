package org.partiql.eval.internal.operator.rex

import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.IonElementException
import com.amazon.ionelement.api.createIonElementLoader
import org.partiql.errors.DataException
import org.partiql.errors.TypeCheckException
import org.partiql.eval.value.Datum
import org.partiql.types.PType
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

@Suppress("DEPRECATION")
internal object CastTable {

    public fun cast(source: Datum, target: PType): Datum {
        if (source.isNull) {
            return Datum.nullValue(target)
        }
        if (source.isMissing) {
            return Datum.missingValue(target)
        }
        if (target.kind == PType.Kind.DYNAMIC) {
            return source
        }
        val cast = castTable[source.type.kind.ordinal][target.kind.ordinal]
            ?: throw TypeCheckException("CAST(${source.type} AS $target) is not supported.")
        return try {
            cast.invoke(source, target)
        } catch (t: Throwable) {
            throw TypeCheckException("Failed to cast $source to $target")
        }
    }

    private val castTable: Array<Array<((Datum, PType) -> Datum)?>> = Array(PType.Kind.values().size) { inputKind ->
        Array(PType.Kind.values().size) { targetKind ->
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

    /**
     * CAST(<bool> AS <type>)
     * TODO: CHAR, VARCHAR, SYMBOL
     */
    private fun registerBool() {
        register(PType.Kind.BOOL, PType.Kind.BOOL) { x, _ -> x }
        register(PType.Kind.BOOL, PType.Kind.TINYINT) { x, _ -> Datum.tinyInt(if (x.boolean) 1 else 0) }
        register(PType.Kind.BOOL, PType.Kind.SMALLINT) { x, _ -> Datum.smallInt(if (x.boolean) 1 else 0) }
        register(PType.Kind.BOOL, PType.Kind.INT) { x, _ -> Datum.int32Value(if (x.boolean) 1 else 0) }
        register(PType.Kind.BOOL, PType.Kind.BIGINT) { x, _ -> Datum.int64Value(if (x.boolean) 1 else 0) }
        register(
            PType.Kind.BOOL,
            PType.Kind.INT_ARBITRARY
        ) { x, _ -> Datum.intArbitrary(if (x.boolean) BigInteger.ONE else BigInteger.ZERO) }
        register(
            PType.Kind.BOOL,
            PType.Kind.DECIMAL
        ) { x, t -> Datum.decimal(if (x.boolean) BigDecimal.ONE else BigDecimal.ZERO, t.precision, t.scale) }
        register(
            PType.Kind.BOOL,
            PType.Kind.DECIMAL_ARBITRARY
        ) { x, _ -> Datum.decimalArbitrary(if (x.boolean) BigDecimal.ONE else BigDecimal.ZERO) }
        register(PType.Kind.BOOL, PType.Kind.REAL) { x, _ -> Datum.real(if (x.boolean) 1F else 0F) }
        register(
            PType.Kind.BOOL,
            PType.Kind.DOUBLE_PRECISION
        ) { x, _ -> Datum.doublePrecision(if (x.boolean) 1.0 else 0.0) }
        register(PType.Kind.BOOL, PType.Kind.STRING) { x, _ -> Datum.stringValue(if (x.boolean) "true" else "false") }
        register(PType.Kind.BOOL, PType.Kind.SYMBOL) { x, _ -> Datum.stringValue(if (x.boolean) "true" else "false") }
    }

    /**
     * CAST(<tinyint> AS <type>)
     * TODO: CHAR, VARCHAR, SYMBOL
     */
    private fun registerTinyInt() {
        register(PType.Kind.TINYINT, PType.Kind.BOOL) { x, _ -> Datum.boolValue(x.byte.toInt() != 0) }
        register(PType.Kind.TINYINT, PType.Kind.TINYINT) { x, _ -> x }
        register(PType.Kind.TINYINT, PType.Kind.SMALLINT) { x, _ -> Datum.smallInt(x.byte.toShort()) }
        register(PType.Kind.TINYINT, PType.Kind.INT) { x, _ -> Datum.int32Value(x.byte.toInt()) }
        register(PType.Kind.TINYINT, PType.Kind.BIGINT) { x, _ -> Datum.int64Value(x.byte.toLong()) }
        register(PType.Kind.TINYINT, PType.Kind.INT_ARBITRARY) { x, _ ->
            Datum.intArbitrary(
                x.byte.toInt().toBigInteger()
            )
        }
        register(PType.Kind.TINYINT, PType.Kind.DECIMAL) { x, t ->
            Datum.decimal(
                x.byte.toInt().toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(PType.Kind.TINYINT, PType.Kind.DECIMAL_ARBITRARY) { x, _ ->
            Datum.decimalArbitrary(
                x.byte.toInt().toBigDecimal()
            )
        }
        register(PType.Kind.TINYINT, PType.Kind.REAL) { x, _ -> Datum.real(x.byte.toFloat()) }
        register(PType.Kind.TINYINT, PType.Kind.DOUBLE_PRECISION) { x, _ -> Datum.doublePrecision(x.byte.toDouble()) }
        register(PType.Kind.TINYINT, PType.Kind.STRING) { x, _ -> Datum.stringValue(x.byte.toString()) }
        register(PType.Kind.TINYINT, PType.Kind.SYMBOL) { x, _ -> Datum.stringValue(x.byte.toString()) }
    }

    /**
     * CAST(<smallint> AS <target>)
     * TODO: CHAR, VARCHAR, SYMBOL
     */
    private fun registerSmallInt() {
        register(PType.Kind.SMALLINT, PType.Kind.BOOL) { x, _ -> Datum.boolValue(x.short.toInt() != 0) }
        register(PType.Kind.SMALLINT, PType.Kind.TINYINT) { x, _ -> datumTinyInt(x.short) }
        register(PType.Kind.SMALLINT, PType.Kind.SMALLINT) { x, _ -> x }
        register(PType.Kind.SMALLINT, PType.Kind.INT) { x, _ -> Datum.int32Value(x.short.toInt()) }
        register(PType.Kind.SMALLINT, PType.Kind.BIGINT) { x, _ -> Datum.int64Value(x.short.toLong()) }
        register(PType.Kind.SMALLINT, PType.Kind.INT_ARBITRARY) { x, _ ->
            Datum.intArbitrary(
                x.short.toInt().toBigInteger()
            )
        }
        register(PType.Kind.SMALLINT, PType.Kind.DECIMAL) { x, t ->
            Datum.decimal(
                x.short.toInt().toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(PType.Kind.SMALLINT, PType.Kind.DECIMAL_ARBITRARY) { x, _ ->
            Datum.decimalArbitrary(
                x.short.toInt().toBigDecimal()
            )
        }
        register(PType.Kind.SMALLINT, PType.Kind.REAL) { x, _ -> Datum.real(x.short.toFloat()) }
        register(PType.Kind.SMALLINT, PType.Kind.DOUBLE_PRECISION) { x, _ -> Datum.doublePrecision(x.short.toDouble()) }
        register(PType.Kind.SMALLINT, PType.Kind.STRING) { x, _ -> Datum.stringValue(x.short.toString()) }
        register(PType.Kind.SMALLINT, PType.Kind.SYMBOL) { x, _ -> Datum.stringValue(x.short.toString()) }
    }

    /**
     * CAST(<int> AS <target>)
     * TODO: CHAR, VARCHAR, SYMBOL
     */
    private fun registerInt() {
        register(PType.Kind.INT, PType.Kind.BOOL) { x, _ -> Datum.boolValue(x.int != 0) }
        register(PType.Kind.INT, PType.Kind.TINYINT) { x, _ -> datumTinyInt(x.int) }
        register(PType.Kind.INT, PType.Kind.SMALLINT) { x, _ -> datumSmallInt(x.int) }
        register(PType.Kind.INT, PType.Kind.INT) { x, _ -> x }
        register(PType.Kind.INT, PType.Kind.BIGINT) { x, _ -> Datum.int64Value(x.int.toLong()) }
        register(PType.Kind.INT, PType.Kind.INT_ARBITRARY) { x, _ -> Datum.intArbitrary(x.int.toBigInteger()) }
        register(PType.Kind.INT, PType.Kind.DECIMAL) { x, t ->
            Datum.decimal(
                x.int.toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(PType.Kind.INT, PType.Kind.DECIMAL_ARBITRARY) { x, _ -> Datum.decimalArbitrary(x.int.toBigDecimal()) }
        register(PType.Kind.INT, PType.Kind.REAL) { x, _ -> Datum.real(x.int.toFloat()) }
        register(PType.Kind.INT, PType.Kind.DOUBLE_PRECISION) { x, _ -> Datum.doublePrecision(x.int.toDouble()) }
        register(PType.Kind.INT, PType.Kind.STRING) { x, _ -> Datum.stringValue(x.int.toString()) }
        register(PType.Kind.INT, PType.Kind.SYMBOL) { x, _ -> Datum.stringValue(x.int.toString()) }
    }

    /**
     * CAST(<bigint> AS <target>)
     * TODO: CHAR, VARCHAR, SYMBOL
     */
    private fun registerBigInt() {
        register(PType.Kind.BIGINT, PType.Kind.BOOL) { x, _ -> Datum.boolValue(x.long != 0L) }
        register(PType.Kind.BIGINT, PType.Kind.TINYINT) { x, _ -> datumTinyInt(x.long) }
        register(PType.Kind.BIGINT, PType.Kind.SMALLINT) { x, _ -> datumSmallInt(x.long) }
        register(PType.Kind.BIGINT, PType.Kind.INT) { x, _ -> datumInt(x.long) }
        register(PType.Kind.BIGINT, PType.Kind.BIGINT) { x, _ -> x }
        register(PType.Kind.BIGINT, PType.Kind.INT_ARBITRARY) { x, _ -> Datum.intArbitrary(x.long.toBigInteger()) }
        register(PType.Kind.BIGINT, PType.Kind.DECIMAL) { x, t ->
            Datum.decimal(
                x.long.toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(
            PType.Kind.BIGINT,
            PType.Kind.DECIMAL_ARBITRARY
        ) { x, _ -> Datum.decimalArbitrary(x.long.toBigDecimal()) }
        register(PType.Kind.BIGINT, PType.Kind.REAL) { x, _ -> Datum.real(x.long.toFloat()) }
        register(PType.Kind.BIGINT, PType.Kind.DOUBLE_PRECISION) { x, _ -> Datum.doublePrecision(x.long.toDouble()) }
        register(PType.Kind.BIGINT, PType.Kind.STRING) { x, _ -> Datum.stringValue(x.long.toString()) }
        register(PType.Kind.BIGINT, PType.Kind.SYMBOL) { x, _ -> Datum.stringValue(x.long.toString()) }
    }

    /**
     * CAST(<int arbitrary> AS <target>)
     * TODO: CHAR, VARCHAR, SYMBOL
     */
    private fun registerIntArbitrary() {
        register(PType.Kind.INT_ARBITRARY, PType.Kind.BOOL) { x, _ -> Datum.boolValue(x.bigInteger != BigInteger.ZERO) }
        register(PType.Kind.INT_ARBITRARY, PType.Kind.TINYINT) { x, _ -> datumTinyInt(x.bigInteger) }
        register(PType.Kind.INT_ARBITRARY, PType.Kind.SMALLINT) { x, _ -> datumSmallInt(x.bigInteger) }
        register(PType.Kind.INT_ARBITRARY, PType.Kind.INT) { x, _ -> datumInt(x.bigInteger) }
        register(PType.Kind.INT_ARBITRARY, PType.Kind.BIGINT) { x, _ -> datumBigInt(x.bigInteger) }
        register(PType.Kind.INT_ARBITRARY, PType.Kind.INT_ARBITRARY) { x, _ -> x }
        register(PType.Kind.INT_ARBITRARY, PType.Kind.DECIMAL) { x, t ->
            Datum.decimal(
                x.bigInteger.toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(
            PType.Kind.INT_ARBITRARY,
            PType.Kind.DECIMAL_ARBITRARY
        ) { x, _ -> Datum.decimalArbitrary(x.bigInteger.toBigDecimal()) }
        register(PType.Kind.INT_ARBITRARY, PType.Kind.REAL) { x, _ -> datumReal(x.bigInteger) }
        register(
            PType.Kind.INT_ARBITRARY,
            PType.Kind.DOUBLE_PRECISION
        ) { x, _ -> datumDoublePrecision(x.bigInteger) }
        register(PType.Kind.INT_ARBITRARY, PType.Kind.STRING) { x, _ -> Datum.stringValue(x.bigInteger.toString()) }
        register(PType.Kind.INT_ARBITRARY, PType.Kind.SYMBOL) { x, _ -> Datum.stringValue(x.bigInteger.toString()) }
    }

    /**
     * CAST(<decimal> AS <target>)
     * TODO: CHAR, VARCHAR, SYMBOL
     */
    private fun registerDecimal() {
        register(PType.Kind.DECIMAL, PType.Kind.BOOL) { x, _ -> Datum.boolValue(x.bigDecimal != BigDecimal.ZERO) }
        register(PType.Kind.DECIMAL, PType.Kind.TINYINT) { x, _ -> datumTinyInt(x.bigDecimal) }
        register(PType.Kind.DECIMAL, PType.Kind.SMALLINT) { x, _ -> datumSmallInt(x.bigDecimal) }
        register(PType.Kind.DECIMAL, PType.Kind.INT) { x, _ -> datumInt(x.bigDecimal) }
        register(PType.Kind.DECIMAL, PType.Kind.BIGINT) { x, _ -> datumBigInt(x.bigDecimal) }
        register(
            PType.Kind.DECIMAL,
            PType.Kind.INT_ARBITRARY
        ) { x, _ -> datumIntArbitrary(x.bigDecimal) }
        register(PType.Kind.DECIMAL, PType.Kind.DECIMAL) { x, _ -> x }
        register(PType.Kind.DECIMAL, PType.Kind.DECIMAL_ARBITRARY) { x, _ -> Datum.decimalArbitrary(x.bigDecimal) }
        register(PType.Kind.DECIMAL, PType.Kind.REAL) { x, _ -> datumReal(x.bigDecimal) }
        register(
            PType.Kind.DECIMAL,
            PType.Kind.DOUBLE_PRECISION
        ) { x, _ -> datumDoublePrecision(x.bigDecimal) }
        register(PType.Kind.DECIMAL, PType.Kind.STRING) { x, _ -> Datum.stringValue(x.bigDecimal.toString()) }
        register(PType.Kind.DECIMAL, PType.Kind.SYMBOL) { x, _ -> Datum.stringValue(x.bigDecimal.toString()) }
    }

    /**
     * CAST(<decimal arbitrary> AS <target>)
     * TODO: CHAR, VARCHAR, SYMBOL
     */
    private fun registerDecimalArbitrary() {
        register(
            PType.Kind.DECIMAL_ARBITRARY,
            PType.Kind.BOOL
        ) { x, _ -> Datum.boolValue(x.bigDecimal != BigDecimal.ZERO) }
        register(PType.Kind.DECIMAL_ARBITRARY, PType.Kind.TINYINT) { x, _ -> datumTinyInt(x.bigDecimal) }
        register(PType.Kind.DECIMAL_ARBITRARY, PType.Kind.SMALLINT) { x, _ -> datumSmallInt(x.bigDecimal) }
        register(PType.Kind.DECIMAL_ARBITRARY, PType.Kind.INT) { x, _ -> datumInt(x.bigDecimal) }
        register(PType.Kind.DECIMAL_ARBITRARY, PType.Kind.BIGINT) { x, _ -> datumBigInt(x.bigDecimal) }
        register(
            PType.Kind.DECIMAL_ARBITRARY,
            PType.Kind.INT_ARBITRARY
        ) { x, _ -> datumIntArbitrary(x.bigDecimal) }
        register(PType.Kind.DECIMAL_ARBITRARY, PType.Kind.DECIMAL) { x, t ->
            Datum.decimal(
                x.bigDecimal,
                t.precision,
                t.scale
            )
        }
        register(PType.Kind.DECIMAL_ARBITRARY, PType.Kind.DECIMAL_ARBITRARY) { x, _ -> x }
        register(PType.Kind.DECIMAL_ARBITRARY, PType.Kind.REAL) { x, _ -> datumReal(x.bigDecimal) }
        register(
            PType.Kind.DECIMAL_ARBITRARY,
            PType.Kind.DOUBLE_PRECISION
        ) { x, _ -> datumDoublePrecision(x.bigDecimal) }
        register(PType.Kind.DECIMAL_ARBITRARY, PType.Kind.STRING) { x, _ -> Datum.stringValue(x.bigDecimal.toString()) }
        register(PType.Kind.DECIMAL_ARBITRARY, PType.Kind.SYMBOL) { x, _ -> Datum.stringValue(x.bigDecimal.toString()) }
    }

    /**
     * CAST(<real> AS <target>)
     * TODO: CHAR, VARCHAR, SYMBOL
     */
    private fun registerReal() {
        register(PType.Kind.REAL, PType.Kind.BOOL) { x, _ -> Datum.boolValue(x.float != 0F) }
        register(PType.Kind.REAL, PType.Kind.TINYINT) { x, _ -> datumTinyInt(x.float) }
        register(PType.Kind.REAL, PType.Kind.SMALLINT) { x, _ -> datumSmallInt(x.float) }
        register(PType.Kind.REAL, PType.Kind.INT) { x, _ -> datumInt(x.float) }
        register(PType.Kind.REAL, PType.Kind.BIGINT) { x, _ -> datumBigInt(x.float) }
        register(PType.Kind.REAL, PType.Kind.INT_ARBITRARY) { x, _ ->
            Datum.intArbitrary(
                x.float.toInt().toBigInteger()
            )
        }
        register(PType.Kind.REAL, PType.Kind.DECIMAL) { x, t ->
            Datum.decimal(
                x.float.toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(
            PType.Kind.REAL,
            PType.Kind.DECIMAL_ARBITRARY
        ) { x, _ -> Datum.decimalArbitrary(x.float.toBigDecimal()) }
        register(PType.Kind.REAL, PType.Kind.REAL) { x, _ -> x }
        register(PType.Kind.REAL, PType.Kind.DOUBLE_PRECISION) { x, _ -> Datum.doublePrecision(x.float.toDouble()) }
        register(PType.Kind.REAL, PType.Kind.STRING) { x, _ -> Datum.stringValue(x.float.toString()) }
        register(PType.Kind.REAL, PType.Kind.SYMBOL) { x, _ -> Datum.stringValue(x.float.toString()) }
    }

    /**
     * CAST(<double precision> AS <target>)
     * TODO: CHAR, VARCHAR, SYMBOL
     */
    private fun registerDoublePrecision() {
        register(PType.Kind.DOUBLE_PRECISION, PType.Kind.BOOL) { x, _ -> Datum.boolValue(x.double != 0.0) }
        register(PType.Kind.DOUBLE_PRECISION, PType.Kind.TINYINT) { x, _ -> datumTinyInt(x.double) }
        register(PType.Kind.DOUBLE_PRECISION, PType.Kind.SMALLINT) { x, _ ->
            Datum.smallInt(
                x.double.toInt().toShort()
            )
        }
        register(PType.Kind.DOUBLE_PRECISION, PType.Kind.INT) { x, _ -> datumInt(x.double) }
        register(PType.Kind.DOUBLE_PRECISION, PType.Kind.BIGINT) { x, _ -> datumBigInt(x.double) }
        register(PType.Kind.DOUBLE_PRECISION, PType.Kind.INT_ARBITRARY) { x, _ ->
            datumIntArbitrary(x.double)
        }
        register(PType.Kind.DOUBLE_PRECISION, PType.Kind.DECIMAL) { x, t ->
            Datum.decimal(
                x.double.toBigDecimal(),
                t.precision,
                t.scale
            )
        }
        register(
            PType.Kind.DOUBLE_PRECISION,
            PType.Kind.DECIMAL_ARBITRARY
        ) { x, _ -> Datum.decimalArbitrary(x.double.toBigDecimal()) }
        register(PType.Kind.DOUBLE_PRECISION, PType.Kind.REAL) { x, _ -> datumReal(x.double) }
        register(PType.Kind.DOUBLE_PRECISION, PType.Kind.DOUBLE_PRECISION) { x, _ -> x }
        register(PType.Kind.DOUBLE_PRECISION, PType.Kind.STRING) { x, _ -> Datum.stringValue(x.double.toString()) }
        register(PType.Kind.DOUBLE_PRECISION, PType.Kind.SYMBOL) { x, _ -> Datum.stringValue(x.double.toString()) }
    }

    /**
     * CAST(<struct> AS <target>)
     */
    private fun registerStruct() {
        register(PType.Kind.STRUCT, PType.Kind.STRUCT) { x, _ -> x }
    }

    /**
     * CAST(<string> AS <target>)
     */
    private fun registerString() {
        register(PType.Kind.STRING, PType.Kind.BOOL) { x, _ ->
            val str = x.string.lowercase()
            when (str) {
                "true " -> Datum.boolValue(true)
                "false" -> Datum.boolValue(false)
                else -> throw TypeCheckException()
            }
        }
        register(PType.Kind.STRING, PType.Kind.TINYINT) { x, t -> cast(numberFromString(x.string), t) }
        register(PType.Kind.STRING, PType.Kind.SMALLINT) { x, t -> cast(numberFromString(x.string), t) }
        register(PType.Kind.STRING, PType.Kind.INT) { x, t -> cast(numberFromString(x.string), t) }
        register(PType.Kind.STRING, PType.Kind.BIGINT) { x, t -> cast(numberFromString(x.string), t) }
        register(PType.Kind.STRING, PType.Kind.INT_ARBITRARY) { x, t -> cast(numberFromString(x.string), t) }
        register(PType.Kind.STRING, PType.Kind.DECIMAL) { x, t -> cast(numberFromString(x.string), t) }
        register(PType.Kind.STRING, PType.Kind.DECIMAL_ARBITRARY) { x, t -> cast(numberFromString(x.string), t) }
        register(PType.Kind.STRING, PType.Kind.REAL) { x, t -> cast(numberFromString(x.string), t) }
        register(PType.Kind.STRING, PType.Kind.DOUBLE_PRECISION) { x, t -> cast(numberFromString(x.string), t) }
        register(PType.Kind.STRING, PType.Kind.STRING) { x, _ -> x }
        register(PType.Kind.STRING, PType.Kind.SYMBOL) { x, _ -> Datum.stringValue(x.string) }
    }
    /**
     * CAST(<string> AS <target>)
     */
    private fun registerSymbol() {
        register(PType.Kind.SYMBOL, PType.Kind.BOOL) { x, _ ->
            val str = x.string.lowercase()
            when (str) {
                "true " -> Datum.boolValue(true)
                "false" -> Datum.boolValue(false)
                else -> throw TypeCheckException()
            }
        }
        register(PType.Kind.SYMBOL, PType.Kind.TINYINT) { x, t -> cast(numberFromString(x.string), t) }
        register(PType.Kind.SYMBOL, PType.Kind.SMALLINT) { x, t -> cast(numberFromString(x.string), t) }
        register(PType.Kind.SYMBOL, PType.Kind.INT) { x, t -> cast(numberFromString(x.string), t) }
        register(PType.Kind.SYMBOL, PType.Kind.BIGINT) { x, t -> cast(numberFromString(x.string), t) }
        register(PType.Kind.SYMBOL, PType.Kind.INT_ARBITRARY) { x, t -> cast(numberFromString(x.string), t) }
        register(PType.Kind.SYMBOL, PType.Kind.DECIMAL) { x, t -> cast(numberFromString(x.string), t) }
        register(PType.Kind.SYMBOL, PType.Kind.DECIMAL_ARBITRARY) { x, t -> cast(numberFromString(x.string), t) }
        register(PType.Kind.SYMBOL, PType.Kind.REAL) { x, t -> cast(numberFromString(x.string), t) }
        register(PType.Kind.SYMBOL, PType.Kind.DOUBLE_PRECISION) { x, t -> cast(numberFromString(x.string), t) }
        register(PType.Kind.SYMBOL, PType.Kind.STRING) { x, _ -> Datum.stringValue(x.string) }
        register(PType.Kind.SYMBOL, PType.Kind.SYMBOL) { x, _ -> x }
    }

    private fun registerBag() {
        register(PType.Kind.BAG, PType.Kind.BAG) { x, _ -> x }
        register(PType.Kind.BAG, PType.Kind.LIST) { x, _ -> Datum.listValue(x) }
        register(PType.Kind.BAG, PType.Kind.SEXP) { x, _ -> Datum.sexpValue(x) }
    }

    private fun registerList() {
        register(PType.Kind.LIST, PType.Kind.BAG) { x, _ -> Datum.bagValue(x) }
        register(PType.Kind.LIST, PType.Kind.LIST) { x, _ -> x }
        register(PType.Kind.LIST, PType.Kind.SEXP) { x, _ -> Datum.sexpValue(x) }
    }

    private fun registerSexp() {
        register(PType.Kind.SEXP, PType.Kind.BAG) { x, _ -> Datum.bagValue(x) }
        register(PType.Kind.SEXP, PType.Kind.LIST) { x, _ -> Datum.listValue(x) }
        register(PType.Kind.SEXP, PType.Kind.SEXP) { x, _ -> x }
    }

    private fun register(source: PType.Kind, target: PType.Kind, cast: (Datum, PType) -> Datum) {
        castTable[source.ordinal][target.ordinal] = cast
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
