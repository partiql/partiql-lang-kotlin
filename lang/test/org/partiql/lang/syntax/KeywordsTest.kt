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

import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionSymbol
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.util.ArgumentsProviderBase

/**
 * Tests that non-reserved keywords parse correctly.
 */
class KeywordsTest : SqlParserTestBase() {

    private class TrimModifiers() : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf("both", "leading", "trailing")
    }

    private class OtherKeywordsTestParams() : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf("public", "user", "domain")
    }

    private class GraphMatchRestrictorRefs() : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf<Pair<String, PartiqlAst.PartiqlAstNode>>(
            "acyclic" to PartiqlAst.GraphMatchRestrictor.RestrictorAcyclic(),
            "simple" to PartiqlAst.GraphMatchRestrictor.RestrictorSimple(),
            "trail" to PartiqlAst.GraphMatchRestrictor.RestrictorTrail()
        )
    }

    @ParameterizedTest
    @ArgumentsSource(TrimModifiers::class)
    fun testTrimModifierAndSubstring(modifier: String) {
        assertExpression(
            source = "SELECT $modifier FROM <<{ '$modifier': 'h' }>> WHERE trim($modifier $modifier from ' h') = $modifier",
            expectedPigBuilder = {
                trimLeadingExpectedModifier(
                    modifier,
                    call(
                        "trim",
                        lit(ionSymbol(modifier)),
                        id(modifier, caseInsensitive(), unqualified()),
                        lit(ionString(" h"))
                    ),
                )
            }
        )
    }

    @ParameterizedTest
    @ArgumentsSource(TrimModifiers::class)
    fun testTrimExplicitIdentifier(modifier: String) {
        assertExpression(
            source = "SELECT $modifier FROM <<{ '$modifier': 'h' }>> WHERE trim(\"$modifier\" from ' h') = $modifier",
            expectedPigBuilder = {
                trimLeadingExpectedModifier(
                    modifier,
                    call("trim", id(modifier, caseSensitive(), unqualified()), lit(ionString(" h"))),
                )
            }
        )
    }

    @ParameterizedTest
    @ArgumentsSource(TrimModifiers::class)
    fun testTrimModifierOnly(modifier: String) {
        assertExpression(
            source = "SELECT $modifier FROM <<{ '$modifier': 'h' }>> WHERE trim($modifier from ' h') = $modifier",
            expectedPigBuilder = {
                trimLeadingExpectedModifier(
                    modifier,
                    call("trim", lit(ionSymbol(modifier)), lit(ionString(" h"))),
                )
            }
        )
    }

    private fun PartiqlAst.Builder.trimLeadingExpectedModifier(
        modifier: String,
        expected: PartiqlAst.Expr
    ): PartiqlAst.Expr.Select =
        select(
            project = projectList(projectExpr(id(modifier, caseInsensitive(), unqualified()))),
            from = scan(
                bag(
                    struct(
                        exprPair(
                            lit(ionString(modifier)), lit(ionString("h"))
                        )
                    )
                )
            ),
            where = eq(expected, id(modifier, caseInsensitive(), unqualified()))
        )

    @ParameterizedTest
    @ArgumentsSource(GraphMatchRestrictorRefs::class)
    fun testAcyclicKeyword(input: Pair<String, PartiqlAst.GraphMatchRestrictor>) {
        val (restrictor, restrictorAst) = input
        assertExpressionNoRoundTrip(
            source = "SELECT $restrictor FROM $restrictor MATCH [$restrictor $restrictor= ($restrictor)]",
            targetParsers = setOf(ParserTypes.PARTIQL_PARSER),
            expectedPigBuilder = {
                select(
                    project = projectList(projectExpr(id(restrictor, caseInsensitive(), unqualified()))),
                    from = graphMatch(
                        id(restrictor, caseInsensitive(), unqualified()),
                        graphMatchExpr(
                            patterns0 = graphMatchPattern(
                                parts0 = pattern(
                                    graphMatchPattern(
                                        restrictorAst,
                                        variable = restrictor,
                                        parts0 = node(variable = restrictor)
                                    )
                                )
                            )
                        )
                    )
                )
            }
        )
    }

    @ParameterizedTest
    @ArgumentsSource(OtherKeywordsTestParams::class)
    fun testOtherKeywords(keyword: String) {
        assertExpression(
            source = "SELECT $keyword FROM \"$keyword\"",
            targetParsers = setOf(ParserTypes.PARTIQL_PARSER),
            expectedPigBuilder = {
                select(
                    project = projectList(projectExpr(id(keyword, caseInsensitive(), unqualified()))),
                    from = scan(id(keyword, caseSensitive(), unqualified()))
                )
            }
        )
    }
}
