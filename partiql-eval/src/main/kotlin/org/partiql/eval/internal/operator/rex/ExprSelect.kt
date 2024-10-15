package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.operator.Expression
import org.partiql.eval.operator.Relation
import org.partiql.spi.value.Datum
import org.partiql.value.PartiQLValueExperimental

/**
 * Invoke the constructor over all inputs.
 *
 * @property input
 * @property constructor
 */
internal class ExprSelect(
    private val input: Relation,
    private val constructor: Expression,
    private val ordered: Boolean,
) : Expression {

    @OptIn(PartiQLValueExperimental::class)
    class Elements(
        private val input: Relation,
        private val constructor: Expression,
        private val env: Environment,
    ) : Iterable<Datum> {

        override fun iterator(): Iterator<Datum> {
            return object : Iterator<Datum> {
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

                override fun next(): Datum {
                    val r = input.next()
                    return constructor.eval(env.push(r))
                }
            }
        }
    }

    @PartiQLValueExperimental
    override fun eval(env: Environment): Datum {
        val elements = Elements(input, constructor, env)
        return when (ordered) {
            true -> Datum.list(elements)
            false -> Datum.bag(elements)
        }
    }
}
