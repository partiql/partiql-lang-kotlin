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
 * SQL TRIM function implementation.
 *
 * Implements the SQL <trim function> as defined in SQL2023 section 6.33 <string value function>.
 *
 * According to SQL specification:
 * - For CHAR/VARCHAR: result type is variable-length character string (VARCHAR) with maximum length equal to the input length
 * - For CLOB: result type is character large object type (CLOB) with maximum length equal to the input length
 *
 * PartiQL extensions:
 * - STRING type (PartiQL-specific unlimited length string) preserves its type
 *
 * Type preservation behavior:
 * - CHAR(n) → VARCHAR(n)
 * - VARCHAR(n) → VARCHAR(n)
 * - CLOB(n) → CLOB(n)
 * - STRING → STRING (PartiQL extension)
 */
internal object FnTrim : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature(FunctionUtils.hide("trim"), listOf(PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val inputType = args[0]
        return when (inputType.code()) {
            PType.CHAR, PType.VARCHAR -> {
                Function.instance(
                    name = FunctionUtils.hide("trim"),
                    returns = PType.varchar(inputType.length),
                    parameters = arrayOf(Parameter("value", inputType)),
                ) { params ->
                    val string = params[0].string
                    val result = string.codepointTrim()
                    Datum.varchar(result, inputType.length)
                }
            }
            PType.CLOB -> {
                Function.instance(
                    name = FunctionUtils.hide("trim"),
                    returns = PType.clob(inputType.length),
                    parameters = arrayOf(Parameter("value", inputType)),
                ) { params ->
                    val string = params[0].bytes.toString(Charsets.UTF_8)
                    val result = string.codepointTrim()
                    Datum.clob(result.toByteArray(), inputType.length)
                }
            }
            PType.STRING -> Function.instance(
                name = FunctionUtils.hide("trim"),
                returns = PType.string(),
                parameters = arrayOf(Parameter("value", inputType)),
            ) { params ->
                val value = params[0].string
                val result = value.codepointTrim()
                Datum.string(result)
            }
            else -> error("Unsupported type for TRIM function: ${inputType.code()}")
        }
    }
}
