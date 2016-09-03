/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

/**
 * A [Bindings] implementation that delegates to a list of bindings.
 */
class DelegateBindings(vararg val delegates: Bindings) : Bindings {
    override fun get(name: String): ExprValue? {
        for (env in delegates) {
            val value = env[name]
            if (value != null) {
                return value
            }
        }
        return null
    }
}