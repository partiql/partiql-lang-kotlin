package org.partiql.lang.typesystem.interfaces.operator.operators.binary

import org.partiql.lang.typesystem.interfaces.operator.BinaryOp
import org.partiql.lang.typesystem.interfaces.operator.OpAlias

/**
 * Used to define [OpAlias.CONCAT] operator
 */
abstract class ConcatOp : BinaryOp() {
    override fun getOperatorAlias(): OpAlias = OpAlias.CONCAT
}
