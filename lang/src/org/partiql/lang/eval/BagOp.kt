package org.partiql.lang.eval

import com.amazon.ion.IonContainer
import com.amazon.ion.IonStruct
import com.amazon.ionelement.api.MetaContainer
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.errors.ErrorCode

fun interface ExprValueBagOp {
    fun eval(lhs: ExprValue, rhs: ExprValue): Sequence<ExprValue> {
        val l = lhs.coerceToBag()
        val r = rhs.coerceToBag()
        return eval(l, r)
    }

    fun eval(lhs: Sequence<ExprValue>, rhs: Sequence<ExprValue>): Sequence<ExprValue>
}

fun PartiqlAst.BagOpType.expr(metas: MetaContainer): ExprValueBagOp = when (this) {
    is PartiqlAst.BagOpType.Union,
    is PartiqlAst.BagOpType.Intersect,
    is PartiqlAst.BagOpType.Except -> {
        throw EvaluationException(
            message = "${this.javaClass.simpleName} operator is not support yet",
            errorCode = ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
            errorContextFrom(metas),
            internal = false
        )
    }
    is PartiqlAst.BagOpType.OuterUnion -> outerUnion
    is PartiqlAst.BagOpType.OuterIntersect -> outerIntersect
    is PartiqlAst.BagOpType.OuterExcept -> outerExcept
}

/**
 * Coercion function F for bag operators described in RFC-0007
 *  - F(absent_value) -> << >>
 *  - F(scalar_value) -> << scalar_value >> # singleton bag
 *  - F(tuple_value)  -> << tuple_value >>  # singleton bag, see future extensions
 *  - F(array_value)  -> bag_value          # discard ordering
 *  - F(bag_value)    -> bag_value          # identity
 */
internal fun ExprValue.coerceToBag(): Sequence<ExprValue> = when {
    isUnknown() -> emptySequence()
    this is StructExprValue || this is Scalar -> sequenceOf(this)
    this is IonExprValue -> when (ionValue) {
        is IonStruct -> sequenceOf(this)
        is IonContainer -> this.asSequence()
        else -> sequenceOf(this)
    }
    else -> this.asSequence()
}

private val outerUnion = ExprValueBagOp { lhs, rhs ->
    sequence {
        val multiplicities = lhs.multiplicities()
        yieldAll(lhs)
        rhs.forEach {
            val m = multiplicities.getOrDefault(it, 0)
            if (m > 0) {
                multiplicities[it] = m - 1
            } else {
                yield(it)
            }
        }
    }
}

private val outerIntersect = ExprValueBagOp { lhs, rhs ->
    sequence {
        val multiplicities = lhs.multiplicities()
        rhs.forEach {
            val m = multiplicities.getOrDefault(it, 0)
            if (m > 0) {
                yield(it)
                multiplicities[it] = m - 1
            }
        }
    }
}

private val outerExcept = ExprValueBagOp { lhs, rhs ->
    sequence {
        val multiplicities = rhs.multiplicities()
        lhs.forEach {
            val m = multiplicities.getOrDefault(it, 0)
            if (m > 0) {
                multiplicities[it] = m - 1
            } else {
                yield(it)
            }
        }
    }
}
