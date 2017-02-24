/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.*

/**
 * Core [ExprValue] over an [IonValue].
 */
class IonExprValue(override val ionValue: IonValue) : BaseExprValue() {
    private val ion = ionValue.system

    private fun String.toIon() = ion.newString(this)
    private fun Int.toIon() = ion.newInt(this)

    override val bindings by lazy {
        Bindings.over { name ->
            // All struct fields get surfaced as top-level names
            // TODO deal with SQL++ syntax rules about this (i.e. only works with schema)
            when (ionValue) {
                is IonStruct -> ionValue[name]?.exprValue()
                else -> null
            }
        }
    }

    override fun iterator(): Iterator<ExprValue> = when (ionValue) {
        is IonList -> ionValue.asSequence().map { it.exprValue() }.iterator()
        else -> listOf(this).iterator()
    }
}
