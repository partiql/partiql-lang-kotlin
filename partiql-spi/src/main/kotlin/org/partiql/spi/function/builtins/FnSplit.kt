package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.function.builtins.FnUtils.isTextOrUnknown
import org.partiql.spi.function.builtins.FnUtils.stringFnResult
import org.partiql.spi.function.builtins.FnUtils.stringFnReturnType
import org.partiql.spi.function.builtins.FnUtils.textValue
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

/**
 * PartiQL `split` function.
 *
 * ```
 * split(string, delimiter)
 * ```
 *
 * Splits `string` around each literal occurrence of `delimiter` and returns the
 * resulting parts as an array. The delimiter is matched literally, not as a regular
 * expression (aligning with DuckDB's `string_split`, Trino's `split`, and
 * Redshift's `SPLIT_TO_ARRAY`, rather than Spark's regex `split`).
 *
 * Both arguments must belong to the text family (CHAR, VARCHAR, STRING, CLOB); each parameter keeps
 * its own argument type, so no coercion between text types is required. If either argument is a
 * literal NULL/MISSING (UNKNOWN), the call resolves and the framework propagates NULL/MISSING.
 *
 * The result is an ARRAY whose element type is derived from `string` via [FnUtils.stringFnReturnType].
 * CHAR widens to VARCHAR (a part may be shorter than the input, and a fixed-length element would have
 * to pad), but the declared length is preserved because splitting only removes delimiters, so no part
 * is ever longer than the input:
 * - CHAR(n)    -> array<VARCHAR(n)>
 * - VARCHAR(n) -> array<VARCHAR(n)>
 * - CLOB(n)    -> array<CLOB(n)>
 * - STRING     -> array<STRING> (PartiQL extension)
 *
 * Notes:
 * - When the delimiter is empty, the input is treated as a single field and returned as a
 *   single-element array (matching the literal-delimiter convention of PostgreSQL,
 *   DuckDB, and Trino; unlike Spark, whose regex `split` splits into characters).
 * - If the delimiter is not found, the result is a single-element array holding
 *   the original string.
 */
internal object FnSplit : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature("split", listOf(PType.dynamic(), PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        // Every argument must be a text type (or UNKNOWN, handled below); anything else does not match.
        if (args.any { !it.isTextOrUnknown() }) return null
        val stringType = args[0]
        val delimiterType = args[1]
        if (args.any { it.code() == PType.UNKNOWN }) {
            return FnUtils.nullResolutionInstance("split", PType.array(stringType.stringFnReturnType()), args)
        }
        // Splitting only removes delimiters, so no part is longer than the input and the element max
        // length is the input length: CHAR(n)/VARCHAR(n) -> VARCHAR(n), CLOB(n) -> CLOB(n),
        // STRING -> STRING. STRING carries no length, so only the bounded types pass one along.
        val length = if (stringType.code() == PType.STRING) null else stringType.length
        return Function.instance(
            name = "split",
            returns = PType.array(stringType.stringFnReturnType(length)),
            parameters = arrayOf(Parameter("string", stringType), Parameter("delimiter", delimiterType)),
        ) { params ->
            val string = params[0].textValue(stringType)
            val delimiter = params[1].textValue(delimiterType)
            Datum.array(split(string, delimiter).map { stringType.stringFnResult(it, length) })
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
