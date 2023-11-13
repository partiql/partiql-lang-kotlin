package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.NaturalExprValueComparators

/** Provides an implementation of the [PartiqlPhysical.Bexpr.Order] operator.*/
public abstract class SortOperatorFactory(name: String) : RelationalOperatorFactory {
    public final override val key: RelationalOperatorFactoryKey = RelationalOperatorFactoryKey(RelationalOperatorKind.SORT, name)
    public abstract fun create(
        sortKeys: List<CompiledSortKey>,
        sourceRelation: RelationExpression
    ): RelationExpression
}

public class CompiledSortKey(val comparator: NaturalExprValueComparators, val value: ValueExpression)
