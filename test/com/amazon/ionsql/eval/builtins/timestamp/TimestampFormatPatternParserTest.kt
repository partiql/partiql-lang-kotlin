package com.amazon.ionsql.eval.builtins.timestamp

import junitparams.*
import org.junit.*
import org.junit.runner.*
import kotlin.test.*

// to reduce test verbosity
private typealias p = PatternAstNode
private typealias t = TextAstNode

@RunWith(JUnitParamsRunner::class)
class TimestampFormatPatternParserTest {

    fun parametersForParse(): List<Pair<String, List<AstNode>>> = listOf(
        "y" to listOf(p("y", PatternType.YEAR, TimestampPrecision.YEAR)),
        "yy" to listOf(p("yy", PatternType.TWO_DIGIT_YEAR, TimestampPrecision.YEAR)),
        "yyyy" to listOf(p("yyyy", PatternType.FOUR_DIGIT_YEAR, TimestampPrecision.YEAR)),
        "M" to listOf(p("M", PatternType.MONTH, TimestampPrecision.MONTH)),
        "MM" to listOf(p("MM", PatternType.TWO_DIGIT_MONTH, TimestampPrecision.MONTH)),
        "d" to listOf(p("d", PatternType.DAY, TimestampPrecision.DAY)),
        "dd" to listOf(p("dd", PatternType.TWO_DIGIT_DAY, TimestampPrecision.DAY)),
        "H" to listOf(p("H", PatternType.TWENTY_FOUR_HOUR, TimestampPrecision.HOUR)),
        "HH" to listOf(p("HH", PatternType.TWO_DIGIT_TWENTY_FOUR_HOUR, TimestampPrecision.HOUR)),
        "h" to listOf(p("h", PatternType.TWELVE_HOUR, TimestampPrecision.HOUR)),
        "hh" to listOf(p("hh", PatternType.TWO_DIGIT_TWELVE_HOUR, TimestampPrecision.HOUR)),
        "a" to listOf(p("a", PatternType.AM_PM, TimestampPrecision.AM_PM)),
        "m" to listOf(p("m", PatternType.MINUTE, TimestampPrecision.MINUTE)),
        "mm" to listOf(p("mm", PatternType.TWO_DIGIT_MINUTE, TimestampPrecision.MINUTE)),
        "s" to listOf(p("s", PatternType.SECOND, TimestampPrecision.SECOND)),
        "ss" to listOf(p("ss", PatternType.TWO_DIGIT_SECOND, TimestampPrecision.SECOND)),
        "S" to listOf(p("S", PatternType.ONE_DIGIT_FRACTION_OF_SECOND, TimestampPrecision.FRACTION_OF_SECOND)),
        "SS" to listOf(p("SS", PatternType.TWO_DIGIT_FRACTION_OF_SECOND, TimestampPrecision.FRACTION_OF_SECOND)),
        "SSS" to listOf(p("SSS", PatternType.THREE_DIGIT_FRACTION_OF_SECOND, TimestampPrecision.FRACTION_OF_SECOND)),
        "x" to listOf(p("x", PatternType.TWO_DIGIT_OFFSET, TimestampPrecision.OFFSET)),
        "xx" to listOf(p("xx", PatternType.FOUR_DIGIT_OFFSET, TimestampPrecision.OFFSET)),
        "xxxx" to listOf(p("xxxx", PatternType.FOUR_DIGIT_OFFSET, TimestampPrecision.OFFSET)),
        "xxx" to listOf(p("xxx", PatternType.FIVE_DIGIT_OFFSET, TimestampPrecision.OFFSET)),
        "xxxxx" to listOf(p("xxxxx", PatternType.FIVE_DIGIT_OFFSET, TimestampPrecision.OFFSET)),
        "X" to listOf(p("X", PatternType.TWO_DIGIT_Z_OFFSET, TimestampPrecision.OFFSET)),
        "XX" to listOf(p("XX", PatternType.FOUR_DIGIT_Z_OFFSET, TimestampPrecision.OFFSET)),
        "XXXX" to listOf(p("XXXX", PatternType.FOUR_DIGIT_Z_OFFSET, TimestampPrecision.OFFSET)),
        "XXX" to listOf(p("XXX", PatternType.FIVE_DIGIT_Z_OFFSET, TimestampPrecision.OFFSET)),
        "XXXXX" to listOf(p("XXXXX", PatternType.FIVE_DIGIT_Z_OFFSET, TimestampPrecision.OFFSET)),

        "yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX" to listOf(
            p("yyyy", PatternType.FOUR_DIGIT_YEAR, TimestampPrecision.YEAR),
            t("-"),
            p("MM", PatternType.TWO_DIGIT_MONTH, TimestampPrecision.MONTH),
            t("-"),
            p("dd", PatternType.TWO_DIGIT_DAY, TimestampPrecision.DAY),
            t("'T'"),
            p("HH", PatternType.TWO_DIGIT_TWENTY_FOUR_HOUR, TimestampPrecision.HOUR),
            t(":"),
            p("mm", PatternType.TWO_DIGIT_MINUTE, TimestampPrecision.MINUTE),
            t(":"),
            p("ss", PatternType.TWO_DIGIT_SECOND, TimestampPrecision.SECOND),
            t("."),
            p("SSS", PatternType.THREE_DIGIT_FRACTION_OF_SECOND, TimestampPrecision.FRACTION_OF_SECOND),
            p("XXXXX", PatternType.FIVE_DIGIT_Z_OFFSET, TimestampPrecision.OFFSET)),

        "yyyyMMddHHmmssSSSXXXXX" to listOf(
            p("yyyy", PatternType.FOUR_DIGIT_YEAR, TimestampPrecision.YEAR),
            p("MM", PatternType.TWO_DIGIT_MONTH, TimestampPrecision.MONTH),
            p("dd", PatternType.TWO_DIGIT_DAY, TimestampPrecision.DAY),
            p("HH", PatternType.TWO_DIGIT_TWENTY_FOUR_HOUR, TimestampPrecision.HOUR),
            p("mm", PatternType.TWO_DIGIT_MINUTE, TimestampPrecision.MINUTE),
            p("ss", PatternType.TWO_DIGIT_SECOND, TimestampPrecision.SECOND),
            p("SSS", PatternType.THREE_DIGIT_FRACTION_OF_SECOND, TimestampPrecision.FRACTION_OF_SECOND),
            p("XXXXX", PatternType.FIVE_DIGIT_Z_OFFSET, TimestampPrecision.OFFSET)))

    @Test
    @Parameters
    fun parse(pair: Pair<String, List<AstNode>>) {
        val tokens = TimestampFormatPatternLexer().tokenize(pair.first)
        val actual = TimestampFormatPatternParser().parse(tokens)
        assertEquals(pair.second, actual)
    }

    @Test(expected = IllegalArgumentException::class)
    fun noPatterns() {
        val tokens = TimestampFormatPatternLexer().tokenize("'some text' /-,:.")
        TimestampFormatPatternParser().parse(tokens)
    }

    @Test(expected = IllegalArgumentException::class)
    fun badPattern() {
        val tokens = TimestampFormatPatternLexer().tokenize("yyyyy")
        TimestampFormatPatternParser().parse(tokens)
    }
}