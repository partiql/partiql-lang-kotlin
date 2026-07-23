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
import org.partiql.spi.utils.StringUtils.codepointTrimTrailing
import org.partiql.spi.value.Datum

/**
 * `trim_trailing(value)` — removes trailing whitespace.
 *
 * Accepts CHAR, VARCHAR, CLOB, and STRING. Follows the [FnTrim] type convention:
 * - CHAR(n)/VARCHAR(n) -> VARCHAR
 * - CLOB(n)            -> CLOB
 * - STRING             -> STRING
 */
internal object FnTrimTrailing : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature(FunctionUtils.hide("trim_trailing"), listOf(PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val valueType = args[0]
        // If any argument is UNKNOWN (literal NULL), return a resolution-only instance; the framework's isNullCall handles propagation.
        if (args.any { it.code() == PType.UNKNOWN }) {
            return FnUtils.nullResolutionInstance(FunctionUtils.hide("trim_trailing"), PType.string(), args)
        }
        return when (valueType.code()) {
            PType.CHAR, PType.VARCHAR -> Function.instance(
                name = FunctionUtils.hide("trim_trailing"),
                returns = PType.varchar(),
                parameters = arrayOf(Parameter("value", valueType)),
            ) { args ->
                val result = args[0].string.codepointTrimTrailing()
                Datum.varchar(result)
            }
            PType.STRING -> Function.instance(
                name = FunctionUtils.hide("trim_trailing"),
                returns = PType.string(),
                parameters = arrayOf(Parameter("value", valueType)),
            ) { args ->
                val result = args[0].string.codepointTrimTrailing()
                Datum.string(result)
            }
            PType.CLOB -> Function.instance(
                name = FunctionUtils.hide("trim_trailing"),
                returns = PType.clob(),
                parameters = arrayOf(Parameter("value", valueType)),
            ) { args ->
                val result = args[0].bytes.toString(Charsets.UTF_8).codepointTrimTrailing()
                Datum.clob(result.toByteArray())
            }
            else -> null
        }
    }
}
