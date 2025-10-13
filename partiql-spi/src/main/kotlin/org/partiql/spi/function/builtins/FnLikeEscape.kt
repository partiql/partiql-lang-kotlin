// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.function.builtins.internal.PErrors
import org.partiql.spi.internal.SqlTypeFamily
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.utils.PatternUtils
import org.partiql.spi.utils.PatternUtils.checkPattern
import org.partiql.spi.utils.PatternUtils.parsePattern
import org.partiql.spi.value.Datum
import java.util.regex.Pattern

/**
 * SQL <LIKE ... ESCAPE ...> predicate implementation.
 *
 * Implements the SQL <like predicate> with an ESCAPE character as defined in SQL:1999 section 8.5.
 *
 * The ESCAPE clause allows treating pattern special characters (`'_'`, `'%'`) as literals
 * by prefixing them with the escape character.
 *
 * Pattern special characters:
 * - `'_'` matches a single character
 * - `'%'` matches zero or more characters
 * - If an ESCAPE character is specified, then:
 *   - A substring of length 2 starting with the ESCAPE character followed by `_`, `%`, or the ESCAPE character
 *     represents a literal.
 *   - Invalid escape sequences raise a data exception (e.g. ESCAPE character length ≠ 1).
 *
 * Type coercion follows SQL precedence:
 * - CHAR > VARCHAR > STRING > CLOB
 *
 * Behavior:
 * - If any of value, pattern, or escape are NULL, the result is UNKNOWN (null).
 * - An escape character must be a single character; result is UNKNOWN (null).
 * - Pattern is converted to a regular expression with escape logic handled.
 *
 * SQL Exception Conditions:
 * - If ESCAPE character length ≠ 1 → data exception — invalid escape character
 * - If pattern contains invalid escape sequences → data exception — invalid escape sequence
 *
 * Example:
 * ```
 * 'abc' LIKE 'a\_c' ESCAPE '\\'    -- true (matches literal underscore)
 * 'a_c' LIKE 'a\_c' ESCAPE '\'     -- true
 * 'abc' LIKE 'a%z' ESCAPE '#'      -- false
 * ```
 *
 * @see FnLike for the variant without ESCAPE clause.
 */
internal object FnLikeEscape : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature(FunctionUtils.hide("like_escape"), listOf(PType.dynamic(), PType.dynamic(), PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val valueType = args[0]
        val patternType = args[1]
        val escapeType = args[2]
        // Check if all are string types
        if (valueType !in SqlTypeFamily.TEXT || patternType !in SqlTypeFamily.TEXT || escapeType !in SqlTypeFamily.TEXT) return null
        // Use type precedence for coercion: CHAR > VARCHAR > STRING > CLOB
        val resultType = maxOf(valueType.code(), patternType.code(), escapeType.code())
        return when (resultType) {
            PType.CHAR, PType.VARCHAR, PType.STRING -> {
                Function.instance(
                    name = FunctionUtils.hide("like_escape"),
                    returns = PType.bool(),
                    parameters = arrayOf(Parameter("value", valueType), Parameter("pattern", patternType), Parameter("escape", escapeType)),
                ) { params ->
                    val value = params[0].string
                    val pattern = params[1].string
                    val escape = params[2].string
                    val (patternString, escapeChar) =
                        try {
                            checkPattern(pattern, escape)
                        } catch (e: IllegalStateException) {
                            throw PErrors.internalErrorException(e)
                        }
                    val likeRegexPattern = when {
                        patternString.isEmpty() -> Pattern.compile("")
                        else -> parsePattern(patternString, escapeChar)
                    }
                    Datum.bool(PatternUtils.matchRegexPattern(value, likeRegexPattern))
                }
            }
            PType.CLOB -> {
                Function.instance(
                    name = FunctionUtils.hide("like_escape"),
                    returns = PType.bool(),
                    parameters = arrayOf(Parameter("value", valueType), Parameter("pattern", patternType), Parameter("escape", escapeType)),
                ) { params ->
                    val value = params[0].bytes.toString(Charsets.UTF_8)
                    val pattern = params[1].bytes.toString(Charsets.UTF_8)
                    val escape = params[2].bytes.toString(Charsets.UTF_8)
                    val (patternString, escapeChar) =
                        try {
                            checkPattern(pattern, escape)
                        } catch (e: IllegalStateException) {
                            throw PErrors.internalErrorException(e)
                        }
                    val likeRegexPattern = when {
                        patternString.isEmpty() -> Pattern.compile("")
                        else -> parsePattern(patternString, escapeChar)
                    }
                    Datum.bool(PatternUtils.matchRegexPattern(value, likeRegexPattern))
                }
            }
            else -> null
        }
    }
}
