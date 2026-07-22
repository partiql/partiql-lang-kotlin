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
 * Accepted argument types:
 * - `string` (first argument): CHAR, VARCHAR, CLOB, STRING
 * - `from` / `to` (second and third arguments): CHAR, VARCHAR, STRING (CHAR/VARCHAR
 *   are implicitly coerced to STRING during function resolution).
 *
 * The declared type of the result is the declared type of the first argument
 * (following the SQL <string value function> convention used by [FnUpper]/[FnLower]):
 * - CHAR(n)    -> CHAR(n)
 * - VARCHAR(n) -> VARCHAR(n)
 * - CLOB(n)    -> CLOB(n)
 * - STRING     -> STRING (PartiQL extension)
 */
internal object FnReplace : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature("replace", listOf(PType.dynamic(), PType.string(), PType.string()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val stringType = args[0]
        return when (stringType.code()) {
            PType.CHAR -> Function.instance(
                name = "replace",
                returns = PType.character(),
                parameters = arrayOf(Parameter("string", stringType), Parameter("from", PType.string()), Parameter("to", PType.string())),
            ) { args ->
                val result = replace(args[0].string, args[1].string, args[2].string)
                Datum.character(result)
            }
            PType.VARCHAR -> Function.instance(
                name = "replace",
                returns = PType.varchar(),
                parameters = arrayOf(Parameter("string", stringType), Parameter("from", PType.string()), Parameter("to", PType.string())),
            ) { args ->
                val result = replace(args[0].string, args[1].string, args[2].string)
                Datum.varchar(result)
            }
            PType.CLOB -> Function.instance(
                name = "replace",
                returns = PType.clob(),
                parameters = arrayOf(Parameter("string", stringType), Parameter("from", PType.string()), Parameter("to", PType.string())),
            ) { args ->
                val result = replace(args[0].bytes.toString(Charsets.UTF_8), args[1].string, args[2].string)
                Datum.clob(result.toByteArray())
            }
            // PType.UNKNOWN is for null propagation
            PType.STRING, PType.UNKNOWN -> Function.instance(
                name = "replace",
                returns = PType.string(),
                parameters = arrayOf(Parameter("string", stringType), Parameter("from", PType.string()), Parameter("to", PType.string())),
            ) { args ->
                val result = replace(args[0].string, args[1].string, args[2].string)
                Datum.string(result)
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
