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
 * PartiQL `split` function.
 *
 * ```
 * split(string, delimiter) -> list<<type of string>>
 * ```
 *
 * Splits [string] around each literal occurrence of [delimiter] and returns the
 * resulting parts as a list. The delimiter is matched literally, not as a regular
 * expression (aligning with DuckDB's `string_split`, Trino's `split`, and
 * Redshift's `SPLIT_TO_ARRAY`, rather than Spark's regex `split`).
 *
 * Accepted argument types:
 * - `string` (first argument): CHAR, VARCHAR, CLOB, STRING
 * - `delimiter` (second argument): CHAR, VARCHAR, STRING (CHAR/VARCHAR are
 *   implicitly coerced to STRING during function resolution).
 *
 * The element type of the resulting list is the declared type of the first argument
 * (following the SQL <string value function> convention used by [FnUpper]/[FnLower]):
 * - CHAR(n)    -> list<CHAR(n)>
 * - VARCHAR(n) -> list<VARCHAR(n)>
 * - CLOB(n)    -> list<CLOB(n)>
 * - STRING     -> list<STRING> (PartiQL extension)
 *
 * Notes:
 * - An empty delimiter is matched between every character.
 * - If the delimiter is not found, the result is a single-element list holding
 *   the original string.
 */
internal object FnSplit : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature("split", listOf(PType.dynamic(), PType.string()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val stringType = args[0]
        // If any argument is UNKNOWN (literal NULL), return a resolution-only instance; the framework's isNullCall handles propagation.
        if (args.any { it.code() == PType.UNKNOWN }) {
            return FnUtils.nullResolutionInstance("split", PType.array(PType.string()), args)
        }
        return when (stringType.code()) {
            PType.CHAR -> Function.instance(
                name = "split",
                returns = PType.array(PType.character()),
                parameters = arrayOf(Parameter("string", stringType), Parameter("delimiter", PType.string())),
            ) { args ->
                Datum.array(split(args[0].string, args[1].string).map { Datum.character(it) })
            }
            PType.VARCHAR -> Function.instance(
                name = "split",
                returns = PType.array(PType.varchar()),
                parameters = arrayOf(Parameter("string", stringType), Parameter("delimiter", PType.string())),
            ) { args ->
                Datum.array(split(args[0].string, args[1].string).map { Datum.varchar(it) })
            }
            PType.CLOB -> Function.instance(
                name = "split",
                returns = PType.array(PType.clob()),
                parameters = arrayOf(Parameter("string", stringType), Parameter("delimiter", PType.string())),
            ) { args ->
                Datum.array(split(args[0].bytes.toString(Charsets.UTF_8), args[1].string).map { Datum.clob(it.toByteArray()) })
            }
            PType.STRING -> Function.instance(
                name = "split",
                returns = PType.array(PType.string()),
                parameters = arrayOf(Parameter("string", stringType), Parameter("delimiter", PType.string())),
            ) { args ->
                Datum.array(split(args[0].string, args[1].string).map { Datum.string(it) })
            }
            else -> null
        }
    }

    /**
     * Splits [string] on the literal [delimiter]. An empty delimiter splits into individual
     * characters (DuckDB behavior).
     */
    private fun split(string: String, delimiter: String): List<String> {
        return if (delimiter.isEmpty()) {
            string.map { it.toString() }
        } else {
            string.split(delimiter)
        }
    }
}
