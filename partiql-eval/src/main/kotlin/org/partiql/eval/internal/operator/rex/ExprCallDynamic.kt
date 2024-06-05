package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.toNull
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.plan.Ref
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

/**
 * This represents Dynamic Dispatch.
 *
 * For the purposes of efficiency, this implementation aims to reduce any re-execution of compiled arguments. It
 * does this by avoiding the compilation of [Candidate.fn] and [Candidate.coercions] into
 * [ExprCallStatic]'s. By doing this, this implementation can evaluate ([eval]) the input [Record], execute and gather the
 * arguments, and pass the [PartiQLValue]s directly to the [Candidate.eval].
 */
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal class ExprCallDynamic(
    private val name: String,
    private val candidates: Array<Candidate>,
    private val args: Array<Operator.Expr>
) : Operator.Expr {

    private val candidateIndex = CandidateIndex.All(candidates)

    override fun eval(env: Environment): Datum {
        val actualArgs = args.map { it.eval(env) }.toTypedArray()
        val actualTypes = actualArgs.map { it.type }
        candidateIndex.get(actualTypes)?.let {
            val transformedArgs = Array(actualArgs.size) {
                actualArgs[it].toPartiQLValue()
            }
            return it.eval(transformedArgs, env)
        }
        val errorString = buildString {
            val argString = actualArgs.joinToString(", ")
            append("Could not dynamically find function ($name) for arguments $argString.")
        }
        throw TypeCheckException(errorString)
    }

    /**
     * This represents a single candidate for dynamic dispatch.
     *
     * This implementation assumes that the [eval] input [Record] contains the original arguments for the desired [fn].
     * It performs the coercions (if necessary) before computing the result.
     *
     * @see ExprCallDynamic
     */
    data class Candidate(
        val fn: Fn,
        val coercions: Array<Ref.Cast?>
    ) {

        /**
         * Memoize creation of nulls
         */
        private val nil = fn.signature.returns.toNull()

        fun eval(originalArgs: Array<PartiQLValue>, env: Environment): Datum {
            val args = originalArgs.mapIndexed { i, arg ->
                if (arg.isNull && fn.signature.isNullCall) {
                    return Datum.of(nil())
                }
                when (val c = coercions[i]) {
                    null -> arg
                    else -> ExprCast(ExprLiteral(Datum.of(arg)), c).eval(env).toPartiQLValue()
                }
            }.toTypedArray()
            return Datum.of(fn.invoke(args))
        }
    }

    private sealed interface CandidateIndex {

        public fun get(args: List<PartiQLValueType>): Candidate?

        /**
         * Preserves the original ordering of the passed-in candidates while making it faster to lookup matching
         * functions. Utilizes both [Direct] and [Indirect].
         *
         * Say a user passes in the following ordered candidates:
         * [
         *      foo(int16, int16) -> int16,
         *      foo(int32, int32) -> int32,
         *      foo(int64, int64) -> int64,
         *      foo(string, string) -> string,
         *      foo(struct, struct) -> struct,
         *      foo(numeric, numeric) -> numeric,
         *      foo(int64, dynamic) -> dynamic,
         *      foo(struct, dynamic) -> dynamic,
         *      foo(bool, bool) -> bool
         * ]
         *
         * With the above candidates, the [CandidateIndex.All] will maintain the original ordering by utilizing:
         * - [CandidateIndex.Direct] to match hashable runtime types
         * - [CandidateIndex.Indirect] to match the dynamic type
         *
         * For the above example, the internal representation of [CandidateIndex.All] is a list of
         * [CandidateIndex.Direct] and [CandidateIndex.Indirect] that looks like:
         * ALL listOf(
         *      DIRECT hashMap(
         *          [int16, int16] --> foo(int16, int16) -> int16,
         *          [int32, int32] --> foo(int32, int32) -> int32,
         *          [int64, int64] --> foo(int64, int64) -> int64
         *          [string, string] --> foo(string, string) -> string,
         *          [struct, struct] --> foo(struct, struct) -> struct,
         *          [numeric, numeric] --> foo(numeric, numeric) -> numeric
         *      ),
         *      INDIRECT listOf(
         *          foo(int64, dynamic) -> dynamic,
         *          foo(struct, dynamic) -> dynamic
         *      ),
         *      DIRECT hashMap(
         *          [bool, bool] --> foo(bool, bool) -> bool
         *      )
         * )
         *
         * @param candidates
         */
        class All(private val candidates: Array<Candidate>) : CandidateIndex {

            private val lookups: List<CandidateIndex>

            init {
                val lookupsMutable = mutableListOf<CandidateIndex>()
                val accumulator = mutableListOf<Pair<List<PartiQLValueType>, Candidate>>()

                // Indicates that we are currently processing dynamic candidates that accept ANY.
                var activelyProcessingAny = true

                candidates.forEach { candidate ->
                    // Gather the input types to the dynamic invocation
                    val lookupTypes = candidate.coercions.mapIndexed { index, cast ->
                        when (cast) {
                            null -> candidate.fn.signature.parameters[index].type
                            else -> cast.input
                        }
                    }
                    val parametersIncludeAny = lookupTypes.any { it == PartiQLValueType.ANY }
                    // A way to simplify logic further below. If it's empty, add something and set the processing type.
                    if (accumulator.isEmpty()) {
                        activelyProcessingAny = parametersIncludeAny
                        accumulator.add(lookupTypes to candidate)
                        return@forEach
                    }
                    when (parametersIncludeAny) {
                        true -> when (activelyProcessingAny) {
                            true -> accumulator.add(lookupTypes to candidate)
                            false -> {
                                activelyProcessingAny = true
                                lookupsMutable.add(Direct.of(accumulator.toList()))
                                accumulator.clear()
                                accumulator.add(lookupTypes to candidate)
                            }
                        }
                        false -> when (activelyProcessingAny) {
                            false -> accumulator.add(lookupTypes to candidate)
                            true -> {
                                activelyProcessingAny = false
                                lookupsMutable.add(Indirect(accumulator.toList()))
                                accumulator.clear()
                                accumulator.add(lookupTypes to candidate)
                            }
                        }
                    }
                }
                // Add any remaining candidates (that we didn't submit due to not ending while switching)
                when (accumulator.isEmpty()) {
                    true -> { /* Do nothing! */ }
                    false -> when (activelyProcessingAny) {
                        true -> lookupsMutable.add(Indirect(accumulator.toList()))
                        false -> lookupsMutable.add(Direct.of(accumulator.toList()))
                    }
                }
                this.lookups = lookupsMutable
            }

            override fun get(args: List<PartiQLValueType>): Candidate? {
                return this.lookups.firstNotNullOfOrNull { it.get(args) }
            }
        }

        /**
         * An O(1) structure to quickly find directly matching dynamic candidates. This is specifically used for runtime
         * types that can be matched directly. AKA int32, int64, etc. This does NOT include [PartiQLValueType.ANY].
         */
        data class Direct private constructor(val directCandidates: HashMap<List<PartiQLValueType>, Candidate>) : CandidateIndex {

            companion object {
                internal fun of(candidates: List<Pair<List<PartiQLValueType>, Candidate>>): Direct {
                    val candidateMap = java.util.HashMap<List<PartiQLValueType>, Candidate>()
                    candidateMap.putAll(candidates)
                    return Direct(candidateMap)
                }
            }

            override fun get(args: List<PartiQLValueType>): Candidate? {
                return directCandidates[args]
            }
        }

        /**
         * Holds all candidates that expect a [PartiQLValueType.ANY] on input. This maintains the original
         * precedence order.
         */
        data class Indirect(private val candidates: List<Pair<List<PartiQLValueType>, Candidate>>) : CandidateIndex {
            override fun get(args: List<PartiQLValueType>): Candidate? {
                candidates.forEach { (types, candidate) ->
                    for (i in args.indices) {
                        if (args[i] != types[i] && types[i] != PartiQLValueType.ANY) {
                            return@forEach
                        }
                    }
                    return candidate
                }
                return null
            }
        }
    }
}
