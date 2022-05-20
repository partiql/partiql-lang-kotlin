package org.partiql.lang.typesystem.interfaces.operator.operators.cast

import org.partiql.lang.typesystem.interfaces.operator.AbstractCastOp
import org.partiql.lang.typesystem.interfaces.operator.OpAlias

/**
 * Used to define [OpAlias.CAN_LOSSLESS_CAST] operator
 */
abstract class CanLosslesCast : AbstractCastOp() {
    override val operatorAlias: OpAlias
        get() = OpAlias.CAN_LOSSLESS_CAST
}
