package org.partiql.eval.internal.helpers

import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorException
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.Severity
import org.partiql.spi.types.PType

internal object PErrors {

    /**
     * Returns a PErrorException with code: [PError.NUMERIC_VALUE_OUT_OF_RANGE].
     */
    fun numericOutOfRangeException(value: String, type: PType): PErrorException {
        val pError = numericOutOfRange(value, type)
        return PErrorException(pError)
    }

    /**
     * Returns a PErrorException with code: [PError.CARDINALITY_VIOLATION].
     */
    fun cardinalityViolationException(): PErrorException {
        val pError = cardinalityViolation()
        return PErrorException(pError)
    }

    /**
     * Returns a PError with code: [PError.NUMERIC_VALUE_OUT_OF_RANGE].
     */
    private fun numericOutOfRange(value: String, type: PType): PError {
        return PError(
            PError.NUMERIC_VALUE_OUT_OF_RANGE,
            Severity.ERROR(),
            PErrorKind.EXECUTION(),
            null,
            mapOf("VALUE" to value, "TYPE" to type)
        )
    }

    /**
     * Returns a PError with code: [PError.CARDINALITY_VIOLATION].
     */
    private fun cardinalityViolation(): PError {
        return PError(
            PError.CARDINALITY_VIOLATION,
            Severity.ERROR(),
            PErrorKind.EXECUTION(),
            null,
            emptyMap()
        )
    }
}
