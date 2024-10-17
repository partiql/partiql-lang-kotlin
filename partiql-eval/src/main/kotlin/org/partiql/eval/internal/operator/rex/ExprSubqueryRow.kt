package org.partiql.eval.internal.operator.rex

import org.partiql.errors.CardinalityViolation
import org.partiql.eval.Environment
import org.partiql.eval.internal.helpers.IteratorSupplier
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.eval.operator.Expression
import org.partiql.eval.operator.Relation
import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * TODO REMOVE ME AFTER FIXING SUBQUERIES.
 */
internal class ExprSubqueryRow(input: Relation, constructor: Expression) :
    Expression {

    // DO NOT USE FINAL
    private var _input = input
    private var _constructor = constructor

    private companion object {

        @JvmStatic
        private val STRUCT = PType.struct()
    }

    override fun eval(env: Environment): Datum {
        val tuple = getFirst(env) ?: return Datum.nullValue()
        val values = IteratorSupplier { tuple.fields }.map { it.value }
        return Datum.list(values)
    }

    /**
     * @See [ExprSubquery.getFirst]
     */
    private fun getFirst(env: Environment): Datum? {
        _input.open(env)
        if (_input.hasNext().not()) {
            _input.close()
            return null
        }
        val firstRecord = _input.next()
        val tuple = _constructor.eval(env.push(firstRecord)).check(STRUCT)
        if (_input.hasNext()) {
            _input.close()
            throw CardinalityViolation()
        }
        _input.close()
        return tuple
    }
}
