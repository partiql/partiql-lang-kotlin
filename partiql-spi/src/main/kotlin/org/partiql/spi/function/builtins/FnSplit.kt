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
 * Argument types are resolved together based on the first argument:
 * - When `string` is CHAR, VARCHAR, or STRING, `delimiter` is a STRING (a CHAR/VARCHAR
 *   delimiter is implicitly coerced to STRING during resolution).
 * - When `string` is a CLOB, `delimiter` is also a CLOB.
 *
 * The list element type follows the SQL <string value function> convention used by [FnTrim]
 * (split may change element lengths, so CHAR is not length-preserving and widens to VARCHAR):
 * - CHAR(n)    -> list<VARCHAR>
 * - VARCHAR(n) -> list<VARCHAR>
 * - CLOB(n)    -> list<CLOB>
 * - STRING     -> list<STRING> (PartiQL extension)
 *
 * Notes:
 * - When the delimiter is empty, the input is treated as a single field and returned as a
 *   single-element list (matching the literal-delimiter convention of PostgreSQL,
 *   DuckDB, and Trino; unlike Spark, whose regex `split` splits into characters).
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
            PType.CHAR, PType.VARCHAR -> Function.instance(
                name = "split",
                returns = PType.array(PType.varchar()),
                parameters = arrayOf(Parameter("string", stringType), Parameter("delimiter", PType.string())),
            ) { args ->
                Datum.array(split(args[0].string, args[1].string).map { Datum.varchar(it) })
            }
            PType.STRING -> Function.instance(
                name = "split",
                returns = PType.array(PType.string()),
                parameters = arrayOf(Parameter("string", stringType), Parameter("delimiter", PType.string())),
            ) { args ->
                Datum.array(split(args[0].string, args[1].string).map { Datum.string(it) })
            }
            PType.CLOB -> Function.instance(
                name = "split",
                returns = PType.array(PType.clob()),
                parameters = arrayOf(Parameter("string", stringType), Parameter("delimiter", PType.clob())),
            ) { args ->
                val string = args[0].bytes.toString(Charsets.UTF_8)
                val delimiter = args[1].bytes.toString(Charsets.UTF_8)
                Datum.array(split(string, delimiter).map { Datum.clob(it.toByteArray()) })
            }
            else -> null
        }
    }

    /**
     * Splits [string] on the literal [delimiter]. An empty delimiter treats the whole input as a
     * single field.
     */
    private fun split(string: String, delimiter: String): List<String> {
        return if (delimiter.isEmpty()) {
            listOf(string)
        } else {
            string.split(delimiter)
        }
    }
}
