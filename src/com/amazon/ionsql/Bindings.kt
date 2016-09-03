/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

/** A simple mapping of name to [ExprValue]. */
interface Bindings {
    /**
     * Looks up a name within the environment.
     *
     * @param name The binding to look up.
     *
     * @return The value mapped to the binding, or `null` if no such binding exists.
     *         For the expression if a `null` **Ion** value is required, a non-`null`
     *         reference value for a `null` **must** be returned.
     */
    operator fun get(name: String): ExprValue?
}