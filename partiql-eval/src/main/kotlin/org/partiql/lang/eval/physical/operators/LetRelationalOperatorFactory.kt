package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.physical.VariableBinding
import org.partiql.lang.eval.relation.RelationIterator
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.planner.transforms.DEFAULT_IMPL_NAME

/**
 * Provides an implementation of the [PartiqlPhysical.Bexpr.Let] operator.
 *
 * @constructor
 *
 * @param name
 */
abstract class LetRelationalOperatorFactory(name: String) : RelationalOperatorFactory {

    final override val key = RelationalOperatorFactoryKey(RelationalOperatorKind.LET, name)

    /**
     * Creates a [RelationExpression] instance for [PartiqlPhysical.Bexpr.Let].
     *
     * @param impl
     * @param sourceBexpr
     * @param bindings list of [VariableBinding]s in the `LET` clause
     * @return
     */
    abstract fun create(
        impl: PartiqlPhysical.Impl,
        sourceBexpr: RelationExpression,
        bindings: List<VariableBinding>
    ): RelationExpression
}

internal object LetRelationalOperatorFactoryDefault : LetRelationalOperatorFactory(DEFAULT_IMPL_NAME) {

    override fun create(
        impl: PartiqlPhysical.Impl,
        sourceBexpr: RelationExpression,
        bindings: List<VariableBinding>
    ) = LetOperator(
        input = sourceBexpr,
        bindings = bindings,
    )
}

internal class LetOperator(
    private val input: RelationExpression,
    private val bindings: List<VariableBinding>
) : RelationExpression {

    override fun evaluate(state: EvaluatorState): RelationIterator {
        val rows = input.evaluate(state)
        return relation(rows.relType) {
            while (rows.nextRow()) {
                bindings.forEach {
                    it.setFunc(state, it.expr(state))
                }
                yield()
            }
        }
    }
}
