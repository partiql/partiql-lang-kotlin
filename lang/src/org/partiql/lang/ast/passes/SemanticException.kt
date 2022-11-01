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

package org.partiql.lang.ast.passes

import org.partiql.lang.SqlException
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.util.propertyValueMapOf

/**
 * The exception to be thrown by semantic passes.
 */
class SemanticException(
    message: String = "",
    errorCode: ErrorCode,
    errorContext: PropertyValueMap = propertyValueMapOf(),
    cause: Throwable? = null
) : SqlException(message, errorCode, errorContext, cause) {

    /**
     * Alternate constructor using a [Problem]. Error message is generated using [ProblemDetails.message].
     */
    constructor(err: Problem, cause: Throwable? = null) :
        this(
            message = "",
            errorCode = ErrorCode.SEMANTIC_PROBLEM,
            errorContext = propertyValueMapOf(
                Property.LINE_NUMBER to err.sourceLocation.lineNum,
                Property.COLUMN_NUMBER to err.sourceLocation.charOffset,
                Property.MESSAGE to err.details.message
            ),
            cause = cause
        )
}
