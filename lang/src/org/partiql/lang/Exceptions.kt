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

package org.partiql.lang

import org.partiql.lang.errors.*
import org.partiql.lang.errors.Property.*


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
 * @param propertyValueMap context for this error
 * @param cause for this exception
 *
 * @constructor a custom error [message], the [errorCode], error context as a [propertyValueMap] and optional [cause] creates an
 * [SqlException]. This is the constructor for the second configuration explained above.
 *
 */
open class SqlException(override var message: String,
                        val errorCode: ErrorCode? = null,
                        val errorContext: PropertyValueMap? = null,
                        cause: Throwable? = null)
    : RuntimeException(message, cause) {


    /**
     * Given  the [errorCode], error context as a [propertyValueMap] and optional [cause] creates an
     * [SqlException] with an auto-generated error message.
     * This is the constructor for the third configuration explained above.
     *
     * @param errorCode the error code for this exception
     * @param propertyValueMap context for this error
     * @param cause for this exception
     */
    constructor(errorCode: ErrorCode, propertyValueMap: PropertyValueMap, cause: Throwable? = null) :
        this("",errorCode, propertyValueMap, cause)

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
     *  * Errormessatge is the **generated** error message
     *
     *
     * TODO: Prepend to the auto-generated message the file name.
     *
     */
    fun generateMessage(): String =
    "${errorCategory(errorCode)}: ${errorLocation(errorContext)}: ${errorMessage(errorCode, errorContext)}"

    private fun errorMessage(errorCode: ErrorCode?, propertyValueMap: PropertyValueMap?): String  =
            errorCode?.getErrorMessage(propertyValueMap) ?: UNKNOWN

    private fun errorLocation(propertyValueMap: PropertyValueMap?): String {
        val lineNo = propertyValueMap?.get(LINE_NUMBER)?.longValue()
        val columnNo = propertyValueMap?.get(COLUMN_NUMBER)?.longValue()

        return "at line ${lineNo ?: UNKNOWN}, column ${columnNo ?: UNKNOWN}"
    }

    private fun errorCategory(errorCode: ErrorCode?): String =
       errorCode?.errorCategory() ?: UNKNOWN

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

