/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ion.*
import com.amazon.ionsql.errors.*
import com.amazon.ionsql.eval.ExprValueType.*
import com.amazon.ionsql.util.*
import java.math.*

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

val ExprValue.orderedNames: List<String>?
    get() = asFacet(OrderedBindNames::class.java)?.orderedNames

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

val ExprValue.name: ExprValue?
    get() = asFacet(Named::class.java)?.name

/** A special wrapper for `UNPIVOT` values as a BAG. */
private class UnpivotedExprValue(private val values: Iterable<ExprValue>) : BaseExprValue() {
    override val type = BAG
    override fun iterator() = values.iterator()

    // XXX this value is only ever produced in a FROM iteration, thus none of these should ever be called
    override val ionValue
        get() = throw UnsupportedOperationException("Synthetic value cannot provide ion value")
}

/** Unpivots a `struct`, and synthesizes a synthetic singleton `struct` for other [ExprValue]. */
internal fun ExprValue.unpivot(ion: IonSystem): ExprValue = when {
    // special case for our special UNPIVOT value to avoid double wrapping
    this is UnpivotedExprValue -> this
    // Wrap into a pseudo-BAG
    type == STRUCT -> UnpivotedExprValue(this)
    // for non-struct, this wraps any value into a BAG with a synthetic name
    else -> UnpivotedExprValue(
        listOf(
            this.namedValue(ion.newString(syntheticColumnName(0)).exprValue())
        )
    )
}

fun ExprValue.booleanValue(): Boolean =
    scalar.booleanValue() ?: errNoContext("Expected non-null boolean: $ionValue", internal = false)

fun ExprValue.numberValue(): Number =
    scalar.numberValue() ?: errNoContext("Expected non-null number: $ionValue", internal = false)

fun ExprValue.timestampValue(): Timestamp =
    scalar.timestampValue() ?: errNoContext("Expected non-null timestamp: $ionValue", internal = false)

fun ExprValue.stringValue(): String =
    scalar.stringValue() ?: errNoContext("Expected non-null string: $ionValue", internal = false)

fun ExprValue.bytesValue(): ByteArray =
    scalar.bytesValue() ?: errNoContext("Expected non-null LOB: $ionValue", internal = false)

/**
 * Implements the `FROM` range operation.
 * Specifically, this is distinct from the normal [ExprValue.iterator] in that
 * types that are **not** [ExprValueType.isRangeFrom] get treated as a singleton
 * as per SQL++ specification.
 */
fun ExprValue.rangeOver(): Iterable<ExprValue> = when {
    type.isRangedFrom -> this
    // everything else ranges as a singleton unnamed value
    else -> listOf(this.unnamedValue())
}


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

val DEFAULT_COMPARATOR = NaturalExprValueComparators.NULLS_FIRST

/** Provides SQL's equality function. */
fun ExprValue.exprEquals(other: ExprValue): Boolean = DEFAULT_COMPARATOR.compare(this, other) == 0

/**
 * Provides the comparison predicate--which is not a total ordering.
 *
 * In particular, this operation will fail for non-comparable types.
 * For a total ordering over the IonSQL++ type space, see [NaturalExprValueComparators]
 */
operator fun ExprValue.compareTo(other: ExprValue): Int {
    return when {
        type.isNull || other.type.isNull ->
            throw EvaluationException("Null value cannot be compared: $this, $other", internal = false)
        type.isDirectlyComparableTo(other.type) -> DEFAULT_COMPARATOR.compare(this, other)
        else -> errNoContext("Cannot compare values: $this, $other", internal = false)
    }
}

/** Types that are cast to the [ExprValueType.isText] types by calling `IonValue.toString()`. */
private val ION_TEXT_STRING_CAST_TYPES = setOf(BOOL, TIMESTAMP)

/**
 * Casts this [ExprValue] to the target type.
 *
 * `MISSING` and `NULL` always convert to themselves no matter the target type.  When the
 * source type and target type are the same, this operation is a no-op.
 *
 * The conversion *to* a particular type is as follows, any conversion not specified raises
 * an [EvaluationException]:
 *
 *  * `BOOL`
 *      * Number types will convert to `false` if numerically equal to zero, `true` otherwise.
 *      * Text types will convert to `true` if case-insensitive compared to the text `"true"`,
 *    `false` otherwise.
 *  * `INT`, `FLOAT`, and `DECIMAL`
 *      * `BOOL` converts as `1` for `true` and `0` for `false`
 *      * Number types will narrow or widen from the source type.  Narrowing is a truncation
 *      * Text types will convert using base-10 integral notation
 *          * For `FLOAT` and `DECIMAL` targets, decimal and e-notation is also supported.
 *  * `TIMESTAMP`
 *      * Text types will convert using the Ion text notation for timestamp (W3C/ISO-8601).
 *  * `STRING` and `SYMBOL`
 *      * `BOOL` converts to `STRING` as `"true"` and `"false"`;
 *        converts to `SYMBOL` as `'true'` and `'false'`.
 *      * Number types convert to decimal form with optional e-notation.
 *      * `TIMESTAMP` converts to the ISO-8601 format.
 *  * `BLOB` and `CLOB` can only convert between each other directly.
 *  * `LIST` and `SEXP`
 *      * Convert directly between each other.
 *      * `BAG` converts with an *arbitrary* order.
 *  * `STRUCT` only supports casting from itself.
 *  * `BAG` converts from `LIST` and `SEXP` by drops order guarantees.
 *
 * Note that *text types* is defined by [ExprValueType.isText], *number types* is defined by
 * [ExprValueType.isNumber], and *LOB types* is defined by [ExprValueType.isLob]
 *
 * @param ion The ion system to synthesize values with.
 * @param type The target type to cast this value to.
 */
fun ExprValue.cast(ion: IonSystem, targetType: ExprValueType, metadata: NodeMetadata?): ExprValue {
    fun castExceptionContext() = metadata?.fillErrorContext(
        PropertyValueMap().also {
            it[Property.CAST_FROM] = this.type.toString()
            it[Property.CAST_TO] = targetType.toString()
        })

    fun Number.exprValue() = ionValue(ion).seal().exprValue()

    fun String.exprValue(type: ExprValueType) = when (type) {
        STRING -> ion.newString(this)
        SYMBOL -> ion.newSymbol(this)

        else -> err("Invalid type for textual conversion: $type (this code should be unreachable)",
                    castExceptionContext(),  internal = true)

    }.seal().exprValue()

    when {
        type.isNull || type == targetType -> return this
        else -> {
            when (targetType) {
                BOOL -> when {
                    type.isNumber -> return when {
                        numberValue().compareTo(0L) == 0 -> ion.newBool(false).seal().exprValue()
                        else -> ion.newBool(true).seal().exprValue()
                    }
                    type.isText -> return when (stringValue().toLowerCase()) {
                        "true" -> ion.newBool(true).seal().exprValue()
                        else -> ion.newBool(false).seal().exprValue()
                    }
                }
                INT -> when {
                    type == BOOL -> return if (booleanValue()) 1L.exprValue() else 0L.exprValue()
                    type.isNumber -> return numberValue().toLong().exprValue()
                    type.isText -> {
                        val value = ion.singleValue(stringValue())
                        return when(value.type) {
                             IonType.INT -> {
                                value as IonInt
                                when(value.integerSize){
                                    IntegerSize.BIG_INTEGER -> errIntOverflow(metadata?.toErrorContext())
                                    else -> value.longValue().exprValue()
                                }
                            }
                            else ->
                                throw EvaluationException(
                                     message = "can't convert string value to INT",
                                     errorCode = ErrorCode.EVALUATOR_CAST_FAILED,
                                     errorContext = castExceptionContext(),
                                     internal = false)
                        }
                    }
                }
                FLOAT -> when {
                    type == BOOL -> return if (booleanValue()) 1.0.exprValue() else 0.0.exprValue()
                    type.isNumber -> return numberValue().toDouble().exprValue()
                    type.isText ->
                        try {
                            return stringValue().toDouble().exprValue()
                        } catch(e: NumberFormatException) {
                            throw EvaluationException(message = "can't convert string value to FLOAT",
                                                      cause = e,
                                                      errorCode = ErrorCode.EVALUATOR_CAST_FAILED,
                                                      errorContext = castExceptionContext(),
                                                      internal = false)
                        }
                }
                DECIMAL -> when {
                    type == BOOL -> return if (booleanValue()) BigDecimal.ONE.exprValue() else BigDecimal.ZERO.exprValue()
                    type.isNumber -> return numberValue().coerce(BigDecimal::class.java).exprValue()
                    type.isText -> try {
                        return bigDecimalOf(stringValue()).exprValue()
                    }
                    catch (e: NumberFormatException)
                    {
                        throw EvaluationException(message = "can't convert string value to DECIMAL",
                                                  cause = e,
                                                  errorCode = ErrorCode.EVALUATOR_CAST_FAILED,
                                                  errorContext = castExceptionContext(),
                                                  internal = false)
                    }
                }
                TIMESTAMP -> when {
                    type.isText -> try {
                        return ion.newTimestamp(Timestamp.valueOf(stringValue())).seal().exprValue()
                    }
                    catch (e: IllegalArgumentException)
                    {
                        throw EvaluationException(message = "can't convert string value to TIMESTAMP",
                                                  cause = e,
                                                  errorCode = ErrorCode.EVALUATOR_CAST_FAILED,
                                                  errorContext = castExceptionContext(),
                                                  internal = false)
                    }
                }
                STRING, SYMBOL -> when {
                    type.isNumber -> return numberValue().toString().exprValue(targetType)
                    type.isText -> return stringValue().exprValue(targetType)
                    type in ION_TEXT_STRING_CAST_TYPES -> return ionValue.toString().exprValue(targetType)
                }
                CLOB -> when {
                    type.isLob -> return ion.newClob(bytesValue()).seal().exprValue()
                }
                BLOB -> when {
                    type.isLob -> return ion.newBlob(bytesValue()).seal().exprValue()
                }
                LIST, SEXP, BAG -> when {
                    type.isSequence -> return SequenceExprValue(ion, targetType, asSequence())
                }
                // no support for anything else
                else -> {}
            }
        }
    }

    // incompatible types
    err("Cannot convert $type to $targetType",
        ErrorCode.EVALUATOR_INVALID_CAST,
        castExceptionContext(), internal = false)
}
