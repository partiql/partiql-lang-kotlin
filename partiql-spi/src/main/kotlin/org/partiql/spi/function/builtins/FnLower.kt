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

internal object FnLower : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature("lower", listOf(PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val inputType = args[0]
        return when (inputType.code()) {
            PType.CHAR -> Function.instance(
                name = "lower",
                returns = PType.character(inputType.length),
                parameters = arrayOf(Parameter("value", inputType)),
            ) { args ->
                val string = args[0].bytes.toString(Charsets.UTF_8)
                val result = string.lowercase()
                Datum.character(result, inputType.length)
            }
            PType.VARCHAR -> Function.instance(
                name = "lower",
                returns = PType.varchar(inputType.length),
                parameters = arrayOf(Parameter("value", inputType)),
            ) { args ->
                val string = args[0].bytes.toString(Charsets.UTF_8)
                val result = string.lowercase()
                Datum.varchar(result, inputType.length)
            }
            PType.STRING -> Function.instance(
                name = "lower",
                returns = PType.string(),
                parameters = arrayOf(Parameter("value", inputType)),
            ) { args ->
                val string = args[0].string
                val result = string.lowercase()
                Datum.string(result)
            }
            PType.CLOB -> Function.instance(
                name = "lower",
                returns = PType.clob(inputType.length),
                parameters = arrayOf(Parameter("value", inputType)),
            ) { args ->
                val string = args[0].bytes.toString(Charsets.UTF_8)
                val result = string.lowercase()
                Datum.clob(result.toByteArray())
            }
            else -> null
        }
    }
}

// Keep the old function names for backward compatibility in Builtins.kt
internal val Fn_LOWER__CHAR__CHAR = FnLower
internal val Fn_LOWER__VARCHAR__VARCHAR = FnLower
internal val Fn_LOWER__STRING__STRING = FnLower
internal val Fn_LOWER__CLOB__CLOB = FnLower
