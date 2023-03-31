package org.partiql.lang.eval

import com.amazon.ionelement.api.MetaContainer
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.ErrorCode
import org.partiql.pig.runtime.DomainNode

/**
 * Evaluable representation of PartiQL bag operators.
 *
 * ```
 * - [OUTER] UNION     [ALL|DISTINCT]
 * - [OUTER] INTERSECT [ALL|DISTINCT]
 * - [OUTER] EXCEPT    [ALL|DISTINCT]
 * ```
 *
 * @see [RFC-0007](https://github.com/partiql/partiql-docs/blob/main/RFCs/0007-rfc-bag-operators.md).
 */
fun interface ExprValueBagOp {
    fun eval(lhs: ExprValue, rhs: ExprValue): Sequence<ExprValue> {
        val l = lhs.coerceToBag()
        val r = rhs.coerceToBag()
        return eval(l, r)
    }

    fun eval(lhs: Sequence<ExprValue>, rhs: Sequence<ExprValue>): Sequence<ExprValue>

    companion object {

        fun create(node: DomainNode, metas: MetaContainer): ExprValueBagOp = when (node) {
            is PartiqlAst.BagOpType.Union,
            is PartiqlPhysical.BagOpType.Union,
            is PartiqlAst.BagOpType.Intersect,
            is PartiqlPhysical.BagOpType.Intersect,
            is PartiqlAst.BagOpType.Except,
            is PartiqlPhysical.BagOpType.Except -> {
                throw EvaluationException(
                    message = "${node.javaClass.simpleName} operator is not support yet",
                    errorCode = ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
                    errorContextFrom(metas),
                    internal = false
                )
            }
            is PartiqlAst.BagOpType.OuterUnion,
            is PartiqlPhysical.BagOpType.OuterUnion -> outerUnion
            is PartiqlAst.BagOpType.OuterIntersect,
            is PartiqlPhysical.BagOpType.OuterIntersect -> outerIntersect
            is PartiqlAst.BagOpType.OuterExcept,
            is PartiqlPhysical.BagOpType.OuterExcept -> outerExcept
            else -> {
                throw EvaluationException(
                    message = "Invalid bag operator ${node.javaClass.simpleName}",
                    errorCode = ErrorCode.INTERNAL_ERROR,
                    errorContextFrom(metas),
                    internal = false
                )
            }
        }
    }
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
    type === ExprValueType.STRUCT || type.isScalar -> sequenceOf(this)
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
