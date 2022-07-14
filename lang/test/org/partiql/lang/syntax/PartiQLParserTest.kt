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
import org.junit.Test

class PartiQLParserTest {

    val ion: IonSystem = IonSystemBuilder.standard().build()
    val parser = PartiQLParser(ion)
    val oldParser = SqlParser(ion)

    @Test
    fun test() {
        // Arrange
        val query = "SELECT a, \"b\" FROM <<true, false, null, missing, `hello`, 'this is a string', 4, 4e2, 4.2, .4, DATE '2022-02-01', TIME '23:11:59.123456789' >>"

        // Act
        val tree = parser.parseQuery(query)
        val stmt = parser.parseAstStatement(query)
        val expected = oldParser.parseAstStatement(query)

        // Print
        println("QUERY              : \"$query\"")
        println("ANTLR TREE         : ${tree.toStringTree(parser.getParser(query))}")
        println("ACTUAL STATEMENT   : $stmt")
        println("EXPECTED STATEMENT : $expected")
    }
}
