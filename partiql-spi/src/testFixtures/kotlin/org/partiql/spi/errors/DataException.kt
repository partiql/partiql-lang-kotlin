package org.partiql.spi.errors

import org.partiql.spi.types.PType

/**
 * A [DataException] represents an unrecoverable query runtime exception.
 */
public class DataException(private val error: PError) : PRuntimeException(error) {

    public constructor(value: String?, type: PType?) : this(pError(value, type)) {
        initCause(cause)
    }

    public constructor(message: String) : this(pError()) {
        initCause(cause)
    }

    private companion object {

        private fun pError(value: String?, type: PType?): PError {
            return PError(
                PError.NUMERIC_VALUE_OUT_OF_RANGE,
                Severity.ERROR(),
                PErrorKind.EXECUTION(),
                null,
                mapOf("VALUE" to value, "TYPE" to type)
            )
        }

        private fun pError(): PError {
            return PError(
                PError.NUMERIC_VALUE_OUT_OF_RANGE,
                Severity.ERROR(),
                PErrorKind.EXECUTION(),
                null,
                emptyMap()
            )
        }
    }

    /**
     * This does not provide the stack trace, as this is very expensive in permissive mode.
     */
    override fun fillInStackTrace(): Throwable = this
}
