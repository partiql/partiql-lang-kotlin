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

package org.partiql.lang.syntax

import org.partiql.lang.SqlException
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.PropertyValueMap

/** Root exception type for syntactic problems. */
open class SyntaxException(
    message: String = "",
    errorCode: ErrorCode,
    errorContext: PropertyValueMap,
    cause: Throwable? = null
) :
    SqlException(message, errorCode, errorContext, cause)

/** Error in the Lexer. */
open class LexerException(
    message: String = "",
    errorCode: ErrorCode,
    errorContext: PropertyValueMap,
    cause: Throwable? = null
) :
    SyntaxException(message, errorCode, errorContext, cause)

/** Error in the parser. */
open class ParserException(
    message: String = "",
    errorCode: ErrorCode,
    errorContext: PropertyValueMap = PropertyValueMap(),
    cause: Throwable? = null
) :
    SyntaxException(message, errorCode, errorContext, cause)
