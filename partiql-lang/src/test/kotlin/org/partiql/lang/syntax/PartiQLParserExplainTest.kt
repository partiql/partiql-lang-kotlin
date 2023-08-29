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

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.lang.ION
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.getAntlrDisplayString
import org.partiql.parser.antlr.PartiQLParser

class PartiQLParserExplainTest : PartiQLParserTestBase() {

    override val targets: Array<ParserTarget> = arrayOf(ParserTarget.DEFAULT, ParserTarget.EXPERIMENTAL)

    data class ParserTestCase(
        val description: String? = null,
        val query: String,
        val expected: PartiqlAst.Builder.() -> PartiqlAst.PartiqlAstNode
    )

    data class ParserErrorTestCase(
        val description: String? = null,
        val query: String,
        val code: ErrorCode,
        val context: Map<Property, Any> = emptyMap()
    )

    @ArgumentsSource(SuccessTestProvider::class)
    @ParameterizedTest
    fun successTests(tc: ParserTestCase) = assertExpression(tc.query, tc.expected)

    @ArgumentsSource(ErrorTestProvider::class)
    @ParameterizedTest
    fun errorTests(tc: ParserErrorTestCase) = checkInputThrowingParserException(tc.query, tc.code, tc.context, assertContext = false)

    class SuccessTestProvider : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            ParserTestCase(
                description = "Simple case",
                query = "EXPLAIN a",
                expected = {
                    explain(
                        domain(
                            query(
                                vr(id("a", caseInsensitive()), unqualified())
                            )
                        )
                    )
                }
            ),
            ParserTestCase(
                description = "Specifying explain type only",
                query = "EXPLAIN (TYPE logical) a",
                expected = {
                    explain(
                        domain(
                            statement = query(
                                vr(id("a", caseInsensitive()), unqualified())
                            ),
                            type = "logical"
                        )
                    )
                }
            ),
            ParserTestCase(
                description = "Specifying explain format only",
                query = "EXPLAIN (FORMAT ion_sexp) a",
                expected = {
                    explain(
                        domain(
                            statement = query(
                                vr(id("a", caseInsensitive()), unqualified())
                            ),
                            format = "ion_sexp"
                        )
                    )
                }
            ),
            ParserTestCase(
                description = "Specifying explain type and format",
                query = "EXPLAIN (TYPE ast, FORMAT ion_sexp) a",
                expected = {
                    explain(
                        domain(
                            statement = query(
                                vr(id("a", caseInsensitive()), unqualified())
                            ),
                            type = "ast",
                            format = "ion_sexp"
                        )
                    )
                }
            ),
            ParserTestCase(
                description = "Specifying explain type and format case in-sensitive",
                query = "EXPLAIN (TypE aST, Format ion_SEXP) a",
                expected = {
                    explain(
                        domain(
                            statement = query(
                                vr(id("a", caseInsensitive()), unqualified())
                            ),
                            type = "aST",
                            format = "ion_SEXP"
                        )
                    )
                }
            ),
        )
    }

    class ErrorTestProvider : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            ParserErrorTestCase(
                description = "Wrong option",
                query = "EXPLAIN (typ AST) a",
                code = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                context = mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 10L,
                    Property.TOKEN_DESCRIPTION to PartiQLParser.REGULAR_IDENTIFIER.getAntlrDisplayString(),
                    Property.TOKEN_VALUE to ION.newSymbol("typ")
                )
            ),
            ParserErrorTestCase(
                description = "Missing target",
                query = "EXPLAIN (TYPE ast, FORMAT ion_sexp)",
                code = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                context = mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 36L,
                    Property.TOKEN_DESCRIPTION to PartiQLParser.EOF.getAntlrDisplayString(),
                    Property.TOKEN_VALUE to ION.newSymbol("EOF")
                ),
            ),
            ParserErrorTestCase(
                description = "Setting option twice",
                query = "EXPLAIN (TYPE ast, TYPE logical) a",
                code = ErrorCode.PARSE_UNEXPECTED_TOKEN,
                context = mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 25L,
                    Property.TOKEN_DESCRIPTION to PartiQLParser.REGULAR_IDENTIFIER.getAntlrDisplayString(),
                    Property.TOKEN_VALUE to ION.newSymbol("logical")
                )
            )
        )
    }
}
