/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ionsql.util.*

/**
 * Base implementation of [ExprValue] that provides a down-casting [Faceted] implementation.
 */
abstract class BaseExprValue: ExprValue {
    final override fun <T : Any?> asFacet(type: Class<T>?): T? = downcast(type)
}
