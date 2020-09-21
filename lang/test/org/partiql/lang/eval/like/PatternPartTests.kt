package org.partiql.lang.eval.like

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class PatternPartTests {

    data class TestCase(val pattern: String, val escapeChar: Int?, val input: String, val shouldMatch: Boolean)

    private fun createTestCase(pattern: String, escapeChar: Char?, vectors: List<Pair<String, Boolean>>) =
        vectors.map { TestCase(pattern, escapeChar?.toInt(), it.first, it.second) }

    fun parametersForPatternTest() = listOf(
        createTestCase("a", null, listOf(
            "a" to true,
            "aa" to false,
            "b" to false,
            "bb" to false
        )),
        createTestCase("aa", null, listOf(
            "a" to false,
            "aa" to true,
            "b" to false,
            "bb" to false
        )),
        createTestCase("_", null, listOf(
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
            "a" to true,
            "bb" to true
        )),
        createTestCase("%%", null, listOf(
            "a" to true,
            "bb" to true
        )),
        createTestCase("a%", null, listOf(
            "a" to true,
            "ab" to true,
            "abcde" to true,
            "b" to false,
            "ba" to false,
            "baa" to false
        )),
        createTestCase("%a", null, listOf(
            "a" to true,
            "ba" to true,
            "edcba" to true,
            "b" to false,
            "ab" to false,
            "aab" to false
        )),
        createTestCase("%a%", null, listOf(
            "a" to true,
            "ab" to true,
            "ba" to true,
            "bab" to true,
            "bbabb" to true,
            "b" to false,
            "bb" to false
        )),
        createTestCase("%_asdf_%", null, listOf(
            "1asdf1" to true,
            "1asdf1x" to true,
            "x1asdf1" to true,
            "xyz1asdf1" to true,
            "1asdf1xyz" to true,
            "xyz1asdf1xyz" to true
        )),
        createTestCase("\\%\\_", '\\', listOf(
            "%_" to true
        ))
    ).flatten()

    @Test
    @Parameters
    fun patternTest(tc: TestCase) {
        val pat = parsePattern(tc.pattern, tc.escapeChar)
        val actualMatches = executePattern(pat, tc.input)

        Assert.assertEquals(tc.shouldMatch, actualMatches)
    }
}