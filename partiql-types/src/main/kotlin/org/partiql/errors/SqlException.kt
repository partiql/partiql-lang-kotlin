/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.errors

/**
 * General exception class for the interpreter.
 *
 * Three configurations for an [SqlException]
 *
 *   1. Provide a [message] and optionally a [cause]
 *       * Used when an error occurs and we can only provide a general human friendly text message
 *   1. Provide a [message] an [ErrorCode] and context as a [PropertyValueMap] and optionally a [cause]
 *       * Used when an error occurs and we want a **custom** message as well as an auto-generated message from error code and error context
 *   1. Provide an [ErrorCode] and context as a [PropertyValueMap] and optionally a [cause]
 *       * Used when an error occurs and we want an auto-generated message from the given error code and error context
 *
 * @param message the message for this exception
 * @param errorCode the error code for this exception
 * @param errorContext context for this error, includes details like line & character offsets, among others.
 * @param cause for this exception
 */
public open class SqlException(
    override var message: String,
    public val errorCode: ErrorCode,
    public val errorContext: PropertyValueMap,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    /**
     * Indicates if this exception is due to an internal error or not.
     *
     * Internal errors are those that are likely due to a bug in PartiQL itself or in the usage of its APIs.
     *
     * Non-internal errors are caused by query authors--i.e. syntax errors, or semantic errors such as undefined
     * variables, etc.
     */
    public open val internal: Boolean get() = false

    /**
     * Given  the [errorCode], error context as a [propertyValueMap] and optional [cause] creates an
     * [SqlException] with an auto-generated error message.
     * This is the constructor for the third configuration explained above.
     *
     * @param errorCode the error code for this exception
     * @param propertyValueMap context for this error
     * @param cause for this exception
     */
    public constructor(errorCode: ErrorCode, propertyValueMap: PropertyValueMap, cause: Throwable? = null) :
        this("", errorCode, propertyValueMap, cause)

    /**
     * Auto-generated message has the structure
     *
     * ```
     *   ErrorCategory ': ' ErrorLocation ': ' ErrorMessage
     * ```
     *
     * where
     *
     *  * ErrorCategory is one of `Lexer Error`, `Parser Error`, `Runtime Error`
     *  * ErrorLocation is the line and column where the error occurred
     *  * ErrorMessage is the **generated** error message
     *
     *
     * TODO: Prepend to the auto-generated message the file name.
     *
     */
    public fun generateMessage(): String =
        "${errorCategory(errorCode)}: ${errorLocation(errorContext)}: ${errorMessage(errorCode, errorContext)}"

    /** Same as [generateMessage] but without the location. */
    public fun generateMessageNoLocation(): String =
        "${errorCategory(errorCode)}: ${errorMessage(errorCode, errorContext)}"

    private fun errorMessage(errorCode: ErrorCode?, propertyValueMap: PropertyValueMap?): String =
        errorCode?.getErrorMessage(propertyValueMap) ?: UNKNOWN

    private fun errorLocation(propertyValueMap: PropertyValueMap?): String {
        val lineNo = propertyValueMap?.get(Property.LINE_NUMBER)?.longValue()
        val columnNo = propertyValueMap?.get(Property.COLUMN_NUMBER)?.longValue()

        return "at line ${lineNo ?: UNKNOWN}, column ${columnNo ?: UNKNOWN}"
    }

    private fun errorCategory(errorCode: ErrorCode?): String =
        errorCode?.category?.message ?: UNKNOWN

    override fun toString(): String =
        when (this.message.isNotBlank()) {
            true -> {
                val msg = this.message
                this.message = "${this.message}\n\t${generateMessage()}\n"
                val result = super.toString()
                this.message = msg
                result
            }
            else -> {
                this.message = "${generateMessage()}\n"
                val result = super.toString()
                this.message = ""
                result
            }
        }
}
