package org.partiql.lang.typesystem.interfaces.operator.operators.binary

import org.partiql.lang.typesystem.interfaces.operator.BinaryOp
import org.partiql.lang.typesystem.interfaces.operator.OpAlias

/**
 * Used to define [OpAlias.MINUS] operator
 */
abstract class MinusOp: BinaryOp() {
    override fun getOperatorAlias(): OpAlias = OpAlias.PLUS
}