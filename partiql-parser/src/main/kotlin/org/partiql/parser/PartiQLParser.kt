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

import org.partiql.ast.Expr
import org.partiql.ast.Statement
import org.partiql.parser.internal.PartiQLParserDefault
import org.partiql.spi.errors.ErrorListenerException
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.nullValue
import kotlin.jvm.Throws

public interface PartiQLParser {

    /**
     * Parses the [source] into an AST.
     * @param source the user's input
     * @param config a configuration object for the parser
     * @throws ErrorListenerException when the [org.partiql.spi.errors.ErrorListener] defined in the [config] throws an
     * [ErrorListenerException], this method halts execution and propagates the exception.
     */
    @Throws(ErrorListenerException::class)
    public fun parse(source: String, config: ParserConfig = ParserConfigBuilder().build()): Result

    public data class Result(
        val source: String,
        val root: Statement,
        val locations: SourceLocations,
    ) {
        public companion object {
            @OptIn(PartiQLValueExperimental::class)
            internal fun empty(source: String): Result {
                val locations = SourceLocations.Mutable().toMap()
                return Result(source, Statement.Query(Expr.Lit(nullValue())), locations)
            }
        }
    }

    public companion object {

        @JvmStatic
        public fun builder(): PartiQLParserBuilder = PartiQLParserBuilder()

        @JvmStatic
        public fun standard(): PartiQLParser = PartiQLParserDefault()
    }
}
