package org.partiql.lang.ots_work.plugins.standard.operators

import com.amazon.ion.IonValue
import com.amazon.ionelement.api.MetaContainer
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.err
import org.partiql.lang.eval.errorContextFrom
import org.partiql.lang.eval.like.parsePattern
import org.partiql.lang.eval.stringValue
import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.Failed
import org.partiql.lang.ots_work.interfaces.Successful
import org.partiql.lang.ots_work.interfaces.TypeInferenceResult
import org.partiql.lang.ots_work.interfaces.Uncertain
import org.partiql.lang.ots_work.interfaces.operator.LikeOp
import org.partiql.lang.ots_work.interfaces.type.BoolType
import org.partiql.lang.ots_work.interfaces.type.ScalarType
import org.partiql.lang.ots_work.plugins.standard.valueFactory
import org.partiql.lang.util.codePointSequence
import org.partiql.lang.util.stringValue
import java.util.regex.Pattern

class StandardLikeOp(
    var metas: MetaContainer? = null,
    var patternLocationMeta: SourceLocationMeta? = null,
    var escapeLocationMeta: SourceLocationMeta? = null,
) : LikeOp() {
    override val validOperandTypes: List<ScalarType> = ALL_TEXT_TYPES

    override fun inferType(value: CompileTimeType, pattern: CompileTimeType, escape: CompileTimeType?): TypeInferenceResult =
        when {
            value.scalarType !in validOperandTypes || pattern.scalarType !in validOperandTypes -> Failed
            escape === null -> Successful(BoolType.compileTimeType)
            escape.scalarType in validOperandTypes -> Uncertain(BoolType.compileTimeType)
            else -> Failed
        }

    override fun invoke(value: ExprValue, pattern: ExprValue, escape: ExprValue?): ExprValue =
        matchRegexPattern(value, getRegexPattern(pattern, escape))

    private fun getRegexPattern(pattern: ExprValue, escape: ExprValue?): (() -> Pattern)? {
        val patternArgs = listOfNotNull(pattern, escape)
        when {
            patternArgs.any { it.type.isUnknown } -> return null
            patternArgs.any { !it.type.isText } -> return {
                err(
                    "LIKE expression must be given non-null strings as input",
                    ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS,
                    errorContextFrom(metas).also {
                        it[Property.LIKE_PATTERN] = pattern.ionValue.toString()
                        if (escape != null) it[Property.LIKE_ESCAPE] = escape.ionValue.toString()
                    },
                    internal = false
                )
            }
            else -> {
                val (patternString: String, escapeChar: Int?) =
                    checkPattern(pattern.ionValue, patternLocationMeta, escape?.ionValue, escapeLocationMeta)
                val likeRegexPattern = when {
                    patternString.isEmpty() -> Pattern.compile("")
                    else -> parsePattern(patternString, escapeChar)
                }
                return { likeRegexPattern }
            }
        }
    }

    private fun matchRegexPattern(value: ExprValue, likePattern: (() -> Pattern)?): ExprValue {
        return when {
            likePattern == null || value.type.isUnknown -> valueFactory.nullValue
            !value.type.isText -> err(
                "LIKE expression must be given non-null strings as input",
                ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS,
                errorContextFrom(metas).also {
                    it[Property.LIKE_VALUE] = value.ionValue.toString()
                },
                internal = false
            )
            else -> valueFactory.newBoolean(likePattern().matcher(value.stringValue()).matches())
        }
    }

    /**
     * Given the pattern and optional escape character in a `LIKE` predicate as [IonValue]s
     * check their validity based on the SQL92 spec and return a triple that contains in order
     *
     * - the search pattern as a string
     * - the escape character, possibly `null`
     * - the length of the search pattern. The length of the search pattern is either
     *   - the length of the string representing the search pattern when no escape character is used
     *   - the length of the string representing the search pattern without counting uses of the escape character
     *     when an escape character is used
     *
     * A search pattern is valid when
     * 1. pattern is not null
     * 1. pattern contains characters where `_` means any 1 character and `%` means any string of length 0 or more
     * 1. if the escape character is specified then pattern can be deterministically partitioned into character groups where
     *     1. A length 1 character group consists of any character other than the ESCAPE character
     *     1. A length 2 character group consists of the ESCAPE character followed by either `_` or `%` or the ESCAPE character itself
     *
     * @param pattern search pattern
     * @param escape optional escape character provided in the `LIKE` predicate
     *
     * @return a triple that contains in order the search pattern as a [String], optionally the code point for the escape character if one was provided
     * and the size of the search pattern excluding uses of the escape character
     */
    private fun checkPattern(
        pattern: IonValue,
        patternLocationMeta: SourceLocationMeta?,
        escape: IonValue?,
        escapeLocationMeta: SourceLocationMeta?
    ): Pair<String, Int?> {

        val patternString = pattern.stringValue()
            ?: err(
                "Must provide a non-null value for PATTERN in a LIKE predicate: $pattern",
                ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
                errorContextFrom(patternLocationMeta),
                internal = false
            )

        escape?.let {
            val escapeCharString = checkEscapeChar(escape, escapeLocationMeta)
            val escapeCharCodePoint = escapeCharString.codePointAt(0) // escape is a string of length 1
            val validEscapedChars = setOf('_'.toInt(), '%'.toInt(), escapeCharCodePoint)
            val iter = patternString.codePointSequence().iterator()

            while (iter.hasNext()) {
                val current = iter.next()
                if (current == escapeCharCodePoint && (!iter.hasNext() || !validEscapedChars.contains(iter.next()))) {
                    err(
                        "Invalid escape sequence : $patternString",
                        ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
                        errorContextFrom(patternLocationMeta).apply {
                            set(Property.LIKE_PATTERN, patternString)
                            set(Property.LIKE_ESCAPE, escapeCharString)
                        },
                        internal = false
                    )
                }
            }
            return Pair(patternString, escapeCharCodePoint)
        }
        return Pair(patternString, null)
    }

    /**
     * Given an [IonValue] to be used as the escape character in a `LIKE` predicate check that it is
     * a valid character based on the SQL Spec.
     *
     *
     * A value is a valid escape when
     * 1. it is 1 character long, and,
     * 1. Cannot be null (SQL92 spec marks this cases as *unknown*)
     *
     * @param escape value provided as an escape character for a `LIKE` predicate
     *
     * @return the escape character as a [String] or throws an exception when the input is invalid
     */
    private fun checkEscapeChar(escape: IonValue, locationMeta: SourceLocationMeta?): String {
        val escapeChar = escape.stringValue() ?: err(
            "Must provide a value when using ESCAPE in a LIKE predicate: $escape",
            ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
            errorContextFrom(locationMeta),
            internal = false
        )
        when (escapeChar) {
            "" -> {
                err(
                    "Cannot use empty character as ESCAPE character in a LIKE predicate: $escape",
                    ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
                    errorContextFrom(locationMeta),
                    internal = false
                )
            }
            else -> {
                if (escapeChar.trim().length != 1) {
                    err(
                        "Escape character must have size 1 : $escapeChar",
                        ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
                        errorContextFrom(locationMeta),
                        internal = false
                    )
                }
            }
        }
        return escapeChar
    }
}
