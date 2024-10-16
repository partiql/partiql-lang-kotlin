package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.internal.operator.rex.ExprCallDynamic.Candidate
import org.partiql.eval.internal.operator.rex.ExprCallDynamic.CoercionFamily.DYNAMIC
import org.partiql.eval.internal.operator.rex.ExprCallDynamic.CoercionFamily.UNKNOWN
import org.partiql.spi.function.Function
import org.partiql.spi.value.Datum
import org.partiql.types.PType
import org.partiql.value.PartiQLValue

/**
 * Implementation of Dynamic Dispatch.
 *
 * For the purposes of efficiency, this implementation aims to reduce any re-execution of compiled arguments.
 *
 * The basic algorithm is,
 *  1. Fetch the next input record.
 *  2. Evaluate the input arguments.
 *  3. Lookup the candidate to dispatch to and invoke.
 *
 * This implementation can evaluate ([eval]) the input [Record], execute and gather the
 * arguments, and pass the [PartiQLValue]s directly to the [Candidate.eval].
 *
 * This implementation also caches previously resolved candidates.
 *
 * TODO paramTypes and paramFamilies _may_ be able to be merged.
 */
internal class ExprCallDynamic(
    private val name: String,
    private val functions: Array<Function.Instance>,
    private val args: Array<Operator.Expr>,
) : Operator.Expr {

    /**
     * @property paramIndices the indices of the [args]
     */
    private val paramIndices: IntRange = args.indices

    /**
     * @property paramTypes represents a two-dimensional array.
     *
     *  1. Dimension-1 maps to a candidate.
     *  2. Dimension-2 maps to that candidate's parameter types.
     *
     * TODO actually make this an array instead of lists.
     */
    private val paramTypes: List<List<PType>> = functions.map { c -> c.parameters.toList() }

    /**
     * @property paramFamilies is a two-dimensional array.
     *
     *   1. Dimension-1 maps to the [candidates]
     *   2. Dimension-2 maps to the [CoercionFamily].
     *
     * TODO actually make this an array instead of lists.
     */
    private val paramFamilies: List<List<CoercionFamily>> =
        functions.map { c -> c.parameters.map { p -> family(p.kind) } }

    /**
     * A memoization cache for the [match] function.
     */
    private val candidates: MutableMap<List<PType>, Candidate> = mutableMapOf()

    override fun eval(): Datum {
        val actualArgs = args.map { it.eval() }.toTypedArray()
        val actualTypes = actualArgs.map { it.type }
        var candidate = candidates[actualTypes]
        if (candidate == null) {
            candidate = match(actualTypes)
                ?: throw TypeCheckException("Could not find function $name with types: $actualTypes.")
            candidates[actualTypes] = candidate
        }
        return candidate.eval(actualArgs)
    }

    /**
     * Logic is as follows: for each candidate (ordered by precedence), loop through its parameters while keeping track
     * of the number of exact matches. If the number of exact matches is greater than the current best match, update the
     * current best match. Simultaneously, ensure that all arguments are coercible to the expected parameter.
     *
     * @return the index of the candidate to invoke; null if method cannot resolve.
     */
    private fun match(args: List<PType>): Candidate? {
        var exactMatches: Int = -1
        var currentMatch: Int? = null
        val argFamilies = args.map { family(it.kind) }
        functions.indices.forEach { candidateIndex ->
            var currentExactMatches = 0
            for (paramIndex in paramIndices) {
                val argType = args[paramIndex]
                val paramType = paramTypes[candidateIndex][paramIndex]
                if (paramType == argType) {
                    currentExactMatches++
                }
                val argFamily = argFamilies[paramIndex]
                val paramFamily = paramFamilies[candidateIndex][paramIndex]
                if (paramFamily != argFamily && argFamily != CoercionFamily.UNKNOWN && paramFamily != CoercionFamily.DYNAMIC) {
                    return@forEach
                }
            }
            if (currentExactMatches > exactMatches) {
                currentMatch = candidateIndex
                exactMatches = currentExactMatches
            }
        }
        return if (currentMatch == null) null else Candidate(functions[currentMatch!!])
    }

    /**
     * This represents SQL:1999 Section 4.1.2 "Type conversions and mixing of data types" and breaks down the different
     * coercion groups.
     *
     * TODO: [UNKNOWN] should likely be removed in the future. However, it is needed due to literal nulls and missings.
     * TODO: [DYNAMIC] should likely be removed in the future. This is currently only kept to map function signatures.
     */
    private enum class CoercionFamily {
        NUMBER,
        STRING,
        BINARY,
        BOOLEAN,
        STRUCTURE,
        DATE,
        TIME,
        TIMESTAMP,
        COLLECTION,
        UNKNOWN,
        DYNAMIC
    }

    private companion object {
        /**
         * Gets the coercion family for the given [PType.Kind].
         *
         * @see CoercionFamily
         * @see PType.Kind
         * @see family
         */
        @JvmStatic
        fun family(type: PType.Kind): CoercionFamily {
            return when (type) {
                PType.Kind.TINYINT -> CoercionFamily.NUMBER
                PType.Kind.SMALLINT -> CoercionFamily.NUMBER
                PType.Kind.INTEGER -> CoercionFamily.NUMBER
                PType.Kind.NUMERIC -> CoercionFamily.NUMBER
                PType.Kind.BIGINT -> CoercionFamily.NUMBER
                PType.Kind.REAL -> CoercionFamily.NUMBER
                PType.Kind.DOUBLE -> CoercionFamily.NUMBER
                PType.Kind.DECIMAL -> CoercionFamily.NUMBER
                PType.Kind.DECIMAL_ARBITRARY -> CoercionFamily.NUMBER
                PType.Kind.STRING -> CoercionFamily.STRING
                PType.Kind.BOOL -> CoercionFamily.BOOLEAN
                PType.Kind.TIMEZ -> CoercionFamily.TIME
                PType.Kind.TIME -> CoercionFamily.TIME
                PType.Kind.TIMESTAMPZ -> CoercionFamily.TIMESTAMP
                PType.Kind.TIMESTAMP -> CoercionFamily.TIMESTAMP
                PType.Kind.DATE -> CoercionFamily.DATE
                PType.Kind.STRUCT -> CoercionFamily.STRUCTURE
                PType.Kind.ARRAY -> CoercionFamily.COLLECTION
                PType.Kind.SEXP -> CoercionFamily.COLLECTION
                PType.Kind.BAG -> CoercionFamily.COLLECTION
                PType.Kind.ROW -> CoercionFamily.STRUCTURE
                PType.Kind.CHAR -> CoercionFamily.STRING
                PType.Kind.VARCHAR -> CoercionFamily.STRING
                PType.Kind.DYNAMIC -> CoercionFamily.DYNAMIC // TODO: REMOVE
                PType.Kind.SYMBOL -> CoercionFamily.STRING
                PType.Kind.BLOB -> CoercionFamily.BINARY
                PType.Kind.CLOB -> CoercionFamily.STRING
                PType.Kind.UNKNOWN -> CoercionFamily.UNKNOWN // TODO: REMOVE
            }
        }
    }

    /**
     * This represents a single candidate for dynamic dispatch.
     *
     * This implementation assumes that the [eval] input values contains the original arguments for the desired [function].
     * It performs the coercions (if necessary) before computing the result.
     *
     * TODO what about MISSING calls?
     *
     * @see ExprCallDynamic
     */
    private class Candidate(private var function: Function.Instance) {

        private var nil = { Datum.nullValue(function.returns) }

        /**
         * Function instance parameters (just types).
         */
        fun eval(args: Array<Datum>): Datum {
            val coerced = Array(args.size) { i ->
                val arg = args[i]
                if (function.isNullCall && arg.isNull) {
                    return nil.invoke()
                }
                val argType = arg.type
                val paramType = function.parameters[i]
                when (paramType == argType) {
                    true -> arg
                    false -> CastTable.cast(arg, paramType)
                }
            }
            return function.invoke(coerced)
        }
    }
}
