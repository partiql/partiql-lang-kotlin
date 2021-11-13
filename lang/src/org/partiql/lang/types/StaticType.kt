/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package org.partiql.lang.types

import org.partiql.lang.ast.passes.inference.isLob
import org.partiql.lang.ast.passes.inference.isNumeric
import org.partiql.lang.ast.passes.inference.isText
import org.partiql.lang.ast.passes.inference.isUnknown
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.name
import org.partiql.lang.eval.numberValue
import org.partiql.lang.eval.stringValue
import org.partiql.lang.eval.timeValue
import org.partiql.lang.util.utf8ByteLength
import java.math.BigDecimal

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
        @JvmField val DATE: DateType = DateType()
        @JvmField val TIME: TimeType = TimeType()
        @JvmField val TIMESTAMP: TimestampType = TimestampType()
        @JvmField val SYMBOL: SymbolType = SymbolType()
        @JvmField val STRING: StringType = StringType()
        @JvmField val CLOB: ClobType = ClobType()
        @JvmField val BLOB: BlobType = BlobType()
        @JvmField val LIST: ListType = ListType()
        @JvmField val SEXP: SexpType = SexpType()
        @JvmField val STRUCT: StructType = StructType()
        @JvmField val BAG: BagType = BagType()

        @JvmStatic
        fun fromExprValueType(exprValueType: ExprValueType): StaticType =
            when(exprValueType) {
                ExprValueType.MISSING -> MISSING
                ExprValueType.NULL -> NULL
                ExprValueType.BOOL -> BOOL
                ExprValueType.INT -> INT
                ExprValueType.FLOAT -> FLOAT
                ExprValueType.DECIMAL -> DECIMAL
                ExprValueType.DATE -> DATE
                ExprValueType.TIME -> TIME
                ExprValueType.TIMESTAMP -> TIMESTAMP
                ExprValueType.SYMBOL -> SYMBOL
                ExprValueType.STRING -> STRING
                ExprValueType.CLOB -> CLOB
                ExprValueType.BLOB -> BLOB
                ExprValueType.LIST -> LIST
                ExprValueType.SEXP -> SEXP
                ExprValueType.STRUCT -> STRUCT
                ExprValueType.BAG -> BAG
            }

        @JvmStatic
        fun fromExprValue(exprValue: ExprValue): StaticType =
            when(exprValue.type) {
                ExprValueType.TIME -> {
                    val timeValue = exprValue.timeValue()
                    TimeType(precision = timeValue.precision, withTimeZone = timeValue.zoneOffset != null)
                }
                else -> fromExprValueType(exprValue.type)
            }

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
            BAG
        )
    }

    /**
     * Checks to see if the given [ExprValue] conforms to the current [StaticType].
     *
     * Throwing [UnsupportedTypeCheckException] is temporary while some classes derived from [StaticType]
     * do not fully implement this function and is thrown when the encounter situations that can't yet be checked.
     */
    @Throws(UnsupportedTypeCheckException::class)
    abstract fun isInstance(value: ExprValue): Boolean

    /**
     * Returns true if [other] [StaticType] is comparable to the current [StaticType]. Currently, two types are
     * comparable if
     *  - they are both numeric
     *  - they are both text
     *  - they are both lobs
     *  - they are both [SingleType]s with the same [SingleType.runtimeType]
     *  - one [StaticType] from current [StaticType]'s [allTypes] is comparable to a [StaticType] from [other]'s
     *   [allTypes]
     */
    abstract fun isComparableTo(other: StaticType): Boolean

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

    /**
     * Checks if this subtype of the given [StaticType].
     *
     * A [StaticType] is subtype of another iff if its type domain in equal or smaller than the given type.
     */
    fun isSubTypeOf(otherType: StaticType): Boolean = this.typeDomain.isNotEmpty() && otherType.typeDomain.containsAll(this.typeDomain)

    abstract val metas: Map<String, Any>

    abstract val typeDomain: Set<ExprValueType>

    /**
     * Convenience method to copy over StaticType to a new instance with given `metas`
     * MissingType is a singleton and there can only be one representation for it
     * i.e. you cannot have two instances of MissingType with different metas.
     */
    internal fun withMetas(metas: Map<String, Any>): StaticType =
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
    internal fun isNullable(): Boolean =
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
    internal abstract val allTypes: List<StaticType>

    abstract fun flatten(): StaticType
}

/**
 * Represents a StaticType of type `ANY`. This has no runtime component.
 */
// TODO: Remove `NULL` from here. This affects inference as operations (especially NAry) can produce
//  `NULL` or `MISSING` depending on a null propagation or an incorrect argument.
data class AnyType(override val metas: Map<String, Any> = mapOf()): StaticType() {
    // AnyType encompasses all PartiQL types (including Null type)
    override fun isInstance(value: ExprValue): Boolean = true
    override fun isComparableTo(other: StaticType): Boolean = true

    override val typeDomain: Set<ExprValueType>
        get() = enumValues<ExprValueType>().toSet()

    /**
     * Converts this into an [AnyOfType] representation. This method is helpful in inference when
     * it wants to iterate over all possible types of an expression.
     */
    fun toAnyOfType() = AnyOfType(typeDomain.map { fromExprValueType(it) }.toSet())

    override fun flatten(): StaticType = this

    override fun toString(): String = "any"

    override val allTypes: List<StaticType>
        get() = ALL_TYPES
}

/**
 * Represents a [StaticType] that is type of a single [ExprValueType].
 */
sealed class SingleType:  StaticType() {
    abstract val runtimeType: ExprValueType
    override val typeDomain: Set<ExprValueType>
        get() = setOf(runtimeType)

    override fun flatten(): StaticType = this

    /**
     * For [SingleType], provides a default implementation of [isInstance] that returns true
     * if the [ExprValueType] of [value] is the same as the [runtimeType].
     *
     * That is appropriate for all [SingleType]-derived classes that do not have specific constraints but this must be
     * overridden in any [SingleType]s that have constraints such as [IntType], [StringType] and [DecimalType].
     * [NullType] also has custom implementation of this function to support `MISSING IS NULL` semantics.
     */
    override fun isInstance(value: ExprValue): Boolean = runtimeType == value.type

    override fun isComparableTo(other: StaticType): Boolean {
        if (this.isUnknown() || other.isUnknown()) {
            return true
        }
        return when (other) {
            is SingleType -> (this.isNumeric() && other.isNumeric()) ||
                (this.isText() && other.isText()) ||
                (this.isLob() && other.isLob()) ||
                (this.runtimeType == other.runtimeType)
            is AnyType -> true
            is AnyOfType -> other.allTypes.filter { !it.isUnknown() }.any { this.isComparableTo(it) }
        }
    }
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
    abstract val elementType: StaticType


    override fun isInstance(value: ExprValue): Boolean {
        if(!super.isInstance(value)) return false

        return when (this.elementType) {
            StaticType.ANY -> true // no need to check every element if the elementType is ANY.

            else -> value.all { this.elementType.isInstance(it) }
        }
    }
}

// Single types from ExprValueType.

/**
 * Represents "null" type from the runtime types.
 *
 * This is not a singleton since there may be more that one representation of a Null type (each with different metas)
 */
data class NullType(override val metas: Map<String, Any> = mapOf()) : SingleType() {
    override val runtimeType: ExprValueType
        get() = ExprValueType.NULL

    override fun isInstance(value: ExprValue): Boolean =
        value.type == ExprValueType.MISSING || value.type == ExprValueType.NULL

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
    override val runtimeType: ExprValueType
        get() = ExprValueType.MISSING

    override val metas: Map<String, Any> = mapOf()

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "missing"

}

data class BoolType(override val metas: Map<String, Any> = mapOf()): SingleType() {
    override val runtimeType: ExprValueType
        get() = ExprValueType.BOOL

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "bool"
}

data class IntType(
    val rangeConstraint: IntRangeConstraint = IntRangeConstraint.UNCONSTRAINED,
    override val metas: Map<String, Any> = mapOf()
): SingleType() {

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

    override val runtimeType: ExprValueType
        get() = ExprValueType.INT

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String =
        when (rangeConstraint) {
            IntRangeConstraint.UNCONSTRAINED -> "int"
            else -> "int${rangeConstraint.numBytes}"
        }

    override fun isInstance(value: ExprValue): Boolean {
        if(value.type != ExprValueType.INT) {
            return false
        }

        val longValue = value.numberValue().toLong()

        return rangeConstraint.validRange.contains(longValue)
    }

}

data class FloatType(override val metas: Map<String, Any> = mapOf()): SingleType() {
    override val runtimeType: ExprValueType
        get() = ExprValueType.FLOAT

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "float"
}

data class DecimalType(
    val precisionScaleConstraint: PrecisionScaleConstraint = PrecisionScaleConstraint.Unconstrained,
    override val metas: Map<String, Any> = mapOf()
) : SingleType() {

    sealed class PrecisionScaleConstraint {
        abstract fun matches(d: BigDecimal): Boolean

        // TODO: Do we need unconstrained precision and scale? What's our limit?
        object Unconstrained : PrecisionScaleConstraint() {
            override fun matches(d: BigDecimal): Boolean = true
        }

        data class Constrained(val precision: Int, val scale: Int = 0): PrecisionScaleConstraint() {
            override fun matches(d: BigDecimal): Boolean {
                val dv = d.stripTrailingZeros()

                val integerDigits = dv.precision() - dv.scale()
                val expectedIntegerDigits = precision - scale
                return integerDigits <= expectedIntegerDigits && dv.scale() <= scale
            }
        }

    }

    override val runtimeType: ExprValueType
        get() = ExprValueType.DECIMAL

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "decimal"

    override fun isInstance(value: ExprValue): Boolean =
        when(value.type) {
            ExprValueType.DECIMAL -> precisionScaleConstraint.matches(value.scalar.numberValue() as BigDecimal)
            else -> false
        }
}

data class DateType(override val metas: Map<String, Any> = mapOf()): SingleType() {
    override val runtimeType: ExprValueType
        get() = ExprValueType.DATE

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "date"
}

data class TimeType(
    val precision: Int? = null,
    val withTimeZone: Boolean = false,
    override val metas: Map<String, Any> = mapOf()
): SingleType() {
    override val runtimeType: ExprValueType
        get() = ExprValueType.TIME

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = when (withTimeZone) {
        true -> "time with time zone"
        false -> "time"
    }
}

data class TimestampType(override val metas: Map<String, Any> = mapOf()): SingleType() {
    override val runtimeType: ExprValueType
        get() = ExprValueType.TIMESTAMP

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "timestamp"
}

data class SymbolType(override val metas: Map<String, Any> = mapOf()): SingleType() {
    override val runtimeType: ExprValueType
        get() = ExprValueType.SYMBOL

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "symbol"
}

data class StringType(
    val lengthConstraint: StringLengthConstraint = StringLengthConstraint.Unconstrained,
    override val metas: Map<String, Any> = mapOf()
) : SingleType() {

    sealed class StringLengthConstraint{
        /** Returns true if the code point count of [value] falls within the constraints.  */
        abstract fun matches(value: ExprValue): Boolean

        object Unconstrained : StringLengthConstraint() {
            override fun matches(value: ExprValue): Boolean = true
        }

        data class Constrained(val length: NumberConstraint): StringLengthConstraint() {
            override fun matches(value: ExprValue): Boolean {
                val str = value.scalar.stringValue()
                    ?: error("value.scalar.stringValue() unexpectedly returned null")
                return length.matches(str.codePointCount(0, str.length))
            }
        }

        data class ByteLengthConstrained(val byteLength: NumberConstraint): StringLengthConstraint() {
            override fun matches(value: ExprValue): Boolean {
                val str = value.scalar.stringValue()
                    ?: error("value.scalar.stringValue() unexpectedly returned null")
                return byteLength.matches(str.utf8ByteLength)
            }
        }
    }

    override val runtimeType: ExprValueType
        get() = ExprValueType.STRING

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "string"

    override fun isInstance(value: ExprValue): Boolean =
        when(value.type) {
            ExprValueType.STRING -> lengthConstraint.matches(value)
            else -> false
        }

    internal constructor(numberConstraint: NumberConstraint) : this(StringLengthConstraint.Constrained(numberConstraint))
}

data class BlobType(override val metas: Map<String, Any> = mapOf()): SingleType() {
    override val runtimeType: ExprValueType
        get() = ExprValueType.BLOB

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "blob"
}

data class ClobType(override val metas: Map<String, Any> = mapOf()): SingleType() {
    override val runtimeType: ExprValueType
        get() = ExprValueType.CLOB

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "clob"
}

/**
 * @param [elementType] type of element within the list.
 */
data class ListType(
    override val elementType: StaticType = ANY,
    override val metas: Map<String, Any> = mapOf()
): CollectionType() {

    override val runtimeType: ExprValueType
        get() = ExprValueType.LIST
    override fun flatten(): StaticType = this

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "list(${elementType})"
}

/**
 * @param [elementType] type of element within the s-exp.
 */
data class SexpType(
    override val elementType: StaticType = ANY,
    override val metas: Map<String, Any> = mapOf()
): CollectionType() {
    override val runtimeType: ExprValueType
        get() = ExprValueType.SEXP
    override fun flatten(): StaticType = this

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "sexp(${elementType})"
}

/**
 * @param [elementType] type of element within the bag.
 */
data class BagType(
    override val elementType: StaticType = ANY,
    override val metas: Map<String, Any> = mapOf()
): CollectionType() {
    override val runtimeType: ExprValueType
        get() = ExprValueType.BAG

    override fun flatten(): StaticType = this

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String = "bag(${elementType})"
}

data class StructType(
    val fields: Map<String, StaticType> = mapOf(),
    val contentClosed: Boolean = false,
    override val metas: Map<String, Any> = mapOf()
) : SingleType() {
    override val runtimeType: ExprValueType
        get() = ExprValueType.STRUCT

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

    /**
     * By far, structs have the most complicated logic behind their instance check.
     *
     * This method returns true if:
     *
     * - All fields are instance of the correct type (as identified by [fields]).
     * - If [contentClosed] and there are no fields other than those listed in [fields].
     *
     * Duplicate fields are supported, but all instances of a field with the same name
     * must match the type specified in [fields].
     *
     * If the struct contains any non-text key, this automatically means that the struct is not an instance of this
     * [StructType].  We do not even have the ability to model that with Ion/Ion Schema anyway.
     */
    override fun isInstance(value: ExprValue): Boolean = when {
        fields.isEmpty() && !contentClosed -> value.type == ExprValueType.STRUCT
        else -> {
            if (value.type != ExprValueType.STRUCT) {
                false
            } else {
                // build a multi-map of fields in the struct.
                val scratchPad = HashMap<String, MutableList<ExprValue>>().also { map ->
                    value.forEach { v ->
                        // return false early if the struct key is not a string or symbol.
                        val structKey = v.name.takeIf { it?.type?.isText ?: false } ?: return false
                        map.getOrPut(structKey.stringValue()) { ArrayList() }.add(v)
                    }
                }

                // now go thru each of the [fields] and remove those that are valid
                fields.forEach { (fieldName, fieldType) ->
                    val fieldValues = scratchPad.remove(fieldName)

                    // Field was *not* present
                    if (fieldValues == null) {
                        // if field was required, the struct is not an instance of this [StructType]
                        if (!fieldType.isOptional()) {
                            return false
                        }
                        // else there is no violation, keep checking other fields
                    } else {
                        // in the case of multiple fields with the same name, all values must match
                        if (!fieldValues.all { fieldType.isInstance(it) }) {
                            return false
                        }
                        // else there is no violation, keep checking other fields
                    }
                }

                // if we reach this point, we didn't find any fields that do not comply with their final types.

                // If no fields remain [value] is an instance of this [StaticType]
                if (scratchPad.none()) {
                    true
                } else {
                    // There are some fields left over, so we only need to check if we are closedContent or not.
                    !contentClosed
                }
            }
        }
    }

}

/**
 * Represents a [StaticType] that's defined by the union of multiple [StaticType]s.
 */
data class AnyOfType(val types: Set<StaticType>, override val metas: Map<String, Any> = mapOf()) : StaticType() {
    override val typeDomain = types.flatMap { it.typeDomain }.toSet()

    /**
     * Flattens a union type by traversing the types and recursively bubbling up the underlying union types.
     *
     * If union type ends up having just one type in it, then that type is returned.
     */
    override fun flatten(): StaticType = this.copy(types = this.types.flatMap {
        when (it) {
            is SingleType -> listOf(it)
            is AnyType -> listOf(it)
            is AnyOfType -> it.types
        }
    }.toSet()).let {
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

    /**
     * Returns true if the value matches any of the [types].
     */
    override fun isInstance(value: ExprValue): Boolean = types.any { it.isInstance(value) }

    override fun isComparableTo(other: StaticType): Boolean {
        if (this.isUnknown() || other.isUnknown()) {
            return true
        }

        val typesA = this.allTypes.filter { !it.isUnknown() }
        val typesB = other.allTypes.filter { !it.isUnknown() }

        typesA.forEach { tA ->
            typesB.forEach { tB ->
                if (tA.isComparableTo(tB)) {
                    return true
                }
            }
        }
        return false
    }
}

sealed class NumberConstraint {

    /** Returns true of [num] matches the constraint. */
    abstract fun matches(num: Int): Boolean

    abstract val value: Int

    data class Equals(override val value: Int): NumberConstraint() {
        override fun matches(num: Int): Boolean = value == num
    }

    data class UpTo(override val value: Int): NumberConstraint() {
        override fun matches(num: Int): Boolean = value >= num
    }
}
