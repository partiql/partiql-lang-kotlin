package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.types.PType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * This represents Dynamic Dispatch.
 *
 * For the purposes of efficiency, this implementation aims to reduce any re-execution of compiled arguments. It
 * does this by avoiding the compilation of [Candidate.fn] into
 * [ExprCallStatic]'s. By doing this, this implementation can evaluate ([eval]) the input [Record], execute and gather the
 * arguments, and pass the [PartiQLValue]s directly to the [Candidate.eval]. This implementation also caches previously
 * resolved candidates.
 *
 * @param name the name of the function
 * @param candidates the ordered set of applicable functions.
 * @param args the arguments to the function
 * @property paramIndices the last index of the [args]
 * @property paramTypes represents a two dimensional array, where the first dimension maps to the [candidates], and the
 * second dimensions maps to the [org.partiql.spi.fn.FnSignature.parameters]'s [PType].
 * @property paramFamilies represents a two dimensional array, where the first dimension maps to the [candidates], and the
 * second dimensions maps to the [org.partiql.spi.fn.FnSignature.parameters]'s [CoercionFamily]. This allows the algorithm
 * to know if an argument is coercible to the target parameter.
 * @property cachedMatches a memoization cache for the [match] function.
 */
@OptIn(PartiQLValueExperimental::class)
internal class ExprCallDynamic(
    private val name: String,
    candidateFns: Array<Fn>,
    private val args: Array<Operator.Expr>
) : Operator.Expr {

    private val candidates = Array(candidateFns.size) { Candidate(candidateFns[it]) }
    private val paramIndices: IntRange = args.indices
    private val paramTypes: List<List<PType>> = this.candidates.map { candidate -> candidate.fn.signature.parameters.map { it.type } }
    private val paramFamilies: List<List<CoercionFamily>> = this.candidates.map { candidate -> candidate.fn.signature.parameters.map { family(it.type.kind) } }
    private val cachedMatches: MutableMap<List<PType>, Int> = mutableMapOf()

    override fun eval(env: Environment): Datum {
        val actualArgs = args.map { it.eval(env) }.toTypedArray()
        val actualTypes = actualArgs.map { it.type }
        val cached = cachedMatches[actualTypes]
        if (cached != null) {
            return candidates[cached].eval(actualArgs)
        }
        val candidateIndex = match(actualTypes) ?: throw TypeCheckException("Could not find function $name with types: $actualTypes.")
        cachedMatches[actualTypes] = candidateIndex
        return candidates[candidateIndex].eval(actualArgs)
    }

    /**
     * Logic is as follows: for each candidate (ordered by precedence), loop through its parameters while keeping track
     * of the number of exact matches. If the number of exact matches is greater than the current best match, update the
     * current best match. Simultaneously, ensure that all arguments are coercible to the expected parameter.
     *
     * @return the index of the candidate to invoke; null if method cannot resolve.
     */
    private fun match(args: List<PType>): Int? {
        var exactMatches: Int = -1
        var currentMatch: Int? = null
        val argFamilies = args.map { family(it.kind) }
        candidates.indices.forEach { candidateIndex ->
            var currentExactMatches = 0
            for (paramIndex in paramIndices) {
                val argType = args[paramIndex]
                val paramType = paramTypes[candidateIndex][paramIndex]
                if (paramType == argType) { currentExactMatches++ }
                val argFamily = argFamilies[paramIndex]
                val paramFamily = paramFamilies[candidateIndex][paramIndex]
                if (paramFamily != argFamily && argFamily != CoercionFamily.UNKNOWN && paramFamily != CoercionFamily.DYNAMIC) { return@forEach }
            }
            if (currentExactMatches > exactMatches) {
                currentMatch = candidateIndex
                exactMatches = currentExactMatches
            }
        }
        return currentMatch
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
                PType.Kind.INT -> CoercionFamily.NUMBER
                PType.Kind.INT_ARBITRARY -> CoercionFamily.NUMBER
                PType.Kind.BIGINT -> CoercionFamily.NUMBER
                PType.Kind.REAL -> CoercionFamily.NUMBER
                PType.Kind.DOUBLE_PRECISION -> CoercionFamily.NUMBER
                PType.Kind.DECIMAL -> CoercionFamily.NUMBER
                PType.Kind.DECIMAL_ARBITRARY -> CoercionFamily.NUMBER
                PType.Kind.STRING -> CoercionFamily.STRING
                PType.Kind.BOOL -> CoercionFamily.BOOLEAN
                PType.Kind.TIME_WITH_TZ -> CoercionFamily.TIME
                PType.Kind.TIME_WITHOUT_TZ -> CoercionFamily.TIME
                PType.Kind.TIMESTAMP_WITH_TZ -> CoercionFamily.TIMESTAMP
                PType.Kind.TIMESTAMP_WITHOUT_TZ -> CoercionFamily.TIMESTAMP
                PType.Kind.DATE -> CoercionFamily.DATE
                PType.Kind.STRUCT -> CoercionFamily.STRUCTURE
                PType.Kind.LIST -> CoercionFamily.COLLECTION
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
     * This implementation assumes that the [eval] input [Record] contains the original arguments for the desired [fn].
     * It performs the coercions (if necessary) before computing the result.
     *
     * @see ExprCallDynamic
     */
    private class Candidate(
        val fn: Fn,
    ) {

        /**
         * Memoize creation of nulls
         */
        private val nil = { Datum.nullValue(fn.signature.returns) }

        fun eval(originalArgs: Array<Datum>): Datum {
            val args = originalArgs.mapIndexed { i, arg ->
                if (arg.isNull && fn.signature.isNullCall) {
                    return nil.invoke()
                }
                val argType = arg.type
                val paramType = fn.signature.parameters[i].type
                when (paramType == argType) {
                    true -> arg.toPartiQLValue()
                    false -> {
                        val argDatum = CastTable.cast(arg, paramType)
                        argDatum.toPartiQLValue()
                    }
                }
            }.toTypedArray()
            return Datum.of(fn.invoke(args))
        }
    }
}
