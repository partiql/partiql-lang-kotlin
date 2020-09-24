package org.partiql.lang.eval.like

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(JUnitParamsRunner::class)
class PatternPartTests {

    data class TestCase(val pattern: String, val escapeChar: Int?, val input: String, val shouldMatch: Boolean)

    private fun createTestCase(pattern: String, escapeChar: Char?, vectors: List<Pair<String, Boolean>>) =
        vectors.map { TestCase(pattern, escapeChar?.toInt(), it.first, it.second) }

    fun parametersForPatternTest() = listOf(
        createTestCase("a", null, listOf(
            "" to false,
            "a" to true,
            "aa" to false,
            "b" to false,
            "bb" to false
        )),
        createTestCase("aa", null, listOf(
            "" to false,
            "a" to false,
            "aa" to true,
            "b" to false,
            "bb" to false
        )),
        createTestCase("_", null, listOf(
            "" to false,
            "a" to true,
            "b" to true,
            "aa" to false,
            "bb" to false
        )),
        createTestCase("__", null, listOf(
            "a" to false,
            "b" to false,
            "aa" to true,
            "bb" to true
        )),
        createTestCase("%", null, listOf(
            "" to true,
            "a" to true,
            "bb" to true
        )),
        createTestCase("%%", null, listOf(
            "" to true,
            "a" to true,
            "bb" to true
        )),
        createTestCase("a%", null, listOf(
            "" to false,
            "a" to true,
            "ab" to true,
            "abcde" to true,
            "b" to false,
            "ba" to false,
            "baa" to false
        )),
        createTestCase("%a", null, listOf(
            "" to false,
            "a" to true,
            "ba" to true,
            "edcba" to true,
            "b" to false,
            "ab" to false,
            "aab" to false
        )),
        createTestCase("%foo%bar%bat%baz%bork%borz%", null, listOf(
            "" to false,
            "foobarbatbazborkborz" to true,
            "000foo1bar22bat333baz444bork555borz666" to true,
            "000foo1bar22bat333baz444bork555borD666" to false
        )),
        createTestCase("%a%", null, listOf(
            "" to false,
            "a" to true,
            "ab" to true,
            "ba" to true,
            "bab" to true,
            "bbabb" to true,
            "b" to false,
            "bb" to false
        )),
        createTestCase("%_asdf_%", null, listOf(
            "" to false,
            "asdf" to false,
            "1asdf1" to true,
            "1asdf1x" to true,
            "x1asdf1" to true,
            "xyz1asdf1" to true,
            "1asdf1xyz" to true,
            "xyz1asdf1xyz" to true
        )),
        createTestCase("\\%\\_", '\\', listOf(
            "" to false,
            "%_" to true
        )),
        createTestCase("%\\%\\__", '\\', listOf(
            "" to false,
            "%_1" to true,
            "asdf%_1" to true
        ))
    ).flatten()

    @Test
    @Parameters
    fun patternTest(tc: TestCase) {
        val pat = parsePattern(tc.pattern, tc.escapeChar)
        val actualMatches = executePattern(pat, tc.input)

        Assert.assertEquals(tc.shouldMatch, actualMatches)
    }

    @Test
    fun patternParserTest() {
        // the parser should consider multiple consecutive % to be the same as one
        val patParts = parsePattern("%%a%%%_%%% %%", escapeChar = null)
        assertEquals(
            listOf(
                PatternPart.ZeroOrMoreOfAnyChar,
                PatternPart.ExactChars("a".codePoints().toArray()),
                PatternPart.ZeroOrMoreOfAnyChar,
                PatternPart.AnyOneChar,
                PatternPart.ZeroOrMoreOfAnyChar,
                PatternPart.ExactChars(" ".codePoints().toArray()),
                PatternPart.ZeroOrMoreOfAnyChar
            ),
            patParts)
    }

    @Test
    fun stressTest() {
        // makes absolutely certain we do not stack overflow on too many consecutive `%` characters
        assertEquals(true, executePattern(parsePattern("%".repeat(10000) + "a", escapeChar = null), "aaaa"))
    }
}