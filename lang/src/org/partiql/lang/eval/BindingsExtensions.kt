/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval

/**
 * Wraps these [Bindings] to delegate lookup to another instance when lookup on this
 * one fails.
 *
 * Note that this doesn't modify an existing [Bindings] but creates a new instance that
 * does delegation.
 *
 * @param fallback The bindings to delegate to when lookup fails to find a name.
 */
fun <T> Bindings<T>.delegate(fallback: Bindings<T>): Bindings<T> =
    object : Bindings<T> {
        override fun get(bindingName: BindingName): T? {
            val binding = this@delegate[bindingName]
            return binding ?: fallback[bindingName]
        }
    }

/**
 * Wraps a binding with a set of names that should not be resolved to anything.
 *
 * @receiver The [Bindings] to delegate over.
 * @param names, the deny listed names
 */
@Deprecated(
    message = "To be replaced with functionally equivalent denyList method.",
    replaceWith = ReplaceWith("denyList", "org.partiql.lang.eval.denyList"),
    level = DeprecationLevel.WARNING
)
fun <T> Bindings<T>.blacklist(vararg names: String) = this.denyList(*names)

/**
 * Wraps a binding with a set of names that should not be resolved to anything.
 *
 * @receiver The [Bindings] to delegate over.
 * @param names, the deny listed names
 */
fun <T> Bindings<T>.denyList(vararg names: String) = object : Bindings<T> {
    val denyListed = names.toSet()
    val loweredDenyListed = names.map { it.toLowerCase() }.toSet()

    override fun get(bindingName: BindingName): T? {
        val isDenyListed = when (bindingName.bindingCase) {
            BindingCase.SENSITIVE -> denyListed.contains(bindingName.name)
            BindingCase.INSENSITIVE -> loweredDenyListed.contains(bindingName.loweredName)
        }
        return when {
            isDenyListed -> null
            else -> this[bindingName]
        }
    }
}
