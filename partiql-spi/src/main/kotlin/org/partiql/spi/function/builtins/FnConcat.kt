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
import org.partiql.spi.value.Datum

internal object FnConcat : FnOverload() {
    const val MAXLENGTH = Int.MAX_VALUE

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature(FunctionUtils.hide("concat"), listOf(PType.dynamic(), PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val lhsType = args[0]
        val rhsType = args[1]
        // Check if both are string types
        val stringTypes = setOf(PType.CHAR, PType.VARCHAR, PType.STRING, PType.CLOB)
        if (lhsType.code() !in stringTypes || rhsType.code() !in stringTypes) {
            return null
        }
        // If string types are different, use precedence: CHAR > VARCHAR > STRING > CLOB
        if (lhsType.code() != rhsType.code()) {
            val resultType = maxOf(lhsType.code(), rhsType.code())
            return when (resultType) {
                PType.VARCHAR -> {
                    val totalLength = (lhsType.length.toLong() + rhsType.length.toLong()).coerceAtMost(MAXLENGTH.toLong()).toInt()
                    Function.instance(
                        name = FunctionUtils.hide("concat"),
                        returns = PType.varchar(totalLength),
                        parameters = arrayOf(Parameter("lhs", lhsType), Parameter("rhs", rhsType)),
                    ) { args ->
                        val arg0 = args[0].string
                        val arg1 = args[1].string
                        Datum.varchar(arg0 + arg1, totalLength)
                    }
                }
                PType.STRING -> Function.instance(
                    name = FunctionUtils.hide("concat"),
                    returns = PType.string(),
                    parameters = arrayOf(Parameter("lhs", lhsType), Parameter("rhs", rhsType)),
                ) { args ->
                    val arg0 = args[0].string
                    val arg1 = args[1].string
                    Datum.string(arg0 + arg1)
                }
                PType.CLOB -> {
                    val totalLength = when {
                        lhsType.code() == PType.STRING || rhsType.code() == PType.STRING -> MAXLENGTH
                        else -> (lhsType.length.toLong() + rhsType.length.toLong()).coerceAtMost(MAXLENGTH.toLong()).toInt()
                    }
                    Function.instance(
                        name = FunctionUtils.hide("concat"),
                        returns = PType.clob(totalLength),
                        parameters = arrayOf(Parameter("lhs", lhsType), Parameter("rhs", rhsType)),
                    ) { args ->
                        val arg0 = args[0].string
                        val arg1 = args[1].string
                        Datum.clob((arg0 + arg1).toByteArray())
                    }
                }
                else -> null
            }
        }
        // With same string types
        return when (lhsType.code()) {
            PType.CHAR -> {
                val totalLength = lhsType.length.toLong() + rhsType.length.toLong()
                val maxLength = totalLength.coerceAtMost(MAXLENGTH.toLong()).toInt()
                Function.instance(
                    name = FunctionUtils.hide("concat"),
                    returns = PType.character(maxLength),
                    parameters = arrayOf(Parameter("lhs", lhsType), Parameter("rhs", rhsType)),
                ) { args ->
                    val arg0 = args[0].string
                    val arg1 = args[1].string
                    val actualLength = arg0.length.toLong() + arg1.length.toLong()
                    if (actualLength > MAXLENGTH) {
                        throw IllegalArgumentException("Total length of concatenated strings exceeds maximum allowed length")
                    }
                    Datum.character(arg0 + arg1, actualLength.toInt())
                }
            }
            PType.VARCHAR -> {
                val totalLength = lhsType.length.toLong() + rhsType.length.toLong()
                val maxLength = totalLength.coerceAtMost(MAXLENGTH.toLong()).toInt()
                Function.instance(
                    name = FunctionUtils.hide("concat"),
                    returns = PType.varchar(maxLength),
                    parameters = arrayOf(Parameter("lhs", lhsType), Parameter("rhs", rhsType)),
                ) { args ->
                    val arg0 = args[0].string
                    val arg1 = args[1].string
                    val actualLength = arg0.length.toLong() + arg1.length.toLong()
                    if (actualLength > MAXLENGTH) {
                        throw IllegalArgumentException("Total length of concatenated strings exceeds maximum allowed length")
                    }
                    Datum.varchar(arg0 + arg1, actualLength.toInt())
                }
            }
            PType.STRING -> Function.instance(
                name = FunctionUtils.hide("concat"),
                returns = PType.string(),
                parameters = arrayOf(Parameter("lhs", lhsType), Parameter("rhs", rhsType)),
            ) { args ->
                val arg0 = args[0].string
                val arg1 = args[1].string
                Datum.string(arg0 + arg1)
            }
            PType.CLOB -> {
                val totalLength = lhsType.length.toLong() + rhsType.length.toLong()
                val maxLength = totalLength.coerceAtMost(MAXLENGTH.toLong()).toInt()
                Function.instance(
                    name = FunctionUtils.hide("concat"),
                    returns = PType.clob(maxLength),
                    parameters = arrayOf(Parameter("lhs", lhsType), Parameter("rhs", rhsType)),
                ) { args ->
                    val arg0 = args[0].bytes
                    val arg1 = args[1].bytes
                    Datum.clob(arg0 + arg1)
                }
            }
            else -> null
        }
    }
}

internal val Fn_CONCAT__CHAR_CHAR__CHAR = FnConcat
internal val Fn_CONCAT__VARCHAR_VARCHAR__VARCHAR = FnConcat
internal val Fn_CONCAT__STRING_STRING__STRING = FnConcat
internal val Fn_CONCAT__CLOB_CLOB__CLOB = FnConcat
