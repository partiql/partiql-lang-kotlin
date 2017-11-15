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
                               cause: Throwable? = null,
                               val internal: Boolean) : IonSqlException(message, errorCode, errorContext, cause) {

    // not adding as a default value for `internal` so it's backward compatible with Java clients
    constructor(message: String,
                errorCode: ErrorCode? = null,
                errorContext: PropertyValueMap? = null,
                cause: Throwable? = null) : this(message = message,
                                                 errorCode = errorCode,
                                                 errorContext = errorContext,
                                                 internal = true,
                                                 cause = cause)

    constructor(cause: Throwable,
                errorCode: ErrorCode? = null,
                errorContext: PropertyValueMap? = null,
                internal: Boolean = true) : this(message = cause.message ?: "<NO MESSAGE>",
                                                 errorCode = errorCode,
                                                 errorContext = errorContext,
                                                 internal = internal,
                                                 cause = cause)
}

/**
 * Shorthand for throwing function evaluation. Separated from [err] to avoid loosing the context unintentionally
 */
internal fun errNoContext(message: String, internal: Boolean): Nothing = err(message, null, internal)

/** Shorthand for throwing evaluation with context. */
internal fun err(message: String, errorContext: PropertyValueMap?, internal: Boolean): Nothing =
    throw EvaluationException(message, errorContext = errorContext, internal = internal)

/** Shorthand for throwing evaluation with context with an error code.. */
internal fun err(message: String, errorCode: ErrorCode, errorContext: PropertyValueMap?, internal: Boolean): Nothing =
    throw EvaluationException(message, errorCode, errorContext, internal = internal)

internal fun errIntOverflow(errorContext: PropertyValueMap? = null): Nothing {
    throw EvaluationException(message = "Int overflow or underflow",
                              errorCode = ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
                              errorContext = errorContext,
                              internal = false)
}
