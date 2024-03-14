package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
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
    candidates: List<Candidate>,
    private val args: Array<Operator.Expr>
) : Operator.Expr {

    private val candidateIndex = CandidateIndex.All(candidates)

    override fun eval(env: Environment): PartiQLValue {
        val actualArgs = args.map { it.eval(env) }.toTypedArray()
        val actualTypes = actualArgs.map { it.type }
        candidateIndex.get(actualTypes)?.let {
            return it.eval(actualArgs, env)
        }
        val errorString = buildString {
            val argString = actualArgs.joinToString(", ")
            append("Could not dynamically find function (${candidateIndex.name}) for arguments $argString.")
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
    internal data class Candidate(
        val fn: Fn,
        val coercions: Array<Ref.Cast?>
    ) {

        private val signatureParameters = fn.signature.parameters.map { it.type }.toTypedArray()

        fun eval(originalArgs: Array<PartiQLValue>, env: Environment): PartiQLValue {
            val args = originalArgs.mapIndexed { i, arg ->
                when (val c = coercions[i]) {
                    null -> arg
                    else -> ExprCast(ExprLiteral(arg), c).eval(env)
                }
            }.toTypedArray()
            return fn.invoke(args)
        }
    }

    private sealed interface CandidateIndex {

        public fun get(args: List<PartiQLValueType>): Candidate?

        /**
         * Preserves the original ordering of the passed-in candidates while making it faster to lookup matching
         * functions. Utilizes both [Direct] and [Indirect].
         *
         * @param candidates
         */
        class All(
            candidates: List<Candidate>,
        ) : CandidateIndex {

            private val lookups: List<CandidateIndex>
            internal val name: String = candidates.first().fn.signature.name

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
         * An O(1) structure to quickly find directly matching dynamic candidates.
         */
        data class Direct private constructor(val directCandidates: Map<List<PartiQLValueType>, Candidate>) : CandidateIndex {

            companion object {
                internal fun of(candidates: List<Pair<List<PartiQLValueType>, Candidate>>) = Direct(candidates.toMap())
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
