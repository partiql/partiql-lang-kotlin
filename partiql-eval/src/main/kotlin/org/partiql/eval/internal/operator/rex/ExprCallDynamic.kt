package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.Row
import org.partiql.eval.internal.operator.rex.ExprCallDynamic.Candidate
import org.partiql.eval.internal.operator.rex.ExprCallDynamic.CoercionFamily.DYNAMIC
import org.partiql.eval.internal.operator.rex.ExprCallDynamic.CoercionFamily.UNKNOWN
import org.partiql.spi.function.Function
import org.partiql.spi.value.Datum
import org.partiql.types.PType

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
 * This implementation can evaluate ([eval]) the input [Row], execute and gather the
 * arguments, and pass the values directly to the [Candidate.eval].
 *
 * This implementation also caches previously resolved candidates.
 *
 * TODO paramTypes and paramFamilies _may_ be able to be merged.
 */
internal class ExprCallDynamic(
    private val name: String,
    private val functions: Array<Function>,
    private val args: Array<ExprValue>
) : ExprValue {

    /**
     * @property paramIndices the indices of the [args]
     */
    private val paramIndices: IntRange = args.indices

    /**
     * A memoization cache for the [match] function.
     */
    private val candidates: MutableMap<List<PType>, Candidate> = mutableMapOf()

    override fun eval(env: Environment): Datum {
        val actualArgs = args.map { it.eval(env) }.toTypedArray()
        val actualTypes = actualArgs.map { it.type }
        var candidate = candidates[actualTypes]
        if (candidate == null) {
            candidate = match(actualTypes) ?: throw TypeCheckException("Could not find function $name with types: $actualTypes.")
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
        val argFamilies = args.map { family(it.code()) }
        functions.indices.forEach { candidateIndex ->
            var currentExactMatches = 0
            val params = functions[candidateIndex].getInstance(args.toTypedArray())?.parameters ?: return@forEach
            for (paramIndex in paramIndices) {
                val argType = args[paramIndex]
                val paramType = params[paramIndex]
                if (paramType.code() == argType.code()) { currentExactMatches++ } // TODO: Convert all functions to use the new modelling, or else we need to only check kinds
                val argFamily = argFamilies[paramIndex]
                val paramFamily = family(paramType.code())
                if (paramFamily != argFamily && argFamily != UNKNOWN && paramFamily != DYNAMIC) { return@forEach }
            }
            if (currentExactMatches > exactMatches) {
                currentMatch = candidateIndex
                exactMatches = currentExactMatches
            }
        }
        return if (currentMatch == null) null else {
            val instance = functions[currentMatch!!].getInstance(args.toTypedArray()) ?: return null
            Candidate(instance)
        }
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
        fun family(type: Int): CoercionFamily {
            return when (type) {
                PType.TINYINT -> CoercionFamily.NUMBER
                PType.SMALLINT -> CoercionFamily.NUMBER
                PType.INTEGER -> CoercionFamily.NUMBER
                PType.NUMERIC -> CoercionFamily.NUMBER
                PType.BIGINT -> CoercionFamily.NUMBER
                PType.REAL -> CoercionFamily.NUMBER
                PType.DOUBLE -> CoercionFamily.NUMBER
                PType.DECIMAL -> CoercionFamily.NUMBER
                PType.STRING -> CoercionFamily.STRING
                PType.BOOL -> CoercionFamily.BOOLEAN
                PType.TIMEZ -> CoercionFamily.TIME
                PType.TIME -> CoercionFamily.TIME
                PType.TIMESTAMPZ -> CoercionFamily.TIMESTAMP
                PType.TIMESTAMP -> CoercionFamily.TIMESTAMP
                PType.DATE -> CoercionFamily.DATE
                PType.STRUCT -> CoercionFamily.STRUCTURE
                PType.ARRAY -> CoercionFamily.COLLECTION
                PType.BAG -> CoercionFamily.COLLECTION
                PType.ROW -> CoercionFamily.STRUCTURE
                PType.CHAR -> CoercionFamily.STRING
                PType.VARCHAR -> CoercionFamily.STRING
                PType.DYNAMIC -> DYNAMIC // TODO: REMOVE
                PType.BLOB -> CoercionFamily.BINARY
                PType.CLOB -> CoercionFamily.STRING
                PType.UNKNOWN -> UNKNOWN // TODO: REMOVE
                PType.VARIANT -> UNKNOWN // TODO: HANDLE VARIANT
                else -> error("Unknown type: $type")
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
