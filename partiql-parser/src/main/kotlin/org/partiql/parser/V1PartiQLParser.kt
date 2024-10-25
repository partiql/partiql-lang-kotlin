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

import org.partiql.ast.v1.Statement
import org.partiql.parser.internal.V1PartiQLParserDefault

public interface V1PartiQLParser {

    @Throws(PartiQLSyntaxException::class, InterruptedException::class)
    public fun parse(source: String): Result

    public data class Result(
        val source: String,
        val root: Statement,
        val locations: SourceLocations,
    )

    public companion object {

        @JvmStatic
        public fun builder(): V1PartiQLParserBuilder = V1PartiQLParserBuilder()

        @JvmStatic
        public fun standard(): V1PartiQLParser = V1PartiQLParserDefault()
    }
}
