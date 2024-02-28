package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.bagValue
import org.partiql.value.listValue

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
    ) : Iterable<PartiQLValue> {

        override fun iterator(): Iterator<PartiQLValue> {
            return object : Iterator<PartiQLValue> {
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

                override fun next(): PartiQLValue {
                    val r = input.next()
                    val result = constructor.eval(env.push(r))
                    return result
                }
            }
        }
    }

    /**
     * @param record
     * @return
     */
    @PartiQLValueExperimental
    override fun eval(env: Environment): PartiQLValue {
        val elements = Elements(input, constructor, env)
        return when (ordered) {
            true -> listValue(elements)
            false -> bagValue(elements)
        }
    }
}
