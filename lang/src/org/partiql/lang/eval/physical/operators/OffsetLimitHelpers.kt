package org.partiql.lang.eval.physical

import com.amazon.ion.IntegerSize
import com.amazon.ion.IonInt
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.err
import org.partiql.lang.eval.errorContextFrom
import org.partiql.lang.eval.numberValue

// The functions in this file look very similar and so the temptation to DRY is quite strong....
// However, there are enough subtle differences between them that avoiding the duplication isn't worth it.

internal fun evalLimitRowCount(rowCountThunk: ExprThunkEnv, env: EvaluatorState, limitLocationMeta: SourceLocationMeta?): Long {
    val limitExprValue = rowCountThunk(env)

    if (limitExprValue.type != ExprValueType.INT) {
        err(
            "LIMIT value was not an integer",
            ErrorCode.EVALUATOR_NON_INT_LIMIT_VALUE,
            errorContextFrom(limitLocationMeta).also {
                it[Property.ACTUAL_TYPE] = limitExprValue.type.toString()
            },
            internal = false
        )
    }

    // `Number.toLong()` (used below) does *not* cause an overflow exception if the underlying [Number]
    // implementation (i.e. Decimal or BigInteger) exceeds the range that can be represented by Longs.
    // This can cause very confusing behavior if the user specifies a LIMIT value that exceeds
    // Long.MAX_VALUE, because no results will be returned from their query.  That no overflow exception
    // is thrown is not a problem as long as PartiQL's restriction of integer values to +/- 2^63 remains.
    // We throw an exception here if the value exceeds the supported range (say if we change that
    // restriction or if a custom [ExprValue] is provided which exceeds that value).
    val limitIonValue = limitExprValue.ionValue as IonInt
    if (limitIonValue.integerSize == IntegerSize.BIG_INTEGER) {
        err(
            "IntegerSize.BIG_INTEGER not supported for LIMIT values",
            ErrorCode.INTERNAL_ERROR,
            errorContextFrom(limitLocationMeta),
            internal = true
        )
    }

    val limitValue = limitExprValue.numberValue().toLong()

    if (limitValue < 0) {
        err(
            "negative LIMIT",
            ErrorCode.EVALUATOR_NEGATIVE_LIMIT,
            errorContextFrom(limitLocationMeta),
            internal = false
        )
    }

    // we can't use the Kotlin's Sequence<T>.take(n) for this since it accepts only an integer.
    // this references [Sequence<T>.take(count: Long): Sequence<T>] defined in [org.partiql.util].
    return limitValue
}

internal fun evalOffsetRowCount(rowCountThunk: ExprThunkEnv, env: EvaluatorState, offsetLocationMeta: SourceLocationMeta?): Long {
    val offsetExprValue = rowCountThunk(env)

    if (offsetExprValue.type != ExprValueType.INT) {
        err(
            "OFFSET value was not an integer",
            ErrorCode.EVALUATOR_NON_INT_OFFSET_VALUE,
            errorContextFrom(offsetLocationMeta).also {
                it[Property.ACTUAL_TYPE] = offsetExprValue.type.toString()
            },
            internal = false
        )
    }

    // `Number.toLong()` (used below) does *not* cause an overflow exception if the underlying [Number]
    // implementation (i.e. Decimal or BigInteger) exceeds the range that can be represented by Longs.
    // This can cause very confusing behavior if the user specifies a OFFSET value that exceeds
    // Long.MAX_VALUE, because no results will be returned from their query.  That no overflow exception
    // is thrown is not a problem as long as PartiQL's restriction of integer values to +/- 2^63 remains.
    // We throw an exception here if the value exceeds the supported range (say if we change that
    // restriction or if a custom [ExprValue] is provided which exceeds that value).
    val offsetIonValue = offsetExprValue.ionValue as IonInt
    if (offsetIonValue.integerSize == IntegerSize.BIG_INTEGER) {
        err(
            "IntegerSize.BIG_INTEGER not supported for OFFSET values",
            ErrorCode.INTERNAL_ERROR,
            errorContextFrom(offsetLocationMeta),
            internal = true
        )
    }

    val offsetValue = offsetExprValue.numberValue().toLong()

    if (offsetValue < 0) {
        err(
            "negative OFFSET",
            ErrorCode.EVALUATOR_NEGATIVE_OFFSET,
            errorContextFrom(offsetLocationMeta),
            internal = false
        )
    }

    return offsetValue
}
