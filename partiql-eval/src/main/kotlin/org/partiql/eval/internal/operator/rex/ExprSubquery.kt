package org.partiql.eval.internal.operator.rex

import org.partiql.errors.CardinalityViolation
import org.partiql.errors.TypeCheckException
import org.partiql.eval.PQLValue
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.IteratorSupplier
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

/**
 * The PartiQL Specification talks about subqueries and how they are coerced. Specifically, subqueries are
 * modeled as a COLL_TO_SCALAR(SELECT VALUE <tuple> ...) where the SELECT VALUE must return a single row containing
 * a TUPLE.
 *
 * @see [getFirst]
 */
internal abstract class ExprSubquery : Operator.Expr {

    abstract val constructor: Operator.Expr
    abstract val input: Operator.Relation

    internal class Row(
        override val constructor: Operator.Expr,
        override val input: Operator.Relation,
    ) : ExprSubquery() {

        @PartiQLValueExperimental
        override fun eval(env: Environment): PQLValue {
            val tuple = getFirst(env) ?: return PQLValue.nullValue()
            val values = IteratorSupplier { tuple.fields }.map { it.value }
            return PQLValue.listValue(values)
        }
    }

    internal class Scalar(
        override val constructor: Operator.Expr,
        override val input: Operator.Relation,
    ) : ExprSubquery() {

        @PartiQLValueExperimental
        override fun eval(env: Environment): PQLValue {
            val tuple = getFirst(env) ?: return PQLValue.nullValue()
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
    }

    /**
     * This grabs the first row of the [input], asserts that the [constructor] evaluates to a TUPLE, and returns the
     * constructed value.
     *
     * @return the constructed [constructor]. Returns null when no rows are returned from the [input].
     * @throws CardinalityViolation when more than one row is returned from the [input].
     * @throws TypeCheckException when the constructor is not a [PartiQLValueType.STRUCT].
     */
    @OptIn(PartiQLValueExperimental::class)
    fun getFirst(env: Environment): PQLValue? {
        input.open(env)
        if (input.hasNext().not()) {
            input.close()
            return null
        }
        val firstRecord = input.next()
        val tuple = constructor.eval(env.push(firstRecord)).check(PartiQLValueType.STRUCT)
        if (input.hasNext()) {
            input.close()
            throw CardinalityViolation()
        }
        input.close()
        return tuple
    }
}
