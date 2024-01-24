package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.missingValue

/**
 * This represents Dynamic Dispatch.
 *
 * For the purposes of efficiency, this implementation aims to reduce any re-execution of compiled arguments. It
 * does this by avoiding the compilation of [Candidate.fn] and [Candidate.coercions] into
 * [ExprCallStatic]'s. By doing this, this implementation can evaluate ([eval]) the input [Record], execute and gather the
 * arguments, and pass the [PartiQLValue]s directly to the [Candidate.eval].
 */
internal class ExprCallDynamic(
    private val candidates: List<Candidate>,
    private val args: Array<Operator.Expr>
) : Operator.Expr {

    @OptIn(PartiQLValueExperimental::class)
    override fun eval(record: Record): PartiQLValue {
        val actualArgs = args.map { it.eval(record) }.toTypedArray()
        candidates.forEach { candidate ->
            if (candidate.matches(actualArgs)) {
                candidate.eval(actualArgs)
            }
        }
        return missingValue()
    }

    /**
     * This represents a single candidate for dynamic dispatch.
     *
     * This implementation assumes that the [eval] input [Record] contains the original arguments for the desired [fn].
     * It performs the coercions (if necessary) before computing the result.
     *
     * @see ExprCallDynamic
     */
    internal class Candidate @OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class) constructor(
        val types: Array<PartiQLValueType>,
        val fn: PartiQLFunction.Scalar,
        val coercions: List<PartiQLFunction.Scalar?>
    ) {

        @OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
        fun eval(originalArgs: Array<PartiQLValue>): PartiQLValue {
            val args = coercions.mapIndexed { index, coercion ->
                coercion?.invoke(arrayOf(originalArgs[index])) ?: originalArgs[index]
            }.toTypedArray()
            return fn.invoke(args)
        }

        @OptIn(PartiQLValueExperimental::class)
        internal fun matches(args: Array<PartiQLValue>): Boolean {
            for (i in args.indices) {
                if (args[i].type != types[i]) {
                    return false
                }
            }
            return true
        }
    }
}
