/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package org.partiql.types

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Represents static types available in the language and ways to extends them to create new types.
 */
sealed class StaticType {
    companion object {

        /**
         * varargs variant, folds [types] into a [Set]
         * The usage of LinkedHashSet is to preserve the order of `types` to ensure behavior is consistent in our tests
         */
        @JvmStatic
        fun unionOf(vararg types: StaticType, metas: Map<String, Any> = mapOf()) =
            unionOf(types.toSet(), metas)

        /**
         * Creates a new [StaticType] as a union of the passed [types]. The values typed by the returned type
         * are defined as the union of all values typed as [types]
         *
         * @param types [StaticType] to be unioned.
         * @return [StaticType] representing the union of [types]
         */
        @JvmStatic
        fun unionOf(types: Set<StaticType>, metas: Map<String, Any> = mapOf()): StaticType = AnyOfType(types, metas)

        // TODO consider making these into an enumeration...

        // Convenient enums to create a bare bones instance of StaticType
        @JvmField val MISSING: MissingType = MissingType
        @JvmField val NULL: NullType = NullType()
        @JvmField val ANY: AnyType = AnyType()
        @JvmField val NULL_OR_MISSING: StaticType = unionOf(NULL, MISSING)
        @JvmField val BOOL: BoolType = BoolType()
        @JvmField val INT2: IntType = IntType(IntType.IntRangeConstraint.SHORT)
        @JvmField val INT4: IntType = IntType(IntType.IntRangeConstraint.INT4)
        @JvmField val INT8: IntType = IntType(IntType.IntRangeConstraint.LONG)
        @JvmField val INT: IntType = IntType(IntType.IntRangeConstraint.UNCONSTRAINED)
        @JvmField val FLOAT: FloatType = FloatType()
        @JvmField val DECIMAL: DecimalType = DecimalType()
        @JvmField val NUMERIC: StaticType = unionOf(INT2, INT4, INT8, INT, FLOAT, DECIMAL)
        @JvmField val DATE: DateType = DateType()
        @JvmField val TIME: TimeType = TimeType()
        @JvmField val TIMESTAMP: TimestampType = TimestampType()
        @JvmField val SYMBOL: SymbolType = SymbolType()
        @JvmField val STRING: StringType = StringType()
        @JvmField val TEXT: StaticType = unionOf(SYMBOL, STRING)
        @JvmField val CLOB: ClobType = ClobType()
        @JvmField val BLOB: BlobType = BlobType()
        @JvmField val LIST: ListType = ListType()
        @JvmField val SEXP: SexpType = SexpType()
        @JvmField val STRUCT: StructType = StructType()
        @JvmField val BAG: BagType = BagType()

        /** All the StaticTypes, except for `ANY`. */
        @JvmStatic
        val ALL_TYPES = listOf(
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
        )
    }

    /**
     *  Returns a nullable version of the current [StaticType].
     *
     *  If it already nullable, returns the original type.
     */
    fun asNullable() =
        when {
            this.isNullable() -> this
            else -> unionOf(this, NULL).flatten()
        }

    /**
     *  Returns an optional version of the current [StaticType].
     *
     *  If it already optional, returns the original type.
     */
    fun asOptional() =
        when {
            this.isOptional() -> this
            else -> unionOf(this, MISSING).flatten()
        }

    abstract val metas: Map<String, Any>

    /**
     * Convenience method to copy over StaticType to a new instance with given `metas`
     * MissingType is a singleton and there can only be one representation for it
     * i.e. you cannot have two instances of MissingType with different metas.
     */
    fun withMetas(metas: Map<String, Any>): StaticType =
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
        }

    /**
     * Type is nullable if it is of Null type or is an AnyOfType that contains a Null type
     */
    fun isNullable(): Boolean =
        when (this) {
            is AnyOfType -> types.any { it.isNullable() }
            is AnyType, is NullType -> true
            else -> false
        }

    /**
     * Type is optional if it is Any, or Missing, or an AnyOfType that contains Any or Missing type
     */
    internal fun isOptional(): Boolean =
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

    abstract fun flatten(): StaticType
}

/**
 * Represents a StaticType of type `ANY`. This has no runtime component.
 */
// TODO: Remove `NULL` from here. This affects inference as operations (especially NAry) can produce
//  `NULL` or `MISSING` depending on a null propagation or an incorrect argument.
data class AnyType(
    override val metas: Map<String, Any> = mapOf()
) : StaticType() {
    /**
     * Converts this into an [AnyOfType] representation. This method is helpful in inference when
     * it wants to iterate over all possible types of an expression.
     */
    fun toAnyOfType() = AnyOfType(ALL_TYPES.toSet())

    override fun flatten(): StaticType = this

    override fun toString(): String = "any"

    override val allTypes: List<StaticType>
        get() = ALL_TYPES
}

/**
 * Represents a [StaticType] that is type of a single [ExprValueType].
 */
sealed class SingleType : StaticType() {
    override fun flatten(): StaticType = this
}

/**
 * Exception thrown when [StaticType.isInstance] cannot perform a type check because of a temporarily unimplemented
 * code path.
 *
 * This exception type needs to exist so that the callers can catch it explicitly and disambiguate from the more
 * generic [NotImplementedError].
 */
class UnsupportedTypeCheckException(message: String) : RuntimeException(message)

/**
 * Represents collection types i.e list, bag and sexp.
 */
sealed class CollectionType : SingleType() {
    abstract val elementType : StaticType
}

// Single types from ExprValueType.

/**
 * Represents "null" type from the runtime types.
 *
 * This is not a singleton since there may be more that one representation of a Null type (each with different metas)
 */
data class NullType(
    override val metas: Map<String, Any> = mapOf(),
) : SingleType() {
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
object MissingType : SingleType() {
    override val metas: Map<String, Any> = mapOf()

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "missing"
}

data class BoolType(
    override val metas: Map<String, Any> = mapOf(),
) : SingleType() {
    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "bool"
}

data class IntType(
    val rangeConstraint: IntRangeConstraint = IntRangeConstraint.UNCONSTRAINED,
    override val metas: Map<String, Any> = mapOf(),
) : SingleType() {

    enum class IntRangeConstraint(val numBytes: Int, val validRange: LongRange) {
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

data class FloatType(
    override val metas: Map<String, Any> = mapOf(),
) : SingleType() {
    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "float"
}

data class DecimalType(
    val precisionScaleConstraint: PrecisionScaleConstraint = PrecisionScaleConstraint.Unconstrained,
    override val metas: Map<String, Any> = mapOf(),
) : SingleType() {

    sealed class PrecisionScaleConstraint {
        abstract fun matches(d: BigDecimal): Boolean

        // TODO: Do we need unconstrained precision and scale? What's our limit?
        object Unconstrained : PrecisionScaleConstraint() {
            override fun matches(d: BigDecimal): Boolean = true
        }

        data class Constrained(val precision: Int, val scale: Int = 0) : PrecisionScaleConstraint() {
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

data class DateType(
    override val metas: Map<String, Any> = mapOf(),
) : SingleType() {
    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "date"
}

data class TimeType(
    val precision: Int? = null,
    val withTimeZone: Boolean = false,
    override val metas: Map<String, Any> = mapOf(),
) : SingleType() {
    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = when (withTimeZone) {
        true -> "time with time zone"
        false -> "time"
    }
}

data class TimestampType(
    override val metas: Map<String, Any> = mapOf(),
) : SingleType() {
    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "timestamp"
}

data class SymbolType(
    override val metas: Map<String, Any> = mapOf(),
) : SingleType() {
    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "symbol"
}

data class StringType(
    val lengthConstraint: StringLengthConstraint = StringLengthConstraint.Unconstrained,
    override val metas: Map<String, Any> = mapOf(),
) : SingleType() {

    sealed class StringLengthConstraint {
        object Unconstrained : StringLengthConstraint()
        data class Constrained(val length: NumberConstraint) : StringLengthConstraint()
    }

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "string"

    constructor(numberConstraint: NumberConstraint) : this(StringLengthConstraint.Constrained(numberConstraint))
}

data class BlobType(
    override val metas: Map<String, Any> = mapOf(),
) : SingleType() {

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "blob"
}

data class ClobType(
    override val metas: Map<String, Any> = mapOf(),
) : SingleType() {
    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "clob"
}

/**
 * @param [elementType] type of element within the list.
 */
data class ListType(
    override val elementType: StaticType = ANY,
    override val metas: Map<String, Any> = mapOf(),
) : CollectionType() {

    override fun flatten(): StaticType = this

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "list($elementType)"
}

/**
 * @param [elementType] type of element within the s-exp.
 */
data class SexpType(
    override val elementType: StaticType = ANY,
    override val metas: Map<String, Any> = mapOf(),
) : CollectionType() {
    override fun flatten(): StaticType = this

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "sexp($elementType)"
}

/**
 * @param [elementType] type of element within the bag.
 */
data class BagType(
    override val elementType: StaticType = ANY,
    override val metas: Map<String, Any> = mapOf(),
) : CollectionType() {
    override fun flatten(): StaticType = this

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "bag($elementType)"
}

data class StructType(
    val fields: Map<String, StaticType> = mapOf(),
    val contentClosed: Boolean = false,
    val primaryKeyFields: List<String> = listOf(),
    val constraints: Set<TupleSchemaConstraint> = setOf(),
    override val metas: Map<String, Any> = mapOf(),
) : SingleType() {
    override fun flatten(): StaticType = this

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String {
        val entries = fields.entries
        val firstSeveral = entries.toList().take(3).joinToString { "${it.key}: ${it.value}" }
        return when {
            entries.size <= 3 -> "struct($firstSeveral)"
            else -> "struct($firstSeveral, ... and ${entries.size - 3} other field(s))"
        }
    }
}

/**
 * Represents a [StaticType] that's defined by the union of multiple [StaticType]s.
 */
data class AnyOfType(
    val types: Set<StaticType>,
    override val metas: Map<String, Any> = mapOf(),
) : StaticType() {
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

sealed class NumberConstraint {

    /** Returns true of [num] matches the constraint. */
    abstract fun matches(num: Int): Boolean

    abstract val value: Int

    data class Equals(override val value: Int) : NumberConstraint() {
        override fun matches(num: Int): Boolean = value == num
    }

    data class UpTo(override val value: Int) : NumberConstraint() {
        override fun matches(num: Int): Boolean = value >= num
    }
}

sealed class TupleSchemaConstraint {
    data class UniqueAttrs(val value: Boolean) : TupleSchemaConstraint()
    data class ClosedSchema(val value: Boolean) : TupleSchemaConstraint()
    data class PrimaryKey(val attrs: Set<String>) : TupleSchemaConstraint()
    data class PartitionKey(val attrs: Set<String>) : TupleSchemaConstraint()
}

internal fun StaticType.isNullOrMissing(): Boolean = (this is NullType || this is MissingType)
internal fun StaticType.isNumeric(): Boolean = (this is IntType || this is FloatType || this is DecimalType)
internal fun StaticType.isText(): Boolean = (this is SymbolType || this is StringType)
internal fun StaticType.isLob(): Boolean = (this is BlobType || this is ClobType)
internal fun StaticType.isUnknown(): Boolean = (this.isNullOrMissing() || this == StaticType.NULL_OR_MISSING)
