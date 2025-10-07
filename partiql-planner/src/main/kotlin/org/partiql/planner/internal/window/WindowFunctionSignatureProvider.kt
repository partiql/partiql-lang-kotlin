package org.partiql.planner.internal.window

import org.partiql.plan.WindowFunctionSignature
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.typer.DynamicTyper
import org.partiql.spi.types.PType

/**
 * Provides the [WindowFunctionSignature] for a given window function name and arguments.
 */
internal object WindowFunctionSignatureProvider {

    /**
     * Returns the [WindowFunctionSignature] for the given window function name and arguments.
     * For now, we only support RANK (0 args), DENSE_RANK (0 args), ROW_NUMBER (0 args), LAG (3 args), and LEAD (3 args).
     * @param name the name of the window function.
     * @param args the arguments to the window function.
     * @return null if unable to create a signature.
     */
    fun get(name: String, args: List<Rex>, ignoreNulls: Boolean): WindowFunctionSignature? {
        return when (args.size) {
            0 -> when (name.lowercase()) {
                "rank" -> RANK
                "dense_rank" -> DENSE_RANK
                "row_number" -> ROW_NUMBER
                else -> null
            }
            3 -> when (val n = name.lowercase()) {
                "lag", "lead" -> lagOrLead(n, args[0], args[1], args[2], ignoreNulls)
                else -> null
            }
            else -> throw IllegalArgumentException("Unknown window function: $name")
        }
    }

    private val RANK = WindowFunctionSignature("rank", emptyList(), PType.bigint(), false)
    private val DENSE_RANK = WindowFunctionSignature("dense_rank", emptyList(), PType.bigint(), false)
    private val ROW_NUMBER = WindowFunctionSignature("row_number", emptyList(), PType.bigint(), false)

    /**
     * Creates the LAG window function signature.
     * TODO: We may eventually want/need to handle coercion of the expr/default. This would require this class
     *  to return a nullable [org.partiql.plan.WindowFunctionNode], not a [WindowFunctionSignature].
     * @return null if unable to create a signature.
     */
    private fun lagOrLead(name: String, expr: Rex, offset: Rex, default: Rex, ignoreNulls: Boolean): WindowFunctionSignature? {
        if (offset.type.code() != PType.BIGINT) {
            return null
        }
        val returnTyper = DynamicTyper()
        returnTyper.accumulate(expr)
        returnTyper.accumulate(default)
        val (returnType, _) = returnTyper.mapping()
        return WindowFunctionSignature(name, listOf(expr.type, offset.type, default.type), returnType, ignoreNulls)
    }
}
