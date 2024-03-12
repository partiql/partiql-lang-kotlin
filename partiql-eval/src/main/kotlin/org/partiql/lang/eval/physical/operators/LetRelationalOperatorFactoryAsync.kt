package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.physical.VariableBindingAsync
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
abstract class LetRelationalOperatorFactoryAsync(name: String) : RelationalOperatorFactory {

    final override val key = RelationalOperatorFactoryKey(RelationalOperatorKind.LET, name)

    /**
     * Creates a [RelationExpressionAsync] instance for [PartiqlPhysical.Bexpr.Let].
     *
     * @param impl
     * @param sourceBexpr
     * @param bindings list of [VariableBindingAsync]s in the `LET` clause
     * @return
     */
    abstract fun create(
        impl: PartiqlPhysical.Impl,
        sourceBexpr: RelationExpressionAsync,
        bindings: List<VariableBindingAsync>
    ): RelationExpressionAsync
}

internal object LetRelationalOperatorFactoryDefaultAsync : LetRelationalOperatorFactoryAsync(DEFAULT_IMPL_NAME) {

    override fun create(
        impl: PartiqlPhysical.Impl,
        sourceBexpr: RelationExpressionAsync,
        bindings: List<VariableBindingAsync>
    ) = LetOperatorAsync(
        input = sourceBexpr,
        bindings = bindings,
    )
}

internal class LetOperatorAsync(
    private val input: RelationExpressionAsync,
    private val bindings: List<VariableBindingAsync>
) : RelationExpressionAsync {

    override suspend fun evaluate(state: EvaluatorState): RelationIterator {
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
