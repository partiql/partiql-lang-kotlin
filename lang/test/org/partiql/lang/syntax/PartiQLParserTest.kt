/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.lang.syntax

import com.amazon.ion.IonSystem
import com.amazon.ion.system.IonSystemBuilder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestFailureReason
import org.partiql.lang.eval.evaluatortestframework.assertEquals
import org.partiql.lang.util.ArgumentsProviderBase

class PartiQLParserTest {

    val ion: IonSystem = IonSystemBuilder.standard().build()
    val parser = PartiQLParser(ion)
    val oldParser = SqlParser(ion)

    @ParameterizedTest
    @ArgumentsSource(QueryCases::class)
    fun test(query: String) {
        // Act
        val expected = oldParser.parseAstStatement(query)
        val stmt = parser.parseAstStatement(query)
        val tree = parser.parseQuery(query)

        // Build Message
        val b = StringBuilder()
        b.appendLine("QUERY              : \"$query\"")
        b.appendLine("ANTLR TREE         : ${tree.toStringTree(parser.getParser(query))}")
        b.appendLine("ACTUAL STATEMENT   : $stmt")
        b.appendLine("EXPECTED STATEMENT : $expected")

        // Assert
        assertEquals(expected, stmt, EvaluatorTestFailureReason.FAILED_TO_EVALUATE_QUERY) {
            b.toString()
        }
    }

    class QueryCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> {
            val queries = listOf(
                "SELECT a, \"b\", @c, @\"d\" FROM <<true, false, null, missing, `hello`, 'this is a string', 4, 4e2, 4.2, .4, DATE '2022-02-01', TIME '23:11:59.123456789' >>",
                "SELECT 5 + 5 / 2 FROM <<>>",
                "SELECT 1 + 2 / 3 - 4 % 5 AND true + 6 OR 7 + 8 FROM <<>>",
                "SELECT false OR 1 * 2 / 3 % 4 = 5 AND true + 6 OR 7 + 8 FROM <<>>",
                "SELECT true OR false FROM <<>>",
                "SELECT 5 <= 2 FROM <<>>",
                "SELECT 5 BETWEEN 1 AND 2 AND 3 LIKE 4 FROM <<>>",
                "SELECT * FROM <<>> ORDER BY 1 ASC NULLS FIRST",
                "SELECT * FROM <<>> ORDER BY 1 ASC NULLS LAST",
                "SELECT * FROM <<>> ORDER BY 1 ASC",
                "SELECT * FROM <<>> ORDER BY 1 DESC",
                "SELECT * FROM <<>> ORDER BY 1",
                "SELECT * FROM <<>> ORDER BY 1 NULLS FIRST",
                "SELECT * FROM <<>> GROUP PARTIAL BY 1 + 1",
                "SELECT * FROM <<>> GROUP BY 1 + 1, 2 + 2",
                "SELECT * FROM <<>> GROUP BY 1 + 1 AS a, 2 + 2 AS b",
                "SELECT * FROM <<>> GROUP BY 1 + 1 AS a, 2 + 2 AS b GROUP AS c",
                "SELECT * FROM <<>> GROUP BY 1 + 1",
                "SELECT * FROM <<>> LIMIT 5",
                "SELECT * FROM <<>> LIMIT 5 + 5"
            )
            return queries
        }
    }
}
