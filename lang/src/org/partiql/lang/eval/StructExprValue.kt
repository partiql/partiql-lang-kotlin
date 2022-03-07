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

package org.partiql.lang.eval

import com.amazon.ion.IonStruct
import com.amazon.ion.IonSystem
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.util.seal

/** Indicates if a struct is ordered or not. */
enum class StructOrdering {
    UNORDERED,
    ORDERED
}

/**
 * Provides a [ExprValueType.STRUCT] implementation lazily backed by a sequence.
 */
internal open class StructExprValue(
    private val ion: IonSystem,
    private val ordering: StructOrdering,
    private val sequence: Sequence<ExprValue>
) : BaseExprValue() {

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
    internal fun createMutableValue(): IonStruct {
        return ion.newEmptyStruct().apply {
            sequence.forEach {
                val nameVal = it.name
                if (nameVal != null && nameVal.type.isText && it.type != ExprValueType.MISSING) {
                    val name = nameVal.stringValue()
                    add(name, it.ionValue.clone())
                }
            }
        }
    }

    /** The backing data structured for operations that require materialization. */
    private data class Materialized(
        val bindings: Bindings<ExprValue>,
        val ordinalBindings: OrdinalBindings,
        val orderedBindNames: OrderedBindNames?
    )

    private val materialized by lazy {
        val bindMap = HashMap<String, ExprValue>()
        val bindList = ArrayList<ExprValue>()
        val bindNames = ArrayList<String>()
        sequence.forEach {
            val name = it.name?.stringValue() ?: errNoContext("Expected non-null name for lazy struct", errorCode = ErrorCode.EVALUATOR_UNEXPECTED_VALUE, internal = false)
            bindMap.putIfAbsent(name, it)
            if (ordering == StructOrdering.ORDERED) {
                bindList.add(it)
                bindNames.add(name)
            }
        }

        val bindings = Bindings.ofMap(bindMap)
        val ordinalBindings = OrdinalBindings.ofList(bindList)
        val orderedBindNames = when (ordering) {
            StructOrdering.ORDERED -> object : OrderedBindNames {
                override val orderedNames = bindNames
            }
            StructOrdering.UNORDERED -> null
        }

        Materialized(bindings, ordinalBindings, orderedBindNames)
    }

    override val bindings: Bindings<ExprValue>
        get() = materialized.bindings

    override val ordinalBindings: OrdinalBindings
        get() = materialized.ordinalBindings

    @Suppress("UNCHECKED_CAST")
    override fun <T> provideFacet(type: Class<T>?): T? = when (type) {
        OrderedBindNames::class.java -> when (ordering) {
            StructOrdering.ORDERED -> materialized.orderedBindNames
            else -> null
        } as T?
        else -> null
    }

    override fun iterator() = sequence.iterator()
}
