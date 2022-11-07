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
 * Provides an implementation of the [PartiqlPhysical.Bexpr.Limit] operator.
 *
 * @constructor
 *
 * @param name
 */
abstract class LimitRelationalOperatorFactory(name: String) : RelationalOperatorFactory {

    final override val key = RelationalOperatorFactoryKey(RelationalOperatorKind.LIMIT, name)

    /**
     * Creates a [RelationExpression] instance for [PartiqlPhysical.Bexpr.Limit].
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

internal object LimitRelationalOperatorFactoryDefault : LimitRelationalOperatorFactory(DEFAULT_IMPL_NAME) {

    override fun create(
        impl: PartiqlPhysical.Impl,
        rowCountExpr: ValueExpression,
        sourceBexpr: RelationExpression
    ) = LimitOperator(
        input = sourceBexpr,
        limit = rowCountExpr
    )
}

internal class LimitOperator(
    private val input: RelationExpression,
    private val limit: ValueExpression,
) : RelationExpression {

    override fun evaluate(state: EvaluatorState): RelationIterator {
        val limit = evalLimitRowCount(limit, state)
        val rows = input.evaluate(state)
        return relation(rows.relType) {
            var rowCount = 0L
            while (rowCount++ < limit && rows.nextRow()) {
                yield()
            }
        }
    }

    private fun evalLimitRowCount(rowCountExpr: ValueExpression, env: EvaluatorState): Long {
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
}
