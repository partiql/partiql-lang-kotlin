package com.amazon.ionsql.eval.builtins.timestamp

import com.amazon.ionsql.errors.*
import com.amazon.ionsql.eval.*
import com.amazon.ionsql.util.*


internal class TimestampFormatPatternParser {

    fun parse(formatPatternString: String): FormatPattern {
        val lexer = TimestampFormatPatternLexer()
        val tokens = lexer.tokenize(formatPatternString)

        var patternCounter = 0
        val formatItems = tokens.map { token ->
            when (token.tokenType) {
                TokenType.TEXT -> TextItem(token.value)
                TokenType.PATTERN                           -> {
                    patternCounter += 1
                    parsePattern(token.value)
                }
            }
        }

        return FormatPattern(formatPatternString, formatItems)
    }

    private fun parsePattern(raw: String): FormatItem = when (raw) {
        // Possible optimization here:  create singleton instances corresponding to each of the branches and return
        // those instead of creating new instances.  This could work because all of the objects here are immutable.
        // This reduces the amount of garbage created during execution of this method.
        "y"            -> YearPatternSymbol(YearFormat.FOUR_DIGIT)
        "yy"           -> YearPatternSymbol(YearFormat.TWO_DIGIT)
        "yyy", "yyyy"  -> YearPatternSymbol(YearFormat.FOUR_DIGIT_ZERO_PADDED)

        "M"            -> MonthPatternSymbol(MonthFormat.MONTH_NUMBER)
        "MM"           -> MonthPatternSymbol(MonthFormat.MONTH_NUMBER_ZERO_PADDED)
        "MMM"          -> MonthPatternSymbol(MonthFormat.ABBREVIATED_MONTH_NAME)
        "MMMM"         -> MonthPatternSymbol(MonthFormat.FULL_MONTH_NAME)
        "MMMMM"        -> MonthPatternSymbol(MonthFormat.FIRST_LETTER_OF_MONTH_NAME)

        "d"            -> DayOfMonthPatternSymbol(TimestampFieldFormat.NUMBER)
        "dd"           -> DayOfMonthPatternSymbol(TimestampFieldFormat.ZERO_PADDED_NUMBER)

        "H"            -> HourOfDayPatternSymbol(HourOfDayFormatFieldFormat.NUMBER_24_HOUR)
        "HH"           -> HourOfDayPatternSymbol(HourOfDayFormatFieldFormat.ZERO_PADDED_NUMBER_24_HOUR)
        "h"            -> HourOfDayPatternSymbol(HourOfDayFormatFieldFormat.NUMBER_12_HOUR)
        "hh"           -> HourOfDayPatternSymbol(HourOfDayFormatFieldFormat.ZERO_PADDED_NUMBER_12_HOUR)

        "a"            -> AmPmPatternSymbol()

        "m"            -> MinuteOfHourPatternSymbol(TimestampFieldFormat.NUMBER)
        "mm"           -> MinuteOfHourPatternSymbol(TimestampFieldFormat.ZERO_PADDED_NUMBER)

        "s"            -> SecondOfMinutePatternPatternSymbol(TimestampFieldFormat.NUMBER)
        "ss"           -> SecondOfMinutePatternPatternSymbol(TimestampFieldFormat.ZERO_PADDED_NUMBER)

        "n"            -> NanoOfSecondPatternSymbol()

        "X"            -> OffsetPatternSymbol(OffsetFieldFormat.ZERO_PADDED_HOUR_OR_Z)
        "XX", "XXXX"   -> OffsetPatternSymbol(OffsetFieldFormat.ZERO_PADDED_HOUR_MINUTE_OR_Z)
        "XXX", "XXXXX" -> OffsetPatternSymbol(OffsetFieldFormat.ZERO_PADDED_HOUR_COLON_MINUTE_OR_Z)

        "x"            -> OffsetPatternSymbol(OffsetFieldFormat.ZERO_PADDED_HOUR)
        "xx", "xxxx"   -> OffsetPatternSymbol(OffsetFieldFormat.ZERO_PADDED_HOUR_MINUTE)
        "xxx", "xxxxx" -> OffsetPatternSymbol(OffsetFieldFormat.ZERO_PADDED_HOUR_COLON_MINUTE)

        else           ->
            //Note: the lexer *should* only return tokens that are full of capital S's so the precision is the length.
            if (raw.first() == 'S')
                FractionOfSecondPatternSymbol(raw.length)
            else
                throw EvaluationException(
                    message = "Invalid symbol in timestamp format pattern",
                    errorCode = ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN_SYMBOL,
                    errorContext = propertyValueMapOf(Property.TIMESTAMP_FORMAT_PATTERN to raw),
                    internal = false
                )
    }
}
