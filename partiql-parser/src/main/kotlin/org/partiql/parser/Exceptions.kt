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
 *
 * @property message
 * @property cause
 * @property location
 * @property context
 */
public open class PartiQLSyntaxException(
    override val message: String,
    override val cause: Throwable? = null,
    val location: SourceLocation = SourceLocation.UNKNOWN,
    val context: Map<String, Any> = emptyMap(),
) : Exception() {

    companion object {

        internal fun wrap(cause: Throwable) = when (cause) {
            is PartiQLSyntaxException -> cause
            is StackOverflowError -> PartiQLSyntaxException(
                message = """
                    |Input query too large. This error typically occurs when there are several nested
                    |expressions/predicates and can usually be fixed by simplifying expressions.
                    """.trimMargin(),
                cause = cause,
            )
            is InterruptedException -> cause
            else -> PartiQLSyntaxException("Unhandled exception.", cause)
        }
    }
}

/**
 * PartiQLParser Exception upon lexing.
 *
 * @property token — Debug token where the Exception occurred
 * @constructor
 *
 * @param message
 * @param cause
 * @param location
 * @param context
 */
public class PartiQLLexerException(
    val token: String,
    message: String = "",
    cause: Throwable? = null,
    location: SourceLocation = SourceLocation.UNKNOWN,
    context: Map<String, Any> = emptyMap(),
) : PartiQLSyntaxException(message, cause, location, context)

/**
 * PartiQLParser Exception upon parsing.
 *
 * @property rule — Debug parser rule where the Exception occurred
 * @constructor
 *
 * @param message
 * @param cause
 * @param location
 * @param context
 */
public class PartiQLParserException(
    val rule: String,
    message: String = "",
    cause: Throwable? = null,
    location: SourceLocation = SourceLocation.UNKNOWN,
    context: Map<String, Any> = emptyMap(),
) : PartiQLSyntaxException(message, cause, location, context)
