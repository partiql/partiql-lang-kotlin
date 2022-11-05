package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.physical.SetVariableFunc
import org.partiql.lang.eval.physical.window.Experimental
import org.partiql.lang.eval.physical.window.WindowFunction

@Experimental
abstract class WindowRelationalOperatorFactory(name: String) : RelationalOperatorFactory {

    final override val key: RelationalOperatorFactoryKey = RelationalOperatorFactoryKey(RelationalOperatorKind.WINDOW, name)

    /** Creates a [RelationExpression] instance for [PartiqlPhysical.Bexpr.Window]. */
    abstract fun create(
        source: RelationExpression,
        windowPartitionList: List<ValueExpression>,
        windowSortSpecList: List<CompiledSortKey>,
        windowFunction: List<CompiledWindowFunction>

    ): RelationExpression
}

@OptIn(Experimental::class)
class CompiledWindowFunction(
    val func: WindowFunction,
    val parameters: List<ValueExpression>,
    val setWindowVal: SetVariableFunc
)
