/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ionsql.util.downcast

/**
 * Base implementation of [ExprValue] that provides a bare minimum implementation of
 * a value.
 */
abstract class BaseExprValue : ExprValue {
    override val scalar: Scalar
        get() = Scalar.empty()
    override val bindings: Bindings
        get() = Bindings.empty()
    override val ordinalBindings: OrdinalBindings
        get() = OrdinalBindings.empty()

    override fun iterator(): Iterator<ExprValue> = emptyList<ExprValue>().iterator()

    final override fun <T : Any?> asFacet(type: Class<T>?): T? =
        downcast(type) ?: provideFacet(type)

    /**
     * Provides a fall-back for providing facets if a sub-class doesn't inherit the facet interface
     * or class.
     *
     * This implementation returns `null`.
     */
    open fun <T> provideFacet(type: Class<T>?): T? = null

    override fun toString(): String = stringify()
}
