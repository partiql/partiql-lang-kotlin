package org.partiql.eval.internal.operator.rex

import org.partiql.errors.CardinalityViolation
import org.partiql.errors.TypeCheckException
import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * Implementation of scalar subquery coercion.
 *
 * TODO REMOVE CONSTRUCTOR – TEMPORARY UNTIL SUBQUERIES ARE FIXED IN THE PLANNER.
 */
internal class ExprSubquery(input: ExprRelation, constructor: ExprValue) :
    ExprValue {

    // DO NOT USE FINAL
    private var _input = input
    private var _constructor = constructor

    private companion object {
        @JvmStatic
        private val STRUCT = PType.struct()
    }

    /**
     * TODO simplify
     */
    override fun eval(env: Environment): Datum {
        val tuple = getFirst(env) ?: return Datum.nullValue()
        val values = tuple.fields.asSequence().map { it.value }.iterator()
        if (values.hasNext().not()) {
            throw TypeCheckException()
        }
        val singleValue = values.next()
        if (values.hasNext()) {
            throw TypeCheckException()
        }
        return singleValue
    }

    /**
     * This grabs the first row of the input, asserts that the constructor evaluates to a TUPLE, and returns the
     * constructed value.
     *
     * @return the constructed constructor. Returns null when no rows are returned from the input.
     * @throws CardinalityViolation when more than one row is returned from the input.
     * @throws TypeCheckException when the constructor is not a struct.
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
