package org.partiql.lang.eval.physical

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.err
import org.partiql.lang.eval.errorContextFrom
import org.partiql.lang.eval.numberValue
import org.partiql.lang.eval.physical.operators.ValueExpression

// The functions in this file look very similar and so the temptation to DRY is quite strong....
// However, there are enough subtle differences between them that avoiding the duplication isn't worth it.

internal fun evalLimitRowCount(rowCountExpr: ValueExpression, env: EvaluatorState): Long {
    val limitExprValue = rowCountExpr(env)

    if (limitExprValue.type != ExprValueType.INT) {
        err(
            "LIMIT value was not an integer",
            ErrorCode.EVALUATOR_NON_INT_LIMIT_VALUE,
            errorContextFrom(rowCountExpr.sourceLocation).also {
                it[Property.ACTUAL_TYPE] = limitExprValue.type.toString()
            },
            internal = false
        )
    }

    val limitValue = limitExprValue.numberValue().toLong()

    if (limitValue < 0) {
        err(
            "negative LIMIT",
            ErrorCode.EVALUATOR_NEGATIVE_LIMIT,
            errorContextFrom(rowCountExpr.sourceLocation),
            internal = false
        )
    }

    // we can't use the Kotlin's Sequence<T>.take(n) for this since it accepts only an integer.
    // this references [Sequence<T>.take(count: Long): Sequence<T>] defined in [org.partiql.util].
    return limitValue
}

internal fun evalOffsetRowCount(rowCountExpr: ValueExpression, state: EvaluatorState): Long {
    val offsetExprValue = rowCountExpr(state)

    if (offsetExprValue.type != ExprValueType.INT) {
        err(
            "OFFSET value was not an integer",
            ErrorCode.EVALUATOR_NON_INT_OFFSET_VALUE,
            errorContextFrom(rowCountExpr.sourceLocation).also {
                it[Property.ACTUAL_TYPE] = offsetExprValue.type.toString()
            },
            internal = false
        )
    }

    val offsetValue = offsetExprValue.numberValue().toLong()

    if (offsetValue < 0) {
        err(
            "negative OFFSET",
            ErrorCode.EVALUATOR_NEGATIVE_OFFSET,
            errorContextFrom(rowCountExpr.sourceLocation),
            internal = false
        )
    }

    return offsetValue
}
