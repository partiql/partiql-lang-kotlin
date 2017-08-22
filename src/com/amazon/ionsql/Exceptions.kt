/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ionsql.errors.*
import com.amazon.ionsql.errors.Property.*


/**
 * General exception class for Ion SQL.
 *
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
 * @param message human friendly detail text message for this exception
 * @param cause the cause for this exception
 *
 * @constructor given a [message] and an optional [cause] creates an [IonSqlException]. This is the constructor for the
 * first configuration explained above
 */
open class IonSqlException(override var message: String, cause: Throwable? = null)
    : RuntimeException(message, cause) {


    private var errorCode: ErrorCode? = null

    fun getErrorCode() = this.errorCode

    private var errorContext: PropertyValueMap? = null

    fun getErrorContext() = this.errorContext

    /**
     * Given a custom error [message], the [errorCode], error context as a [propertyValueMap] and optional [cause] creates an
     * [IonSqlException]. This is the constructor for the second configuration explained above.
     *
     * @param message the message for this exception
     * @param errorCode the error code for this exception
     * @param propertyValueMap context for this error
     * @param cause for this exception
     *
     */
    constructor(message: String, errorCode: ErrorCode, propertyValueMap: PropertyValueMap, cause: Throwable? = null) :
        this(message, cause) {
        this.errorCode = errorCode
        this.errorContext = propertyValueMap
    }

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
        this("", cause) {
        this.errorCode = errorCode
        this.errorContext = propertyValueMap

    }


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

    override fun toString(): String {
        if (this.message.isNotBlank()) {
            this.message = "${this.message}\n\t${generateMessage()}\n"
        } else {
            this.message = "${generateMessage()}\n"
        }
        return super.toString()
    }


}
