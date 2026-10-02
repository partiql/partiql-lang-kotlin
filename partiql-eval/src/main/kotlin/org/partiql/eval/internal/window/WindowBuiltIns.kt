package org.partiql.eval.internal.window

import org.partiql.eval.ExprValue
import org.partiql.eval.WindowFunction
import org.partiql.plan.WindowFunctionSignature

internal object WindowBuiltIns {
    fun get(signature: WindowFunctionSignature, arguments: List<ExprValue>): WindowFunction {
        return when (signature.name) {
            "row_number" -> RowNumberFunction()
            "rank" -> RankFunction()
            "dense_rank" -> DenseRankFunction()
            "percent_rank" -> PercentRankFunction()
            "cume_dist" -> CumeDistFunction()
            "ntile" -> NtileFunction(arguments[0])
            "lag" -> {
                val expr = arguments[0]
                val offset = arguments[1]
                val default = arguments[2]
                LagFunction(expr, offset, default, signature.isIgnoreNulls)
            }
            "lead" -> {
                val expr = arguments[0]
                val offset = arguments[1]
                val default = arguments[2]
                LeadFunction(expr, offset, default, signature.isIgnoreNulls)
            }
            "first_value" -> FirstValueFunction(arguments[0], signature.isIgnoreNulls, signature.returnType)
            "last_value" -> LastValueFunction(arguments[0], signature.isIgnoreNulls, signature.returnType)
            "nth_value" -> {
                val value = arguments[0]
                val nthRow = arguments[1]
                val fromLast = arguments[2]
                NthValueFunction(value, nthRow, fromLast, signature.isIgnoreNulls, signature.returnType)
            }
            else -> throw IllegalArgumentException("Unknown window function: ${signature.name}")
        }
    }
}
