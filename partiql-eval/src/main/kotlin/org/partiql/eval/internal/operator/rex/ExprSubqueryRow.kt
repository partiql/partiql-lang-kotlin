package org.partiql.eval.internal.operator.rex

import org.partiql.errors.CardinalityViolation
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.IteratorSupplier
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * TODO REMOVE ME AFTER FIXING SUBQUERIES.
 */
internal class ExprSubqueryRow(
    private val env: Environment,
    private val input: Operator.Relation,
    private val constructor: Operator.Expr,
) : Operator.Expr {

    private val struct = PType.struct()

    override fun eval(): Datum {
        val tuple = getFirst(env) ?: return Datum.nullValue()
        val values = IteratorSupplier { tuple.fields }.map { it.value }
        return Datum.list(values)
    }

    /**
     * @See [ExprSubquery.getFirst]
     */
    private fun getFirst(env: Environment): Datum? {
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
