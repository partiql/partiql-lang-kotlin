package com.amazon.ionsql.errors

import com.amazon.ionsql.eval.ExprValue

/**
 * Represents the contract and API between error handlers and the evaluator.
 */
interface ErrorHandler {
    /**
     * Called to handle an error in the IonSQL++ **evaluator** from which we may recover.
     * The call passes the [ErrorCode] and context about the error as a [PropertyValueMap].
     *
     * The handler can throw a runtime exception to exit the evaluation or return a value
     * to replace the sub-expression that caused the error, e.g., IONSQL++ NULL value, and
     * attempt to proceed with evaluation.
     *
     * @param errorCode error code for this evaluation error
     * @param context bag of properties related to the evaluation error
     * @throws IonSqlException to exit evaluation.
     */
    fun  handle(errorCode: ErrorCode, context: PropertyValueMap) : ExprValue


}