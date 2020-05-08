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

package org.partiql.lang.util

import com.amazon.ion.*
import org.partiql.lang.eval.*
import java.math.*

@JvmName("IonValueUtils")

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

private fun IonInt.javaValue(): Number = when (integerSize) {
    IntegerSize.BIG_INTEGER -> bigIntegerValue()
    else                    -> longValue()
}

fun IonValue.numberValue(): Number = when {
    isNullValue -> err("Expected non-null number: $this")
    else        -> when (this) {
        is IonInt     -> javaValue()
        is IonFloat   -> doubleValue()
        is IonDecimal -> decimalValue()
        else          -> err("Expected number: $this")
    }
}

fun IonValue.longValue(): Long {
    val number = numberValue()
    return when (number) {
        is Int        -> number.toLong()
        is Long       -> number
        is BigInteger -> number.longValueExact()
        else          -> err("Number is not a long: $number")
    }
}

fun IonValue.doubleValue(): Double = when {
    isNullValue      -> err("Expected non-null double: $this")
    this is IonFloat -> doubleValue()
    else             -> err("Expected double: $this")
}

fun IonValue.bigDecimalValue(): BigDecimal = when {
    isNullValue        -> err("Expected non-null decimal: $this")
    this is IonDecimal -> decimalValue()
    else               -> err("Expected decimal: $this")
}

fun IonValue.booleanValue(): Boolean = when (this) {
    is IonBool -> booleanValue()
    else       -> err("Expected boolean: $this")
}

fun IonValue.timestampValue(): Timestamp = when (this) {
    is IonTimestamp -> timestampValue()
    else            -> err("Expected timestamp: $this")
}

fun IonValue.stringValue(): String? = when (this) {
    is IonText -> stringValue()
    else       -> err("Expected text: $this")
}

fun IonValue.bytesValue(): ByteArray = when (this) {
    is IonLob -> bytes
    else      -> err("Expected LOB: $this")
}

fun IonValue.numberValueOrNull(): Number? =
    when (this) {
        is IonInt     -> javaValue()
        is IonFloat   -> doubleValue()
        is IonDecimal -> decimalValue()
        else          -> null
    }


fun IonValue.longValueOrNull(): Long? {
    val number = numberValue()
    return when (number) {
        is Int        -> number.toLong()
        is Long       -> number
        is BigInteger -> number.longValueExact()
        else          -> null
    }
}

fun IonValue.doubleValueOrNull(): Double? = when {
    this is IonFloat -> doubleValue()
    else             -> null
}

fun IonValue.bigDecimalValueOrNull(): BigDecimal? = when {
    this is IonDecimal -> bigDecimalValue()
    else               -> null
}

fun IonValue.booleanValueOrNull(): Boolean? = when (this) {
    is IonBool -> booleanValue()
    else       -> null
}

fun IonValue.timestampValueOrNull(): Timestamp? = when (this) {
    is IonTimestamp -> timestampValue()
    else            -> null
}

fun IonValue.stringValueOrNull(): String? = when (this) {
    is IonText -> stringValue()
    else       -> null
}

fun IonValue.bytesValueOrNull(): ByteArray? = when (this) {
    is IonLob -> bytes
    else      -> null
}

val IonValue.isNumeric: Boolean
    get() = when (this) {
        is IonInt, is IonFloat, is IonDecimal -> true
        else                                  -> false
    }

val IonValue.isUnsignedInteger: Boolean
    get() = when (this) {
        is IonInt -> longValue() >= 0
        else      -> false
    }

val IonValue.isNonNullText: Boolean
    get() = when (this) {
        is IonText -> !isNullValue
        else       -> false
    }

val IonValue.ordinal: Int
    get() = container.indexOf(this)

val IonValue.isText: Boolean
    get() = when (this) {
        is IonText -> true
        else       -> false
    }

/** Creates a new [IonSexp] from a legacy AST [IonSexp] that strips out meta nodes. */
fun IonSexp.filterMetaNodes(): IonValue {
    var target = this@filterMetaNodes

    while (target[0].stringValue() == "meta") {
        val tmpTarget = target[1]
        if(tmpTarget !is IonSexp) {
            return tmpTarget.clone()
        }
        target = tmpTarget.asIonSexp()
    }

    return system.newEmptySexp().apply {
        val isLiteral = target[0].stringValue() == "lit"
        for (child in target) {
            add(
                when {
                    !isLiteral && child is IonSexp -> child.filterMetaNodes()
                    else                           -> child.clone()
                }
            )
        }
    }
}

fun IonSexp.singleArgWithTag(tagName: String): IonValue =
    this.args.map { it.asIonSexp() }.singleOrNull { it.tagText == tagName } ?: err("Could not locate s-exp child with tag $tagName")

fun IonSexp.singleArgWithTagOrNull(tagName: String): IonValue? =
    this.args.map { it.asIonSexp() }.singleOrNull { it.tagText == tagName }

fun Iterable<IonValue>.toListOfIonSexp() = this.map { it.asIonSexp() }
fun IonValue.asIonInt() = this as? IonInt ?: err("Expected an IonInt but found ${this.type}.")
fun IonValue.asIonSexp() = this as? IonSexp ?: err("Expected an IonSexp but found ${this.type}.")
fun IonValue.asIonStruct() = this as? IonStruct ?: err("Expected an IonStruct but found ${this.type}.")
fun IonValue.asIonSymbol() = this as? IonSymbol ?: err("Expected an IonSymbol but found ${this.type}.")
fun IonStruct.field(nameOfField: String) = this.get(nameOfField) ?: err("Expected struct field '$nameOfField' was not present.")

val IonSexp.tagText: String get() {
    if(this.isEmpty) {
        err("IonSexp was empty")
    }

    val tag = this[0] as? IonSymbol ?: err("First element of IonSexp was not a symbol")

    return tag.stringValue()
}

val IonSexp.args: List<IonValue> get() = this.drop(1)

val IonSexp.arity: Int get() = this.size - 1

fun IonValue.isAstLiteral(): Boolean =
    this is IonSexp &&
    this[0].stringValue() == "lit" //TODO AST node names should be refactored to statics
