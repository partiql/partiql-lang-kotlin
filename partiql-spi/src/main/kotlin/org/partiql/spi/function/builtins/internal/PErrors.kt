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

    fun divisionByZeroException(dividend: Any, dividendType: PType): PErrorException {
        return PErrorException(
            PError(
                PError.DIVISION_BY_ZERO,
                Severity.ERROR(),
                PErrorKind.EXECUTION(),
                null,
                mapOf(
                    "DIVIDEND" to dividend.toString(),
                    "DIVIDEND_TYPE" to dividendType
                )
            )
        )
    }

    fun numericValueOutOfRangeException(value: String, type: PType): PErrorException {
        return PErrorException(
            PError(
                PError.NUMERIC_VALUE_OUT_OF_RANGE,
                Severity.ERROR(),
                PErrorKind.EXECUTION(),
                null,
                mapOf(
                    "VALUE" to value,
                    "TYPE" to type
                )
            )
        )
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
