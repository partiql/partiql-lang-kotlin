/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package org.partiql.types

import org.partiql.value.PartiQLTimestampExperimental
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Represents static types available in the language and ways to extends them to create new types.
 */
public sealed class StaticType {
    public companion object {

        /**
         * varargs variant, folds [types] into a [Set]
         * The usage of LinkedHashSet is to preserve the order of `types` to ensure behavior is consistent in our tests
         */
        @JvmStatic
        public fun unionOf(vararg types: StaticType, metas: Map<String, Any> = mapOf()): StaticType =
            unionOf(types.toSet(), metas)

        /**
         * Creates a new [StaticType] as a union of the passed [types]. The values typed by the returned type
         * are defined as the union of all values typed as [types]
         *
         * @param types [StaticType] to be unioned.
         * @return [StaticType] representing the union of [types]
         */
        @JvmStatic
        public fun unionOf(types: Set<StaticType>, metas: Map<String, Any> = mapOf()): StaticType = AnyOfType(types, metas)

        // TODO consider making these into an enumeration...

        // Convenient enums to create a bare bones instance of StaticType
        @JvmField public val MISSING: MissingType = MissingType
        @JvmField public val NULL: NullType = NullType()
        @JvmField public val ANY: AnyType = AnyType()
        @JvmField public val NULL_OR_MISSING: StaticType = unionOf(NULL, MISSING)
        @JvmField public val BOOL: BoolType = BoolType()
        @JvmField public val INT2: IntType = IntType(IntType.IntRangeConstraint.SHORT)
        @JvmField public val INT4: IntType = IntType(IntType.IntRangeConstraint.INT4)
        @JvmField public val INT8: IntType = IntType(IntType.IntRangeConstraint.LONG)
        @JvmField public val INT: IntType = IntType(IntType.IntRangeConstraint.UNCONSTRAINED)
        @JvmField public val FLOAT: FloatType = FloatType()
        @JvmField public val DECIMAL: DecimalType = DecimalType()
        @JvmField public val NUMERIC: StaticType = unionOf(INT2, INT4, INT8, INT, FLOAT, DECIMAL)
        @JvmField public val DATE: DateType = DateType()
        @JvmField public val TIME: TimeType = TimeType()
        // This used to refer to timestamp with arbitrary precision, with time zone (ion timestamp always has timezone)
        @OptIn(PartiQLTimestampExperimental::class)
        @JvmField public val TIMESTAMP: TimestampType = TimestampType(null, true)
        @JvmField public val SYMBOL: SymbolType = SymbolType()
        @JvmField public val STRING: StringType = StringType()
        @JvmField public val TEXT: StaticType = unionOf(SYMBOL, STRING)
        @JvmField public val CHAR: StaticType = StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.Equals(1)))
        @JvmField public val CLOB: ClobType = ClobType()
        @JvmField public val BLOB: BlobType = BlobType()
        @JvmField public val LIST: ListType = ListType()
        @JvmField public val SEXP: SexpType = SexpType()
        @JvmField public val STRUCT: StructType = StructType()
        @JvmField public val BAG: BagType = BagType()
        @JvmField public val GRAPH: GraphType = GraphType()

        /** All the StaticTypes, except for `ANY`. */
        @OptIn(PartiQLTimestampExperimental::class)
        @JvmStatic
        public val ALL_TYPES: List<SingleType> = listOf(
            MISSING,
            NULL,
            BOOL,
            INT2,
            INT4,
            INT8,
            INT,
            FLOAT,
            DECIMAL,
            SYMBOL,
            DATE,
            TIME,
            TIMESTAMP,
            STRING,
            CLOB,
            BLOB,
            LIST,
            SEXP,
            STRUCT,
            BAG,
            GRAPH
        )
    }

    /**
     *  Returns a nullable version of the current [StaticType].
     *
     *  If it already nullable, returns the original type.
     */
    public fun asNullable(): StaticType =
        when {
            this.isNullable() -> this
            else -> unionOf(this, NULL).flatten()
        }

    /**
     *  Returns an optional version of the current [StaticType].
     *
     *  If it already optional, returns the original type.
     */
    public fun asOptional(): StaticType =
        when {
            this.isOptional() -> this
            else -> unionOf(this, MISSING).flatten()
        }

    public abstract val metas: Map<String, Any>

    /**
     * Convenience method to copy over StaticType to a new instance with given `metas`
     * MissingType is a singleton and there can only be one representation for it
     * i.e. you cannot have two instances of MissingType with different metas.
     */
    public fun withMetas(metas: Map<String, Any>): StaticType =
        when (this) {
            is AnyType -> copy(metas = metas)
            is ListType -> copy(metas = metas)
            is SexpType -> copy(metas = metas)
            is BagType -> copy(metas = metas)
            is NullType -> copy(metas = metas)
            is MissingType -> MissingType
            is BoolType -> copy(metas = metas)
            is IntType -> copy(metas = metas)
            is FloatType -> copy(metas = metas)
            is DecimalType -> copy(metas = metas)
            is TimestampType -> copy(metas = metas)
            is SymbolType -> copy(metas = metas)
            is StringType -> copy(metas = metas)
            is BlobType -> copy(metas = metas)
            is ClobType -> copy(metas = metas)
            is StructType -> copy(metas = metas)
            is AnyOfType -> copy(metas = metas)
            is DateType -> copy(metas = metas)
            is TimeType -> copy(metas = metas)
            is GraphType -> copy(metas = metas)
        }

    /**
     * Type is nullable if it is of Null type or is an AnyOfType that contains a Null type
     */
    public fun isNullable(): Boolean =
        when (this) {
            is AnyOfType -> types.any { it.isNullable() }
            is AnyType, is NullType -> true
            else -> false
        }

    /**
     * Type is missable if it is MISSING or is an AnyOfType that contains a MISSING type
     *
     * @return
     */
    public fun isMissable(): Boolean =
        when (this) {
            is AnyOfType -> types.any { it.isMissable() }
            is AnyType, is MissingType -> true
            else -> false
        }

    /**
     * Type is optional if it is Any, or Missing, or an AnyOfType that contains Any or Missing type
     */
    private fun isOptional(): Boolean =
        when (this) {
            is AnyType, MissingType -> true // Any includes Missing type
            is AnyOfType -> types.any { it.isOptional() }
            else -> false
        }

    /**
     * Returns a list of all possible types that are in the domain of the current type.
     *
     * - For non-union types, this returns list with a single entry.
     * - For union types, this returns the complete list of all types in the union.  (Nested unions are flattened.)
     */
    public abstract val allTypes: List<StaticType>

    public abstract fun flatten(): StaticType
}

/**
 * Represents a StaticType of type `ANY`. This has no runtime component.
 */
// TODO: Remove `NULL` from here. This affects inference as operations (especially NAry) can produce
//  `NULL` or `MISSING` depending on a null propagation or an incorrect argument.
public data class AnyType(override val metas: Map<String, Any> = mapOf()) : StaticType() {
    /**
     * Converts this into an [AnyOfType] representation. This method is helpful in inference when
     * it wants to iterate over all possible types of an expression.
     */
    public fun toAnyOfType(): AnyOfType = AnyOfType(ALL_TYPES.toSet())

    override fun flatten(): StaticType = this

    override fun toString(): String = "any"

    override val allTypes: List<StaticType>
        get() = ALL_TYPES
}

/**
 * Represents a [StaticType] that is type of a single [StaticType].
 */
public sealed class SingleType : StaticType() {
    override fun flatten(): StaticType = this
}

/**
 * Exception thrown when [StaticTypeUtils.isInstance](https://javadoc.io/doc/org.partiql/partiql-lang-kotlin/latest/partiql-lang/org.partiql.lang.types/-static-type-utils/is-instance.html)
 * cannot perform a type check because of a temporarily unimplemented code path.
 *
 * This exception type needs to exist so that the callers can catch it explicitly and disambiguate from the more
 * generic [NotImplementedError].
 */
public class UnsupportedTypeCheckException(message: String) : RuntimeException(message)

/**
 * Represents collection types i.e list, bag and sexp.
 */
public sealed class CollectionType : SingleType() {
    public abstract val elementType: StaticType
    public abstract val constraints: Set<CollectionConstraint>

    internal fun validateCollectionConstraints() {
        if (elementType !is StructType && constraints.any { it is TupleCollectionConstraint }) {
            throw UnsupportedTypeConstraint("Only collection of tuples can have tuple constraints")
        }
    }
}

/**
 * Exception thrown when a [StaticType] is initialized with an unsupported type constraint.
 */
public class UnsupportedTypeConstraint(message: String) : Exception(message)

// Single types from ExprValueType.

/**
 * Represents "null" type from the runtime types.
 *
 * This is not a singleton since there may be more that one representation of a Null type (each with different metas)
 */
public data class NullType(override val metas: Map<String, Any> = mapOf()) : SingleType() {
    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "null"
}

/**
 * Represents missing type from the runtime types.
 *
 * This is a singleton unlike the rest of the types as there cannot be
 * more that one representations of a missing type.
 */
public object MissingType : SingleType() {
    override val metas: Map<String, Any> = mapOf()

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "missing"
}

public data class BoolType(override val metas: Map<String, Any> = mapOf()) : SingleType() {
    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "bool"
}

public data class IntType(
    val rangeConstraint: IntRangeConstraint = IntRangeConstraint.UNCONSTRAINED,
    override val metas: Map<String, Any> = mapOf()
) : SingleType() {

    public enum class IntRangeConstraint(public val numBytes: Int, public val validRange: LongRange) {
        /** SQL's SMALLINT (2 Bytes) */
        SHORT(2, Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()),

        /** SQL's INT4 (4 bytes) */
        INT4(4, Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()),

        /** SQL's BIGINT (8 bytes) */
        LONG(8, Long.MIN_VALUE..Long.MAX_VALUE),

        /**
         * An "unconstrained" integer with an implementation-defined constraint that happens to be 8 bytes for this
         * implementation.
         */
        UNCONSTRAINED(8, Long.MIN_VALUE..Long.MAX_VALUE),
    }

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String =
        when (rangeConstraint) {
            IntRangeConstraint.UNCONSTRAINED -> "int"
            else -> "int${rangeConstraint.numBytes}"
        }
}

public data class FloatType(override val metas: Map<String, Any> = mapOf()) : SingleType() {
    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "float"
}

public data class DecimalType(
    val precisionScaleConstraint: PrecisionScaleConstraint = PrecisionScaleConstraint.Unconstrained,
    override val metas: Map<String, Any> = mapOf()
) : SingleType() {

    public sealed class PrecisionScaleConstraint {
        public abstract fun matches(d: BigDecimal): Boolean

        // TODO: Do we need unconstrained precision and scale? What's our limit?
        public object Unconstrained : PrecisionScaleConstraint() {
            override fun matches(d: BigDecimal): Boolean = true
        }

        public data class Constrained(val precision: Int, val scale: Int = 0) : PrecisionScaleConstraint() {
            override fun matches(d: BigDecimal): Boolean {
                // check scale
                val decimalPoint = if (d.scale() >= 0) d.scale() else 0
                if (decimalPoint > scale) {
                    return false
                }
                // check integer part
                val integerPart = d.setScale(0, RoundingMode.DOWN)
                val integerLength = if (integerPart.signum() != 0) integerPart.precision() - integerPart.scale() else 0
                // PartiQL precision semantics -> the maximum number of total digit (left of decimal place + right of decimal place)
                // PartiQL scale semantics -> the total number of digit after the decimal point.
                val expectedIntegerDigits = precision - scale

                return expectedIntegerDigits >= integerLength
            }
        }
    }

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "decimal"
}

public data class DateType(override val metas: Map<String, Any> = mapOf()) : SingleType() {
    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "date"
}

public data class TimeType(
    val precision: Int? = null,
    val withTimeZone: Boolean = false,
    override val metas: Map<String, Any> = mapOf()
) : SingleType() {
    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = when (withTimeZone) {
        true -> "time with time zone"
        false -> "time"
    }
}

public class TimestampType internal constructor(internal val timestampType: PartiQLTimestampType = PartiQLTimestampType()) : SingleType() {

    override val metas: Map<String, Any> = timestampType.metas
    override val allTypes: List<StaticType> = listOf(this)
    @PartiQLTimestampExperimental
    public val precision: Int? = timestampType.precision
    @PartiQLTimestampExperimental
    public val withTimeZone: Boolean = timestampType.withTimeZone

    // Preserve the original semantics (ion timestamp). An arbitrary timestamp type with time zone.
    public constructor(metas: Map<String, Any> = mapOf()) : this(PartiQLTimestampType(null, true, metas))

    /**
     * @param precision specifies the number of digits in the fractional seconds.
     *  If omitted, the default value is 6 as specified in the SQL spec.
     *  If null, then the timestamp can have arbitrary constraint.
     * @param withTimeZone If true, then the underlying data must be associated with either unknown timezone(-00:00) or an UTC offset.
     * @param metas Metadata associated with the timestamp type.
     */
    @PartiQLTimestampExperimental
    public constructor(
        precision: Int? = 6,
        withTimeZone: Boolean = false,
        metas: Map<String, Any> = mapOf()
    ) : this(PartiQLTimestampType(precision, withTimeZone, metas))

    override fun hashCode(): Int {
        return timestampType.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is TimestampType) {
            false
        } else {
            this.timestampType == other.timestampType
        }
    }

    @PartiQLTimestampExperimental
    public fun copy(precision: Int? = this.precision, withTimeZone: Boolean = this.withTimeZone, metas: Map<String, Any>): TimestampType =
        TimestampType(PartiQLTimestampType(precision, withTimeZone, metas))

    // not propagate the opt-in requirement for copy method.
    @OptIn(PartiQLTimestampExperimental::class)
    public fun copy(metas: Map<String, Any>): TimestampType =
        TimestampType(PartiQLTimestampType(this.precision, this.withTimeZone, metas))

    // This function is preserved to avoid behavior change.
    // The legacy timestamp type is actually an arbitrary precision timestamp with time zone
    // To string should print something like "timestamp(..) with time zone". which is a breaking change.
    override fun toString(): String = "timestamp"

    @PartiQLTimestampExperimental
    public fun toStringExperimental(): String = when (withTimeZone) {
        true -> precision?.let { "timestamp($it) with time zone" } ?: TODO("Syntax for arbitrary timestamp is not yet supported")
        false -> precision?.let { "timestamp($it)" } ?: TODO("Syntax for arbitrary timestamp is not yet supported")
    }
}

// TODO: Delete this workaround when https://github.com/partiql/partiql-docs/blob/datetime/RFCs/0047-datetime-data-type.md is approved.
internal data class PartiQLTimestampType(
    val precision: Int? = 6,
    val withTimeZone: Boolean = false,
    val metas: Map<String, Any> = mapOf()
)

public data class SymbolType(override val metas: Map<String, Any> = mapOf()) : SingleType() {
    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "symbol"
}

public data class StringType(
    val lengthConstraint: StringLengthConstraint = StringLengthConstraint.Unconstrained,
    override val metas: Map<String, Any> = mapOf()
) : SingleType() {

    public sealed class StringLengthConstraint {
        public object Unconstrained : StringLengthConstraint()
        public data class Constrained(val length: NumberConstraint) : StringLengthConstraint()
    }

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "string"

    public constructor(numberConstraint: NumberConstraint) : this(StringLengthConstraint.Constrained(numberConstraint))
}

public data class BlobType(override val metas: Map<String, Any> = mapOf()) : SingleType() {
    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "blob"
}

public data class ClobType(override val metas: Map<String, Any> = mapOf()) : SingleType() {
    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "clob"
}

/**
 * @param [elementType] type of element within the list.
 */
public data class ListType(
    override val elementType: StaticType = ANY,
    override val metas: Map<String, Any> = mapOf(),
    override val constraints: Set<CollectionConstraint> = setOf()
) : CollectionType() {

    init {
        validateCollectionConstraints()
    }
    override fun flatten(): StaticType = this

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "list($elementType)"
}

/**
 * @param [elementType] type of element within the s-exp.
 */
public data class SexpType(
    override val elementType: StaticType = ANY,
    override val metas: Map<String, Any> = mapOf(),
    override val constraints: Set<CollectionConstraint> = setOf(),
) : CollectionType() {
    init {
        validateCollectionConstraints()
    }
    override fun flatten(): StaticType = this

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "sexp($elementType)"
}

/**
 * @param [elementType] type of element within the bag.
 */
public data class BagType(
    override val elementType: StaticType = ANY,
    override val metas: Map<String, Any> = mapOf(),
    override val constraints: Set<CollectionConstraint> = setOf(),
) : CollectionType() {
    init {
        this.validateCollectionConstraints()
    }
    override fun flatten(): StaticType = this

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "bag($elementType)"
}

/**
 * Describes a PartiQL Struct.
 *
 * @param fields the key-value pairs of the struct
 * @param contentClosed when true, denotes that no other attributes may be present
 * @param primaryKeyFields fields designated as primary keys
 * @param constraints set of constraints applied to the Struct
 * @param metas meta-data
 */
public data class StructType(
    val fields: List<Field> = listOf(),
    // `TupleConstraint` already has `Open` constraint which overlaps with `contentClosed`.
    // In addition, `primaryKeyFields` must not exist on the `StructType` as `PrimaryKey`
    // is a property of collection of tuples. As we have plans to define PartiQL types in
    // more details it's foreseeable to have an refactor of our types in future and have a
    // new definition of this type as `Tuple`. See the following issue for more details:
    // https://github.com/partiql/partiql-spec/issues/49
    // TODO remove `contentClosed` and `primaryKeyFields` if after finalizing our type specification we're
    // still going with `StructType`.
    val contentClosed: Boolean = false,
    val primaryKeyFields: List<String> = listOf(),
    val constraints: Set<TupleConstraint> = setOf(),
    override val metas: Map<String, Any> = mapOf(),
) : SingleType() {

    public constructor(
        fields: Map<String, StaticType>,
        contentClosed: Boolean = false,
        primaryKeyFields: List<String> = listOf(),
        constraints: Set<TupleConstraint> = setOf(),
        metas: Map<String, Any> = mapOf(),
    ) : this(
        fields.map { Field(it.key, it.value) },
        contentClosed,
        primaryKeyFields,
        constraints,
        metas
    )

    /**
     * The key-value pair of a StructType, where the key represents the name of the field and the value represents
     * its [StaticType]. Note: multiple [Field]s within a [StructType] may contain the same [key], and therefore,
     * multiple same-named keys may refer to distinct [StaticType]s. To determine the [StaticType]
     * of a reference to a field, especially in the case of duplicates, it depends on the ordering of the [StructType]
     * (denoted by the presence of [TupleConstraint.Ordered] in the [StructType.constraints]).
     * - If ORDERED: the PartiQL specification says to grab the first encountered matching field.
     * - If UNORDERED: it is implementation-defined. However, gather all possible types, merge them using [AnyOfType].
     */
    public data class Field(
        val key: String,
        val value: StaticType
    )

    override fun flatten(): StaticType = this

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String {
        val firstSeveral = fields.take(3).joinToString { "${it.key}: ${it.value}" }
        return when {
            fields.size <= 3 -> "struct($firstSeveral, $constraints)"
            else -> "struct($firstSeveral, ... and ${fields.size - 3} other field(s), $constraints)"
        }
    }
}

public data class GraphType(
    override val metas: Map<String, Any> = mapOf()
) : SingleType() {

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "graph"
}

/**
 * Represents a [StaticType] that's defined by the union of multiple [StaticType]s.
 */
public data class AnyOfType(val types: Set<StaticType>, override val metas: Map<String, Any> = mapOf()) : StaticType() {
    /**
     * Flattens a union type by traversing the types and recursively bubbling up the underlying union types.
     *
     * If union type ends up having just one type in it, then that type is returned.
     */
    override fun flatten(): StaticType = this.copy(
        types = this.types.flatMap {
            when (it) {
                is SingleType -> listOf(it)
                is AnyType -> listOf(it)
                is AnyOfType -> it.types
            }
        }.toSet()
    ).let {
        when {
            it.types.size == 1 -> it.types.first()
            it.types.filterIsInstance<AnyOfType>().any() -> it.flatten()
            else -> it
        }
    }

    override fun toString(): String =
        when (val flattenedType = flatten()) {
            is AnyOfType -> {
                val unionedTypes = flattenedType.types
                when (unionedTypes.size) {
                    0 -> "\$null"
                    1 -> unionedTypes.first().toString()
                    else -> {
                        val types = unionedTypes.joinToString { it.toString() }
                        "union($types)"
                    }
                }
            }
            else -> flattenedType.toString()
        }

    override val allTypes: List<StaticType>
        get() = this.types.map { it.flatten() }
}

public sealed class NumberConstraint {

    /** Returns true of [num] matches the constraint. */
    public abstract fun matches(num: Int): Boolean

    public abstract val value: Int

    public data class Equals(override val value: Int) : NumberConstraint() {
        override fun matches(num: Int): Boolean = value == num
    }

    public data class UpTo(override val value: Int) : NumberConstraint() {
        override fun matches(num: Int): Boolean = value >= num
    }
}

/**
 * Represents Tuple constraints; this is still experimental.
 * and subject to change upon finalization of the following:
 * - https://github.com/partiql/partiql-spec/issues/49
 * - https://github.com/partiql/partiql-docs/issues/37
 */
public sealed class TupleConstraint {
    public data class UniqueAttrs(val value: Boolean) : TupleConstraint()
    public data class Open(val value: Boolean) : TupleConstraint()

    /**
     * The presence of the [Ordered] on a [StructType] represents that the [StructType] is ORDERED. The absence of
     * this constrain represents the opposite -- AKA that the [StructType] is UNORDERED
     */
    public object Ordered : TupleConstraint() {

        override fun toString(): String = "Ordered"
    }
}

/**
 * An Interface for constraints that are only applicable to collection of tuples, e.g. `PrimaryKey`.
 */
public interface TupleCollectionConstraint

/**
 * Represents Collection constraints; this is still experimental.
 * and subject to change upon finalization of the following:
 * - https://github.com/partiql/partiql-spec/issues/49
 * - https://github.com/partiql/partiql-docs/issues/37
 */
public sealed class CollectionConstraint {
    public data class PrimaryKey(val keys: Set<String>) : TupleCollectionConstraint, CollectionConstraint()
    public data class PartitionKey(val keys: Set<String>) : TupleCollectionConstraint, CollectionConstraint()
}

internal fun StaticType.isNullOrMissing(): Boolean = (this is NullType || this is MissingType)
internal fun StaticType.isNumeric(): Boolean = (this is IntType || this is FloatType || this is DecimalType)
internal fun StaticType.isText(): Boolean = (this is SymbolType || this is StringType)
internal fun StaticType.isUnknown(): Boolean = (this.isNullOrMissing() || this == StaticType.NULL_OR_MISSING)
