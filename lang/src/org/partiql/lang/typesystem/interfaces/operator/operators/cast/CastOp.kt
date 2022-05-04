package org.partiql.lang.typesystem.interfaces.operator.operators.cast

import org.partiql.lang.typesystem.interfaces.operator.AbstractCastOp
import org.partiql.lang.typesystem.interfaces.operator.OpAlias

/**
 * Used to define [OpAlias.CAST] operator
 */
abstract class CastOp: AbstractCastOp() {
    override fun getOperatorAlias(): OpAlias = OpAlias.CAST
}