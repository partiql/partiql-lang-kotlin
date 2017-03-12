/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.util

import com.amazon.ion.*
import com.amazon.ionsql.eval.ExprValue
import com.amazon.ionsql.eval.IonExprValue
import java.math.BigInteger

private fun err(message: String): Nothing = throw IllegalArgumentException(message)

fun IonValue.seal(): IonValue = apply { makeReadOnly() }

operator fun IonValue.get(name: String): IonValue? = when (this) {
    is IonStruct -> get(name)
    else -> err("Expected struct: $this")
}

val IonValue.size: Int
    get() = when (this) {
        is IonContainer -> size()
        else -> err("Expected container: $this")
    }

val IonValue.lastIndex: Int
    get() = when (this) {
        is IonSequence -> size - 1
        else -> err("Expected sequence $this")
    }

operator fun IonValue.get(index: Int): IonValue = when (this) {
    is IonSequence -> get(index)
    else -> err("Expected sequence: $this")
}

operator fun IonValue.iterator(): Iterator<IonValue> = when (this) {
    is IonContainer -> iterator()
    else -> err("Expected container: $this")
}

fun IonValue.asSequence(): Sequence<IonValue> = when (this) {
    is IonContainer -> Sequence { iterator() }
    else -> err("Expected container: $this")
}

fun IonValue.numberValue(): Number = when {
    isNullValue -> err("Expected non-null number: $this")
    else -> when (this) {
        is IonInt -> longValue()
        is IonFloat -> doubleValue()
        is IonDecimal -> bigDecimalValue()
        else -> err("Expected number: $this")
    }
}

private val MAX_INT = Int.MAX_VALUE.toLong()
private val MIN_INT = Int.MIN_VALUE.toLong()

fun IonValue.intValue(): Int {
    val number = numberValue()
    if (number > MAX_INT || number < MIN_INT) {
        err("Number out of integer range: $number")
    }
    return when (number) {
        is Int -> number
        is Long -> number.toInt()
        is BigInteger -> number.intValueExact()
        else -> err("Number is not an integer: $number")
    }
}

fun IonValue.longValue(): Long {
    val number = numberValue()
    return when (number) {
        is Int -> number.toLong()
        is Long -> number
        is BigInteger -> number.longValueExact()
        else -> err("Number is not a long: $number")
    }
}

fun IonValue.booleanValue(): Boolean? = when (this) {
    is IonBool -> booleanValue()
    else -> err("Expected boolean: $this")
}

fun IonValue.timestampValue(): Timestamp = when (this) {
    is IonTimestamp -> timestampValue()
    else -> err("Expected timestamp: $this")
}

fun IonValue.stringValue(): String? = when (this) {
    is IonText -> stringValue()
    else -> err("Expected text: $this")
}

fun IonValue.bytesValue(): ByteArray? = when (this) {
    is IonLob -> bytes
    else -> err("Expected LOB: $this")
}

val IonValue.isNumeric: Boolean
    get() = when (this) {
        is IonInt, is IonFloat, is IonDecimal -> true
        else -> false
    }

val IonValue.isUnsignedInteger: Boolean
    get() = when (this) {
        is IonInt -> longValue() >= 0
        else -> false
    }

val IonValue.isNonNullText: Boolean
    get() = when (this) {
        is IonText -> !isNullValue
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
