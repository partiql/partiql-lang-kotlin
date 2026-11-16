package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.errors.Severity
import org.partiql.spi.value.Datum

/**
 * Raises, in strict mode, the problem the planner reported for an expression it proved will fail.
 *
 * @property error the planner's problem, or null if the plan did not record one (a plan built without it).
 */
internal class ExprError(private val error: PError?) : ExprValue {

    override fun eval(env: Environment): Datum {
        if (error == null) {
            throw PErrors.internalErrorException(IllegalStateException("A warning has been converted into an error."))
        }
        // A new error for each throw: compiled plans can be cached and shared across threads, and callers may change
        // the error they catch.
        val e = error.copy()
        e.severity = Severity.ERROR()
        e.kind = PErrorKind.EXECUTION()
        throw PRuntimeException(e)
    }
}
