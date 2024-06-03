package org.partiql.planner.intern

import org.partiql.value.PartiQLValueType

/**
 * Facade for instantiating types; we can drop T once the switch from StaticType to PType is complete.
 */
internal interface SqlTypes<T> {

    /**
     * Create a type from the enum.
     */
    fun create(type: PartiQLValueType): T

    /**
     * Create a type form the enum with the given arguments.
     */
    fun create(type: PartiQLValueType, vararg args: Any): T

    /**
     * The `dynamic` type.
     */
    fun dynamic(): T

    /**
     * The `dynamic` type with known variants.
     */
    fun dynamic(variants: Set<T>): T

    /**
     * The `dynamic` type with known variants.
     */
    fun dynamic(variants: List<T>): T = dynamic(variants.toSet())

    /**
     * The `dynamic` type with known variants.
     */
    fun dynamic(vararg variants: T): T = dynamic(variants.toSet())

    /**
     * Boolean type.
     */
    fun bool(): T

    /**
     * Signed 8-bit integer.
     *
     * Aliases: TINYINT, INT1.
     */
    fun tinyint(): T

    /**
     * Signed 16-bit integer.
     *
     * Aliases: SMALLINT, INT2.
     */
    fun smallint(): T

    /**
     * Signed 32-bit integer.
     *
     * Aliases: INTEGER, INT, INT4.
     */
    fun int(): T

    /**
     * Signed 64-bit integer.
     *
     * Aliases: BIGINT, INT8.
     */
    fun bigint(): T

    /**
     * Exact numeric type with arbitrary precision and scale zero (0).
     *
     * Aliases: NUMERIC.
     */
    fun numeric(): T

    /**
     * Exact numeric type with the given decimal precision and scale zero (0).
     *
     * Aliases: NUMERIC(p), DECIMAL(p).
     */
    fun numeric(precision: Int): T

    /**
     * Numeric value with the given decimal precision and scale.
     *
     * Aliases: NUMERIC(p, s), DECIMAL(p, s).
     */
    fun numeric(precision: Int, scale: Int): T

    /**
     * Exact numeric type with arbitrary precision and scale.
     *
     * Aliases: DECIMAL.
     */
    fun decimal(): T

    /**
     * Approximate numeric type with binary precision equal to or greater than the given precision.
     *
     * Aliases: FLOAT(p).
     */
    fun float(precision: Int): T

    /**
     * Approximate numeric type for the IEEE-754 32-bit floating point.
     *
     * Aliases: REAL, FLOAT4.
     */
    fun real(): T

    /**
     * Approximate numeric type for the IEEE-754 64-bit floating point.
     *
     * Aliases: DOUBLE, DOUBLE PRECISION, FLOAT8.
     */
    fun double(): T

    /**
     * Unicode codepoint sequence with fixed length.
     *
     * Aliases: CHAR.
     */
    fun char(length: Int): T

    /**
     * Unicode codepoint sequence with arbitrary length.
     *
     * Aliases: VARCHAR, STRING.
     */
    fun varchar(): T

    /**
     * Unicode codepoint sequence with max length.
     *
     * Aliases: VARCHAR(N).
     */
    fun varchar(length: Int): T

    /**
     * TODO
     */
    fun date(): T

    /**
     * TODO
     */
    fun time(): T

    /**
     * TODO
     */
    fun time(precision: Int): T

    /**
     * TODO
     */
    fun timestamp(): T

    /**
     *
     */
    fun timestamp(precision: Int): T

    /**
     * Aliases: CLOB
     */
    fun clob(): T

    /**
     * Aliases: BLOB
     */
    fun blob(): T

    /**
     * Variable-length, ordered collection of elements with type DYNAMIC.
     *
     * Aliases: ARRAY, LIST
     */
    fun array(): T

    /**
     * Variable-length, ordered collection of elements with the given type.
     *
     * Aliases: T ARRAY, ARRAY<T>.
     */
    fun array(element: T): T

    /**
     * Fixed-length, ordered collection of elements with the given type.
     *
     * Aliases: T ARRAY[N], ARRAY<T>[N].
     */
    fun array(element: T, size: Int): T

    /**
     * Variable-length, unordered collection of elements with type DYNAMIC.
     *
     * Aliases: BAG
     */
    fun bag(): T

    /**
     * Variable-length, unordered collection of elements with the given type.
     *
     * Aliases: T BAG, BAG<T>.
     */
    fun bag(element: T): T

    /**
     * Fixed-length, unordered collection of elements with the given type.
     *
     * Aliases: T BAG[N], BAG<T>[N].
     */
    fun bag(element: T, size: Int): T

    /**
     * Ordered collection of name-value pairs with some known fields; always open.
     */
    fun row(attributes: List<Pair<String, T>>): T

    /**
     * Ordered collection of name-value pairs with some known fields; always open.
     */
    fun row(vararg attributes: Pair<String, T>): T = row(attributes.toList())

    /**
     * Unordered collection of name-value pairs; always open.
     */
    fun struct(): T

    /**
     * Unordered collection of name-value pairs with some known fields; always open.
     */
    fun struct(attributes: Collection<Pair<String, T>>): T

    /**
     * Unordered collection of name-value pairs with some known fields; always open.
     */
    fun struct(vararg attributes: Pair<String, T>): T = row(attributes.toList())
}
