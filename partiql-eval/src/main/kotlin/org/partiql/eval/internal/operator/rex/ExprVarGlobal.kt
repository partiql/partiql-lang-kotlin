package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.planner.catalog.Name
import org.partiql.spi.connector.ConnectorBindings

internal class ExprVarGlobal(
    private val name: Name,
    private val bindings: ConnectorBindings,
) : Operator.Expr {

    // TODO: Potentially make ConnectorBindings return PQLValue
    override fun eval(env: Environment): Datum {
        return bindings.getBinding(name)!!.getDatum()
    }
}
