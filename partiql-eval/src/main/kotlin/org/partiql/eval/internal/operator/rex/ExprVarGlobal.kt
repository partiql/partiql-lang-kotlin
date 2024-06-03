package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.PQLValue
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorPath
import org.partiql.value.PartiQLValueExperimental

@OptIn(PartiQLValueExperimental::class)
internal class ExprVarGlobal(
    private val path: ConnectorPath,
    private val bindings: ConnectorBindings,
) : Operator.Expr {

    // TODO: Potentially make ConnectorBindings return PQLValue
    override fun eval(env: Environment): PQLValue = PQLValue.of(bindings.getValue(path))
}
