package org.partiql.lang.eval.physical.operators

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.physical.SetVariableFunc
import org.partiql.lang.eval.physical.window.ExperimentalWindowFunc
import org.partiql.lang.eval.physical.window.WindowFunction

// TODO: Remove from experimental once https://github.com/partiql/partiql-docs/issues/31 is resolved and a RFC is approved
@ExperimentalWindowFunc
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

@ExperimentalWindowFunc
class CompiledWindowFunction(
    val func: WindowFunction,
    val parameters: List<ValueExpression>,
    /**
     * This is [PartiqlPhysical.VarDecl] instead of [SetVariableFunc] because we would like to access the index of variable in the register
     * when processing rows within the partition.
     */
    val windowVarDecl: PartiqlPhysical.VarDecl
)
