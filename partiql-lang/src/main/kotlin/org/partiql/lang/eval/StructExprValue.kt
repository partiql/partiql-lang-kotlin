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

import org.partiql.errors.ErrorCode

/** Indicates if a struct is ordered or not. */
enum class StructOrdering {
    UNORDERED,
    ORDERED
}

/**
 * Provides a [ExprValueType.STRUCT] implementation lazily backed by a sequence.
 */
internal open class StructExprValue(
    private val ordering: StructOrdering,
    private val sequence: Sequence<ExprValue>
) : BaseExprValue() {

    override val type = ExprValueType.STRUCT

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
