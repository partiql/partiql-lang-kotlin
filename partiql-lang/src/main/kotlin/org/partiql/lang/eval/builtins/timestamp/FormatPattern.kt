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

import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.util.propertyValueMapOf

/**
 * Represents a parsed timestamp format pattern.
 */
internal class FormatPattern(val formatPatternString: String, val formatItems: List<FormatItem>) {

    companion object {

        /**
         * Constructs a new instance of [FormatPattern] using the specified format pattern string.
         */
        fun fromString(pattern: String): FormatPattern = TimestampFormatPatternParser().parse(pattern)
    }

    /**
     * Indicates the least significant of the fields in this format pattern or `null` if no fields are specified.
     * When parsing, this will correspond to the precision of the Ion timestamp.
     */
    val leastSignificantField: TimestampField? by lazy {
        formatSymbols
            .filter { it.field.precisionRank != null }
            .sortedByDescending { it.field.precisionRank }
            .firstOrNull()
            ?.field
    }

    /**
     * Lazily filtered list of [PatternSymbol] instances present in [formatItems].
     */
    val formatSymbols: List<PatternSymbol> by lazy {
        formatItems.filterIsInstance<PatternSymbol>()
    }

    /**
     * True if this [FormatPattern] contains a two digit year.
     */
    val has2DigitYear: Boolean by lazy {
        formatSymbols.filterIsInstance<YearPatternSymbol>().any { it.format == YearFormat.TWO_DIGIT }
    }

    /**
     * True if this [FormatPattern] contains an offset symbol.
     */
    val hasOffset: Boolean by lazy {
        formatSymbols.filterIsInstance<OffsetPatternSymbol>().any()
    }

    /**
     * True if this [FormatPattern] has an AM/PM offset symbol.
     */
    val hasAmPm: Boolean by lazy {
        formatSymbols.filterIsInstance<AmPmPatternSymbol>().any()
    }

    /**
     * Validates the timestamp for parsing, throwing an `EvaluationException` if this format pattern cannot yield a
     * valid Ion timestamp.
     */
    fun validateForTimestampParsing() {
        checkForFieldsNotValidForParsing()
        checkDuplicatefields()
        checkFieldCombination()
        checkAmPmMismatch()
    }

    /**
     * Validates that duplicate fields are not included.
     */
    private fun checkDuplicatefields() {

        val duplicatedField = formatSymbols.groupingBy { it.field }
            .eachCount()
            .filter { it.value > 1 } // Appears more than once in field
            .asSequence()
            .sortedByDescending { it.value } // Sort descending by number of appearances
            .firstOrNull()

        if (duplicatedField != null) {
            throw EvaluationException(
                message = "timestamp format pattern duplicate fields",
                errorCode = ErrorCode.EVALUATOR_TIMESTAMP_FORMAT_PATTERN_DUPLICATE_FIELDS,
                errorContext = propertyValueMapOf(
                    Property.TIMESTAMP_FORMAT_PATTERN to formatPatternString,
                    Property.TIMESTAMP_FORMAT_PATTERN_FIELDS to duplicatedField.key
                ),
                internal = false
            )
        }
    }

    /**
     * Validates that when 12 hour hour of day field is present, am/pm must also be included
     * and that when 24 hour of day field is present, am/pm is *not* included.
     */
    private fun checkAmPmMismatch() {
        formatSymbols.filterIsInstance<HourOfDayPatternSymbol>().firstOrNull()?.let {

            val hasAmPm = formatSymbols.filterIsInstance<AmPmPatternSymbol>().any()
            when (it.format.clock) {
                HourClock.TwelveHour -> {
                    if (!hasAmPm) {
                        throw EvaluationException(
                            message = "timestamp format pattern contains 12-hour hour of day field but doesn't " + "contain an am/pm field.",
                            errorCode = ErrorCode.EVALUATOR_TIMESTAMP_FORMAT_PATTERN_HOUR_CLOCK_AM_PM_MISMATCH,
                            errorContext = propertyValueMapOf(Property.TIMESTAMP_FORMAT_PATTERN to formatPatternString),
                            internal = false
                        )
                    }
                }
                HourClock.TwentyFourHour -> {
                    if (hasAmPm) {
                        throw EvaluationException(
                            message = "timestamp format pattern contains 24-hour hour of day field and also " + "contains an am/pm field.",
                            errorCode = ErrorCode.EVALUATOR_TIMESTAMP_FORMAT_PATTERN_HOUR_CLOCK_AM_PM_MISMATCH,
                            errorContext = propertyValueMapOf(Property.TIMESTAMP_FORMAT_PATTERN to formatPatternString),
                            internal = false
                        )
                    }
                }
            } // end when
        } // end let
    }

    /**
     * Ensures that the format pattern includes symbols that can yield a valid Ion timestamp.
     */
    private fun checkFieldCombination() {

        fun err(missingFields: String): Nothing =
            throw EvaluationException(
                message = "timestamp format pattern missing fields",
                errorCode = ErrorCode.EVALUATOR_INCOMPLETE_TIMESTAMP_FORMAT_PATTERN,
                errorContext = propertyValueMapOf(
                    Property.TIMESTAMP_FORMAT_PATTERN to formatPatternString,
                    Property.TIMESTAMP_FORMAT_PATTERN_FIELDS to missingFields
                ),
                internal = false
            )

        fun errIfMissingTimestampFields(vararg fields: TimestampField) {
            val missingFields = fields.filter { requiredField -> formatSymbols.all { it.field != requiredField } }

            if (missingFields.any()) {
                err(missingFields.asSequence().joinToString(", "))
            }
        }

        // Minimum precision for patterns containing offset or am/pm symbols is HOUR.
        // NOTE: HOUR is not a valid precision for an Ion timestamp but when a format pattern's
        // leastSignificantField is HOUR, the minute field defaults to 00.
        if (hasOffset || hasAmPm) {
            errIfMissingTimestampFields(
                TimestampField.YEAR, TimestampField.MONTH_OF_YEAR, TimestampField.DAY_OF_MONTH,
                TimestampField.HOUR_OF_DAY
            )
        }

        when (leastSignificantField) {
            null -> {
                // If most precise field is null there are no format symbols corresponding to any timestamp fields.
                err("YEAR")
            }
            TimestampField.YEAR -> {
                // the year field is the most coarse of the timestamp fields
                // it does not require any other fields to make a complete timestamp
            }
            TimestampField.MONTH_OF_YEAR -> errIfMissingTimestampFields(TimestampField.YEAR)
            TimestampField.DAY_OF_MONTH -> errIfMissingTimestampFields(TimestampField.YEAR, TimestampField.MONTH_OF_YEAR)
            TimestampField.HOUR_OF_DAY -> errIfMissingTimestampFields(
                TimestampField.YEAR,
                TimestampField.MONTH_OF_YEAR, TimestampField.DAY_OF_MONTH
            )
            TimestampField.MINUTE_OF_HOUR -> errIfMissingTimestampFields(
                TimestampField.YEAR,
                TimestampField.MONTH_OF_YEAR, TimestampField.DAY_OF_MONTH, TimestampField.HOUR_OF_DAY
            )
            TimestampField.SECOND_OF_MINUTE -> errIfMissingTimestampFields(
                TimestampField.YEAR,
                TimestampField.MONTH_OF_YEAR, TimestampField.DAY_OF_MONTH, TimestampField.HOUR_OF_DAY,
                TimestampField.MINUTE_OF_HOUR
            )
            TimestampField.FRACTION_OF_SECOND -> errIfMissingTimestampFields(
                TimestampField.YEAR,
                TimestampField.MONTH_OF_YEAR, TimestampField.DAY_OF_MONTH, TimestampField.HOUR_OF_DAY,
                TimestampField.MINUTE_OF_HOUR, TimestampField.SECOND_OF_MINUTE
            )

            TimestampField.OFFSET, TimestampField.AM_PM -> {
                throw IllegalStateException("OFFSET, AM_PM should never be the least significant field!")
            }
        }
    }
    /**
     * Ensures that this format pattern doesn't contain any format symbols which are valid for timestamp formatting
     * but not for parsing.
     */
    private fun checkForFieldsNotValidForParsing() {
        if (formatSymbols.filterIsInstance<MonthPatternSymbol>().any { it.format == MonthFormat.FIRST_LETTER_OF_MONTH_NAME }) {
            throw EvaluationException(
                message = "timestamp format pattern missing fields",
                errorCode = ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN_SYMBOL_FOR_PARSING,
                errorContext = propertyValueMapOf(Property.TIMESTAMP_FORMAT_PATTERN to formatPatternString),
                internal = false
            )
        }
    }
}
