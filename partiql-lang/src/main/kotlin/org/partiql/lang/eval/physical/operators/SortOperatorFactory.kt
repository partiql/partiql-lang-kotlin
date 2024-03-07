package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.NaturalExprValueComparators

/** Provides an implementation of the [PartiqlPhysical.Bexpr.Order] operator.*/
@Deprecated("To be removed in the next major version.", replaceWith = ReplaceWith("SortOperatorFactoryAsync"))
public abstract class SortOperatorFactory(name: String) : RelationalOperatorFactory {
    public final override val key: RelationalOperatorFactoryKey = RelationalOperatorFactoryKey(RelationalOperatorKind.SORT, name)
    @Deprecated("To be removed in the next major version.", replaceWith = ReplaceWith("SortOperatorFactoryAsync.create"))
    public abstract fun create(
        sortKeys: List<CompiledSortKey>,
        sourceRelation: RelationExpression
    ): RelationExpression
}

@Deprecated("To be removed in the next major version.", replaceWith = ReplaceWith("CompiledSortKeyAsync"))
public class CompiledSortKey(val comparator: NaturalExprValueComparators, val value: ValueExpression)
