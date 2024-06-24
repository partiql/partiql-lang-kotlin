package org.partiql.planner.internal

import org.partiql.types.Field
import org.partiql.types.PType
import org.partiql.types.PType.Kind

/**
 * A basic internal type factory.
 */
internal object SqlTypes {

    private fun type(kind: Kind) = PType { kind }

    /**
     * The `dynamic` type.
     */
    fun dynamic(): PType = type(Kind.DYNAMIC)

    /**
     * Boolean type.
     */
    fun bool(): PType = type(Kind.BOOL)

    /**
     * Signed 8-bit integer.
     *
     * Aliases: PTypeINYINT, INT1.
     */
    fun tinyint(): PType = type(Kind.TINYINT)

    /**
     * Signed 16-bit integer.
     *
     * Aliases: SMALLINT, INT2.
     */
    fun smallint(): PType = type(Kind.SMALLINT)

    /**
     * Signed 32-bit integer.
     *
     * Aliases: INTEGER, INT, INT4.
     */
    fun int(): PType = type(Kind.INT)

    /**
     * Signed 64-bit integer.
     *
     * Aliases: BIGINT, INT8.
     */
    fun bigint(): PType = type(Kind.BIGINT)

    /**
     * Exact numeric type with arbitrary precision and scale zero (0).
     *
     * Aliases: NUMERIC.
     */
    fun numeric(): PType = type(Kind.INT_ARBITRARY)

    /**
     * Exact numeric type with the given decimal precision and scale zero (0).
     *
     * Aliases: NUMERIC(p), DECIMAL(p).
     */
    fun decimal(precision: Int): PType = object : PType {
        override fun getKind(): Kind = Kind.DECIMAL
        override fun getPrecision(): Int = precision
    }

    /**
     * Numeric value with the given decimal precision and scale.
     *
     * Aliases: NUMERIC(p, s), DECIMAL(p, s).
     */
    fun decimal(precision: Int, scale: Int): PType = object : PType {
        override fun getKind(): Kind = Kind.DECIMAL
        override fun getPrecision(): Int = precision
        override fun getScale(): Int = scale
    }

    /**
     * Exact numeric type with arbitrary precision and scale.
     *
     * Aliases: DECIMAL.
     */
    fun decimal(): PType = type(Kind.DECIMAL_ARBITRARY)

    /**
     * Approximate numeric type with binary precision equal to or greater than the given precision.
     *
     * Aliases: FLOAT(p).
     */
    fun float(precision: Int): PType = TODO("PType does not support precision")

    /**
     * Approximate numeric type for the IEEE-754 32-bit floating point.
     *
     * Aliases: REAL, FLOAT4.
     */
    fun real(): PType = type(Kind.REAL)

    /**
     * Approximate numeric type for the IEEE-754 64-bit floating point.
     *
     * Aliases: DOUBLE, DOUBLE PRECISION, FLOAT8.
     */
    fun double(): PType = type(Kind.DOUBLE_PRECISION)

    /**
     * Unicode codepoint sequence with fixed length.
     *
     * Aliases: CHAR.
     */
    fun char(length: Int): PType = object : PType {
        override fun getKind(): Kind = Kind.CHAR
        override fun getLength(): Int = length
    }

    /**
     * Unicode codepoint sequence with max length.
     *
     * Aliases: VARCHAR(N).
     */
    fun varchar(length: Int): PType = object : PType {
        override fun getKind(): Kind = Kind.VARCHAR
        override fun getLength(): Int = length
    }

    /**
     * Unicode codepoint sequence with arbitrary length.
     *
     * Aliases: VARCHAR, STRING.
     */
    fun varchar(): PType = type(Kind.STRING)

    /**
     * TODO
     */
    fun date(): PType = type(Kind.DATE)

    /**
     * TODO
     */
    fun time(): PType = type(Kind.TIME_WITHOUT_TZ)

    /**
     * TODO
     */
    fun time(precision: Int): PType = object : PType {
        override fun getKind(): Kind = Kind.TIME_WITHOUT_TZ
        override fun getPrecision(): Int = precision
    }

    /**
     * TODO
     */
    fun timestamp(): PType = type(Kind.TIMESTAMP_WITHOUT_TZ)

    /**
     * TODO
     */
    fun timestamp(precision: Int): PType = object : PType {
        override fun getKind(): Kind = Kind.TIMESTAMP_WITHOUT_TZ
        override fun getPrecision(): Int = precision
    }

    /**
     * Aliases: CLOB
     */
    fun clob(): PType = type(Kind.CLOB)

    /**
     * Aliases: BLOB
     */
    fun blob(): PType = type(Kind.BLOB)

    /**
     * Variable-length, ordered collection of elements with type DYNAMIC.
     *
     * Aliases: ARRAY, LIST
     */
    fun array(): PType = type(Kind.LIST)

    /**
     * Variable-length, ordered collection of elements with the given type.
     *
     * Aliases: PType ARRAY, ARRAY<T>.
     */
    fun array(element: PType): PType = object : PType {
        override fun getKind(): Kind = Kind.LIST
        override fun getTypeParameter(): PType = element
    }

    /**
     * Fixed-length, ordered collection of elements with the given type.
     *
     * Aliases: PType ARRAY[N], ARRAY<T>[N].
     */
    fun array(element: PType, size: Int): PType = object : PType {
        override fun getKind(): Kind = Kind.LIST
        override fun getLength(): Int = size
        override fun getTypeParameter(): PType = element
    }

    /**
     * Variable-length, unordered collection of elements with type DYNAMIC.
     *
     * Aliases: BAG
     */
    fun bag(): PType = type(Kind.BAG)

    /**
     * Variable-length, unordered collection of elements with the given type.
     *
     * Aliases: PType BAG, BAG<T>.
     */
    fun bag(element: PType): PType = object : PType {
        override fun getKind(): Kind = Kind.BAG
        override fun getTypeParameter(): PType = element
    }

    /**
     * Fixed-length, unordered collection of elements with the given type.
     *
     * Aliases: PType BAG[N], BAG<T>[N].
     */
    fun bag(element: PType, size: Int): PType = object : PType {
        override fun getKind(): Kind = Kind.BAG
        override fun getLength(): Int = size
        override fun getTypeParameter(): PType = element
    }

    /**
     * Ordered collection of name-value pairs with some known fields; always open.
     */
    fun row(attributes: List<Pair<String, PType>>): PType = object : PType {
        private val fields = attributes.map { Field.of(it.first, it.second) }.toMutableList()
        override fun getKind(): Kind = Kind.ROW
        override fun getFields(): MutableCollection<Field> = fields
    }

    /**
     * Ordered collection of name-value pairs with some known fields; always open.
     */
    fun row(vararg attributes: Pair<String, PType>): PType = row(attributes.toList())

    /**
     * Unordered collection of name-value pairs; always open.
     */
    fun struct(): PType = type(Kind.STRUCT)

    /**
     * Unordered collection of name-value pairs with some known fields; always open.
     */
    fun struct(attributes: Collection<Pair<String, PType>>): PType = object : PType {
        private val fields = attributes.map { Field.of(it.first, it.second) }.toMutableList()
        override fun getKind(): Kind = Kind.STRUCT
        override fun getFields(): MutableCollection<Field> = fields
    }

    /**
     * Unordered collection of name-value pairs with some known fields; always open.
     */
    fun struct(vararg attributes: Pair<String, PType>): PType = row(attributes.toList())
}
