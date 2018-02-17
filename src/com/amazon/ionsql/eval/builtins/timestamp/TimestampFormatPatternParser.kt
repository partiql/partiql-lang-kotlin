package com.amazon.ionsql.eval.builtins.timestamp

// TODO javadoc on all things

// in order from lower to highest precision
internal enum class TimestampPrecision {
    YEAR, MONTH, DAY, HOUR, AM_PM, MINUTE, SECOND, FRACTION_OF_SECOND, OFFSET
}

internal enum class PatternType {
    YEAR,
    TWO_DIGIT_YEAR,
    FOUR_DIGIT_YEAR,
    MONTH,
    TWO_DIGIT_MONTH,
    DAY,
    TWO_DIGIT_DAY,
    TWELVE_HOUR,
    TWO_DIGIT_TWELVE_HOUR,
    TWENTY_FOUR_HOUR,
    TWO_DIGIT_TWENTY_FOUR_HOUR,
    AM_PM,
    MINUTE,
    TWO_DIGIT_MINUTE,
    SECOND,
    TWO_DIGIT_SECOND,
    ONE_DIGIT_FRACTION_OF_SECOND,
    TWO_DIGIT_FRACTION_OF_SECOND,
    THREE_DIGIT_FRACTION_OF_SECOND,
    TWO_DIGIT_OFFSET,
    FOUR_DIGIT_OFFSET,
    FIVE_DIGIT_OFFSET,
    TWO_DIGIT_Z_OFFSET,
    FOUR_DIGIT_Z_OFFSET,
    FIVE_DIGIT_Z_OFFSET
}

sealed class AstNode {
    abstract val raw: String
}

internal data class TextAstNode(override val raw: String) : AstNode()
internal data class PatternAstNode(override val raw: String, val type: PatternType, val precision: TimestampPrecision) : AstNode()

internal class TimestampFormatPatternParser {
    fun parse(tokens: List<Token>): List<AstNode> {
        var patternCounter = 0
        val ast = tokens.map { token ->
            when (token.tokenType) {
                TokenType.TEXT    -> TextAstNode(token.value)
                TokenType.PATTERN -> {
                    patternCounter += 1
                    parsePattern(token.value)
                }
            }
        }

        if(patternCounter == 0) {
            throw IllegalArgumentException("At least one pattern is required")
        }

        return ast
    }

    private fun parsePattern(raw: String): AstNode = when (raw) {
        "y"            -> PatternAstNode(raw, PatternType.YEAR, TimestampPrecision.YEAR)
        "yy"           -> PatternAstNode(raw, PatternType.TWO_DIGIT_YEAR, TimestampPrecision.YEAR)
        "yyyy"         -> PatternAstNode(raw, PatternType.FOUR_DIGIT_YEAR, TimestampPrecision.YEAR)
        "M"            -> PatternAstNode(raw, PatternType.MONTH, TimestampPrecision.MONTH)
        "MM"           -> PatternAstNode(raw, PatternType.TWO_DIGIT_MONTH, TimestampPrecision.MONTH)
        "d"            -> PatternAstNode(raw, PatternType.DAY, TimestampPrecision.DAY)
        "dd"           -> PatternAstNode(raw, PatternType.TWO_DIGIT_DAY, TimestampPrecision.DAY)
        "H"            -> PatternAstNode(raw, PatternType.TWENTY_FOUR_HOUR, TimestampPrecision.HOUR)
        "HH"           -> PatternAstNode(raw, PatternType.TWO_DIGIT_TWENTY_FOUR_HOUR, TimestampPrecision.HOUR)
        "h"            -> PatternAstNode(raw, PatternType.TWELVE_HOUR, TimestampPrecision.HOUR)
        "hh"           -> PatternAstNode(raw, PatternType.TWO_DIGIT_TWELVE_HOUR, TimestampPrecision.HOUR)
        "a"            -> PatternAstNode(raw, PatternType.AM_PM, TimestampPrecision.AM_PM)
        "m"            -> PatternAstNode(raw, PatternType.MINUTE, TimestampPrecision.MINUTE)
        "mm"           -> PatternAstNode(raw, PatternType.TWO_DIGIT_MINUTE, TimestampPrecision.MINUTE)
        "s"            -> PatternAstNode(raw, PatternType.SECOND, TimestampPrecision.SECOND)
        "ss"           -> PatternAstNode(raw, PatternType.TWO_DIGIT_SECOND, TimestampPrecision.SECOND)
        "S"            -> PatternAstNode(raw, PatternType.ONE_DIGIT_FRACTION_OF_SECOND, TimestampPrecision.FRACTION_OF_SECOND)
        "SS"           -> PatternAstNode(raw, PatternType.TWO_DIGIT_FRACTION_OF_SECOND, TimestampPrecision.FRACTION_OF_SECOND)
        "SSS"          -> PatternAstNode(raw, PatternType.THREE_DIGIT_FRACTION_OF_SECOND, TimestampPrecision.FRACTION_OF_SECOND)
        "x"            -> PatternAstNode(raw, PatternType.TWO_DIGIT_OFFSET, TimestampPrecision.OFFSET)
        "xx", "xxxx"   -> PatternAstNode(raw, PatternType.FOUR_DIGIT_OFFSET, TimestampPrecision.OFFSET)
        "xxx", "xxxxx" -> PatternAstNode(raw, PatternType.FIVE_DIGIT_OFFSET, TimestampPrecision.OFFSET)
        "X"            -> PatternAstNode(raw, PatternType.TWO_DIGIT_Z_OFFSET, TimestampPrecision.OFFSET)
        "XX", "XXXX"   -> PatternAstNode(raw, PatternType.FOUR_DIGIT_Z_OFFSET, TimestampPrecision.OFFSET)
        "XXX", "XXXXX" -> PatternAstNode(raw, PatternType.FIVE_DIGIT_Z_OFFSET, TimestampPrecision.OFFSET)
        else           -> throw IllegalArgumentException("Unknown pattern $raw")
    }
}
