/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ion.*
import com.amazon.ionsql.eval.ExprValueType.*
import com.amazon.ionsql.util.*
import java.util.*

/**
 * Wraps the given [ExprValue] with a delegate that provides the [OrderedBindNames] facet.
 */
fun ExprValue.orderedNamesValue(names: List<String>): ExprValue =
    object : ExprValue by this, OrderedBindNames {
        override val orderedNames = names

        override fun <T : Any?> asFacet(type: Class<T>?): T? =
            downcast(type) ?: this@orderedNamesValue.asFacet(type)
    }

/** Wraps the this [ExprValue] as a [Named] instance. */
fun ExprValue.asNamed(): Named = object : Named {
    override val name: ExprValue
        get() = this@asNamed
}

/** Binds the given name value as a [Named] facet delegate over this [ExprValue]. */
fun ExprValue.namedValue(nameValue: ExprValue): ExprValue = object : ExprValue by this, Named {
    override val name = nameValue

    override fun <T : Any?> asFacet(type: Class<T>?): T? =
        downcast(type) ?: this@namedValue.asFacet(type)
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
    return !left.hasNext() && !right.hasNext()
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
