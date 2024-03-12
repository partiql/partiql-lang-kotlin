package org.partiql.lang.eval.physical.operators

import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.err
import org.partiql.lang.eval.errorContextFrom
import org.partiql.lang.eval.numberValue
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.relation.RelationIterator
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.planner.transforms.DEFAULT_IMPL_NAME

/**
 * Provides an implementation of the [PartiqlPhysical.Bexpr.Offset] operator.
 *
 * @constructor
 *
 * @param name
 */
abstract class OffsetRelationalOperatorFactoryAsync(name: String) : RelationalOperatorFactory {

    final override val key = RelationalOperatorFactoryKey(RelationalOperatorKind.OFFSET, name)

    /**
     * Creates a [RelationExpressionAsync] instance for [PartiqlPhysical.Bexpr.Offset].
     *
     * @param impl
     * @param rowCountExpr
     * @param sourceBexpr
     * @return
     */
    abstract fun create(
        impl: PartiqlPhysical.Impl,
        rowCountExpr: ValueExpressionAsync,
        sourceBexpr: RelationExpressionAsync
    ): RelationExpressionAsync
}

internal object OffsetRelationalOperatorFactoryDefaultAsync : OffsetRelationalOperatorFactoryAsync(DEFAULT_IMPL_NAME) {

    override fun create(
        impl: PartiqlPhysical.Impl,
        rowCountExpr: ValueExpressionAsync,
        sourceBexpr: RelationExpressionAsync
    ) = OffsetOperatorAsync(
        input = sourceBexpr,
        offset = rowCountExpr,
    )
}

internal class OffsetOperatorAsync(
    private val input: RelationExpressionAsync,
    private val offset: ValueExpressionAsync,
) : RelationExpressionAsync {

    override suspend fun evaluate(state: EvaluatorState): RelationIterator {
        val skipCount: Long = evalOffsetRowCount(offset, state)
        val rows = input.evaluate(state)
        return relation(rows.relType) {
            var rowCount = 0L
            while (rowCount++ < skipCount) {
                // stop iterating if we run out of rows before we hit the offset.
                if (!rows.nextRow()) {
                    return@relation
                }
            }
            yieldAll(rows)
        }
    }

    private suspend fun evalOffsetRowCount(rowCountExpr: ValueExpressionAsync, state: EvaluatorState): Long {
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

        val originalOffsetValue = offsetExprValue.numberValue()
        val offsetValue = originalOffsetValue.toLong()
        if (originalOffsetValue != offsetValue as Number) { // Make sure `Number.toLong()` is a lossless transformation
            err(
                "Integer exceeds Long.MAX_VALUE provided as OFFSET value",
                ErrorCode.INTERNAL_ERROR,
                errorContextFrom(rowCountExpr.sourceLocation),
                internal = true
            )
        }

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
}
