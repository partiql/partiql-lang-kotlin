/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.*

fun IonValue.seal(): IonValue = apply { makeReadOnly() }

operator fun IonValue.get(name: String): IonValue? = when (this) {
    is IonStruct -> get(name)
    else -> throw IllegalArgumentException("Expected struct: $this")
}

operator fun IonValue.get(index: Int): IonValue = when (this) {
    is IonSequence -> get(index)
    else -> throw IllegalArgumentException("Expected sequence: $this")
}

operator fun IonValue.iterator(): Iterator<IonValue> = when (this) {
    is IonContainer -> iterator()
    else -> throw IllegalArgumentException("Expected container: $this")
}

fun IonValue.asSequence(): Sequence<IonValue> = iterator().asSequence()

fun IonValue.stringValue(): String? = when (this) {
    is IonText -> stringValue()
    else -> throw IllegalArgumentException("Expected text: $this")
}

fun IonValue.numberValue(): Number = when {
    isNullValue -> throw IllegalArgumentException("Expected non-null number: $this")
    else -> when (this) {
        is IonInt -> longValue()
        is IonFloat -> doubleValue()
        is IonDecimal -> bigDecimalValue()
        else -> throw IllegalArgumentException("Expected number: $this")
    }
}

fun IonValue.booleanValue(): Boolean? = when (this) {
    is IonBool -> booleanValue()
    else -> throw IllegalArgumentException("Expected boolean: $this")
}

val IonValue.isNumeric: Boolean
    get() = when (this) {
        is IonInt, is IonFloat, is IonDecimal -> true
        else -> false
    }

val IonValue.ordinal: Int
    get() = container.indexOf(this)

fun IonValue.exprValue(): ExprValue = IonExprValue(this)