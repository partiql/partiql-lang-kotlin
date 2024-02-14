package org.partiql.eval.internal.operator.rex

import org.partiql.errors.CardinalityViolation
import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StructValue
import org.partiql.value.check
import org.partiql.value.listValue
import org.partiql.value.nullValue

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
    abstract val env: Environment

    internal class Row(
        override val constructor: Operator.Expr,
        override val input: Operator.Relation,
        override val env: Environment
    ) : ExprSubquery() {

        @PartiQLValueExperimental
        override fun eval(record: Record): PartiQLValue {
            val tuple = getFirst(record) ?: return nullValue()
            val values = tuple.values.iterator()
            return listValue(values.asSequence().toList())
        }
    }

    internal class Scalar(
        override val constructor: Operator.Expr,
        override val input: Operator.Relation,
        override val env: Environment
    ) : ExprSubquery() {

        @PartiQLValueExperimental
        override fun eval(record: Record): PartiQLValue {
            val tuple = getFirst(record) ?: return nullValue()
            val values = tuple.values.iterator()
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
     * @throws TypeCheckException when the constructor is not a [StructValue].
     */
    @OptIn(PartiQLValueExperimental::class)
    fun getFirst(record: Record): StructValue<*>? {
        return env.scope(record) {
            input.open()
            val firstRecord = input.next() ?: return@scope null
            val tuple = constructor.eval(firstRecord).check<StructValue<*>>()
            val secondRecord = input.next()
            if (secondRecord != null) {
                throw CardinalityViolation()
            }
            tuple
        }
    }
}
