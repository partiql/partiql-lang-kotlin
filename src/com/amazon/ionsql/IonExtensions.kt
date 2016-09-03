/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.*

operator fun IonValue.get(name: String): IonValue = when (this) {
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

fun IonValue.stringValue(): String? = when (this) {
    is IonText -> stringValue()
    else -> throw IllegalArgumentException("Expected text: $this")
}

fun IonValue.longValue(): Long? = when (this) {
    is IonInt -> longValue()
    else -> throw IllegalArgumentException("Expected int: $this")
}

fun IonValue.doubleValue(): Double? = when (this) {
    is IonFloat -> doubleValue()
    else -> throw IllegalArgumentException("Expected float: $this")
}

fun IonValue.decimalValue(): Decimal? = when (this) {
    is IonDecimal -> decimalValue()
    else -> throw IllegalArgumentException("Expected decimal: $this")
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

val IonValue.ordinal: Int
    get() = container.indexOf(this)

fun IonValue.exprValue(): ExprValue = IonExprValue(this)