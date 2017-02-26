/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ionsql.util.*

/**
 * Wraps the given [ExprValue] with a delegate that provides the [OrderedBindNames] facet.
 */
fun ExprValue.orderedNamesValue(names: List<String>): ExprValue =
    object : ExprValue by this, OrderedBindNames {
        override val orderedNames: List<String>
            get() = names

        override fun <T : Any?> asFacet(type: Class<T>?): T? =
            downcast(type) ?: this@orderedNamesValue.asFacet(type)
    }

/** Wraps the given [ExprValue] as a [Named] instance */
fun ExprValue.asNamed(): Named = object : Named {
    override val name: ExprValue
        get() = this@asNamed
}

/** Wraps this [ExprValue] in a delegate that always masks the [Named] facet. */
fun ExprValue.unnamedValue(): ExprValue = when (asFacet(Named::class.java)) {
    null -> this
    else -> object : ExprValue by this {
        override fun <T : Any?> asFacet(type: Class<T>?): T? =
            when (type) {
                // always mask the name facet
                Named::class.java -> null
                else -> this@unnamedValue.asFacet(type)
            }
    }
}

/** Unpivots a `struct`, and does nothing for any other [ExprValue]. */
fun ExprValue.unpivot(): ExprValue = when (type) {
    // enable iteration for structs
    ExprValueType.STRUCT -> object : ExprValue by this {
        override fun iterator() =
            ionValue.asSequence().map { it.exprValue() }.iterator()
    }
    // for non-struct, this is a no-op
    else -> this
}
