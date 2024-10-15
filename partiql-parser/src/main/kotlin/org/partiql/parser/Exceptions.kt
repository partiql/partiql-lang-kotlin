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
    val location: SourceLocation = SourceLocation.UNKNOWN,
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
    public val token: String,
    public val tokenType: String,
    message: String = "",
    cause: Throwable? = null,
    location: SourceLocation = SourceLocation.UNKNOWN,
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
    public val rule: String,
    public val token: String,
    public val tokenType: String,
    message: String = "",
    cause: Throwable? = null,
    location: SourceLocation = SourceLocation.UNKNOWN,
) : PartiQLSyntaxException(message, cause, location)
