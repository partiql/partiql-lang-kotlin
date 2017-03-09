/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.util

import com.amazon.ion.facet.Faceted
import com.amazon.ion.facet.Facets

/**
 * Simple dynamic downcast for a type.
 */
fun <T : Any?> Any.downcast(type: Class<T>?): T? = when {
    type == null -> throw NullPointerException()
    type.isInstance(this) -> type.cast(this)
    else -> null
}
