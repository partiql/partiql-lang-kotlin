package org.partiql.lang.eval.physical.operators

import org.partiql.lang.ast.WindowPartitionList
import org.partiql.lang.ast.WindowSortSpecList
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.NaturalExprValueComparators
import org.partiql.lang.eval.physical.EvaluatorState


class CompiledSortKey(val comparator: NaturalExprValueComparators, val value: ValueExpression)

abstract class WindowRelationalOperatorFactory(name: String) : RelationalOperatorFactory {

    final override val key: RelationalOperatorFactoryKey = RelationalOperatorFactoryKey(RelationalOperatorKind.WINDOW, name)


    /** Creates a [RelationExpression] instance for [PartiqlPhysical.Bexpr.Window]. */
    abstract fun create(
        /**
         * Contains any static arguments needed by the operator implementation that were supplied by the planner
         * pass which specified the operator implementation.
         */
        impl: PartiqlPhysical.Impl,

        /** Invokes the source binding expression. */
        source: RelationExpression,

        windowPartitionList: List<ValueExpression>?,

        windowSortSpecList: List<CompiledSortKey>?,

        windowExpression: PartiqlPhysical.WindowExpression,

        windowFunctionParameter: List<ValueExpression>,

    ): RelationExpression
}

class sortBasedWindowOperator(name:String) : WindowRelationalOperatorFactory(name){
    override fun create(
        impl: PartiqlPhysical.Impl,
        source: RelationExpression,
        windowPartitionList: List<ValueExpression>?,
        windowSortSpecList: List<CompiledSortKey>?,
        windowExpression: PartiqlPhysical.WindowExpression,
        windowFunctionParameter: List<ValueExpression>
    ): RelationExpression {
        TODO("Not yet implemented")
    }

}
