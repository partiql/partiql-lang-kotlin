package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorPath
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

@OptIn(PartiQLValueExperimental::class)
internal class ExprGlobal(
    private val path: ConnectorPath,
    private val bindings: ConnectorBindings,
) : Operator.Expr {

    override fun eval(record: Record): PartiQLValue = bindings.getValue(path)
}
