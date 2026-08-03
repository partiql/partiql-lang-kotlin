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

/**
 * PartiQL `replace` function.
 *
 * ```
 * replace(string, from, to)
 * ```
 *
 * Replaces all (non-overlapping) literal occurrences of `from` in `string` with `to`. The match is a
 * plain literal, not a regular expression. An empty `from` leaves `string` unchanged.
 *
 * This mirrors the 3-argument form found in Redshift (`REPLACE`), DuckDB
 * (`replace`), Trino (`replace/3`), and Spark (`replace/3`).
 *
 * All three arguments must belong to the text family (CHAR, VARCHAR, STRING, CLOB); each parameter
 * keeps its own argument type, so no coercion between text types is required. If any argument is a
 * literal NULL/MISSING (UNKNOWN), the call resolves and the framework propagates NULL/MISSING.
 *
 * The result type is derived from `string` via [FnUtils.stringFnReturnType], with no input length
 * carried over. Unlike TRIM/SUBSTRING/SPLIT, replace can make the string *longer* — a `to` longer than
 * `from` grows it by `occurrences * (to.length - from.length)`, a factor that depends on the runtime
 * values of `string`, `from`, and `to`, none of which are known at plan time. CHAR also widens to
 * VARCHAR, since a fixed-length result would have to pad:
 * - CHAR(n)    -> VARCHAR(255)
 * - VARCHAR(n) -> VARCHAR(255)
 * - CLOB(n)    -> CLOB
 * - STRING     -> STRING (PartiQL extension)
 *
 * TODO: the VARCHAR(255) result length is unsound and silently truncates. Because no upper bound is
 *   computable from the argument types (see above), [FnUtils.stringFnReturnType] falls back to its
 *   arbitrary 255 default, and [FnUtils.stringFnResult] boxes the value with `Datum.varchar(value, 255)`,
 *   which truncates to that length. So a correct result longer than 255 characters is silently cut:
 *   `replace(CAST(<300 a's> AS VARCHAR(300)), 'a', 'bb')` should be 600 characters but yields 255.
 *   CLOB is unaffected (it defaults to the maximum length) and STRING is unbounded; only CHAR/VARCHAR
 *   inputs are wrong. Fixing this means giving CHAR/VARCHAR an unbounded result here rather than
 *   inheriting the 255 default — note that `stringFnReturnType`/`stringFnResult` are shared with the
 *   length-preserving functions, so the default cannot simply be changed in place without checking
 *   every caller.
 */
internal object FnReplace : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature("replace", listOf(PType.dynamic(), PType.dynamic(), PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        // Every argument must be a text type (or UNKNOWN, handled below); anything else does not match.
        if (args.any { !it.isTextOrUnknown() }) return null
        val stringType = args[0]
        if (args.any { it.code() == PType.UNKNOWN }) {
            return FnUtils.nullResolutionInstance("replace", stringType.stringFnReturnType(), args)
        }

        return Function.instance(
            name = "replace",
            returns = stringType.stringFnReturnType(),
            parameters = arrayOf(Parameter("string", stringType), Parameter("from", args[1]), Parameter("to", args[2])),
        ) { params ->
            val string = params[0].textValue(stringType)
            val from = params[1].textValue(args[1])
            val to = params[2].textValue(args[2])
            stringType.stringFnResult(replace(string, from, to))
        }
    }

    /**
     * Literal (non-regex) replace-all. If [from] is empty, [string] is returned unchanged
     * (matches Trino/DuckDB/Redshift behavior).
     */
    private fun replace(string: String, from: String, to: String): String {
        return if (from.isEmpty()) string else string.replace(from, to)
    }
}
