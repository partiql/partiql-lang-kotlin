package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.booleanValue
import org.partiql.lang.eval.isNotUnknown
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.relation.RelationIterator
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.planner.transforms.DEFAULT_IMPL_NAME

/**
 * Provides an implementation of the [PartiqlPhysical.Bexpr.Filter] operator.
 *
 * @constructor
 *
 * @param name
 */
abstract class FilterRelationalOperatorFactoryAsync(name: String) : RelationalOperatorFactory {

    final override val key = RelationalOperatorFactoryKey(RelationalOperatorKind.FILTER, name)

    /**
     * Creates a [RelationExpressionAsync] instance for [PartiqlPhysical.Bexpr.Filter].
     *
     * @param impl
     * @param predicate
     * @param sourceBexpr
     * @return
     */
    abstract fun create(
        impl: PartiqlPhysical.Impl,
        predicate: ValueExpressionAsync,
        sourceBexpr: RelationExpressionAsync
    ): RelationExpressionAsync
}

internal object FilterRelationalOperatorFactoryDefaultAsync : FilterRelationalOperatorFactoryAsync(DEFAULT_IMPL_NAME) {
    override fun create(
        impl: PartiqlPhysical.Impl,
        predicate: ValueExpressionAsync,
        sourceBexpr: RelationExpressionAsync
    ) = SelectOperatorDefaultAsync(
        input = sourceBexpr,
        predicate = predicate
    )
}

internal class SelectOperatorDefaultAsync(
    val input: RelationExpressionAsync,
    val predicate: ValueExpressionAsync,
) : RelationExpressionAsync {

    override suspend fun evaluate(state: EvaluatorState): RelationIterator {
        val input = input.evaluate(state)
        return relation(RelationType.BAG) {
            while (true) {
                if (!input.nextRow()) {
                    break
                } else {
                    val matches = predicate.invoke(state)
                    if (matches.isNotUnknown() && matches.booleanValue()) {
                        yield()
                    }
                }
            }
        }
    }
}
