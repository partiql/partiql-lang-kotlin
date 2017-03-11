/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ionsql.IonSqlException

/** Error for evaluation problems. */
open class EvaluationException(message: String,
                               cause: Throwable? = null)
    : IonSqlException(message, cause)

/** Shorthand for throwing evaluation errors. */
internal fun err(message: String): Nothing = throw EvaluationException(message)
