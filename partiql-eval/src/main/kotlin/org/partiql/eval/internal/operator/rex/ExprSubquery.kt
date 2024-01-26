package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.CollectionValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StructValue
import org.partiql.value.check
import org.partiql.value.listValue

internal abstract class ExprSubquery : Operator.Expr {

    abstract val subquery: Operator.Expr

    internal class Row(
        override val subquery: Operator.Expr
    ) : ExprSubquery() {
        @PartiQLValueExperimental
        override fun eval(record: Record): PartiQLValue {
            val values = getFirstAndOnlyTupleValues(record)
            return listValue(values.asSequence().toList())
        }
    }

    internal class Scalar(
        override val subquery: Operator.Expr
    ) : ExprSubquery() {
        @PartiQLValueExperimental
        override fun eval(record: Record): PartiQLValue {
            val values = getFirstAndOnlyTupleValues(record)
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
     * Procedure is as follows:
     * 1. Asserts that the [subquery] returns a collection containing a single value. Throws a [TypeCheckException] if not.
     * 2. Gets the first value from [subquery].
     * 3. Asserts that the first value is a TUPLE ([StructValue]). Throws a [TypeCheckException] if not.
     * 4. Returns an [Iterator] of the values contained within the [StructValue].
     */
    @OptIn(PartiQLValueExperimental::class)
    fun getFirstAndOnlyTupleValues(record: Record): Iterator<PartiQLValue> {
        val result = subquery.eval(record)
        if (result !is CollectionValue<*>) {
            throw TypeCheckException()
        }
        val resultIterator = result.iterator()
        if (resultIterator.hasNext().not()) {
            throw TypeCheckException()
        }
        val tuple = resultIterator.next().check<StructValue<*>>()
        if (resultIterator.hasNext()) {
            throw TypeCheckException()
        }
        return tuple.values.iterator()
    }
}
