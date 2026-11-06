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
     * For now, we support:
     *  - RANK (0 args), DENSE_RANK (0 args), ROW_NUMBER (0 args), PERCENT_RANK (0 args), CUME_DIST (0 args)
     *  - NTILE (1 arg: number of tiles)
     *  - LAG (3 args), LEAD (3 args)
     *  - FIRST_VALUE (1 arg), LAST_VALUE (1 arg)
     *  - NTH_VALUE (3 args: value, nth row, and a boolean literal that is true iff FROM LAST is specified)
     * @param name the name of the window function.
     * @param args the arguments to the window function.
     * @return null if unable to create a signature.
     */
    fun get(name: String, args: List<Rex>, ignoreNulls: Boolean): WindowFunctionSignature? {
        return when (val n = name.lowercase()) {
            "rank" -> if (args.isEmpty()) RANK else null
            "dense_rank" -> if (args.isEmpty()) DENSE_RANK else null
            "row_number" -> if (args.isEmpty()) ROW_NUMBER else null
            "percent_rank" -> if (args.isEmpty()) PERCENT_RANK else null
            "cume_dist" -> if (args.isEmpty()) CUME_DIST else null
            "ntile" -> if (args.size == 1) ntile(args[0]) else null
            "lag", "lead" -> if (args.size == 3) lagOrLead(n, args[0], args[1], args[2], ignoreNulls) else null
            "first_value", "last_value" -> if (args.size == 1) firstOrLastValue(n, args[0], ignoreNulls) else null
            "nth_value" -> if (args.size == 3) nthValue(args[0], args[1], args[2], ignoreNulls) else null
            else -> null
        }
    }

    private val RANK = WindowFunctionSignature("rank", emptyList(), PType.bigint(), false)
    private val DENSE_RANK = WindowFunctionSignature("dense_rank", emptyList(), PType.bigint(), false)
    private val ROW_NUMBER = WindowFunctionSignature("row_number", emptyList(), PType.bigint(), false)
    // SQL:2023 Subclause 9.23, Syntax Rules 5g and 5h leave the precision of the approximate numeric result of
    // PERCENT_RANK and CUME_DIST implementation-defined (IV082, IV083); PartiQL uses DOUBLE PRECISION for both.
    private val PERCENT_RANK = WindowFunctionSignature("percent_rank", emptyList(), PType.doublePrecision(), false)
    private val CUME_DIST = WindowFunctionSignature("cume_dist", emptyList(), PType.doublePrecision(), false)

    /**
     * Creates the LAG window function signature. We use the [DynamicTyper] since the output of lag/lead is the "common supertype" (as
     * described by the SQL specification and is extended to handle PartiQL's dynamic typing) of the expr/default arguments.
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

    /**
     * NTILE: the declared type of <number of tiles> shall be exact numeric with scale 0; the declared type of the
     * result is an implementation-defined exact numeric type with scale 0 (BIGINT). See SQL:2011 6.10 SR 7.i.
     * @return null if unable to create a signature.
     */
    private fun ntile(tiles: Rex): WindowFunctionSignature? {
        if (!isExactNumericScaleZero(tiles.type)) {
            return null
        }
        return WindowFunctionSignature("ntile", listOf(tiles.type), PType.bigint(), false)
    }

    /**
     * FIRST_VALUE/LAST_VALUE: the declared type of the result is the declared type of the <value expression>.
     * See SQL:2011 6.10 SR 8.c.
     */
    private fun firstOrLastValue(name: String, value: Rex, ignoreNulls: Boolean): WindowFunctionSignature {
        return WindowFunctionSignature(name, listOf(value.type), value.type, ignoreNulls)
    }

    /**
     * NTH_VALUE: the declared type of <nth row> shall be exact numeric with scale 0; the declared type of the result is
     * the declared type of the <value expression>. See SQL:2011 6.10 SR 8.a and 8.c.
     * @param fromLast a boolean literal that is true iff FROM LAST is specified.
     * @return null if unable to create a signature.
     */
    private fun nthValue(value: Rex, nthRow: Rex, fromLast: Rex, ignoreNulls: Boolean): WindowFunctionSignature? {
        if (!isExactNumericScaleZero(nthRow.type) || fromLast.type.code() != PType.BOOL) {
            return null
        }
        return WindowFunctionSignature("nth_value", listOf(value.type, nthRow.type, fromLast.type), value.type, ignoreNulls)
    }

    /**
     * @return true if the [type] is an exact numeric type with scale 0, OR if the type is only known at runtime, OR if
     * the type is that of the null value. Runtime checks are left to the evaluator.
     */
    private fun isExactNumericScaleZero(type: PType): Boolean {
        return when (type.code()) {
            PType.TINYINT, PType.SMALLINT, PType.INTEGER, PType.BIGINT -> true
            PType.NUMERIC, PType.DECIMAL -> type.scale == 0
            PType.DYNAMIC, PType.VARIANT, PType.UNKNOWN -> true
            else -> false
        }
    }
}
