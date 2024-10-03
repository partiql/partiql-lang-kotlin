package org.partiql.cli

import org.partiql.spi.errors.ErrorCode

/**
 * This is used by PicoCLI for de-serializing user-input for converting warnings to errors.
 *
 * @see MainCommand.warningsAsErrors
 */
enum class ErrorCodeString(val code: Int) {
    ALL(-1),
    UNKNOWN(ErrorCode.UNKNOWN),
    INTERNAL_ERROR(ErrorCode.INTERNAL_ERROR),
    UNRECOGNIZED_TOKEN(ErrorCode.UNRECOGNIZED_TOKEN),
    UNEXPECTED_TOKEN(ErrorCode.UNEXPECTED_TOKEN),
    ALWAYS_MISSING(ErrorCode.ALWAYS_MISSING),
}
