/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval

import com.amazon.ion.IonBool
import com.amazon.ion.IonReader
import com.amazon.ion.IonSystem
import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import com.amazon.ion.Timestamp
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.time.Time
import org.partiql.lang.util.propertyValueMapOf
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import kotlin.collections.asSequence

const val MISSING_ANNOTATION = "\$missing"
const val BAG_ANNOTATION = "\$bag"
const val DATE_ANNOTATION = "\$date"
const val TIME_ANNOTATION = "\$time"

/**
 * Provides a standard way of creating instances of ExprValue.
 *
 * Applications integrating with the PartiQL interpreter should use this to create standard instances
 * of [ExprValue].
 */
interface ExprValueFactory {
    companion object {
        /** Returns a standard instance of [ExprValueFactory]. */
        @JvmStatic
        fun standard(ion: IonSystem): ExprValueFactory = ExprValueFactoryImpl(ion)
    }

    /** The IonSystem instance. */
    val ion: IonSystem

    /** A possibly memoized, immutable [ExprValue] representing the PartiQL missing value. */
    val missingValue: ExprValue

    /** A possibly memoized, immutable [ExprValue] representing the PartiQL null value. */
    val nullValue: ExprValue

    /** A possibly memoized, immutable [ExprValue] representing an empty struct. */
    val emptyStruct: ExprValue

    /** A possibly memoized, immutable [ExprValue] representing an empty list. */
    val emptyList: ExprValue

    /** A possibly memoized, immutable [ExprValue] representing an empty sexp. */
    val emptySexp: ExprValue

    /** A possibly memoized, immutable [ExprValue] representing an empty bag. */
    val emptyBag: ExprValue

    /** Returns a possibly memoized instance of [ExprValue] representing the specified [Boolean]. */
    fun newBoolean(value: Boolean): ExprValue

    /** Returns a possibly memoized [ExprValue] instance representing the specified [String]. */
    fun newString(value: String): ExprValue

    /** Returns a PartiQL `INT` ][ExprValue] instance representing the specified [Int]. */
    fun newInt(value: Int): ExprValue

    /** Returns a PartiQL `INT` [ExprValue] instance representing the specified [Long]. */
    fun newInt(value: Long): ExprValue

    /** Returns a PartiQL `FLOAT` [ExprValue] instance representing the specified [Float]. */
    fun newFloat(value: Double): ExprValue

    /** Returns a PartiQL `DECIMAL` [ExprValue] instance representing the specified [Int]. */
    fun newDecimal(value: Int): ExprValue

    /** Returns a PartiQL `DECIMAL` [ExprValue] instance representing the specified [Long]. */
    fun newDecimal(value: Long): ExprValue

    /** Returns a PartiQL `DECIMAL` [ExprValue] instance representing the specified [BigDecimal]. */
    fun newDecimal(value: BigDecimal): ExprValue

    /** Returns a PartiQL `DATE` [ExprValue] instance representing the specified [LocalDate]. */
    fun newDate(value: LocalDate): ExprValue

    /** Returns a PartiQL `DATE` [ExprValue] instance representing the specified year, month and day. */
    fun newDate(year: Int, month: Int, day: Int): ExprValue

    /** Returns a PartiQL `DATE` [ExprValue] instance representing the specified date string of the format yyyy-MM-dd. */
    fun newDate(dateString: String): ExprValue

    /** Returns a PartiQL `TIMESTAMP` [ExprValue] instance representing the specified [Timestamp]. */
    fun newTimestamp(value: Timestamp): ExprValue

    /** Returns a PartiQL `TIME` [ExprValue] instance representing the specified [Time]. */
    fun newTime(value: Time): ExprValue

    /** Returns an  PartiQL `SYMBOL` [ExprValue] instance representing the specified [String]. */
    fun newSymbol(value: String): ExprValue

    /** Returns a PartiQL `CLOB` [ExprValue] instance representing the specified [ByteArray]. */
    fun newClob(value: ByteArray): ExprValue

    /** Returns a PartiQL `BLOB` [ExprValue] instance representing the specified [ByteArray]. */
    fun newBlob(value: ByteArray): ExprValue

    /**
     * Returns a possibly lazily evaluated instance of [ExprValue] representing a `PartiQL` `STRUCT`.
     * The [ExprValue] instances within [values] should be [Named].
     *
     * [ordering] specifies if the field order is to be preserved or not.
     */
    fun newStruct(value: Sequence<ExprValue>, ordering: StructOrdering): ExprValue

    /** See newStruct(Sequence) */
    fun newStruct(value: Iterable<ExprValue>, ordering: StructOrdering): ExprValue

    /** Returns a possibly lazily evaluated instance of [ExprValue] representing a `PartiQL` `BAG`. */
    fun newBag(value: Sequence<ExprValue>): ExprValue

    /** See newBag(Sequence) */
    fun newBag(value: Iterable<ExprValue>): ExprValue

    /** Returns a possibly lazily evaluated instance of [ExprValue] representing a `PartiQL` `LIST`. */
    fun newList(value: Sequence<ExprValue>): ExprValue

    /** See newList(Sequence) */
    fun newList(value: Iterable<ExprValue>): ExprValue

    /** Returns a possibly lazily evaluated instance of [ExprValue] representing a `PartiQL` `SEXP`. */
    fun newSexp(value: Sequence<ExprValue>): ExprValue

    /** See newSexp(Sequence) */
    fun newSexp(value: Iterable<ExprValue>): ExprValue

    /**
     * Creates a new [ExprValue] instance from any Ion value.
     *
     * If possible, prefer the use of the other methods instead because they might return [ExprValue] instances
     * that are better optimized for their specific data type (depending on implementation).
     */
    fun newFromIonValue(value: IonValue): ExprValue

    /**
     * Creates a new [ExprValue] instance from the next value available from the specified [IonReader].
     *
     * Implementations should not close the [IonReader].
     */
    fun newFromIonReader(reader: IonReader): ExprValue
}

private class ExprValueFactoryImpl(override val ion: IonSystem) : ExprValueFactory {

    companion object {
        private val MAX_LONG_VALUE = BigInteger.valueOf(Long.MAX_VALUE)
        private val MIN_LONG_VALUE = BigInteger.valueOf(Long.MIN_VALUE)
    }

    override val missingValue = MissingExprValue(ion)
    override val nullValue = NullExprValue(ion)

    private val trueValue = TrueBoolExprValue(ion)
    private val falseValue = FalseBoolExprValue(ion)

    private val emptyString = StringExprValue(ion, "")

    override val emptyStruct = newStruct(sequenceOf(), StructOrdering.UNORDERED)

    override val emptyList = newList(sequenceOf())

    override val emptySexp = newSexp(sequenceOf())

    override val emptyBag = newBag(sequenceOf())

    override fun newBoolean(value: Boolean): ExprValue =
        if (value) trueValue else falseValue

    override fun newString(value: String): ExprValue =
        when {
            value.isEmpty() -> emptyString
            else -> StringExprValue(ion, value)
        }

    override fun newInt(value: Int): ExprValue = IntExprValue(ion, value.toLong())

    override fun newInt(value: Long) =
        IntExprValue(ion, value)

    override fun newFloat(value: Double): ExprValue =
        FloatExprValue(ion, value)

    override fun newDecimal(value: BigDecimal): ExprValue =
        DecimalExprValue(ion, value)

    override fun newDecimal(value: Int): ExprValue =
        DecimalExprValue(ion, BigDecimal.valueOf(value.toLong()))

    override fun newDecimal(value: Long): ExprValue =
        DecimalExprValue(ion, BigDecimal.valueOf(value))

    override fun newDate(value: LocalDate): ExprValue =
        DateExprValue(ion, value)

    override fun newDate(year: Int, month: Int, day: Int) =
        newDate(LocalDate.of(year, month, day))

    override fun newDate(dateString: String) =
        newDate(LocalDate.parse(dateString))

    override fun newTimestamp(value: Timestamp): ExprValue =
        TimestampExprValue(ion, value)

    override fun newTime(value: Time): ExprValue =
        TimeExprValue(ion, value)

    override fun newSymbol(value: String): ExprValue =
        SymbolExprValue(ion, value)

    override fun newClob(value: ByteArray): ExprValue =
        ClobExprValue(ion, value)

    override fun newBlob(value: ByteArray): ExprValue =
        BlobExprValue(ion, value)

    override fun newFromIonValue(value: IonValue): ExprValue =
        value.toExprValue()

    override fun newFromIonReader(reader: IonReader): ExprValue =
        newFromIonValue(ion.newValue(reader))

    override fun newStruct(value: Sequence<ExprValue>, ordering: StructOrdering): ExprValue =
        StructExprValue(ion, ordering, value)

    override fun newStruct(value: Iterable<ExprValue>, ordering: StructOrdering): ExprValue =
        newStruct(value.asSequence(), ordering)

    override fun newBag(value: Sequence<ExprValue>): ExprValue =
        SequenceExprValue(ion, ExprValueType.BAG, value)

    override fun newBag(value: Iterable<ExprValue>): ExprValue = newBag(value.asSequence())

    override fun newList(value: Sequence<ExprValue>): ExprValue =
        SequenceExprValue(ion, ExprValueType.LIST, value.mapIndexed { i, v -> v.namedValue(newInt(i)) })

    override fun newList(value: Iterable<ExprValue>): ExprValue = newList(value.asSequence())

    override fun newSexp(value: Sequence<ExprValue>): ExprValue =
        SequenceExprValue(ion, ExprValueType.SEXP, value.mapIndexed { i, v -> v.namedValue(newInt(i)) })

    override fun newSexp(value: Iterable<ExprValue>): ExprValue = newSexp(value.asSequence())
}

/**
 * A base class for the `NULL` value, intended to be memoized.
 *
 * [ionType] indicates which ion type this null has. When we are querying an Ion file, if we find the result is, e.g.,
 * `ion.int`, we have to return a null value of type int and cannot ignore the type of it. We might need to consider
 * add a [metas] field in [ExprValue], instead.
 */
internal class NullExprValue(private val ion: IonSystem, private val ionType: IonType = IonType.NULL) : BaseExprValue() {
    override val type: ExprValueType get() = ExprValueType.NULL
    override val ionValue by lazy { toIonValue(ion) }

    @Suppress("UNCHECKED_CAST")
    override fun <T> provideFacet(type: Class<T>?): T? = when (type) {
        IonType::class.java -> ionType as T?
        else -> null
    }
}

/** A base class for the `MISSING` value, intended to be memoized. */
private class MissingExprValue(private val ion: IonSystem) : BaseExprValue() {
    override val type: ExprValueType get() = ExprValueType.MISSING
    override val ionValue by lazy { toIonValue(ion) }
}

/** An ExprValue class just for boolean values. [value] holds a memoized instance of [IonBool].
 */
private abstract class BooleanExprValue : BaseExprValue(), Scalar {
    override val scalar: Scalar
        get() = this

    override val type: ExprValueType
        get() = ExprValueType.BOOL
}

/** Basic implementation for scalar [ExprValue] types. */
private abstract class ScalarExprValue : BaseExprValue(), Scalar {
    override val scalar: Scalar
        get() = this
}

/** A base class for the `true` boolean value, intended to be memoized. */
private class TrueBoolExprValue(private val ion: IonSystem) : BooleanExprValue() {
    override fun booleanValue(): Boolean = true
    override val ionValue by lazy { toIonValue(ion) }
}

/** A base class for the `false` boolean value, intended to be memoized. */
private class FalseBoolExprValue(private val ion: IonSystem) : BooleanExprValue() {
    override fun booleanValue(): Boolean = false
    override val ionValue by lazy { toIonValue(ion) }
}

private class StringExprValue(private val ion: IonSystem, val value: String) : ScalarExprValue() {
    override val type: ExprValueType = ExprValueType.STRING
    override fun stringValue() = value
    override val ionValue by lazy { toIonValue(ion) }
}

private class IntExprValue(private val ion: IonSystem, val value: Long) : ScalarExprValue() {
    override val type: ExprValueType = ExprValueType.INT
    override fun numberValue() = value
    override val ionValue by lazy { toIonValue(ion) }
}

private class FloatExprValue(private val ion: IonSystem, val value: Double) : ScalarExprValue() {
    override val type: ExprValueType = ExprValueType.FLOAT
    override fun numberValue() = value
    override val ionValue by lazy { toIonValue(ion) }
}

private class DecimalExprValue(private val ion: IonSystem, val value: BigDecimal) : ScalarExprValue() {
    override val type: ExprValueType = ExprValueType.DECIMAL
    override fun numberValue() = value
    override val ionValue by lazy { toIonValue(ion) }
}

/**
 * [ExprValue] to represent DATE in PartiQL.
 * [LocalDate] represents date without time and time zone.
 */
private class DateExprValue(private val ion: IonSystem, val value: LocalDate) : ScalarExprValue() {

    init {
        // validate that the local date is not an extended date.
        if (value.year < 0 || value.year > 9999) {
            err(
                "Year should be in the range 0 to 9999 inclusive.",
                ErrorCode.EVALUATOR_DATE_FIELD_OUT_OF_RANGE,
                propertyValueMapOf(),
                false
            )
        }
    }

    override val type: ExprValueType = ExprValueType.DATE
    override fun dateValue(): LocalDate? = value
    override val ionValue by lazy { toIonValue(ion) }
}

private class TimestampExprValue(private val ion: IonSystem, val value: Timestamp) : ScalarExprValue() {
    override val type: ExprValueType = ExprValueType.TIMESTAMP
    override fun timestampValue(): Timestamp? = value
    override val ionValue by lazy { toIonValue(ion) }
}

private class TimeExprValue(private val ion: IonSystem, val value: Time) : ScalarExprValue() {
    override val type = ExprValueType.TIME
    override fun timeValue(): Time = value
    override val ionValue by lazy { toIonValue(ion) }
}

private class SymbolExprValue(private val ion: IonSystem, val value: String) : ScalarExprValue() {
    override val type: ExprValueType = ExprValueType.SYMBOL
    override fun stringValue() = value
    override val ionValue by lazy { toIonValue(ion) }
}

private class ClobExprValue(private val ion: IonSystem, val value: ByteArray) : ScalarExprValue() {
    override val type: ExprValueType = ExprValueType.CLOB
    override fun bytesValue() = value
    override val ionValue by lazy { toIonValue(ion) }
}

private class BlobExprValue(private val ion: IonSystem, val value: ByteArray) : ScalarExprValue() {
    override val type: ExprValueType = ExprValueType.BLOB
    override fun bytesValue() = value
    override val ionValue by lazy { toIonValue(ion) }
}

/**
 * Provides an [ExprValue] over a function that yields a [Sequence].
 * This implementation is used to implement lazy sequences of values.
 *
 * The [ExprValue.ionValue] property lazily generates an [IonList] from the sequence **once**.
 * The [ExprValue.ordinalBindings] property lazily generates a backing [List] **once**
 * for non-`BAG` types.
 *
 * @param ion The underlying [IonSystem] for generating values.
 * @param type The reported [ExprValueType] for this value.
 * @param sequence The [Sequence] generating function.
 */
internal class SequenceExprValue( // dl todo: make private again
    private val ion: IonSystem,
    override val type: ExprValueType,
    private val sequence: Sequence<ExprValue>
) : BaseExprValue() {

    init {
        if (!type.isSequence) {
            errNoContext("Cannot bind non-sequence type to sequence: $type", errorCode = ErrorCode.EVALUATOR_INVALID_BINDING, internal = true)
        }
    }

    override val ordinalBindings: OrdinalBindings by lazy {
        when (type) {
            // no ordinal access over BAG
            ExprValueType.BAG -> OrdinalBindings.EMPTY
            else -> {
                // materialize the sequence as a backing list
                OrdinalBindings.ofList(toList())
            }
        }
    }

    override fun iterator() = sequence.iterator()

    override val ionValue by lazy { toIonValue(ion) }
}
