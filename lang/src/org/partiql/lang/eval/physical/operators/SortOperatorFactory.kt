package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.NaturalExprValueComparators
import org.partiql.lang.eval.err
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.util.interruptibleFold

/** Provides an implementation of the [PartiqlPhysical.Bexpr.Order] operator.*/
public abstract class SortOperatorFactory(name: String) : RelationalOperatorFactory {
    public final override val key: RelationalOperatorFactoryKey = RelationalOperatorFactoryKey(RelationalOperatorKind.SORT, name)
    public abstract fun create(
        impl: PartiqlPhysical.Impl,
        sortKeys: List<CompiledSortKey>,
        sourceRelation: RelationExpression
    ): RelationExpression
}

public class CompiledSortKey(val comparator: NaturalExprValueComparators, val value: ValueExpression)

/**
 * A simple [SortOperatorFactory] that sorts relations completely in memory.
 */
internal class InMemorySortFactory(name: String) : SortOperatorFactory(name) {
    override fun create(
        impl: PartiqlPhysical.Impl,
        sortKeys: List<CompiledSortKey>,
        sourceRelation: RelationExpression
    ): RelationExpression = RelationExpression { state ->
        val source = sourceRelation.evaluate(state)
        relation(RelationType.LIST) {
            val rows = mutableListOf<Array<ExprValue>>()
            val comparator = getSortingComparator(sortKeys, state)

            // Consume Input
            while (source.nextRow()) {
                rows.add(state.registers.clone())
            }

            // Perform Sort
            val sortedRows = rows.sortedWith(comparator)

            // Yield Sorted Rows
            val iterator = sortedRows.iterator()
            while (iterator.hasNext()) {
                state.load(iterator.next())
                yield()
            }
        }
    }
}

/**
 * Returns a [Comparator] that compares arrays of registers by using un-evaluated sort keys. It does this by modifying
 * the [state] to allow evaluation of the [sortKeys]
 */
private fun getSortingComparator(sortKeys: List<CompiledSortKey>, state: EvaluatorState): Comparator<Array<ExprValue>> {
    val initial: Comparator<Array<ExprValue>>? = null
    return sortKeys.interruptibleFold(initial) { intermediate, sortKey ->
        if (intermediate == null) {
            return@interruptibleFold compareBy<Array<ExprValue>, ExprValue>(sortKey.comparator) { row ->
                state.load(row)
                sortKey.value(state)
            }
        }
        return@interruptibleFold intermediate.thenBy(sortKey.comparator) { row ->
            state.load(row)
            sortKey.value(state)
        }
    } ?: err(
        "Order BY comparator cannot be null",
        ErrorCode.EVALUATOR_ORDER_BY_NULL_COMPARATOR,
        null,
        internal = true
    )
}
