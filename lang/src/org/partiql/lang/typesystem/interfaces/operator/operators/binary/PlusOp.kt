package org.partiql.lang.typesystem.interfaces.operator.operators.binary

import org.partiql.lang.typesystem.interfaces.operator.BinaryOp
import org.partiql.lang.typesystem.interfaces.operator.OpAlias

/**
 * Used to define [OpAlias.PLUS] operator
 */
abstract class PlusOp : BinaryOp() {
    override fun getOperatorAlias(): OpAlias = OpAlias.PLUS
}
