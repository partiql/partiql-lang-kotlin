package org.partiql.lang.typesystem.interfaces.operator.operators.binary

import org.partiql.lang.typesystem.interfaces.operator.BinaryOp
import org.partiql.lang.typesystem.interfaces.operator.OpAlias

/**
 * Used to define [OpAlias.LT] operator
 */
abstract class LtOp : BinaryOp() {
    override val operatorAlias: OpAlias
        get() = OpAlias.LT
}
