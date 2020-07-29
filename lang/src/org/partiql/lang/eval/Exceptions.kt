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

package org.partiql.lang.eval

import org.partiql.lang.*
import org.partiql.lang.ast.*
import org.partiql.lang.errors.*
import org.partiql.lang.util.*

/** Error for evaluation problems. */
open class EvaluationException(message: String,
                               errorCode: ErrorCode? = null,
                               errorContext: PropertyValueMap? = null,
                               cause: Throwable? = null,
                               val internal: Boolean) : SqlException(message, errorCode, errorContext, cause) {


    constructor(cause: Throwable,
                errorCode: ErrorCode? = null,
                errorContext: PropertyValueMap? = null,
                internal: Boolean) : this(message = cause.message ?: "<NO MESSAGE>",
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

fun errorContextFrom(location: SourceLocationMeta?): PropertyValueMap {
    val errorContext = PropertyValueMap()
    if(location != null) {
        fillErrorContext(errorContext, location)
    }
    return errorContext
}

fun fillErrorContext(errorContext: PropertyValueMap, location: SourceLocationMeta?) {
    if(location != null) {
        errorContext[Property.LINE_NUMBER] = location.lineNum
        errorContext[Property.COLUMN_NUMBER] = location.charOffset
    }
}

fun fillErrorContext(errorContext: PropertyValueMap, metaContainer: MetaContainer) {
    val location = metaContainer.find(SourceLocationMeta.TAG) as? SourceLocationMeta
    if(location != null) {
        fillErrorContext(errorContext, location)
    }
}

fun errorContextFrom(metaContainer: MetaContainer?): PropertyValueMap {
    if(metaContainer == null) {
        return PropertyValueMap()
    }
    val location = metaContainer.find(SourceLocationMeta.TAG) as? SourceLocationMeta
    return if(location != null) {
        errorContextFrom(location)
    } else {
        PropertyValueMap()
    }
}