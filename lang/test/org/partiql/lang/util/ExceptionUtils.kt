package org.partiql.lang.util

/**
 * Finds the root cause of a given [Throwable] instance.
 *
 * If the receiver [Throwable.cause] is null, returns the receiver.  Otherwise, returns the innermost cause in
 * the chain of causes.
 */
val Throwable.rootCause: Throwable?
    get() {
        var current = this
        while (current.cause != null) {
            current = current.cause!!
        }

        return cause
    }