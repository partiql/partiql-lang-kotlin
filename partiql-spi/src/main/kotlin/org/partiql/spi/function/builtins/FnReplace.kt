// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

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
        return when (stringType.code()) {
            PType.CHAR, PType.VARCHAR -> Function.instance(
                name = "replace",
                returns = PType.varchar(),
                parameters = arrayOf(Parameter("string", stringType), Parameter("from", PType.string()), Parameter("to", PType.string())),
            ) { args ->
                val result = replace(args[0].string, args[1].string, args[2].string)
                Datum.varchar(result)
            }
            PType.STRING -> Function.instance(
                name = "replace",
                returns = PType.string(),
                parameters = arrayOf(Parameter("string", stringType), Parameter("from", PType.string()), Parameter("to", PType.string())),
            ) { args ->
                val result = replace(args[0].string, args[1].string, args[2].string)
                Datum.string(result)
            }
            PType.CLOB -> Function.instance(
                name = "replace",
                returns = PType.clob(),
                parameters = arrayOf(Parameter("string", stringType), Parameter("from", PType.clob()), Parameter("to", PType.clob())),
            ) { args ->
                val string = args[0].bytes.toString(Charsets.UTF_8)
                val from = args[1].bytes.toString(Charsets.UTF_8)
                val to = args[2].bytes.toString(Charsets.UTF_8)

                val result = replace(string, from, to)
                Datum.clob(result.toByteArray())
            }
            else -> null
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
