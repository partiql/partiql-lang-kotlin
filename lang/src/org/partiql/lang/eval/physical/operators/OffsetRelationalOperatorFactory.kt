package org.partiql.lang.eval.physical.operators

import com.amazon.ion.IntegerSize
import com.amazon.ion.IonInt
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
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
abstract class OffsetRelationalOperatorFactory(name: String) : RelationalOperatorFactory {

    final override val key = RelationalOperatorFactoryKey(RelationalOperatorKind.OFFSET, name)

    /**
     * Creates a [RelationExpression] instance for [PartiqlPhysical.Bexpr.Offset].
     *
     * @param impl
     * @param rowCountExpr
     * @param sourceBexpr
     * @return
     */
    abstract fun create(
        impl: PartiqlPhysical.Impl,
        rowCountExpr: ValueExpression,
        sourceBexpr: RelationExpression
    ): RelationExpression
}

internal object OffsetRelationalOperatorFactoryDefault : OffsetRelationalOperatorFactory(DEFAULT_IMPL_NAME) {

    override fun create(
        impl: PartiqlPhysical.Impl,
        rowCountExpr: ValueExpression,
        sourceBexpr: RelationExpression
    ) = OffsetOperator(
        input = sourceBexpr,
        offset = rowCountExpr,
    )
}

internal class OffsetOperator(
    private val input: RelationExpression,
    private val offset: ValueExpression,
) : RelationExpression {

    override fun evaluate(state: EvaluatorState): RelationIterator {
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

    private fun evalOffsetRowCount(rowCountExpr: ValueExpression, state: EvaluatorState): Long {
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
}
