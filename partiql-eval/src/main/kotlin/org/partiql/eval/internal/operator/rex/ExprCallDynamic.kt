package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.plan.Ref
import org.partiql.planner.FunctionResolver
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

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
    candidates: Array<Candidate>,
    private val args: Array<Operator.Expr>
) : Operator.Expr {

    private val candidateImpls = candidates.map { it }
    private val candidates = candidates.map { it.fn.signature }

    override fun eval(env: Environment): Datum {
        val actualArgs = args.map { it.eval(env) }.toTypedArray()
        val transformedArgs = Array(actualArgs.size) {
            actualArgs[it].toPartiQLValue()
        }
        val actualTypes = actualArgs.map { it.type }
        val match = FunctionResolver.resolve(candidates, actualTypes) as FunctionResolver.FnMatch.Static
        return candidateImpls[match.index].eval(transformedArgs, env)
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
        private val nil = { Datum.nullValue(fn.signature.returns) }

        fun eval(originalArgs: Array<PartiQLValue>, env: Environment): Datum {
            val args = originalArgs.mapIndexed { i, arg ->
                if (arg.isNull && fn.signature.isNullCall) {
                    return nil.invoke()
                }
                when (val c = coercions[i]) {
                    null -> arg
                    else -> ExprCast(ExprLiteral(Datum.of(arg)), c).eval(env).toPartiQLValue()
                }
            }.toTypedArray()
            return Datum.of(fn.invoke(args))
        }
    }
}
