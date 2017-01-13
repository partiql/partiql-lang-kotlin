/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.*
import java.math.BigInteger

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

private val MAX_INT = Int.MAX_VALUE.toLong()
private val MIN_INT = Int.MIN_VALUE.toLong()

fun IonValue.intValue(): Int {
    val number = numberValue()
    if (number > MAX_INT || number < MIN_INT) {
        throw IllegalArgumentException("Number out of integer range: $number")
    }
    return when (number) {
        is Int -> number
        is Long -> number.toInt()
        is BigInteger -> number.intValueExact()
        else -> throw IllegalArgumentException("Number is not an integer: $number")
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

/** Creates a new [IonSexp] from an AST [IonSexp] that strips out meta nodes. */
fun IonSexp.filterMetaNodes(): IonSexp = system.newEmptySexp().apply {
    var target = this@filterMetaNodes
    while (target[0].stringValue() == "meta") {
        target = target[1] as IonSexp
    }
    for (child in target) {
        add(
            when (child) {
                is IonSexp -> child.filterMetaNodes()
                else -> child.clone()
            }
        )
    }
}
