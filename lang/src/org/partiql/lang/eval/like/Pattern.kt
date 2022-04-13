package org.partiql.lang.eval.like

import java.util.regex.Pattern

private const val ANY_MANY = '%'.toInt()
private const val ANY_ONE = '_'.toInt()

/**
 * Translates a SQL-style `LIKE` pattern to a regular expression.
 *
 * @param likePattern A `LIKE` match pattern (i.e. a string where '%' means '.*?' and '_' means '.').
 * @param escapeChar The escape character for the `LIKE` pattern.
 *
 * @return a [Pattern] which is a regular expression corresponding to the specified `LIKE` pattern.
 */
internal fun parsePattern(likePattern: String, escapeChar: Int?): Pattern {
    val buf = StringBuilder(likePattern.length + 8)
    buf.append("^")

    var isEscaped = false
    var wildcardMin: Int? = null
    var wildcardUnbounded = false
    var literal = StringBuilder()

    // If a wildcard (e.g. a sequence of '%' and '_') has been accumulated, write out the regex equivalent
    val flushWildcard = {
        if (wildcardMin != null) {
            if (wildcardUnbounded) {
                if (wildcardMin == 0) {
                    buf.append(".*?")
                } else if (wildcardMin == 1) {
                    buf.append(".+?")
                } else {
                    buf.append(".{$wildcardMin,}?")
                }
            } else {
                if (wildcardMin == 1) {
                    buf.append(".")
                } else {
                    buf.append(".{$wildcardMin,$wildcardMin}")
                }
            }
            wildcardMin = null
            wildcardUnbounded = false
        }
    }

    // if a literal has been accumulated, write it out, regex-quoted
    val flushLiteral = {
        if (!literal.isEmpty()) {
            buf.append(Pattern.quote(literal.toString()))
            literal.clear()
        }
    }

    for (codepoint in likePattern.codePoints()) {
        if (!isEscaped && (codepoint == escapeChar)) {
            isEscaped = true // skip to the next codepoint
        } else {
            if (!isEscaped) {
                when (codepoint) {
                    ANY_ONE -> {
                        flushLiteral()
                        wildcardMin = (wildcardMin ?: 0) + 1
                    }
                    ANY_MANY -> {
                        flushLiteral()
                        wildcardMin = (wildcardMin ?: 0)
                        wildcardUnbounded = true
                    }
                    else -> {
                        flushWildcard()
                        literal.append(Character.toChars(codepoint))
                    }
                }
            } else {
                flushWildcard()
                literal.append(Character.toChars(codepoint))
                isEscaped = false
            }
        }
    }

    flushLiteral()
    flushWildcard()

    buf.append("$")
    return Pattern.compile(buf.toString())
}
