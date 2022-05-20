package org.partiql.lang.typesystem.interfaces.operator.operators.binary

import org.partiql.lang.typesystem.interfaces.operator.BinaryOp
import org.partiql.lang.typesystem.interfaces.operator.OpAlias

/**
 * Used to define [OpAlias.LTE] operator
 */
abstract class LteOp : BinaryOp() {
    override val operatorAlias: OpAlias
        get() = OpAlias.LTE
}
