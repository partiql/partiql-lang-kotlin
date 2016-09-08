/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.*

/**
 * Core [ExprValue] over an [IonValue].
 */
class IonExprValue(override val ionValue: IonValue) : ExprValue {
    private val ion = ionValue.system

    private fun String.toIon() = ion.newString(this)
    private fun Int.toIon() = ion.newInt(this)

    override fun bind(parent: Bindings): Bindings = Bindings.over { name ->
        val container = ionValue.container

        // all struct fields get surfaced as top-level names
        val member = when (ionValue) {
            is IonStruct -> ionValue[name]?.exprValue()
            else -> null
        }

        member ?: when (name) {
            SYS_NAME -> when (container) {
                is IonStruct -> ionValue.fieldName?.toIon()?.exprValue()
                // note that we don't surface the ordinal to the datagram
                is IonList, is IonSexp -> ionValue.ordinal.toIon().exprValue()
                else -> null
            }
            else -> null
        }
    }.delegate(
        Bindings.over(this)
    ).delegate(
        parent
    )

    override fun iterator(): Iterator<ExprValue> = when (ionValue) {
        is IonContainer -> ionValue.asSequence().map { it.exprValue() }.iterator()
        else -> listOf(this).iterator()
    }
}