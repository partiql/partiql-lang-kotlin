/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ion.*
import com.amazon.ionsql.util.*
import java.util.*

/**
 * Provides a [ExprValueType.STRUCT] implementation lazily backed by a sequence.
 */
class SequenceStruct(private val ion: IonSystem,
                     private val isOrdered: Boolean,
                     sequence: Sequence<ExprValue>) : BaseExprValue() {
    private val nonMissingSeq by lazy { sequence.filter { it.type != ExprValueType.MISSING }.toList() }
    override val type = ExprValueType.STRUCT

    override val ionValue by lazy {
        createMutableValue().seal()
    }

    /**
     * [SequenceExprValue] may call this function to get a mutable instance of the IonValue that it can add
     * directly to its lazily constructed list.  This is a performance optimization--otherwise the value would be
     * cloned twice: once by this class's implementation of [ionValue], and again before adding the value to its list.
     *
     * Note: it is not possible to add a sealed (non-mutuable) [IonValue] as a child of a container.
     */
    fun createMutableValue(): IonStruct {
        return ion.newEmptyStruct().apply {
            nonMissingSeq.forEach {
                val nameVal = it.name
                if (nameVal != null && nameVal.type.isText) {
                    val name = nameVal.stringValue()
                    add(name, it.ionValue.clone())
                }
            }
        }
    }


    /** The backing data structured for operations that require materialization. */
    private data class Materialized(val bindings: Bindings,
                                    val ordinalBindings: OrdinalBindings,
                                    val orderedBindNames: OrderedBindNames?)

    private val materialized by lazy {
        val bindMap = HashMap<String, ExprValue>()
        val bindList = ArrayList<ExprValue>()
        val bindNames = ArrayList<String>()
        nonMissingSeq.forEach {
            val name = it.name?.stringValue() ?: errNoContext("Expected non-null name for lazy struct", internal = false)
            bindMap.putIfAbsent(name, it)
            if (isOrdered) {
                bindList.add(it)
                bindNames.add(name)
            }
        }

        val bindings = Bindings.over { bindingName -> bindMap.get(bindingName) }
        val ordinalBindings = OrdinalBindings.over {
            when {
                it < 0 -> null
                it >= bindList.size -> null
                else -> bindList[it]
            }
        }
        val orderedBindNames = when {
            isOrdered -> object : OrderedBindNames {
                override val orderedNames = bindNames
            }
            else -> null
        }

        Materialized(bindings, ordinalBindings, orderedBindNames)
    }
    override val bindings: Bindings
        get() = materialized.bindings

    override val ordinalBindings: OrdinalBindings
        get() = materialized.ordinalBindings

    @Suppress("UNCHECKED_CAST")
    override fun <T> provideFacet(type: Class<T>?): T? = when (type) {
        OrderedBindNames::class.java -> when {
            isOrdered -> materialized.orderedBindNames
            else -> null
        } as T?
        else -> null
    }

    override fun iterator() = nonMissingSeq.iterator()
}
