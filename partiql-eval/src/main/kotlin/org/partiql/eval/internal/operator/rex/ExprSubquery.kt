package org.partiql.eval.internal.operator.rex

import org.partiql.errors.CardinalityViolation
import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * TODO THIS IMPLEMENTATION IS TEMPORARY UNTIL SUBQUERIES ARE FIXED IN THE PLANNER.
 */
internal class ExprSubquery(
    private val env: Environment,
    private val input: Operator.Relation,
    private val constructor: Operator.Expr,
) : Operator.Expr {

    private val struct = PType.struct()

    /**
     * TODO simplify
     */
    override fun eval(): Datum {
        val tuple = getFirst() ?: return Datum.nullValue()
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
     * This grabs the first row of the input, asserts it's a struct, and returns the constructed value.
     *
     * @return The single value or null when no rows are returned from the input.
     * @throws CardinalityViolation when more than one row is returned from the input.
     * @throws TypeCheckException when the constructor is not a struct.
     */
    private fun getFirst(): Datum? {
        input.open()
        // no input, return null
        if (input.hasNext().not()) {
            input.close()
            return null
        }
        // get first value
        val row = input.next()
        val value = env.scope(row) { constructor.eval() }.check(struct)
        // check if there are more rows
        if (input.hasNext()) {
            input.close()
            throw CardinalityViolation()
        }
        // done
        input.close()
        return value
    }
}
