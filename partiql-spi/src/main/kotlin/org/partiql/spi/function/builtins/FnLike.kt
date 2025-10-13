// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.internal.SqlTypeFamily
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.utils.PatternUtils.matchRegexPattern
import org.partiql.spi.utils.PatternUtils.parsePattern
import org.partiql.spi.value.Datum
import java.util.regex.Pattern

/**
 * SQL <LIKE> predicate implementation.
 *
 * Implements the SQL <like predicate> as defined in SQL:1999 section 8.5.
 *
 * Evaluates whether a character string matches a specified pattern using the SQL standard
 * pattern matching rules.
 *
 * Pattern special characters:
 * - `'_'` matches exactly one character
 * - `'%'` matches zero or more characters
 *
 * Type coercion follows SQL precedence:
 * - CHAR > VARCHAR > STRING > CLOB
 *
 * Behavior:
 * - If either value or pattern is NULL, result is UNKNOWN (null).
 * - The pattern must be a valid string; otherwise, result is UNKNOWN (null).
 * - The pattern is translated to a regular expression internally.
 *
 * Example:
 * ```
 * 'abc' LIKE 'a_c'      -- true
 * 'abc' LIKE 'a%'       -- true
 * 'abc' LIKE 'a%z'      -- false
 * ```
 *
 * @see FnLikeEscape for the variant with ESCAPE clause.
 */
internal object FnLike : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature(FunctionUtils.hide("like"), listOf(PType.dynamic(), PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val valueType = args[0]
        val patternType = args[1]
        // Check if both are string types
        if (valueType !in SqlTypeFamily.TEXT || patternType !in SqlTypeFamily.TEXT) return null
        // Use type precedence for coercion: CHAR > VARCHAR > STRING > CLOB
        val resultType = if (valueType.code() != patternType.code()) {
            maxOf(valueType.code(), patternType.code())
        } else {
            valueType.code()
        }
        return when (resultType) {
            PType.CHAR, PType.VARCHAR, PType.STRING -> {
                Function.instance(
                    name = FunctionUtils.hide("like"),
                    returns = PType.bool(),
                    parameters = arrayOf(Parameter("value", valueType), Parameter("pattern", patternType)),
                ) { params ->
                    val value = params[0].string
                    val pattern = params[1].string
                    val likeRegexPattern = when {
                        pattern.isEmpty() -> Pattern.compile("")
                        else -> parsePattern(pattern, null)
                    }
                    Datum.bool(matchRegexPattern(value, likeRegexPattern))
                }
            }
            PType.CLOB -> {
                Function.instance(
                    name = FunctionUtils.hide("like"),
                    returns = PType.bool(),
                    parameters = arrayOf(Parameter("value", valueType), Parameter("pattern", patternType)),
                ) { params ->
                    val value = params[0].bytes.toString(Charsets.UTF_8)
                    val pattern = params[1].bytes.toString(Charsets.UTF_8)
                    val likeRegexPattern = when {
                        pattern.isEmpty() -> Pattern.compile("")
                        else -> parsePattern(pattern, null)
                    }
                    Datum.bool(matchRegexPattern(value, likeRegexPattern))
                }
            }
            else -> null
        }
    }
}
