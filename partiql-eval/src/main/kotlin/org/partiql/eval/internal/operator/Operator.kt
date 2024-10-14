package org.partiql.eval.internal.operator

import org.partiql.eval.internal.Environment
import org.partiql.spi.value.Datum

internal sealed interface IOperator

/**
 * Expr represents an evaluable expression tree which returns a value.
 */
internal interface IExpr : IOperator {

    fun eval(env: Environment): Datum
}

