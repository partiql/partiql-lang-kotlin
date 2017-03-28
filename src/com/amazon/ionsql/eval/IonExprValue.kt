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

    override val type = when {
        ionValue.isNullValue -> ExprValueType.NULL
        else -> ExprValueType.fromIonType(ionValue.type)
    }

    override val scalar: Scalar by lazy {
        object : Scalar() {
            override fun booleanValue(): Boolean? = ionValue.booleanValue()
            override fun numberValue(): Number? = ionValue.numberValue()
            override fun timestampValue(): Timestamp? = ionValue.timestampValue()
            override fun stringValue(): String? = ionValue.stringValue()
            override fun bytesValue(): ByteArray? = ionValue.bytesValue()
        }
    }

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

    override val ordinalBindings by lazy {
        OrdinalBindings.over { index ->
            when (ionValue) {
                is IonSequence -> ionValue[index]?.exprValue()
                else -> null
            }
        }
    }

    override fun iterator() = when (ionValue) {
        is IonContainer -> ionValue.asSequence().map { it.exprValue() }.iterator()
        else -> emptyList<ExprValue>().iterator()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> provideFacet(type: Class<T>?) = when(type) {
        Named::class.java -> namedFacet
        else -> null
    } as T?
}
