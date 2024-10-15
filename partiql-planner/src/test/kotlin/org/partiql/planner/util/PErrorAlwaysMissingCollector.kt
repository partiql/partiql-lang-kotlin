package org.partiql.planner.util

import org.partiql.spi.errors.PError
import org.partiql.spi.errors.Severity

/**
 * This test implementation of an ErrorCollector converts certain warnings to errors.
 */
// TODO: Should there really be the functions, casts, and variable references here?
class PErrorAlwaysMissingCollector : PErrorCollector() {
    override fun report(error: PError) {
        when (error.code()) {
            PError.PATH_KEY_NEVER_SUCCEEDS,
            PError.PATH_INDEX_NEVER_SUCCEEDS,
            PError.FUNCTION_TYPE_MISMATCH,
            PError.UNDEFINED_CAST,
            PError.VAR_REF_NOT_FOUND,
            PError.ALWAYS_MISSING,
            PError.PATH_SYMBOL_NEVER_SUCCEEDS -> {
                error.severity = Severity.ERROR()
                super.report(error)
            }
            else -> super.report(error)
        }
    }
}
