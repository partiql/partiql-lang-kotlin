/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ionsql.errors.*
import com.amazon.ionsql.errors.Property.*


/**
 * General exception class for Ion SQL.
 *
 * See [IONSQL-207](https://issues.amazon.com/issues/IONSQL-207)
 *
 * Three configurations for an [IonSqlException]
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
 * [IonSqlException]. This is the constructor for the second configuration explained above.
 *

 */
open class IonSqlException(override var message: String,
                           val errorCode: ErrorCode? = null,
                           val errorContext: PropertyValueMap? = null,
                           cause: Throwable? = null)
    : RuntimeException(message, cause) {


    /**
     * Given  the [errorCode], error context as a [propertyValueMap] and optional [cause] creates an
     * [IonSqlException] with an auto-generated error message.
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

    // See [IONSQL-207](https://issues.amazon.com/issues/IONSQL-207)
    override fun toString(): String {
        when (this.message.isNotBlank()) {
            true -> {
                val msg = this.message
                this.message = "${this.message}\n\t${generateMessage()}\n"
                val result = super.toString()
                this.message = msg
                return result
            }
            else -> {
                this.message = "${generateMessage()}\n"
                val result = super.toString()
                this.message = ""
                return result
            }
        }
    }
}

