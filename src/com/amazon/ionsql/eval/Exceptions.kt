/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ionsql.*
import com.amazon.ionsql.errors.*

/** Error for evaluation problems. */
open class EvaluationException(message: String,
                               errorCode: ErrorCode? = null,
                               errorContext: PropertyValueMap? = null,
                               cause: Throwable? = null) : IonSqlException(message, errorCode, errorContext, cause) {
    constructor(cause: Throwable,
                errorCode: ErrorCode? = null,
                errorContext: PropertyValueMap? = null) : this(message = cause.message ?: "<NO MESSAGE>",
                                                               errorCode = errorCode,
                                                               errorContext = errorContext,
                                                               cause = cause)
}

///** Shorthand for throwing evaluation errors. */
//internal fun err(message: String): Nothing = throw EvaluationException(message)

/**
 * Shorthand for throwing function evaluation. Separated from [err] to avoid loosing the context unintentionally
 */
internal fun errNoContext(message: String): Nothing = err(message, null)

/** Shorthand for throwing evaluation with context. */
internal fun err(message: String, errorContext: PropertyValueMap?): Nothing = when (errorContext) {
    null -> throw EvaluationException(message)
    else -> throw EvaluationException(message, errorContext = errorContext)
}

/** Shorthand for throwing evaluation with error code and context. */
internal fun err(message: String, errorCode: ErrorCode, errorContext: PropertyValueMap?): Nothing =
    throw EvaluationException(message, errorCode = errorCode, errorContext = errorContext)


