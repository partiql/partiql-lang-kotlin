package org.partiql.lang.eval.like

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(JUnitParamsRunner::class)
class PatternTests {

    data class TestCase(val pattern: String, val escapeChar: Int?, val input: String, val shouldMatch: Boolean)

    private fun createTestCase(pattern: String, escapeChar: Char?, vectors: List<Pair<String, Boolean>>) =
        vectors.map { TestCase(pattern, escapeChar?.toInt(), it.first, it.second) }

    fun parametersForPatternTest() = listOf(
        createTestCase(
            "a", null,
            listOf(
                "" to false,
                "a" to true,
                "aa" to false,
                "b" to false,
                "bb" to false
            )
        ),
        createTestCase(
            "aa", null,
            listOf(
                "" to false,
                "a" to false,
                "aa" to true,
                "b" to false,
                "bb" to false
            )
        ),
        createTestCase(
            "_", null,
            listOf(
                "" to false,
                "a" to true,
                "b" to true,
                "aa" to false,
                "bb" to false
            )
        ),
        createTestCase(
            "__", null,
            listOf(
                "a" to false,
                "b" to false,
                "aa" to true,
                "bb" to true
            )
        ),
        createTestCase(
            "%", null,
            listOf(
                "" to true,
                "a" to true,
                "bb" to true
            )
        ),
        createTestCase(
            "%%", null,
            listOf(
                "" to true,
                "a" to true,
                "bb" to true
            )
        ),
        createTestCase(
            "a%", null,
            listOf(
                "" to false,
                "a" to true,
                "ab" to true,
                "abcde" to true,
                "b" to false,
                "ba" to false,
                "baa" to false
            )
        ),
        createTestCase(
            "%a", null,
            listOf(
                "" to false,
                "a" to true,
                "ba" to true,
                "edcba" to true,
                "b" to false,
                "ab" to false,
                "aab" to false
            )
        ),
        createTestCase(
            "%foo%bar%bat%baz%bork%borz%", null,
            listOf(
                "" to false,
                "foobarbatbazborkborz" to true,
                "000foo1bar22bat333baz444bork555borz666" to true,
                "000foo1bar22bat333baz444bork555borD666" to false
            )
        ),
        createTestCase(
            "%a%", null,
            listOf(
                "" to false,
                "a" to true,
                "ab" to true,
                "ba" to true,
                "bab" to true,
                "bbabb" to true,
                "b" to false,
                "bb" to false
            )
        ),
        createTestCase(
            "%_asdf_%", null,
            listOf(
                "" to false,
                "asdf" to false,
                "1asdf1" to true,
                "1asdf1x" to true,
                "x1asdf1" to true,
                "xyz1asdf1" to true,
                "1asdf1xyz" to true,
                "xyz1asdf1xyz" to true
            )
        ),
        createTestCase(
            "\\%\\_", '\\',
            listOf(
                "" to false,
                "%_" to true
            )
        ),
        createTestCase(
            "%\\%\\__", '\\',
            listOf(
                "" to false,
                "%_1" to true,
                "asdf%_1" to true
            )
        )
    ).flatten()

    @Test
    @Parameters
    fun patternTest(tc: TestCase) {
        val pat = parsePattern(tc.pattern, tc.escapeChar)
        val actualMatches = pat.matcher(tc.input).matches()

        Assert.assertEquals(tc.shouldMatch, actualMatches)
    }

    @Test
    fun patternParserTest() {
        // the parser should consider multiple consecutive % to be the same as one
        val pattern = parsePattern("%%a%%%_%%% %%", escapeChar = null)
        assertEquals("^.*?\\Qa\\E.+?\\Q \\E.*?$", pattern.pattern())
    }

    @Test
    fun stressTest() {
        // makes absolutely certain we do not stack overflow on too many consecutive `%` characters
        assertEquals(true, parsePattern("%".repeat(10000) + "a", escapeChar = null).matcher("aaaa").matches())
    }

    @Test
    fun like() {
        val escape = '\\'.toInt()

        assertEquals("^\\Qfoo\\E$", parsePattern("foo", escape).pattern())

        assertEquals("^.*?\\Qfoo\\E$", parsePattern("%foo", escape).pattern())
        assertEquals("^\\Qfoo\\E.*?$", parsePattern("foo%", escape).pattern())
        assertEquals("^\\Qfoo\\E.*?\\Qbar\\E$", parsePattern("foo%bar", escape).pattern())
        assertEquals("^\\Qfoo\\E.*?\\Qbar\\E$", parsePattern("foo%%bar", escape).pattern())
        assertEquals("^\\Qfoo\\E.*?\\Qbar\\E$", parsePattern("foo%%%bar", escape).pattern())
        assertEquals("^\\Qfoo\\E.*?\\Qbar\\E$", parsePattern("foo%%%%bar", escape).pattern())
        assertEquals(
            "^.*?\\Qfoo\\E.*?\\Qbar\\E.*?$",
            parsePattern("%foo%%%%bar%", escape).pattern(),
        )
        assertEquals(
            "^.*?\\Qfoo\\E.*?\\Qbar%baz\\E.*?$",
            parsePattern("%foo%%%%bar\\%baz%", escape).pattern(),
        )
        assertEquals(
            "^.*?\\Qfoo\\E.*?\\Qbar%baz\\E.*?$",
            parsePattern("%foo%%%%bar*%baz%", '*'.toInt()).pattern(),
        )
        assertEquals("^.\\Qfoo\\E$", parsePattern("_foo", escape).pattern())
        assertEquals("^\\Qfoo\\E.$", parsePattern("foo_", escape).pattern())
        assertEquals("^\\Qfoo\\E.\\Qbar\\E$", parsePattern("foo_bar", escape).pattern())
        assertEquals("^\\Qfoo\\E.{2,2}\\Qbar\\E$", parsePattern("foo__bar", escape).pattern())
        assertEquals("^\\Qfoo\\E.{2,}?\\Qbar\\E$", parsePattern("foo_%_bar", escape).pattern())
        assertEquals("^\\Qfoo\\E.{2,}?\\Qbar\\E$", parsePattern("foo_%_%bar", escape).pattern())
        assertEquals("^\\Qfoo\\E.{2,}?\\Qbar\\E$", parsePattern("foo%_%%_%bar", escape).pattern())
        assertEquals(
            "^\\Qfoo\\E.\\Q.*?\\E.\\Qbar\\E$",
            parsePattern("foo_.*?_bar", escape).pattern(),
        )
    }

    @Test
    fun likeMatch() {
        val pat = parsePattern("foo_.*?_bar", '\\'.toInt())

        assert(pat.matcher("foos.*?qbar").matches())
    }
}
