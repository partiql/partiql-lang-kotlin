// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.internal.SqlTypeFamily
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.value.Datum

/**
 * SQL concatenation function implementation.
 *
 * Implements the SQL <concatenation> as defined in SQL2023 section 6.32 <string value expression>.
 *
 * According to SQL specification, result type is determined by precedence:
 * - If either argument is STRING: result is STRING (no length parameter)
 * - If either argument is CLOB: result is CLOB with length = min(L1 + L2, max_clob_length)
 * - If either argument is VARCHAR: result is VARCHAR with length = min(L1 + L2, max_varchar_length)
 * - If both arguments are CHAR: result is CHAR with length = min(L1 + L2, max_char_length)
 *
 * Type precedence behavior:
 * - STRING || any → STRING
 * - CLOB(n) || CHAR(m)/VARCHAR(m) → CLOB(n + m)
 * - VARCHAR(n) || CHAR(m) → VARCHAR(n + m)
 * - CHAR(n) || CHAR(m) → CHAR(n + m)
 *
 * Length overflow handling:
 * - If L1 + L2 exceeds maximum allowed length, an exception is raised at compile time
 */
internal object FnConcat : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature(FunctionUtils.hide("concat"), listOf(PType.dynamic(), PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val lhsType = args[0]
        val rhsType = args[1]
        // Check if both are string types
        if (lhsType !in SqlTypeFamily.TEXT || rhsType !in SqlTypeFamily.TEXT) return null
        // If string types are different, use precedence: CHAR > VARCHAR > STRING > CLOB
        val resultType = if (lhsType.code() != rhsType.code()) {
            maxOf(lhsType.code(), rhsType.code())
        } else {
            lhsType.code()
        }
        return createConcatFunction(lhsType, rhsType, resultType)
    }

    private fun createConcatFunction(lhsType: PType, rhsType: PType, resultType: Int): Fn? {
        return when (resultType) {
            PType.CHAR -> {
                val totalLength = FnUtils.addLengths(FnUtils.getTypeLength(lhsType), FnUtils.getTypeLength(rhsType))
                Function.instance(
                    name = FunctionUtils.hide("concat"),
                    returns = PType.character(totalLength),
                    parameters = arrayOf(Parameter("lhs", lhsType), Parameter("rhs", rhsType)),
                ) { args ->
                    val arg0 = args[0].string
                    val arg1 = args[1].string
                    Datum.character(arg0 + arg1, totalLength)
                }
            }
            PType.VARCHAR -> {
                val totalLength = FnUtils.addLengths(FnUtils.getTypeLength(lhsType), FnUtils.getTypeLength(rhsType))
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
            PType.CLOB -> {
                val totalLength = FnUtils.addLengths(FnUtils.getTypeLength(lhsType), FnUtils.getTypeLength(rhsType))
                Function.instance(
                    name = FunctionUtils.hide("concat"),
                    returns = PType.clob(totalLength),
                    parameters = arrayOf(Parameter("lhs", lhsType), Parameter("rhs", rhsType)),
                ) { args ->
                    val arg0 = args[0].string
                    val arg1 = args[1].string
                    Datum.clob((arg0 + arg1).toByteArray(), totalLength)
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
            else -> null
        }
    }
}
