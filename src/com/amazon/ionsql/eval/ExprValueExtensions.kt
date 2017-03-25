/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ion.IonSystem
import com.amazon.ion.IonText
import com.amazon.ion.IonTimestamp
import com.amazon.ion.Timestamp
import com.amazon.ionsql.eval.ExprValueType.*
import com.amazon.ionsql.util.*
import java.math.BigDecimal
import java.util.*

/**
 * Wraps the given [ExprValue] with a delegate that provides the [OrderedBindNames] facet.
 */
fun ExprValue.orderedNamesValue(names: List<String>): ExprValue =
    object : ExprValue by this, OrderedBindNames {
        override val orderedNames = names
        override fun <T : Any?> asFacet(type: Class<T>?): T? =
            downcast(type) ?: this@orderedNamesValue.asFacet(type)
        override fun toString(): String = stringify()
    }

/** Wraps this [ExprValue] as a [Named] instance. */
fun ExprValue.asNamed(): Named = object : Named {
    override val name: ExprValue
        get() = this@asNamed
}

/** Binds the given name value as a [Named] facet delegate over this [ExprValue]. */
fun ExprValue.namedValue(nameValue: ExprValue): ExprValue = object : ExprValue by this, Named {
    override val name = nameValue
    override fun <T : Any?> asFacet(type: Class<T>?): T? =
        downcast(type) ?: this@namedValue.asFacet(type)
    override fun toString(): String = stringify()
}

/** Wraps this [ExprValue] in a delegate that always masks the [Named] facet. */
fun ExprValue.unnamedValue(): ExprValue = when (asFacet(Named::class.java)) {
    null -> this
    else -> object : ExprValue by this {
        override fun <T : Any?> asFacet(type: Class<T>?): T? =
            when (type) {
                // always mask the name facet
                Named::class.java -> null
                else -> this@unnamedValue.asFacet(type)
            }
        override fun toString(): String = stringify()
    }
}

/**
 * A special wrapper over non-`struct` values for `UNPIVOT`, only applicable in computing
 * iteration within a FROM-clause or wildcarded-path
 */
private class SingletonUnpivotStruct(ion: IonSystem, value: ExprValue) : BaseExprValue() {
    private val singleton = listOf(
        value.namedValue(
            ion.newString(syntheticColumnName(0)).exprValue()
        )
    )

    override fun iterator() = singleton.iterator()

    // XXX this value is only ever produced in a FROM iteration, thus none of these should ever be called
    override val type
        get() = throw UnsupportedOperationException("Synthetic value cannot provide type")
    override val ionValue
        get() = throw UnsupportedOperationException("Synthetic value cannot provide ion value")
    override val bindings
        get() = throw UnsupportedOperationException("Synthetic value cannot provide bindings")
}

/** Unpivots a `struct`, and synthesizes a synthetic singleton `struct` for other [ExprValue]. */
internal fun ExprValue.unpivot(ion: IonSystem): ExprValue = when {
    // special case for our special UNPIVOT pseudo-struct
    this is SingletonUnpivotStruct -> this
    // enable iteration for structs
    type == STRUCT -> object : ExprValue by this {
        override fun iterator() =
            ionValue.asSequence().map { it.exprValue() }.iterator()
    }
    // for non-struct, this wraps any value into a synthetic singleton pseudo-struct
    else -> SingletonUnpivotStruct(ion, this)
}

fun ExprValue.booleanValue(): Boolean =
    ionValue.booleanValue() ?: err("Expected non-null boolean: $ionValue")

fun ExprValue.numberValue(): Number = ionValue.numberValue()

fun ExprValue.timestampValue(): Timestamp = ionValue.timestampValue()

fun ExprValue.stringValue(): String =
    ionValue.stringValue() ?: err("Expected non-null string: $ionValue")

fun ExprValue.bytesValue(): ByteArray =
    ionValue.bytesValue() ?: err("Expected non-null LOB: $ionValue")

val ExprValue.size: Int get() = ionValue.size

/** A very simple string representation--to be used for diagnostic purposes only. */
fun ExprValue.stringify(): String = when (type) {
    MISSING -> "MISSING"
    BAG -> StringBuilder().apply {
        append("<<")
        this@stringify.forEachIndexed { i, e ->
            if (i > 0) {
                append(",")
            }
            append(e)
        }
        append(">>")
    }.toString()
    else -> ionValue.toString()
}

/** Provides SQL's equality function. */
fun ExprValue.exprEquals(other: ExprValue): Boolean {
    return when {
        // null/missing is never equal to anything
        this.type.isNull || other.type.isNull -> false
        // arithmetic equality
        this.type.isNumber && other.type.isNumber ->
            this.numberValue().compareTo(other.numberValue()) == 0
        // text equality for symbols/strings
        this.type.isText && other.type.isText ->
            this.stringValue() == other.stringValue()
        // LOB equality for byte content
        this.type.isLob && other.type.isLob ->
            Arrays.equals(this.bytesValue(), other.bytesValue())
        // at this point, types better line up
        this.type != other.type -> false
        else -> when (this.type) {
            BOOL -> this.booleanValue() == other.booleanValue()
            // TODO consider being more lax about offset and precision
            TIMESTAMP -> this.timestampValue() == other.timestampValue()
            LIST, SEXP -> this.listEquals(other)
            STRUCT -> this.structEquals(other)
            BAG -> this.bagEquals(other)
            else -> throw IllegalStateException("Invalid type fall-through: $type")
        }
    }
}

private fun ExprValue.listEquals(other: ExprValue): Boolean {
    val left = this.iterator()
    val right = other.iterator()

    while (left.hasNext()) {
        if (!right.hasNext()) {
            return false
        }

        val leftChild = left.next()
        val rightChild = right.next()
        if (!leftChild.exprEquals(rightChild)) {
            return false
        }
    }
    return !right.hasNext()
}

private fun ExprValue.structEquals(other: ExprValue): Boolean {
    // FIXME strict Ion equality is not right for this
    // TODO use a total natural ordering to calculate this
    return this.ionValue == other.ionValue
}

private fun ExprValue.bagEquals(other: ExprValue): Boolean {
    // FIXME strict Ion equality is not right for this
    // TODO use a total natural ordering to calculate this
    return this.ionValue == other.ionValue
}

operator fun ExprValue.compareTo(other: ExprValue): Int {
    val first = this.ionValue
    val second = other.ionValue

    return when {
        // nulls can't compare
        first.isNullValue || second.isNullValue ->
            throw EvaluationException("Null value cannot be compared: $first, $second")
        // compare the number types
        first.isNumeric && second.isNumeric ->
            first.numberValue().compareTo(second.numberValue())
        // timestamps compare against timestamps
        first is IonTimestamp && second is IonTimestamp ->
            first.timestampValue().compareTo(second.timestampValue())
        // string/symbol compare against themselves
        first is IonText && second is IonText ->
            first.stringValue().compareTo(second.stringValue())
        // TODO should bool/LOBs/aggregates compare?
        else -> err("Cannot compare values: $first, $second")
    }
}

/** The types that CAST to `string` as their Ion serialization*/
private val ION_TEXT_STRING_CAST_TYPES = setOf(BOOL, TIMESTAMP, CLOB, BLOB, LIST, SEXP, STRUCT)

/**
 * Casts this [ExprValue] to the target type.
 *
 * `MISSING` and `NULL` always convert to themselves no matter the target type.  When the
 * source type and target type are the same, this operation is a non-operation.
 *
 * The conversion *to* a particular type is as follows, any conversion not specified raises
 * an [EvaluationException]:
 *
 *  * `BOOL`
 *      * Number types will convert to `false` if numerically equal to zero, `true` otherwise.
 *      * Text types will convert to `true` if case insensitive compared to the text `"true"`,
 *    false otherwise.
 *  * `INT`, `FLOAT`, and `DECIMAL`
 *      * `BOOL` converts as `1` for `true` and `0` for `false`
 *      * Number types will narrow or widen from the source type.
 *      * Text types will convert using base-10 integral notation
 *          * For `FLOAT` and `DECIMAL` targets, decimal and e-notation is also supported.
 *  * `TIMESTAMP`
 *      * Text types will convert using the Ion text notation for timestamp (W3C/ISO-8601).
 *  * `STRING` and `SYMBOL`
 *      * `BOOL` converts to the text `"true"` and `"false"`.
 *      * Number types convert to decimal form with optional e-notation.
 *      * LOB types convert to their Ion textual representation.
 *      * `LIST`, `SEXP`, and `STRUCT` convert to their Ion textual representation.
 *      * `BAG` convert to a SQL++ literal form as the text image.
 *  * `BLOB` and `CLOB` can only convert between each other directly.
 *  * `LIST` and `SEXP`
 *      * Convert directly between each other.
 *      * `BAG` converts with an *arbitrary* order.
 *  * `STRUCT` only supports casting from itself.
 *  * `BAG` converts from `LIST` and `SEXP` by eliding order (order no longer is guaranteed
 *    after this operation).
 *
 * Note that *text types* is defined by [ExprValueType.isText], *number types* is defined by
 * [ExprValueType.isNumber], and *LOB types* is defined by [ExprValueType.isLob]
 *
 * @param ion The ion system to synthesize values with.
 * @param type The target type to cast this value to.
 */
fun ExprValue.cast(ion: IonSystem, targetType: ExprValueType): ExprValue {
    // TODO refactor this out appropriately (probably we need to refactor as a sort of mixin)
    fun boolTrue() = ion.newBool(true).seal().exprValue()
    fun boolFalse() = ion.newBool(false).seal().exprValue()

    fun Number.exprValue() = ionValue(ion).seal().exprValue()

    fun String.exprValue(type: ExprValueType) = when (type) {
        STRING -> ion.newString(this)
        SYMBOL -> ion.newSymbol(this)
        else -> err("Invalid type for textual conversion: $type")
    }.seal().exprValue()

    fun ExprValue.bagToString(): String = StringBuilder().apply {
        append("<<")
        this@bagToString.forEachIndexed { i, child ->
            if (i > 0) {
                append(",")
            }
            when (child.type) {
                BAG -> append(child.bagToString())
                else -> {
                    append("`")
                    append(child.ionValue.toString())
                    append("`")
                }
            }
        }
        append(">>")
    }.toString()

    when {
        type.isNull || type == targetType -> return this
        else -> {
            when (targetType) {
                BOOL -> when {
                    type.isNumber -> return when {
                        numberValue().compareTo(0L) == 0 -> boolFalse()
                        else -> boolTrue()
                    }
                    type.isText -> return when (stringValue().toLowerCase()) {
                        "true" -> boolTrue()
                        else -> boolFalse()
                    }
                }
                INT -> when {
                    type == BOOL -> return if (booleanValue()) 1L.exprValue() else 0L.exprValue()
                    type.isNumber -> return numberValue().toLong().exprValue()
                    type.isText -> return stringValue().toLong().exprValue()
                }
                FLOAT -> when {
                    type == BOOL -> return if (booleanValue()) 1.0.exprValue() else 0.0.exprValue()
                    type.isNumber -> return numberValue().toDouble().exprValue()
                    type.isText -> return stringValue().toDouble().exprValue()
                }
                DECIMAL -> when {
                    type == BOOL -> return if (booleanValue()) BigDecimal.ONE.exprValue() else BigDecimal.ZERO.exprValue()
                    type.isNumber -> return numberValue().coerce(BigDecimal::class.java).exprValue()
                    type.isText -> return BigDecimal(stringValue()).exprValue()
                }
                TIMESTAMP -> when {
                    type.isText -> return ion.newTimestamp(Timestamp.valueOf(stringValue())).seal().exprValue()
                }
                STRING, SYMBOL -> when {
                    type.isNumber -> return numberValue().toString().exprValue(targetType)
                    type.isText -> return stringValue().exprValue(targetType)
                    type in ION_TEXT_STRING_CAST_TYPES -> return ionValue.toString().exprValue(targetType)
                    type == BAG -> return bagToString().exprValue(targetType)
                }
                CLOB -> when {
                    type.isLob -> return ion.newClob(ionValue.bytesValue()).seal().exprValue()
                }
                BLOB -> when {
                    type.isLob -> return ion.newBlob(ionValue.bytesValue()).seal().exprValue()
                }
                LIST, SEXP, BAG -> when {
                    // XXX s-expressions behave like scalars, so we need to generate the sequence ourselves.
                    type == SEXP -> return SequenceExprValue(
                        ion, targetType, ionValue.asSequence().map { it.exprValue() }
                    )
                    type.isSequence -> return SequenceExprValue(ion, targetType, asSequence())
                }
            }
        }
    }

    // incompatible types
    err("Cannot convert $type to $targetType")
}
