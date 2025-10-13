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
 * SQL LOWER function implementation.
 *
 * Implements the SQL <fold> function as defined in SQL2023 section 6.33 <string value function>.
 *
 * According to SQL specification:
 * - The declared type of the result is the declared type of the <character value expression>
 * - For CHAR, VARCHAR, and CLOB types, the length parameter is preserved from the input type
 * - For STRING type, no length parameter is applicable
 *
 * Type preservation behavior:
 * - CHAR(n) → CHAR(n)
 * - VARCHAR(n) → VARCHAR(n)
 * - CLOB(n) → CLOB(n)
 * - STRING → STRING
 */
internal object FnLower : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature("lower", listOf(PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val inputType = args[0]
        return when (inputType.code()) {
            PType.CHAR -> {
                Function.instance(
                    name = "lower",
                    returns = PType.character(inputType.length),
                    parameters = arrayOf(Parameter("value", inputType)),
                ) { params ->
                    val string = params[0].string
                    val result = string.lowercase()
                    Datum.character(result, inputType.length)
                }
            }
            PType.VARCHAR -> {
                Function.instance(
                    name = "lower",
                    returns = PType.varchar(inputType.length),
                    parameters = arrayOf(Parameter("value", inputType)),
                ) { params ->
                    val string = params[0].string
                    val result = string.lowercase()
                    Datum.varchar(result, inputType.length)
                }
            }
            PType.CLOB -> {
                Function.instance(
                    name = "lower",
                    returns = PType.clob(inputType.length),
                    parameters = arrayOf(Parameter("value", inputType)),
                ) { params ->
                    val string = params[0].bytes.toString(Charsets.UTF_8)
                    val result = string.lowercase()
                    Datum.clob(result.toByteArray(), inputType.length)
                }
            }
            PType.STRING -> Function.instance(
                name = "lower",
                returns = PType.string(),
                parameters = arrayOf(Parameter("value", inputType)),
            ) { params ->
                val string = params[0].string
                val result = string.lowercase()
                Datum.string(result)
            }
            else -> error("Unsupported type for LOWER function: ${inputType.code()}")
        }
    }
}
