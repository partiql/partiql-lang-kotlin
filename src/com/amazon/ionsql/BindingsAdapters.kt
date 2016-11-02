/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

/**
 * Wraps a binding with a set of names that should not be resolved to anything.
 *
 * @param bindings The bindings to delegate over.
 * @param names, the blacklisted names
 */
fun Bindings.blacklist(vararg names: String): Bindings {
    val blacklisted = names.toSet()
    return Bindings.over {
        when (it) {
            in blacklisted -> null
            else -> get(it)
        }
    }
}

/**
 * Wraps these [Bindings] to delegate lookup to another instance when lookup on this
 * one fails.
 *
 * Note that this doesn't modify an existing [Bindings] but creates a new instance that
 * does delegation.
 *
 * @param fallback The bindings to delegate to when lookup fails to find a name.
 */
fun Bindings.delegate(fallback: Bindings): Bindings = Bindings.over {
    val binding = this[it]
    binding ?: fallback[it]
}
