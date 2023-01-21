package org.partiql.lang.eval.physical.operators

import org.partiql.annotations.ExperimentalWindowFunctions
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.physical.SetVariableFunc
import org.partiql.lang.eval.physical.window.WindowFunction

@ExperimentalWindowFunctions
abstract class WindowRelationalOperatorFactory(name: String) : RelationalOperatorFactory {

    final override val key: RelationalOperatorFactoryKey = RelationalOperatorFactoryKey(RelationalOperatorKind.WINDOW, name)

    /** Creates a [RelationExpression] instance for [PartiqlPhysical.Bexpr.Window]. */
    abstract fun create(
        source: RelationExpression,
        windowPartitionList: List<ValueExpression>,
        windowSortSpecList: List<CompiledSortKey>,
        compiledWindowFunctions: List<CompiledWindowFunction>

    ): RelationExpression
}

@ExperimentalWindowFunctions
class CompiledWindowFunction(
    val func: WindowFunction,
    val parameters: List<ValueExpression>,
    /**
     * This is [PartiqlPhysical.VarDecl] instead of [SetVariableFunc] because we would like to access the index of variable in the register
     * when processing rows within the partition.
     */
    val windowVarDecl: PartiqlPhysical.VarDecl
)
