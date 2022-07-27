package org.partiql.lang.eval

import com.amazon.ionelement.api.MetaContainer
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.errors.ErrorCode

fun interface ExprValueBagOp {
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
