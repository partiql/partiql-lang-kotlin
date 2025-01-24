package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.IteratorSupplier
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.eval.internal.helpers.ValueUtility.checkStruct
import org.partiql.spi.value.Datum

/**
 * TODO REMOVE ME AFTER FIXING SUBQUERIES.
 */
internal class ExprSubqueryRow(input: ExprRelation, constructor: ExprValue) :
    ExprValue {

    // DO NOT USE FINAL
    private var _input = input
    private var _constructor = constructor

    override fun eval(env: Environment): Datum {
        val tuple = getFirst(env) ?: return Datum.nullValue()
        val values = IteratorSupplier { tuple.fields }.map { it.value }
        return Datum.array(values)
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
        val tuple = _constructor.eval(env.push(firstRecord)).checkStruct()
        if (_input.hasNext()) {
            _input.close()
            throw PErrors.cardinalityViolationException()
        }
        _input.close()
        return tuple
    }
}
