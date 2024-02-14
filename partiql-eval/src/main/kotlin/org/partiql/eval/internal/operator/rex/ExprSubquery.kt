package org.partiql.eval.internal.operator.rex

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
import java.util.Stack

/**
 * The PartiQL Specification talks about subqueries and how they are coerced. Specifically, subqueries are
 * modeled as a COLL_TO_SCALAR(SELECT VALUE <tuple> ...) where the SELECT VALUE must return a single row containing
 * a TUPLE.
 *
 * @see [getValues]
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
            val values = getValues(record) ?: return nullValue()
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
            val values = getValues(record) ?: return nullValue()
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
     * This grabs the first row of the [input], asserts that the [constructor] evaluates to a TUPLE, and returns an
     * [Iterator] of the [constructor]'s values. These values are then used by [Scalar] and [Row].
     *
     * @return an [Iterator] of the [constructor]'s values of the first row from the [input]. Returns null when
     * no rows are returned from the [input].
     * @throws TypeCheckException when more than one row is returned from the [input].
     */
    @OptIn(PartiQLValueExperimental::class)
    fun getValues(record: Record): Iterator<PartiQLValue>? {
        env.push(record)
        input.open()
        val firstRecord = input.next()
        if (firstRecord == null) {
            env.pop()
            return null
        }
        val tuple = constructor.eval(firstRecord).check<StructValue<*>>()
        val secondRecord = input.next()
        env.pop()
        if (secondRecord != null) {
            throw TypeCheckException()
        }
        return tuple.values.iterator()
    }
}
