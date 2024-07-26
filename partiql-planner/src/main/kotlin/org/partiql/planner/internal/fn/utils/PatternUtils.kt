package org.partiql.planner.internal.fn.utils

import org.partiql.planner.internal.fn.utils.StringUtils.codePointSequence
import java.util.regex.Pattern

internal object PatternUtils {

    private const val ANY_MANY = '%'.code
    private const val ANY_ONE = '_'.code

    private const val PATTERN_ADDITIONAL_BUFFER = 8

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

    internal fun matchRegexPattern(value: String, likePattern: Pattern): Boolean = likePattern.matcher(value).matches()

    /**
     * A search pattern is valid when
     * 1. pattern is not null
     * 1. pattern contains characters where `_` means any 1 character and `%` means any string of length 0 or more
     * 1. if the escape character is specified then pattern can be deterministically partitioned into character groups where
     *     1. A length 1 character group consists of any character other than the ESCAPE character
     *     1. A length 2 character group consists of the ESCAPE character followed by either `_` or `%` or the ESCAPE character itself
     */
    internal fun checkPattern(
        pattern: String,
        escape: String,
    ): Pair<String, Int> {
        val escapeCharString = checkEscapeChar(escape)
        val escapeCharCodePoint = escapeCharString.codePointAt(0) // escape is a string of length 1
        val validEscapedChars = setOf(ANY_MANY, ANY_ONE, escapeCharCodePoint)
        val iter = pattern.codePointSequence().iterator()

        while (iter.hasNext()) {
            val current = iter.next()
            if (current == escapeCharCodePoint && (!iter.hasNext() || !validEscapedChars.contains(iter.next()))) {
                throw IllegalStateException("Invalid Escape Sequence")
            }
        }
        return Pair(pattern, escapeCharCodePoint)
    }

    private fun checkEscapeChar(escape: String): String {
        when {
            escape.isEmpty() -> {
                throw IllegalStateException("Cannot use empty character as ESCAPE character in a LIKE predicate: $escape")
            }
            else -> {
                if (escape.trim().length != 1) {
                    throw IllegalStateException("Escape character must have size 1 : $escape")
                }
            }
        }
        return escape
    }
}
