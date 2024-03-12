package org.partiql.lang.eval.physical.operators

import org.partiql.annotations.ExperimentalWindowFunctions
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.physical.SetVariableFunc
import org.partiql.lang.eval.physical.window.NavigationWindowFunctionAsync

@ExperimentalWindowFunctions
abstract class WindowRelationalOperatorFactoryAsync(name: String) : RelationalOperatorFactory {

    final override val key: RelationalOperatorFactoryKey = RelationalOperatorFactoryKey(RelationalOperatorKind.WINDOW, name)

    /** Creates a [RelationExpressionAsync] instance for [PartiqlPhysical.Bexpr.Window]. */
    abstract fun create(
        source: RelationExpressionAsync,
        windowPartitionList: List<ValueExpressionAsync>,
        windowSortSpecList: List<CompiledSortKeyAsync>,
        compiledWindowFunctions: List<CompiledWindowFunctionAsync>

    ): RelationExpressionAsync
}

@ExperimentalWindowFunctions
class CompiledWindowFunctionAsync(
    val func: NavigationWindowFunctionAsync,
    val parameters: List<ValueExpressionAsync>,
    /**
     * This is [PartiqlPhysical.VarDecl] instead of [SetVariableFunc] because we would like to access the index of variable in the register
     * when processing rows within the partition.
     */
    val windowVarDecl: PartiqlPhysical.VarDecl
)
