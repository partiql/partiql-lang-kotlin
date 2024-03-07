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

        internal fun matches(inputs: Array<PartiQLValue>): Boolean {
            for (i in inputs.indices) {
                val inputType = inputs[i].type
                val parameterType = signatureParameters[i]
                val c = coercions[i]
                when (c) {
                    // coercion might be null if one of the following is true
                    // Function parameter is ANY,
                    // Input type is null
                    // input type is the same as function parameter
                    null -> {
                        if (!(inputType == parameterType || inputType == PartiQLValueType.NULL || parameterType == PartiQLValueType.ANY)) {
                            return false
                        }
                    }
                    else -> {
                        // checking the input type is expected by the coercion
                        if (inputType != c.input) return false
                        // checking the result is expected by the function signature
                        // this should branch should never be reached, but leave it here for clarity
                        if (c.target != parameterType) error("Internal Error: Cast Target does not match Function Parameter")
                    }
                }
            }
            return true
        }
    }
}
