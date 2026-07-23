// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.utils.StringUtils.codepointTrim
import org.partiql.spi.value.Datum

/**
 * `trim_chars(value, chars)` — removes leading and trailing characters contained in [chars].
 *
 * Accepts CHAR, VARCHAR, CLOB, and STRING for `value`; `chars` accepts CHAR/VARCHAR/STRING
 * (implicitly coerced to STRING). Follows the [FnTrim] type convention:
 * - CHAR(n)/VARCHAR(n) -> VARCHAR
 * - CLOB(n)            -> CLOB
 * - STRING             -> STRING
 */
internal object FnTrimChars : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature(FunctionUtils.hide("trim_chars"), listOf(PType.dynamic(), PType.string()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val valueType = args[0]
        // If any argument is UNKNOWN (literal NULL), return a resolution-only instance; the framework's isNullCall handles propagation.
        if (args.any { it.code() == PType.UNKNOWN }) {
            return FnUtils.nullResolutionInstance(FunctionUtils.hide("trim_chars"), PType.string(), args)
        }
        return when (valueType.code()) {
            PType.CHAR, PType.VARCHAR -> Function.instance(
                name = FunctionUtils.hide("trim_chars"),
                returns = PType.varchar(),
                parameters = arrayOf(Parameter("value", valueType), Parameter("chars", PType.string())),
            ) { args ->
                val result = args[0].string.codepointTrim(args[1].string)
                Datum.varchar(result)
            }
            PType.STRING -> Function.instance(
                name = FunctionUtils.hide("trim_chars"),
                returns = PType.string(),
                parameters = arrayOf(Parameter("value", valueType), Parameter("chars", PType.string())),
            ) { args ->
                val result = args[0].string.codepointTrim(args[1].string)
                Datum.string(result)
            }
            PType.CLOB -> Function.instance(
                name = FunctionUtils.hide("trim_chars"),
                returns = PType.clob(),
                parameters = arrayOf(Parameter("value", valueType), Parameter("chars", PType.string())),
            ) { args ->
                val result = args[0].bytes.toString(Charsets.UTF_8).codepointTrim(args[1].string)
                Datum.clob(result.toByteArray())
            }
            else -> null
        }
    }
}
