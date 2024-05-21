package org.partiql.eval.internal.operator.rex

import org.partiql.eval.PQLValue
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValueExperimental

/**
 * Invoke the constructor over all inputs.
 *
 * @property input
 * @property constructor
 */
internal class ExprSelect(
    private val input: Operator.Relation,
    private val constructor: Operator.Expr,
    private val ordered: Boolean,
) : Operator.Expr {

    @OptIn(PartiQLValueExperimental::class)
    class Elements(
        private val input: Operator.Relation,
        private val constructor: Operator.Expr,
        private val env: Environment,
    ) : Iterable<PQLValue> {

        override fun iterator(): Iterator<PQLValue> {
            return object : Iterator<PQLValue> {
                private var _init = false

                override fun hasNext(): Boolean {
                    if (!_init) {
                        input.open(env)
                        _init = true
                    }
                    val hasNext = input.hasNext()
                    if (!hasNext) {
                        input.close()
                    }
                    return hasNext
                }

                override fun next(): PQLValue {
                    val r = input.next()
                    return constructor.eval(env.push(r))
                }
            }
        }
    }

    @PartiQLValueExperimental
    override fun eval(env: Environment): PQLValue {
        val elements = Elements(input, constructor, env)
        return when (ordered) {
            true -> PQLValue.listValue(elements)
            false -> PQLValue.bagValue(elements)
        }
    }
}
