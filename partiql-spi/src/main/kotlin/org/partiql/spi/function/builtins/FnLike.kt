// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.function.builtins.FnUtils.isTextOrUnknown
import org.partiql.spi.function.builtins.FnUtils.textValue
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
 * Both arguments must belong to the text family (CHAR, VARCHAR, STRING, CLOB); each parameter keeps
 * its own argument type, so no coercion between text types is required. If either argument is a
 * literal NULL/MISSING (UNKNOWN), the call resolves and the framework propagates NULL/MISSING.
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
        // Both arguments must be a text type (or UNKNOWN, handled below); anything else does not match.
        if (args.any { !it.isTextOrUnknown() }) return null
        val valueType = args[0]
        val patternType = args[1]
        // If either argument is UNKNOWN (literal NULL), return a resolution-only instance; the framework's isNullCall handles propagation.
        if (args.any { it.code() == PType.UNKNOWN }) {
            return FnUtils.nullResolutionInstance(FunctionUtils.hide("like"), PType.bool(), args)
        }
        return Function.instance(
            name = FunctionUtils.hide("like"),
            returns = PType.bool(),
            parameters = arrayOf(Parameter("value", valueType), Parameter("pattern", patternType)),
        ) { params ->
            // Either operand may be a byte-backed CLOB, so read each according to its own type.
            val value = params[0].textValue(valueType)
            val pattern = params[1].textValue(patternType)
            val likeRegexPattern = when {
                pattern.isEmpty() -> Pattern.compile("")
                else -> parsePattern(pattern, null)
            }
            Datum.bool(matchRegexPattern(value, likeRegexPattern))
        }
    }
}
