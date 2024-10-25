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

import org.partiql.ast.v1.Query
import org.partiql.ast.v1.Statement
import org.partiql.ast.v1.expr.ExprLit
import org.partiql.parser.internal.V1PartiQLParserDefault
import org.partiql.spi.Context
import org.partiql.spi.errors.PErrorListenerException
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.nullValue

public interface V1PartiQLParser {

    /**
     * Parses the [source] into an AST.
     * @param source the user's input
     * @param ctx a configuration object for the parser
     * @throws PErrorListenerException when the [org.partiql.spi.errors.PErrorListener] defined in the [ctx] throws an
     * [PErrorListenerException], this method halts execution and propagates the exception.
     */
    @Throws(PErrorListenerException::class)
    public fun parse(source: String, ctx: Context): Result

    /**
     * Parses the [source] into an AST.
     * @param source the user's input
     * @throws PErrorListenerException when the [org.partiql.spi.errors.PErrorListener] defined in the context throws an
     * [PErrorListenerException], this method halts execution and propagates the exception.
     */
    @Throws(PErrorListenerException::class)
    public fun parse(source: String): Result {
        return parse(source, Context.standard())
    }

    public data class Result(
        val source: String,
        val root: Statement,
        val locations: SourceLocations,
    ) {
        public companion object {
            @OptIn(PartiQLValueExperimental::class)
            internal fun empty(source: String): Result {
                val locations = SourceLocations.Mutable().toMap()
                return Result(source, Query(ExprLit(nullValue())), locations)
            }
        }
    }

    public companion object {

        @JvmStatic
        public fun builder(): V1PartiQLParserBuilder = V1PartiQLParserBuilder()

        @JvmStatic
        public fun standard(): V1PartiQLParser = V1PartiQLParserDefault()
    }
}
