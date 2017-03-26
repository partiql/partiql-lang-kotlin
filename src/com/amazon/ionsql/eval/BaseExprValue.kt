/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ionsql.eval.ExprValueType.*
import com.amazon.ionsql.util.*

/**
 * Base implementation of [ExprValue] that provides a down-casting [Faceted] implementation.
 */
abstract class BaseExprValue: ExprValue {
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
