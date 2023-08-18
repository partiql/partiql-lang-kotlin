/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.planner

import org.partiql.errors.ErrorCode
import org.partiql.errors.PropertyValueMap
import org.partiql.errors.SqlException

/** Error for planning problems. */
public open class PlannerException(
    message: String,
    errorCode: ErrorCode,
    errorContext: PropertyValueMap = PropertyValueMap(),
    cause: Throwable? = null,
    public override val internal: Boolean
) : SqlException(message, errorCode, errorContext, cause) {

    public constructor(
        cause: Throwable,
        errorCode: ErrorCode,
        errorContext: PropertyValueMap = PropertyValueMap(),
        internal: Boolean
    ) : this(
        message = cause.message ?: "<NO MESSAGE>",
        errorCode = errorCode,
        errorContext = errorContext,
        internal = internal,
        cause = cause
    )
}
