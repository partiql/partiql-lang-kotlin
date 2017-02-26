/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

/**
 * Facet for a value to indicate that it either has a name within some context
 * or an ordinal position.
 *
 * An implementation should not provide this facet if it does not provide a meaningful name.
 */
interface Named {
    /**
     * The name of this value, generally a `string` for values that have a field name in
     * a `struct` or an `int` for values that have some ordinal in a collection.
     */
    val name: ExprValue
}
