/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ion.*
import com.amazon.ionsql.util.*

/**
 * Core [ExprValue] over an [IonValue].
 */
class IonExprValue(override val ionValue: IonValue) : BaseExprValue() {
    private val ion = ionValue.system

    private val namedFacet: Named? = when {
        ionValue.fieldName != null -> ion.newString(ionValue.fieldName).seal().exprValue().asNamed()
        ionValue.type != IonType.DATAGRAM
            && ionValue.container != null
            && ionValue.ordinal >= 0 -> ion.newInt(ionValue.ordinal).seal().exprValue().asNamed()
        else -> null
    }

    private fun String.toIon() = ion.newString(this)
    private fun Int.toIon() = ion.newInt(this)

    override val type = ExprValueType.fromIonType(ionValue.type)

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

    override fun iterator() = when (ionValue) {
        is IonList -> ionValue.asSequence().map { it.exprValue() }.iterator()
        else -> listOf(this).iterator()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> provideFacet(type: Class<T>?) = when(type) {
        Named::class.java -> namedFacet
        else -> null
    } as T?
}
