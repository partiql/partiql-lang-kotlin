package org.partiql.spi.function.builtins.internal

import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorException
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.Severity
import org.partiql.spi.types.PType

internal object PErrors {
    fun internalErrorException(cause: Throwable): PErrorException {
        return PErrorException(internalError(cause))
    }

    fun unexpectedTypeException(actual: PType, expected: List<PType>): PErrorException {
        return PErrorException(unexpectedType(actual, expected))
    }

    private fun internalError(cause: Throwable): PError {
        return PError(
            PError.INTERNAL_ERROR,
            Severity.ERROR(),
            PErrorKind.EXECUTION(),
            null,
            mapOf(
                "CAUSE" to cause
            )
        )
    }

    private fun unexpectedType(actual: PType, expected: List<PType>): PError {
        return PError(
            PError.TYPE_UNEXPECTED,
            Severity.ERROR(),
            PErrorKind.EXECUTION(),
            null,
            mapOf(
                "ACTUAL_TYPE" to actual.toString(),
                "EXPECTED_TYPES" to expected
            )
        )
    }
}
