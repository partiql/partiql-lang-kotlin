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

package org.partiql.lang.syntax.impl

import com.amazon.ion.IonSystem
import com.amazon.ion.system.IonSystemBuilder
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.junit.Assert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.syntax.ParserException

/**
 * This test class is used for spying the [PartiQLPigParser].
 */
internal class PartiQLPigParserSpyTest {

    val ion: IonSystem = IonSystemBuilder.standard().build()
    val parser = spyk<PartiQLPigParser>()

    /**
     * Shows that the PartiQLParser can parse different queries sequentially.
     */
    @org.junit.Test(expected = org.junit.Test.None::class)
    fun twoQueries() {
        val query00 = "SELECT a1 FROM t1"
        val query01 = "SELECT a2 FROM t2"
        parser.parseAstStatement(query00)
        parser.parseAstStatement(query01)
    }

    /**
     * Shows that the PartiQLParser can parse same queries sequentially.
     */
    @Test
    fun twoSameQueries() {
        val query00 = "SELECT a1 FROM t1"
        val ast00 = parser.parseAstStatement(query00)
        val ast01 = parser.parseAstStatement(query00)
        Assert.assertEquals(ast00, ast01)
    }

    /**
     * This test shows that SLL and LL parsing can happen sequentially
     */
    @Test
    fun manuallyShowDoubleParsingSameQueriesWorks() {
        val query00 = "SELECT a1 FROM t1"
        val ast00 = parser.parseQuery(query00) { parser.createParserSLL(it) }
        val ast01 = parser.parseQuery(query00) { parser.createParserLL(it) }
        Assert.assertEquals(ast00, ast01)
    }

    /**
     * This test shows that SLL and LL parsing can happen sequentially using different queries
     */
    @org.junit.Test(expected = org.junit.Test.None::class)
    fun manuallyShowDoubleParsingWorks() {
        val query00 = "SELECT a1 FROM t1"
        val query01 = "SELECT a2 FROM t2"
        parser.parseQuery(query00) { parser.createParserSLL(it) }
        parser.parseQuery(query01) { parser.createParserLL(it) }
    }

    /**
     * Shows that for simple queries, we'll only parse using the SLL parser.
     */
    @Test
    fun sllParsesCorrectly() {
        // Arrange
        val query00 = "SELECT a FROM t"

        // Act
        val ast = parser.parseAstStatement(query00)

        // Assert
        verify(exactly = 1) { parser.createParserSLL(any()) }
        verify(exactly = 0) { parser.createParserLL(any()) }
        Assert.assertEquals(
            ast,
            PartiqlAst.build {
                query(
                    select(
                        project = projectList(
                            projectExpr(
                                vr(id("a", regular()), unqualified())
                            )
                        ),
                        from = scan(
                            vr(id("t", regular()), unqualified())
                        )
                    )
                )
            }
        )
    }

    /**
     * Shows we can parse using LL after SLL throws a ParseCancellationException
     */
    @Test
    fun sllThrowsException() {
        // Arrange
        val query00 = "SELECT a FROM t"
        every {
            parser.createParserSLL(any())
        } throws ParseCancellationException()

        // Act
        val ast = parser.parseAstStatement(query00)

        // Assert
        verify(exactly = 1) { parser.createParserSLL(any()) }
        verify(exactly = 1) { parser.createParserLL(any()) }
        Assert.assertEquals(
            ast,
            PartiqlAst.build {
                query(
                    select(
                        project = projectList(
                            projectExpr(
                                vr(id("a", regular()), unqualified())
                            )
                        ),
                        from = scan(
                            vr(id("t", regular()), unqualified())
                        )
                    )
                )
            }
        )
    }

    /**
     * Shows that SLL parsing throws a ParseCancellationException
     */
    @Test
    fun badQueryThrowsSLLCancellationException() {
        val query00 = "1++++++++++++"
        assertThrows<ParseCancellationException> {
            parser.parseQuery(query00) { parser.createParserSLL(it) }
        }
    }

    /**
     * Shows that LL parsing throws a ParserException
     */
    @Test
    fun badQueryThrowsLLParserException() {
        val query00 = "1++++++++++++"
        assertThrows<ParserException> {
            parser.parseQuery(query00) { parser.createParserLL(it) }
        }
    }

    /**
     * Shows that the public API will parse a bad query twice using SLL and LL
     */
    @Test
    fun badQueryThrowsParserException() {
        val query00 = "1++++++++++++"
        assertThrows<ParserException> {
            parser.parseAstStatement(query00)
        }
        verify(exactly = 1) { parser.createParserSLL(any()) }
        verify(exactly = 1) { parser.createParserLL(any()) }
    }
}
