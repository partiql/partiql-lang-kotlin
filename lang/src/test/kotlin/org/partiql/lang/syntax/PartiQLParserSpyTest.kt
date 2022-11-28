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
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.lang.domains.PartiqlAst

/**
 * This test class is used for spying the [PartiQLParser].
 */
internal class PartiQLParserSpyTest {

    val ion: IonSystem = IonSystemBuilder.standard().build()
    val parser = spyk<PartiQLParser>()

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
        assertEquals(ast00, ast01)
    }

    /**
     * This test shows that SLL and LL parsing can happen sequentially
     */
    @Test
    fun manuallyShowDoubleParsingSameQueriesWorks() {
        val query00 = "SELECT a1 FROM t1"
        val ast00 = PartiQLVisitor(ion).visit(parser.parseUsingSLL(query00))
        val ast01 = PartiQLVisitor(ion).visit(parser.parseUsingLL(query00))
        assertEquals(ast00, ast01)
    }

    /**
     * This test shows that SLL and LL parsing can happen sequentially using different queries
     */
    @org.junit.Test(expected = org.junit.Test.None::class)
    fun manuallyShowDoubleParsingWorks() {
        val query00 = "SELECT a1 FROM t1"
        val query01 = "SELECT a2 FROM t2"
        PartiQLVisitor(ion).visit(parser.parseUsingSLL(query00))
        PartiQLVisitor(ion).visit(parser.parseUsingLL(query01))
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
        verify(exactly = 1) { parser.parseUsingSLL(query00) }
        verify(exactly = 0) { parser.parseUsingLL(query00) }
        assertEquals(
            ast,
            PartiqlAst.build {
                query(
                    select(
                        project = projectList(
                            projectExpr(
                                id("a", caseInsensitive(), unqualified())
                            )
                        ),
                        from = scan(
                            id("t", caseInsensitive(), unqualified())
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
            parser.parseUsingSLL(query00)
        } throws ParseCancellationException()

        // Act
        val ast = parser.parseAstStatement(query00)

        // Assert
        verify(exactly = 1) { parser.parseUsingSLL(query00) }
        verify(exactly = 1) { parser.parseUsingLL(query00) }
        assertEquals(
            ast,
            PartiqlAst.build {
                query(
                    select(
                        project = projectList(
                            projectExpr(
                                id("a", caseInsensitive(), unqualified())
                            )
                        ),
                        from = scan(
                            id("t", caseInsensitive(), unqualified())
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
            parser.parseUsingSLL(query00)
        }
    }

    /**
     * Shows that LL parsing throws a ParserException
     */
    @Test
    fun badQueryThrowsLLParserException() {
        val query00 = "1++++++++++++"
        assertThrows<ParserException> {
            parser.parseUsingLL(query00)
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
        verify(exactly = 1) { parser.parseUsingSLL(query00) }
        verify(exactly = 1) { parser.parseUsingLL(query00) }
    }
}
