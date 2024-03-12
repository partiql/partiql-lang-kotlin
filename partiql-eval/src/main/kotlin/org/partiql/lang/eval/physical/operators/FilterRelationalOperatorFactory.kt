package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.booleanValue
import org.partiql.lang.eval.internal.ext.isNotUnknown
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
@Deprecated("To be removed in the next major version.", replaceWith = ReplaceWith("FilterRelationalOperatorFactoryAsync"))
abstract class FilterRelationalOperatorFactory(name: String) : RelationalOperatorFactory {

    final override val key = RelationalOperatorFactoryKey(RelationalOperatorKind.FILTER, name)

    /**
     * Creates a [RelationExpression] instance for [PartiqlPhysical.Bexpr.Filter].
     *
     * @param impl
     * @param predicate
     * @param sourceBexpr
     * @return
     */
    @Deprecated("To be removed in the next major version.", replaceWith = ReplaceWith("FilterRelationalOperatorFactoryAsync.create"))
    abstract fun create(
        impl: PartiqlPhysical.Impl,
        predicate: ValueExpression,
        sourceBexpr: RelationExpression
    ): RelationExpression
}

internal object FilterRelationalOperatorFactoryDefault : FilterRelationalOperatorFactory(DEFAULT_IMPL_NAME) {
    override fun create(
        impl: PartiqlPhysical.Impl,
        predicate: ValueExpression,
        sourceBexpr: RelationExpression
    ) = SelectOperatorDefault(
        input = sourceBexpr,
        predicate = predicate
    )
}

internal class SelectOperatorDefault(
    val input: RelationExpression,
    val predicate: ValueExpression,
) : RelationExpression {

    override fun evaluate(state: EvaluatorState): RelationIterator {
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
