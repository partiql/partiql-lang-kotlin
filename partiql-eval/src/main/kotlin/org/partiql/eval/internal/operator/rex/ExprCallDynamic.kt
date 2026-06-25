package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.CoercionFamily
import org.partiql.eval.internal.helpers.DatumUtils.lowerSafe
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

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
    private val functions: Array<FnOverload>,
    private val args: Array<ExprValue>
) : ExprValue {

    /**
     * @property paramIndices the indices of the [args]
     */
    private val paramIndices: IntRange = args.indices

    /**
     * A memoization cache for the [match] function.
     */
    private val candidates: MutableMap<ParameterTypes, Candidate> = mutableMapOf()

    /**
     * Used as the keys of the hash map: [ExprCallDynamic.candidates].
     */
    private class ParameterTypes(val types: Array<PType>) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            other as ParameterTypes // We can immediately cast, since this is a private class only used for the cache.
            return types.contentEquals(other.types)
        }

        override fun hashCode(): Int {
            return types.contentHashCode()
        }
    }

    override fun eval(env: Environment): Datum {
        val actualArgs = Array(args.size) { args[it].eval(env).lowerSafe() }
        val actualTypes = Array(actualArgs.size) { actualArgs[it].type }
        val paramTypes = ParameterTypes(actualTypes)
        var candidate = candidates[paramTypes]
        if (candidate == null) {
            candidate = match(actualTypes) ?: throw PErrors.functionTypeMismatchException(name, actualTypes, functions.toList())
            candidates[paramTypes] = candidate
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
    private fun match(args: Array<PType>): Candidate? {
        var exactMatches: Int = -1
        var currentMatch: Int? = null
        val argFamilies = args.map { CoercionFamily.family(it.code()) }
        functions.indices.forEach { candidateIndex ->
            var currentExactMatches = 0
            val params = functions[candidateIndex].getInstance(args)?.signature?.parameters ?: return@forEach
            for (paramIndex in paramIndices) {
                val argType = args[paramIndex]
                val paramType = params[paramIndex]
                if (paramType.type.code() == argType.code()) { currentExactMatches++ } // TODO: Convert all functions to use the new modelling, or else we need to only check kinds
                val argFamily = argFamilies[paramIndex]
                val paramFamily = CoercionFamily.family(paramType.type.code())
                if (paramFamily != argFamily && argFamily != CoercionFamily.UNKNOWN && paramFamily != CoercionFamily.DYNAMIC) { return@forEach }
            }
            if (currentExactMatches > exactMatches) {
                currentMatch = candidateIndex
                exactMatches = currentExactMatches
            }
        }
        return if (currentMatch == null) null else {
            val instance = functions[currentMatch!!].getInstance(args) ?: return null
            Candidate(instance)
        }
    }

    /**
     * This represents a single candidate for dynamic dispatch.
     *
     * This implementation assumes that the [eval] input values contains the original arguments for the desired [function].
     * It performs the coercions (if necessary) before computing the result.
     *
     * @see ExprCallDynamic
     */
    private class Candidate(private var function: Fn) {

        private var nil = { Datum.nullValue(function.signature.returns) }
        private var missing = { Datum.missing(function.signature.returns) }

        /**
         * Function instance parameters (just types).
         */
        fun eval(args: Array<Datum>): Datum {
            val coerced = Array(args.size) { i ->
                val arg = args[i]
                if (function.signature.isNullCall && arg.isNull) {
                    return nil.invoke()
                }
                if (function.signature.isMissingCall && arg.isMissing) {
                    return missing.invoke()
                }
                val argType = arg.type
                val paramType = function.signature.parameters[i]
                // Skip cast for MAP params — function signatures default to MAP<STRING, DYNAMIC>,
                // but the actual map may have non-string keys. The function body handles key casting.
                when (paramType.type.code() == PType.MAP || paramType.type == argType) {
                    true -> arg
                    false -> CastTable.cast(arg, paramType.type)
                }
            }
            return function.invoke(coerced)
        }
    }
}
