package org.partiql.planner.intern

import org.partiql.types.PType
import org.partiql.value.PartiQLValueType

/**
 * Facade for instantiating types; we can drop T once the switch from StaticType to PType is complete.
 */
internal object SqlTypes {

    /**
     * Create a type from the enum.
     */
    fun create(type: PartiQLValueType): PType

    /**
     * Create a type form the enum with the given arguments.
     */
    fun create(type: PartiQLValueType, vararg args: Any): PType

    /**
     * The `dynamic` type.
     */
    fun dynamic(): PType

    /**
     * The `dynamic` type with known variants.
     */
    fun dynamic(variants: Set<T>): PType

    /**
     * The `dynamic` type with known variants.
     */
    fun dynamic(variants: List<T>): PType = dynamic(variants.toSet())

    /**
     * The `dynamic` type with known variants.
     */
    fun dynamic(vararg variants: PType): PType = dynamic(variants.toSet())

    /**
     * Boolean type.
     */
    fun bool(): PType

    /**
     * Signed 8-bit integer.
     *
     * Aliases: PTypeINYINT, INT1.
     */
    fun tinyint(): PType

    /**
     * Signed 16-bit integer.
     *
     * Aliases: SMALLINT, INT2.
     */
    fun smallint(): PType

    /**
     * Signed 32-bit integer.
     *
     * Aliases: INTEGER, INT, INT4.
     */
    fun int(): PType

    /**
     * Signed 64-bit integer.
     *
     * Aliases: BIGINT, INT8.
     */
    fun bigint(): PType

    /**
     * Exact numeric type with arbitrary precision and scale zero (0).
     *
     * Aliases: NUMERIC.
     */
    fun numeric(): PType

    /**
     * Exact numeric type with the given decimal precision and scale zero (0).
     *
     * Aliases: NUMERIC(p), DECIMAL(p).
     */
    fun decimal(precision: Int): PType

    /**
     * Numeric value with the given decimal precision and scale.
     *
     * Aliases: NUMERIC(p, s), DECIMAL(p, s).
     */
    fun decimal(precision: Int, scale: Int): PType

    /**
     * Exact numeric type with arbitrary precision and scale.
     *
     * Aliases: DECIMAL.
     */
    fun decimal(): PType

    /**
     * Approximate numeric type with binary precision equal to or greater than the given precision.
     *
     * Aliases: FLOAT(p).
     */
    fun float(precision: Int): PType

    /**
     * Approximate numeric type for the IEEE-754 32-bit floating point.
     *
     * Aliases: REAL, FLOAT4.
     */
    fun real(): PType

    /**
     * Approximate numeric type for the IEEE-754 64-bit floating point.
     *
     * Aliases: DOUBLE, DOUBLE PRECISION, FLOAT8.
     */
    fun double(): PType

    /**
     * Unicode codepoint sequence with fixed length.
     *
     * Aliases: CHAR.
     */
    fun char(length: Int): PType

    /**
     * Unicode codepoint sequence with arbitrary length.
     *
     * Aliases: VARCHAR, STRING.
     */
    fun varchar(): PType

    /**
     * Unicode codepoint sequence with max length.
     *
     * Aliases: VARCHAR(N).
     */
    fun varchar(length: Int): PType

    /**
     * TODO
     */
    fun date(): PType

    /**
     * TODO
     */
    fun time(): PType

    /**
     * TODO
     */
    fun time(precision: Int): PType

    /**
     * TODO
     */
    fun timestamp(): PType

    /**
     *
     */
    fun timestamp(precision: Int): PType

    /**
     * Aliases: CLOB
     */
    fun clob(): PType

    /**
     * Aliases: BLOB
     */
    fun blob(): PType

    /**
     * Variable-length, ordered collection of elements with type DYNAMIC.
     *
     * Aliases: ARRAY, LIST
     */
    fun array(): PType

    /**
     * Variable-length, ordered collection of elements with the given type.
     *
     * Aliases: PType ARRAY, ARRAY<T>.
     */
    fun array(element: PType): PType

    /**
     * Fixed-length, ordered collection of elements with the given type.
     *
     * Aliases: PType ARRAY[N], ARRAY<T>[N].
     */
    fun array(element: PType, size: Int): PType

    /**
     * Variable-length, unordered collection of elements with type DYNAMIC.
     *
     * Aliases: BAG
     */
    fun bag(): PType

    /**
     * Variable-length, unordered collection of elements with the given type.
     *
     * Aliases: PType BAG, BAG<T>.
     */
    fun bag(element: PType): PType

    /**
     * Fixed-length, unordered collection of elements with the given type.
     *
     * Aliases: PType BAG[N], BAG<T>[N].
     */
    fun bag(element: PType, size: Int): PType

    /**
     * Ordered collection of name-value pairs with some known fields; always open.
     */
    fun row(attributes: List<Pair<String, T>>): PType

    /**
     * Ordered collection of name-value pairs with some known fields; always open.
     */
    fun row(vararg attributes: Pair<String, T>): PType = row(attributes.toList())

    /**
     * Unordered collection of name-value pairs; always open.
     */
    fun struct(): PType

    /**
     * Unordered collection of name-value pairs with some known fields; always open.
     */
    fun struct(attributes: Collection<Pair<String, T>>): PType

    /**
     * Unordered collection of name-value pairs with some known fields; always open.
     */
    fun struct(vararg attributes: Pair<String, T>): PType = row(attributes.toList())
}
