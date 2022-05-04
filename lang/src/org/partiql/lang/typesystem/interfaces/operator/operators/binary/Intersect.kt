package org.partiql.lang.typesystem.interfaces.operator.operators.binary

import org.partiql.lang.typesystem.interfaces.operator.BinaryOp
import org.partiql.lang.typesystem.interfaces.operator.OpAlias

/**
 * Used to define [OpAlias.INTERSECT] operator
 */
abstract class Intersect: BinaryOp() {
    override fun getOperatorAlias(): OpAlias = OpAlias.INTERSECT
}