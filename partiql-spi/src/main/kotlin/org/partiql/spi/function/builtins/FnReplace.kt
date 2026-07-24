// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.function.builtins.FnUtils.stringFnDatum
import org.partiql.spi.function.builtins.FnUtils.stringFnReturn
import org.partiql.spi.function.builtins.FnUtils.textValue
import org.partiql.spi.internal.SqlTypeFamily
import org.partiql.spi.types.PType

/**
 * PartiQL `replace` function.
 *
 * ```
 * replace(string, from, to) -> <type of string>
 * ```
 *
 * Replaces all (non-overlapping) literal occurrences of [from] in the input
 * string with [to]. The match is a plain literal, not a regular expression.
 *
 * This mirrors the 3-argument form found in Redshift (`REPLACE`), DuckDB
 * (`replace`), Trino (`replace/3`), and Spark (`replace/3`).
 *
 * Argument types are resolved together based on the first argument:
 * - When `string` is CHAR, VARCHAR, or STRING, `from` and `to` are STRING (a CHAR/VARCHAR
 *   `from`/`to` is implicitly coerced to STRING during resolution).
 * - When `string` is a CLOB, `from` and `to` are also CLOB.
 *
 * The result type follows the SQL <string value function> convention used by [FnTrim] (replace
 * may change the length, so CHAR is not length-preserving and widens to VARCHAR):
 * - CHAR(n)    -> VARCHAR
 * - VARCHAR(n) -> VARCHAR
 * - CLOB(n)    -> CLOB
 * - STRING     -> STRING (PartiQL extension)
 */
internal object FnReplace : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature("replace", listOf(PType.dynamic(), PType.string(), PType.string()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val stringType = args[0]
        // If any argument is UNKNOWN (literal NULL), return a resolution-only instance; the framework's isNullCall handles propagation.
        if (args.any { it.code() == PType.UNKNOWN }) {
            return FnUtils.nullResolutionInstance("replace", PType.string(), args)
        }
        if (stringType !in SqlTypeFamily.TEXT) return null
        // `from`/`to` match the CLOB family for a CLOB `string`, otherwise STRING.
        val patternType = if (stringType.code() == PType.CLOB) PType.clob() else PType.string()
        return Function.instance(
            name = "replace",
            returns = stringType.stringFnReturn(),
            parameters = arrayOf(Parameter("string", stringType), Parameter("from", patternType), Parameter("to", patternType)),
        ) { params ->
            val string = params[0].textValue(stringType)
            val from = params[1].textValue(patternType)
            val to = params[2].textValue(patternType)
            stringType.stringFnDatum(replace(string, from, to))
        }
    }

    /**
     * Literal (non-regex) replace-all. If [from] is empty the string is returned unchanged
     * (matches Trino/DuckDB/Redshift behavior).
     */
    private fun replace(string: String, from: String, to: String): String {
        return if (from.isEmpty()) string else string.replace(from, to)
    }
}
