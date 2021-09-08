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

import com.amazon.ion.*
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.time.Time
import org.partiql.lang.util.*
import java.math.*
import java.time.LocalDate

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
    fun newSymbol(value: String) : ExprValue

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

    override val missingValue = MissingExprValue(ion.newNull())
    override val nullValue = NullExprValue(ion.newNull())

    private val trueValue = TrueBoolExprValue(ion.newBool(true))
    private val falseValue = FalseBoolExprValue(ion.newBool(false))

    private val emptyString = StringExprValue(ion, "")

    override val emptyStruct = newStruct(sequenceOf(), StructOrdering.UNORDERED)

    override val emptyList = newList(sequenceOf())

    override val emptySexp = newSexp(sequenceOf())

    override val emptyBag = newBag(sequenceOf())

    override fun newBoolean(value: Boolean): ExprValue =
        if(value) trueValue else falseValue

    override fun newString(value: String): ExprValue =
        when {
            value.isEmpty() -> emptyString
            else            -> StringExprValue(ion, value)
        }

    override fun newInt(value: Int):ExprValue = IntExprValue(ion, value.toLong())

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
        IonExprValue(this, value)

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
        SequenceExprValue(ion, ExprValueType.LIST, value.mapIndexed { i, v -> v.namedValue(newInt(i))})

    override fun newList(value: Iterable<ExprValue>): ExprValue = newList(value.asSequence())

    override fun newSexp(value: Sequence<ExprValue>): ExprValue =
        SequenceExprValue(ion, ExprValueType.SEXP, value.mapIndexed { i, v -> v.namedValue(newInt(i))})

    override fun newSexp(value: Iterable<ExprValue>): ExprValue = newSexp(value.asSequence())
}

/** A base class for the `NULL` value, intended to be memoized. */
private class NullExprValue(value: IonNull): BaseExprValue() {
    override val ionValue = value
    override val type: ExprValueType get() = ExprValueType.NULL
}

/** A base class for the `MISSING` value, intended to be memoized. */
private class MissingExprValue(value: IonNull): BaseExprValue() {
    override val ionValue = value
    override val type: ExprValueType get() = ExprValueType.MISSING
}

/** An ExprValue class just for boolean values. [value] holds a memoized instance of [IonBool].
 */
private abstract class BooleanExprValue(value: IonBool): BaseExprValue(), Scalar {
    override val scalar: Scalar
        get() = this

    override val type: ExprValueType
        get() = ExprValueType.BOOL

    override val ionValue = value
}

/** Basic implementation for scalar [ExprValue] types. */
private abstract class ScalarExprValue : BaseExprValue(), Scalar {
    override val scalar: Scalar
        get() = this

    abstract fun ionValueFun(): IonValue

    // LazyThreadSafetyMode.PUBLICATION is ok here because the worst that can happen is that [ionValueFun] is invoked
    // from multiple threads.  This should be ok because [IonSystem] is thread-safe.
    override val ionValue by lazy(LazyThreadSafetyMode.PUBLICATION) { ionValueFun().seal() }
}

/** A base class for the `true` boolean value, intended to be memoized. */
private class TrueBoolExprValue(val value: IonBool) : BooleanExprValue(value) {
    override fun booleanValue(): Boolean? = true
}

/** A base class for the `false` boolean value, intended to be memoized. */
private class FalseBoolExprValue(val value: IonBool): BooleanExprValue(value) {
    override fun booleanValue(): Boolean? = false
}

private class StringExprValue(val ion: IonSystem, val value: String): ScalarExprValue() {
    override val type: ExprValueType = ExprValueType.STRING
    override fun stringValue() = value
    override fun ionValueFun(): IonValue = ion.newString(value)
}

private class IntExprValue(val ion: IonSystem, val value: Long): ScalarExprValue() {
    override val type: ExprValueType = ExprValueType.INT
    override fun numberValue() = value
    override fun ionValueFun(): IonValue = ion.newInt(value)
}

private class FloatExprValue(val ion: IonSystem, val value: Double): ScalarExprValue() {
    override val type: ExprValueType = ExprValueType.FLOAT
    override fun numberValue() = value
    override fun ionValueFun(): IonValue = ion.newFloat(value)
}

private class DecimalExprValue(val ion: IonSystem, val value: BigDecimal): ScalarExprValue() {
    override val type: ExprValueType = ExprValueType.DECIMAL
    override fun numberValue() = value
    override fun ionValueFun(): IonValue = ion.newDecimal(value)
}

/**
 * [ExprValue] to represent DATE in PartiQL.
 * [LocalDate] represents date without time and time zone.
 */
private class DateExprValue(val ion: IonSystem, val value: LocalDate): ScalarExprValue() {

    init {
        // validate that the local date is not an extended date.
        if (value.year < 0 || value.year > 9999) {
            err("Year should be in the range 0 to 9999 inclusive.",
                ErrorCode.EVALUATOR_DATE_FIELD_OUT_OF_RANGE,
                propertyValueMapOf(),
                false)
        }
    }
    private val PARTIQL_DATE_ANNOTATION = "\$partiql_date"

    private fun createIonDate() =
        ion.newTimestamp(Timestamp.forDay(value.year, value.monthValue, value.dayOfMonth)).apply {
            addTypeAnnotation(PARTIQL_DATE_ANNOTATION)
        }.seal()

    override val type: ExprValueType = ExprValueType.DATE
    override fun dateValue(): LocalDate? = value
    override fun ionValueFun(): IonValue = createIonDate()
}

private class TimestampExprValue(val ion: IonSystem, val value: Timestamp): ScalarExprValue() {
    override val type: ExprValueType = ExprValueType.TIMESTAMP
    override fun timestampValue(): Timestamp? = value
    override fun ionValueFun(): IonValue = ion.newTimestamp(value)
}

private class TimeExprValue(val ion: IonSystem, val value: Time): ScalarExprValue() {
    override val type = ExprValueType.TIME
    override fun timeValue(): Time = value
    override fun ionValueFun() = value.toIonValue(ion)
}

private class SymbolExprValue(val ion: IonSystem, val value: String): ScalarExprValue() {
    override val type: ExprValueType = ExprValueType.SYMBOL
    override fun stringValue() = value
    override fun ionValueFun(): IonValue = ion.newSymbol(value)
}

private class ClobExprValue(val ion: IonSystem, val value: ByteArray): ScalarExprValue() {
    override val type: ExprValueType = ExprValueType.CLOB
    override fun bytesValue() = value
    override fun ionValueFun(): IonValue = ion.newClob(value)
}

private class BlobExprValue(val ion: IonSystem, val value: ByteArray): ScalarExprValue() {
    override val type: ExprValueType = ExprValueType.BLOB
    override fun bytesValue() = value
    override fun ionValueFun(): IonValue = ion.newBlob(value)
}

/**
 * Core [ExprValue] over an [IonValue].
 */
internal class IonExprValue(private val valueFactory: ExprValueFactory, override val ionValue: IonValue) : BaseExprValue() {

    init {
        if(valueFactory.ion !== ionValue.system) {
            throw IllegalArgumentException("valueFactory must have the same instance of IonSystem as ionValue")
        }
    }

    private val namedFacet: Named? = when {
        ionValue.fieldName != null -> valueFactory.newString(ionValue.fieldName).asNamed()
        ionValue.type != IonType.DATAGRAM
        && ionValue.container != null
        && ionValue.ordinal >= 0 -> valueFactory.newInt(ionValue.ordinal).asNamed()
        else -> null
    }

    override val type = when {
        ionValue.isNullValue -> ExprValueType.NULL
        else -> ExprValueType.fromIonType(ionValue.type)
    }

    override val scalar: Scalar by lazy {
        object : Scalar {
            override fun booleanValue(): Boolean? = ionValue.booleanValueOrNull()
            override fun numberValue(): Number? =  ionValue.numberValueOrNull()
            override fun timestampValue(): Timestamp? = ionValue.timestampValueOrNull()
            override fun stringValue(): String? = ionValue.stringValueOrNull()
            override fun bytesValue(): ByteArray? = ionValue.bytesValueOrNull()
        }
    }

    override val bindings by lazy {
        if (ionValue is IonStruct) {
            IonStructBindings(valueFactory, ionValue)
        }
        else {
            Bindings.empty<ExprValue>()
        }
    }

    override val ordinalBindings: OrdinalBindings by lazy {
        object : OrdinalBindings {
            override fun get(index: Int): ExprValue? =
                when (ionValue) {
                    is IonSequence -> {
                        when {
                            index < 0 -> null
                            index >= ionValue.size -> null
                            else -> valueFactory.newFromIonValue(ionValue[index])
                                .namedValue(valueFactory.newInt(index))
                        }
                    }
                    else -> null
                }
        }
    }

    override fun iterator() = when (ionValue) {
        is IonContainer -> ionValue.asSequence()
            .map { v -> valueFactory.newFromIonValue(v) }.iterator()
        else -> emptyList<ExprValue>().iterator()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> provideFacet(type: Class<T>?) = when(type) {
        Named::class.java -> namedFacet
        else -> null
    } as T?
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
internal class SequenceExprValue( //dl todo: make private again
    private val ion: IonSystem,
    override val type: ExprValueType,
    private val sequence: Sequence<ExprValue>) : BaseExprValue() {

    init {
        if (!type.isSequence) {
            errNoContext("Cannot bind non-sequence type to sequence: $type", internal = true)
        }
    }

    override val ionValue: IonValue by lazy {
        sequence
            .mapTo(
                when (type) {
                    ExprValueType.BAG, ExprValueType.LIST -> ion.newEmptyList()
                    ExprValueType.SEXP                    -> ion.newEmptySexp()
                    else                                  -> throw IllegalStateException("Invalid type: $type")
                }
            ) {
                if(it is StructExprValue)
                    it.createMutableValue()
                else
                    it.ionValue.clone()
            }
            .seal()
    }

    override val ordinalBindings: OrdinalBindings by lazy {
        when (type) {
            // no ordinal access over BAG
            ExprValueType.BAG -> OrdinalBindings.EMPTY
            else              -> {
                // materialize the sequence as a backing list
                OrdinalBindings.ofList(toList())
            }
        }
    }

    override fun iterator() = sequence.iterator()
}
