package org.partiql.eval.internal.helpers

import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.errors.Severity
import org.partiql.spi.function.FnProvider
import org.partiql.spi.types.PType

internal object PErrors {

    /**
     * Returns a PRuntimeException with code: [PError.NUMERIC_VALUE_OUT_OF_RANGE].
     */
    fun numericOutOfRangeException(value: String, type: PType): PRuntimeException {
        val pError = numericOutOfRange(value, type)
        return PRuntimeException(pError)
    }

    /**
     * Returns a PRuntimeException with code: [PError.FUNCTION_TYPE_MISMATCH].
     */
    fun functionTypeMismatchException(name: String, actualTypes: List<PType>, candidates: List<FnProvider>): PRuntimeException {
        val pError = functionTypeMismatch(name, actualTypes, candidates)
        return PRuntimeException(pError)
    }

    /**
     * Returns a PRuntimeException with code: [PError.PATH_INDEX_NEVER_SUCCEEDS].
     */
    fun pathIndexFailureException(): PRuntimeException {
        val pError = pathIndexFailure()
        return PRuntimeException(pError)
    }

    /**
     * Returns a PRuntimeException with code: [PError.PATH_KEY_NEVER_SUCCEEDS].
     */
    fun pathKeyFailureException(): PRuntimeException {
        val pError = pathKeyFailure()
        return PRuntimeException(pError)
    }

    /**
     * Returns a PRuntimeException with code: [PError.PATH_SYMBOL_NEVER_SUCCEEDS].
     */
    fun pathSymbolFailureException(): PRuntimeException {
        val pError = pathSymbolFailure()
        return PRuntimeException(pError)
    }

    fun castUndefinedException(input: PType, target: PType): PRuntimeException {
        val pError = PError(
            PError.UNDEFINED_CAST,
            Severity.ERROR(),
            PErrorKind.EXECUTION(),
            null,
            mapOf(
                "INPUT_TYPE" to input,
                "TARGET_TYPE" to target
            )
        )
        return PRuntimeException(pError)
    }

    fun invalidStringCastException(value: String): PRuntimeException {
        val pError = PError(
            PError.INVALID_CHAR_VALUE_FOR_CAST,
            Severity.ERROR(),
            PErrorKind.EXECUTION(),
            null,
            mapOf(
                "VALUE" to value,
            )
        )
        return PRuntimeException(pError)
    }

    fun internalErrorException(cause: Throwable): PRuntimeException {
        val pError = PError(
            PError.INTERNAL_ERROR,
            Severity.ERROR(),
            PErrorKind.EXECUTION(),
            null,
            mapOf(
                "CAUSE" to cause
            )
        )
        return PRuntimeException(pError)
    }

    /**
     * Returns a PRuntimeException with code: [PError.TYPE_UNEXPECTED].
     */
    fun arrayExpectedException(actual: PType): PRuntimeException {
        val pError = typeUnexpected(actual, listOf(PType.array()))
        return PRuntimeException(pError)
    }

    /**
     * Returns a PRuntimeException with code: [PError.TYPE_UNEXPECTED].
     */
    fun collectionExpectedException(actual: PType): PRuntimeException {
        val pError = typeUnexpected(actual, listOf(PType.bag(), PType.array()))
        return PRuntimeException(pError)
    }

    /**
     * Returns a PRuntimeException with code: [PError.TYPE_UNEXPECTED].
     */
    fun structureExpectedException(actual: PType): PRuntimeException {
        val pError = typeUnexpected(actual, listOf(PType.row(), PType.struct()))
        return PRuntimeException(pError)
    }

    /**
     * Returns a PRuntimeException with code: [PError.TYPE_UNEXPECTED].
     */
    fun unexpectedTypeException(actual: PType, expected: List<PType>): PRuntimeException {
        val pError = typeUnexpected(actual, expected)
        return PRuntimeException(pError)
    }

    /**
     * Returns a PRuntimeException with code: [PError.CARDINALITY_VIOLATION].
     */
    fun cardinalityViolationException(): PRuntimeException {
        val pError = cardinalityViolation()
        return PRuntimeException(pError)
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

    /**
     * Returns a PError with code: [PError.FUNCTION_TYPE_MISMATCH].
     */
    private fun functionTypeMismatch(name: String, actualTypes: List<PType>, candidates: List<FnProvider>): PError {
        return PError(
            PError.FUNCTION_TYPE_MISMATCH,
            Severity.ERROR(),
            PErrorKind.EXECUTION(),
            null,
            mapOf(
                "FN_ID" to name,
                "ARG_TYPES" to actualTypes,
                "CANDIDATES" to candidates
            )
        )
    }

    /**
     * Returns a PError with code: [PError.PATH_INDEX_NEVER_SUCCEEDS].
     */
    private fun pathIndexFailure(): PError {
        return PError(
            PError.PATH_INDEX_NEVER_SUCCEEDS,
            Severity.ERROR(),
            PErrorKind.EXECUTION(),
            null,
            emptyMap()
        )
    }

    /**
     * Returns a PError with code: [PError.PATH_KEY_NEVER_SUCCEEDS].
     */
    private fun pathKeyFailure(): PError {
        return PError(
            PError.PATH_KEY_NEVER_SUCCEEDS,
            Severity.ERROR(),
            PErrorKind.EXECUTION(),
            null,
            emptyMap()
        )
    }

    /**
     * Returns a PError with code: [PError.PATH_SYMBOL_NEVER_SUCCEEDS].
     */
    private fun pathSymbolFailure(): PError {
        return PError(
            PError.PATH_SYMBOL_NEVER_SUCCEEDS,
            Severity.ERROR(),
            PErrorKind.EXECUTION(),
            null,
            emptyMap()
        )
    }

    private fun typeUnexpected(actual: PType, expected: List<PType>): PError {
        return PError(
            PError.TYPE_UNEXPECTED,
            Severity.ERROR(),
            PErrorKind.EXECUTION(),
            null,
            mapOf(
                "EXPECTED_TYPES" to expected,
                "ACTUAL_TYPE" to actual
            )
        )
    }
}
