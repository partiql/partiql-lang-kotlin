/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.parser

import org.partiql.spi.SourceLocation

/**
 * PartiQLParser Syntax Exception
 * TODO: Delete this in favor of the error listener.
 *
 * @property message
 * @property cause
 * @property location
 */
internal open class PartiQLSyntaxException(
    override val message: String,
    override val cause: Throwable? = null,
    private val location: SourceLocation? = null,
) : Exception()

/**
 * PartiQLParser Exception upon lexing.
 * TODO: Delete this in favor of the error listener.
 *
 * @property token — Debug token where the Exception occurred
 * @constructor
 *
 * @param message
 * @param cause
 * @param location
 */
internal class PartiQLLexerException(
    val token: String,
    val tokenType: String,
    message: String = "",
    cause: Throwable? = null,
    location: SourceLocation? = null,
) : PartiQLSyntaxException(message, cause, location)

/**
 * PartiQLParser Exception upon parsing.
 * TODO: Delete this in favor of the error listener.
 *
 * @property rule   Debug rule where the Exception occurred
 * @property token  Debug token where the Exception occurred
 * @constructor
 *
 * @param message
 * @param cause
 * @param location
 */
internal class PartiQLParserException(
    val rule: String,
    val token: String,
    val tokenType: String,
    message: String = "",
    cause: Throwable? = null,
    location: SourceLocation? = null,
) : PartiQLSyntaxException(message, cause, location)
