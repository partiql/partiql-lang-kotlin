/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval.builtins.timestamp

import org.partiql.lang.eval.*
import org.junit.*
import org.junit.Assert.*

class TimestampFormatPatternLexerTest {

    private fun text(s: String) = Token(TokenType.TEXT, s)
    private fun pattern(s: String) = Token(TokenType.PATTERN, s)

    private fun assertTokens(s: String, vararg tokens: Token) = assertEquals(tokens.toList(),
                                                                             TimestampFormatPatternLexer().tokenize(s))

    @Test
    fun singlePatternToken() = assertTokens("y", pattern("y"))

    @Test
    fun multiplePatternTokensTogether() = assertTokens("ym", pattern("y"), pattern("m"))

    @Test
    fun multiplePatternTokensTogetherWithMultipleCharacters() = assertTokens("yymm", pattern("yy"), pattern("mm"))

    @Test
    fun longSinglePatternToken() = assertTokens("yyyyyyyyyyyyyyyyyyy", pattern("yyyyyyyyyyyyyyyyyyy"))

    @Test
    fun allPatternCharacters() = assertTokens("yMdahHmsSXx",
                                              pattern("y"),
                                              pattern("M"),
                                              pattern("d"),
                                              pattern("a"),
                                              pattern("h"),
                                              pattern("H"),
                                              pattern("m"),
                                              pattern("s"),
                                              pattern("S"),
                                              pattern("X"),
                                              pattern("x"))

    @Test
    fun allNonEscapedText() = assertTokens(" /-,:.", text(" /-,:."))

    @Test
    fun onlyText() = assertTokens("'some quoted text'-----'more quoted'.''''",
                                  text("'some quoted text'"),
                                  text("-----"),
                                  text("'more quoted'"),
                                  text("."),
                                  text("''"),
                                  text("''"))

    @Test
    fun withWhitespace() = assertTokens("y y y y",
                                        pattern("y"),
                                        text(" "),
                                        pattern("y"),
                                        text(" "),
                                        pattern("y"),
                                        text(" "),
                                        pattern("y"))

    @Test
    fun withNonEscapedTextAndPattern() = assertTokens("y/m-d,h:y.s",
                                                      pattern("y"),
                                                      text("/"),
                                                      pattern("m"),
                                                      text("-"),
                                                      pattern("d"),
                                                      text(","),
                                                      pattern("h"),
                                                      text(":"),
                                                      pattern("y"),
                                                      text("."),
                                                      pattern("s"))

    @Test
    fun withNonEscapedTextWhitespaceAndPattern() = assertTokens("yyyy-mm-dd HH:hh",
                                                                pattern("yyyy"),
                                                                text("-"),
                                                                pattern("mm"),
                                                                text("-"),
                                                                pattern("dd"),
                                                                text(" "),
                                                                pattern("HH"),
                                                                text(":"),
                                                                pattern("hh"))

    @Test
    fun withQuotes() = assertTokens("y'TT'y", pattern("y"), text("'TT'"), pattern("y"))

    @Test
    fun ionTimestampDefaultPattern() = assertTokens("yyyy-MM-dd'T'HH:mm:ss.SSSX",
                                                    pattern("yyyy"),
                                                    text("-"),
                                                    pattern("MM"),
                                                    text("-"),
                                                    pattern("dd"),
                                                    text("'T'"),
                                                    pattern("HH"),
                                                    text(":"),
                                                    pattern("mm"),
                                                    text(":"),
                                                    pattern("ss"),
                                                    text("."),
                                                    pattern("SSS"),
                                                    pattern("X"))

    @Test(expected = EvaluationException::class)
    fun openQuotes() {
        TimestampFormatPatternLexer().tokenize("y'TT")
    }

    @Test(expected = EvaluationException::class)
    fun unknownCharacters() {
        TimestampFormatPatternLexer().tokenize("yyyyP")
    }
}