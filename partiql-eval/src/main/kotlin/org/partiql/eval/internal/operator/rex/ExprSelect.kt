package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
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
    private val env: Environment,
    private val isTopLevel: Boolean = false
) : Operator.Expr {

    @OptIn(PartiQLValueExperimental::class)
    class Elements(
        private val input: Operator.Relation,
        private val constructor: Operator.Expr,
        private val outer: Record,
        private val env: Environment,
    ) : Iterable<PartiQLValue> {

        override fun iterator(): Iterator<PartiQLValue> {
            return object : Iterator<PartiQLValue> {
                private var _init = false

                override fun hasNext(): Boolean = env.scope(outer) {
                    if (!_init) {
                        input.open()
                        _init = true
                    }
                    val hasNext = input.hasNext()
                    if (!hasNext) {
                        input.close()
                    }
                    hasNext
                }

                override fun next(): PartiQLValue {
                    return env.scope(outer) {
                        val r = input.next()
                        val result = constructor.eval(r)
                        result
                    }
                }
            }
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    class GreedyElements(
        private val input: Operator.Relation,
        private val constructor: Operator.Expr,
        private val outer: Record,
        private val env: Environment,
    ) : Iterable<PartiQLValue> {

        val elements = mutableListOf<PartiQLValue>()
        init {
            env.scope(outer) {
                input.open()
                while (input.hasNext()) {
                    val r = input.next()
                    val e = constructor.eval(r)
                    elements.add(e)
                }
                input.close()
            }
        }

        override fun iterator(): Iterator<PartiQLValue> = elements.iterator()
    }

    /**
     * @param record
     * @return
     */
    @PartiQLValueExperimental
    override fun eval(record: Record): PartiQLValue {
        val elements = when (isTopLevel) {
            true -> Elements(input, constructor, record, env)
            false -> GreedyElements(input, constructor, record, env)
        }
        return when (ordered) {
            true -> listValue(elements)
            false -> bagValue(elements)
        }
    }
}
