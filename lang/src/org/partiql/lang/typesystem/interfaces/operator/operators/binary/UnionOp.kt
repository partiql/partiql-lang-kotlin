package org.partiql.lang.typesystem.interfaces.operator.operators.binary

import org.partiql.lang.typesystem.interfaces.operator.BinaryOp
import org.partiql.lang.typesystem.interfaces.operator.OpAlias

/**
 * Used to define [OpAlias.UNION] operator
 */
abstract class UnionOp : BinaryOp() {
    override fun getOperatorAlias(): OpAlias = OpAlias.UNION
}
