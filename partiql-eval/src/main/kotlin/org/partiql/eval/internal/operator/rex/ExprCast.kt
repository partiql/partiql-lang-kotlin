package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.operator.Expression
import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * Implementation of a CAST expression.
 *
 * @constructor
 * TODO
 *
 * @param operand
 * @param target
 */
internal class ExprCast(operand: Expression, target: PType) :
    Expression {

    // DO NOT USE FINAL
    private var _operand = operand
    private var _target = target

    override fun eval(env: Environment): Datum {
        return CastTable.cast(_operand.eval(env), _target)
    }
}
