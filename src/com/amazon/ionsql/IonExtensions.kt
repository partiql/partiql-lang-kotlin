/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.*

operator fun IonValue.get(name: String): IonValue =
        when (this) {
            is IonStruct -> get(name)
            else -> throw IllegalArgumentException()
        }

operator fun IonValue.get(index: Int): IonValue =
        when (this) {
            is IonSequence -> get(index)
            else -> throw IllegalArgumentException()
        }

operator fun IonValue.iterator(): Iterator<IonValue> =
        when (this) {
            is IonContainer -> iterator()
            else -> throw IllegalArgumentException()
        }

fun IonValue.stringValue(): String? =
        when (this) {
            is IonText -> stringValue()
            else -> throw IllegalArgumentException()
        }

fun IonValue.longValue(): Long? =
        when (this) {
            is IonInt -> longValue()
            else -> throw IllegalArgumentException()
        }

fun IonValue.doubleValue(): Double? =
        when (this) {
            is IonFloat -> doubleValue()
            else -> throw IllegalArgumentException()
        }

fun IonValue.decimalValue(): Decimal? =
        when (this) {
            is IonDecimal -> decimalValue()
            else -> throw IllegalArgumentException()
        }