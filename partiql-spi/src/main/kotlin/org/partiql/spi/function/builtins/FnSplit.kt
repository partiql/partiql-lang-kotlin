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
        if (stringType !in SqlTypeFamily.TEXT) return null
        // The delimiter matches the CLOB family for a CLOB `string`, otherwise STRING.
        val delimiterType = if (stringType.code() == PType.CLOB) PType.clob() else PType.string()
        val elementType = stringType.stringFnReturn()
        return Function.instance(
            name = "split",
            returns = PType.array(elementType),
            parameters = arrayOf(Parameter("string", stringType), Parameter("delimiter", delimiterType)),
        ) { params ->
            val string = params[0].textValue(stringType)
            val delimiter = params[1].textValue(delimiterType)
            Datum.array(split(string, delimiter).map { stringType.stringFnDatum(it) })
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
