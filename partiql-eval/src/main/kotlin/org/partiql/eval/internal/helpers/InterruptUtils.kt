package org.partiql.eval.internal.helpers

/**
 * Checks whether the current thread has been interrupted. If so, throws [InterruptedException].
 *
 * This is used to support cooperative cancellation of long-running queries. Callers schedule a
 * [Thread.interrupt] on a timer; the eval engine checks for the interrupt flag at key iteration
 * points (joins, scans, aggregations) and aborts promptly.
 */
internal fun checkInterrupted(message: String = "thread interrupted") {
    if (Thread.interrupted()) {
        throw InterruptedException(message)
    }
}
