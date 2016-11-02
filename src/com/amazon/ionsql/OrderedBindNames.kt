/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

/**
 * Facet to provide an ordered list of [String] names that are directly bound to
 * a given [ExprValue].
 */
interface OrderedBindNames {
    val orderedNames: List<String>
}
