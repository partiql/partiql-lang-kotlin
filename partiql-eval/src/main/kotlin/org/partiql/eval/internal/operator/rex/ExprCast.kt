package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

/**
 * Implementation of a CAST expression.
 *
 * @constructor
 * TODO
 *
 * @param operand
 * @param target
 */
internal class ExprCast(operand: ExprValue, target: PType) :
    ExprValue {

    // DO NOT USE FINAL
    private var _operand = operand
    private var _target = target

    override fun eval(env: Environment): Datum {
        return CastTable.cast(_operand.eval(env), _target)
    }
}
