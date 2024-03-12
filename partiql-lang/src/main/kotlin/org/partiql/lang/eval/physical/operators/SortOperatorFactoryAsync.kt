package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.NaturalExprValueComparators

/** Provides an implementation of the [PartiqlPhysical.Bexpr.Sort] operator.*/
public abstract class SortOperatorFactoryAsync(name: String) : RelationalOperatorFactory {
    public final override val key: RelationalOperatorFactoryKey = RelationalOperatorFactoryKey(RelationalOperatorKind.SORT, name)
    public abstract fun create(
        sortKeys: List<CompiledSortKeyAsync>,
        sourceRelation: RelationExpressionAsync
    ): RelationExpressionAsync
}

public class CompiledSortKeyAsync(val comparator: NaturalExprValueComparators, val value: ValueExpressionAsync)
