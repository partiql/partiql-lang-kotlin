// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import com.amazon.ion.IonValue
import org.partiql.errors.TypeCheckException
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.StringValue
import org.partiql.value.boolValue
import org.partiql.value.check
import java.util.regex.Pattern

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_LIKE__STRING_STRING__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "like",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", STRING),
            FunctionParameter("pattern", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<StringValue>()
        val pattern = args[1].check<StringValue>()
        val pps = LikeUtils.getRegexPattern(pattern, null)
        return LikeUtils.matchRegexPattern(value, pps)
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_LIKE__SYMBOL_SYMBOL__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "like",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", SYMBOL),
            FunctionParameter("pattern", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function like not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_LIKE__CLOB_CLOB__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "like",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("value", CLOB),
            FunctionParameter("pattern", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        TODO("Function like not implemented")
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object LikeUtils {

    private const val ANY_MANY = '%'.code
    private const val ANY_ONE = '_'.code
    private const val PATTERN_ADDITIONAL_BUFFER = 8

    fun matchRegexPattern(value: StringValue, likePattern: (() -> Pattern)): BoolValue {
        return boolValue(likePattern().matcher(value.value!!).matches())
    }

    fun getRegexPattern(pattern: StringValue, escape: StringValue?): (() -> Pattern) {
        val (patternString: String, escapeChar: Int?) =
            checkPattern(pattern.value!!, escape?.value)
        val likeRegexPattern = when {
            patternString.isEmpty() -> Pattern.compile("")
            else -> parsePattern(patternString, escapeChar)
        }
        return { likeRegexPattern }
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
        pattern: String,
        escape: String?,
    ): Pair<String, Int?> {

        escape?.let {
            val escapeCharString = checkEscapeChar(escape)
            val escapeCharCodePoint = escapeCharString.codePointAt(0) // escape is a string of length 1
            val validEscapedChars = setOf('_'.code, '%'.code, escapeCharCodePoint)
            val iter = pattern.codePointSequence().iterator()

            while (iter.hasNext()) {
                val current = iter.next()
                if (current == escapeCharCodePoint && (!iter.hasNext() || !validEscapedChars.contains(iter.next()))) {
                    // TODO: Invalid escape sequence
                    throw TypeCheckException()
                }
            }
            return Pair(pattern, escapeCharCodePoint)
        }
        return Pair(pattern, null)
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
    private fun checkEscapeChar(escape: String): String {
        when (escape) {
            "" -> {
                // Cannot use empty character as ESCAPE character in a LIKE predicate
                throw TypeCheckException()
            }

            else -> {
                if (escape.trim().length != 1) {
                    // Escape character must have size 1
                    throw TypeCheckException()
                }
            }
        }
        return escape
    }

    /** Provides a lazy sequence over the code points in the given string. */
    fun String.codePointSequence(): Sequence<Int> {
        val text = this
        return Sequence {
            var pos = 0
            object : Iterator<Int> {
                override fun hasNext(): Boolean = pos < text.length
                override fun next(): Int {
                    val cp = text.codePointAt(pos)
                    pos += Character.charCount(cp)
                    return cp
                }
            }
        }
    }

    /**
     * Translates a SQL-style `LIKE` pattern to a regular expression.
     *
     * Roughly the algorithm is to
     *   - call `Pattern.quote` on the literal parts of the pattern
     *   - translate a single `_` (with no contiguous `%`) to `.`
     *   - translate a consecutive <n> `_` (with no contiguous `%`) to `.{n,n}`
     *   - translate any number of consecutive `%` to `.*?`
     *   - translate any number of consecutive `%` with a `_` contiguous to `.+?`
     *   - translate any number of consecutive `%` with <n> `_` contiguous to `.{n,}?`
     *   - prefix the pattern translated via the above rule with '^' and suffix with '$'
     *
     * @param likePattern A `LIKE` match pattern (i.e. a string where '%' means zero or more <any char> and '_' means 1 <any char>).
     * @param escapeChar The escape character for the `LIKE` pattern.
     *
     * @return a [Pattern] which is a regular expression corresponding to the specified `LIKE` pattern.
     *
     * Examples:
     * ```
     *   val ESCAPE = '\\'.toInt()
     *
     *   assertEquals("^.*?\\Qfoo\\E$",                 parsePattern("%foo", ESCAPE).pattern())
     *   assertEquals("^\\Qfoo\\E.*?$",                 parsePattern("foo%", ESCAPE).pattern())
     *   assertEquals("^\\Qfoo\\E.*?\\Qbar\\E$",        parsePattern("foo%bar", ESCAPE).pattern())
     *   assertEquals("^\\Qfoo\\E.*?\\Qbar\\E$",        parsePattern("foo%%bar", ESCAPE).pattern())
     *   assertEquals("^\\Qfoo\\E.*?\\Qbar\\E$",        parsePattern("foo%%%bar", ESCAPE).pattern())
     *   assertEquals("^\\Qfoo\\E.*?\\Qbar\\E$",        parsePattern("foo%%%%bar", ESCAPE).pattern())
     *   assertEquals("^.*?\\Qfoo\\E.*?\\Qbar\\E.*?$",
     *                parsePattern("%foo%%%%bar%", ESCAPE).pattern())
     *   assertEquals("^\\Qfoo\\E.{2,}?\\Qbar\\E$",     parsePattern("foo_%_bar", ESCAPE).pattern())
     *   assertEquals("^\\Qfoo\\E.{2,}?\\Qbar\\E$",     parsePattern("foo_%_%bar", ESCAPE).pattern())
     *   assertEquals("^\\Qfoo\\E.{2,}?\\Qbar\\E$",     parsePattern("foo%_%%_%bar", ESCAPE).pattern())
     * ```
     *
     *
     * @see java.util.regex.Pattern
     */
    internal fun parsePattern(likePattern: String, escapeChar: Int?): Pattern {
        val buf = StringBuilder(likePattern.length + PATTERN_ADDITIONAL_BUFFER)
        buf.append("^")

        var isEscaped = false
        var wildcardMin = -1
        var wildcardUnbounded = false
        val literal = StringBuilder()

        // If a wildcard (e.g. a sequence of '%' and '_') has been accumulated, write out the regex equivalent
        val flushWildcard = {
            if (wildcardMin != -1) {
                if (wildcardUnbounded) {
                    when (wildcardMin) {
                        0 -> buf.append(".*?")
                        1 -> buf.append(".+?")
                        else -> buf.append(".{$wildcardMin,}?")
                    }
                } else {
                    when (wildcardMin) {
                        1 -> buf.append(".")
                        else -> buf.append(".{$wildcardMin,$wildcardMin}")
                    }
                }
                wildcardMin = -1
                wildcardUnbounded = false
            }
        }

        // if a literal has been accumulated, write it out, regex-quoted
        val flushLiteral = {
            if (literal.isNotEmpty()) {
                buf.append(Pattern.quote(literal.toString()))
                literal.clear()
            }
        }

        for (codepoint in likePattern.codePoints()) {
            if (!isEscaped) {
                if (codepoint == escapeChar) {
                    isEscaped = true
                    continue // skip to the next codepoint
                }
                when (codepoint) {
                    ANY_ONE -> {
                        flushLiteral()
                        wildcardMin = maxOf(wildcardMin, 0) + 1
                    }

                    ANY_MANY -> {
                        flushLiteral()
                        wildcardMin = maxOf(wildcardMin, 0)
                        wildcardUnbounded = true
                    }

                    else -> {
                        flushWildcard()
                        literal.appendCodePoint(codepoint)
                    }
                }
            } else {
                flushWildcard()
                literal.appendCodePoint(codepoint)
                isEscaped = false
            }
        }

        flushLiteral()
        flushWildcard()

        buf.append("$")
        return Pattern.compile(buf.toString())
    }
}
