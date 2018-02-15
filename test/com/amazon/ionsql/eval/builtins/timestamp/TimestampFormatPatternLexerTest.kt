package com.amazon.ionsql.eval.builtins.timestamp

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

    @Test(expected = IllegalArgumentException::class)
    fun openQuotes() {
        TimestampFormatPatternLexer().tokenize("y'TT")
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownCharacters() {
        TimestampFormatPatternLexer().tokenize("yyyyP")
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyString() {
        TimestampFormatPatternLexer().tokenize("")
    }
}