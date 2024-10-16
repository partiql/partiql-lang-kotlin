package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum

/**
 * Variable expression necessarily holds a reference to the interpreter stack (environment).
 */
internal class ExprVar(
    private val env: Environment,
    private val depth: Int,
    private val offset: Int,
) : Operator.Expr {

    override fun eval(): Datum = env.get(depth, offset)
}
