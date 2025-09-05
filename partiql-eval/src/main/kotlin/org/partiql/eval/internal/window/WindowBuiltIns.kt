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
            "percent_rank" -> TODO()
            "cume_dist" -> TODO()
            "ntile" -> TODO()
            "lag" -> {
                val expr = arguments[0]
                val offset = arguments[1]
                val default = arguments[2]
                LagFunction(expr, offset, default)
            }
            "lead" -> {
                val expr = arguments[0]
                val offset = arguments[1]
                val default = arguments[2]
                LeadFunction(expr, offset, default)
            }
            "first_value" -> TODO()
            "last_value" -> TODO()
            "nth_value" -> TODO()
            else -> throw IllegalArgumentException("Unknown window function: ${signature.name}")
        }
    }
}
