package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.plan.Ref
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.value.DynamicType
import org.partiql.value.PartiQLType
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
    private val candidates: List<Candidate>,
    private val args: Array<Operator.Expr>
) : Operator.Expr {

    override fun eval(env: Environment): PartiQLValue {
        val actualArgs = args.map { it.eval(env) }.toTypedArray()
        candidates.forEach { candidate ->
            if (candidate.matches(actualArgs)) {
                return candidate.eval(actualArgs, env)
            }
        }
        val errorString = buildString {
            val argString = actualArgs.joinToString(", ")
            append("Could not dynamically find function for arguments $argString in $candidates.")
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
    internal class Candidate(
        val fn: Fn,
        val types: Array<PartiQLType>,
        val coercions: Array<Ref.Cast?>
    ) {

        fun eval(originalArgs: Array<PartiQLValue>, env: Environment): PartiQLValue {
            val args = originalArgs.mapIndexed { i, arg ->
                when (val c = coercions[i]) {
                    null -> arg
                    else -> ExprCast(ExprLiteral(arg), c).eval(env)
                }
            }.toTypedArray()
            return fn.invoke(args)
        }

        internal fun matches(args: Array<PartiQLValue>): Boolean {
            for (i in args.indices) {
                if (types[i] is DynamicType) {
                    return true
                }
                if (args[i].type != types[i]) {
                    return false
                }
            }
            return true
        }
    }
}
