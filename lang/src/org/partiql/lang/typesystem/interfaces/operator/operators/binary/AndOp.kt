package org.partiql.lang.typesystem.interfaces.operator.operators.binary

import org.partiql.lang.typesystem.interfaces.operator.BinaryOp
import org.partiql.lang.typesystem.interfaces.operator.OpAlias

/**
 * Used to define [OpAlias.AND] operator
 */
abstract class AndOp: BinaryOp() {
    override fun getOperatorAlias(): OpAlias = OpAlias.AND
}