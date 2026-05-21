package org.partiql.eval.internal.helpers

import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.errors.Severity

/**
 * Checks whether the current thread has been interrupted. If so, throws [PRuntimeException]
 * with [PError.INTERRUPTED].
 *
 * This is used to support cooperative cancellation of long-running queries. Callers schedule a
 * [Thread.interrupt] on a timer; the eval engine checks for the interrupt flag at key
 * iteration points (joins, aggregations, projections) and aborts promptly.
 */
internal fun checkInterrupted() {
    if (Thread.interrupted()) {
        throw PRuntimeException(PError(PError.INTERRUPTED, Severity.ERROR(), PErrorKind.EXECUTION(), null, emptyMap()))
    }
}
