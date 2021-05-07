package org.partiql.lang.util

/** Throws [InterruptedException] if [Thread.interrupted] is set. */
internal fun checkThreadInterrupted() {
    if(Thread.interrupted()) {
        throw InterruptedException()
    }
}

/**
 * Like a regular [map], but checks [Thread.interrupted] before each iteration and throws
 * [InterruptedException] if it is set.
 *
 * This should be used instead of the regular [map] where there is a potential for a large
 * number of items in the receiver [List] to allow long running operations to be aborted
 * by the caller.
 */
internal inline fun <T, R> List<T>.interruptibleMap(crossinline block: (T) -> R): List<R> =
    this.map { checkThreadInterrupted(); block(it) }

/**
 * Like a regular [fold], but checks [Thread.interrupted] before each iteration and throws
 * [InterruptedException] if it is set.
 *
 * This should be used instead of the regular [fold] where there is a potential for a large
 * number of items in the receiver [List] to allow long running operations to be aborted
 * by the caller.
 */
internal inline fun <T, A> List<T>.interruptibleFold(initial: A, crossinline block: (A, T) -> A) =
    this.fold(initial) { acc, curr -> checkThreadInterrupted(); block(acc, curr) }
