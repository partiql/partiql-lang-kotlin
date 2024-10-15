package org.partiql.planner.util

import org.partiql.spi.errors.Error

/**
 * This test implementation of an ErrorCollector converts certain warnings to errors.
 */
// TODO: Should there really be the functions, casts, and variable references here?
class ErrorAlwaysMissingCollector : ErrorCollector() {
    override fun warning(error: Error) {
        when (error.code) {
            Error.PATH_KEY_NEVER_SUCCEEDS,
            Error.PATH_INDEX_NEVER_SUCCEEDS,
            Error.FUNCTION_TYPE_MISMATCH,
            Error.UNDEFINED_CAST,
            Error.VAR_REF_NOT_FOUND,
            Error.ALWAYS_MISSING,
            Error.PATH_SYMBOL_NEVER_SUCCEEDS -> error(error)
            else -> super.warning(error)
        }
    }
}
