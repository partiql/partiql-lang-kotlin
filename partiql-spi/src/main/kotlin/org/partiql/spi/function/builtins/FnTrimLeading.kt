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
import org.partiql.spi.utils.StringUtils.codepointTrimLeading
import org.partiql.spi.value.Datum

/**
 * `trim_leading(value)` — removes leading whitespace.
 *
 * Accepts CHAR, VARCHAR, CLOB, and STRING. Follows the [FnTrim] type convention:
 * - CHAR(n)/VARCHAR(n) -> VARCHAR
 * - CLOB(n)            -> CLOB
 * - STRING             -> STRING
 */
internal object FnTrimLeading : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature(FunctionUtils.hide("trim_leading"), listOf(PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val valueType = args[0]
        // If any argument is UNKNOWN (literal NULL), return a resolution-only instance; the framework's isNullCall handles propagation.
        if (args.any { it.code() == PType.UNKNOWN }) {
            return FnUtils.nullResolutionInstance(FunctionUtils.hide("trim_leading"), PType.string(), args)
        }
        return when (valueType.code()) {
            PType.CHAR, PType.VARCHAR -> Function.instance(
                name = FunctionUtils.hide("trim_leading"),
                returns = PType.varchar(),
                parameters = arrayOf(Parameter("value", valueType)),
            ) { args ->
                val result = args[0].string.codepointTrimLeading()
                Datum.varchar(result)
            }
            // PType.UNKNOWN is for null propagation
            PType.STRING, PType.UNKNOWN -> Function.instance(
                name = FunctionUtils.hide("trim_leading"),
                returns = PType.string(),
                parameters = arrayOf(Parameter("value", valueType)),
            ) { args ->
                val result = args[0].string.codepointTrimLeading()
                Datum.string(result)
            }
            PType.CLOB -> Function.instance(
                name = FunctionUtils.hide("trim_leading"),
                returns = PType.clob(),
                parameters = arrayOf(Parameter("value", valueType)),
            ) { args ->
                val result = args[0].bytes.toString(Charsets.UTF_8).codepointTrimLeading()
                Datum.clob(result.toByteArray())
            }
            else -> null
        }
    }
}
