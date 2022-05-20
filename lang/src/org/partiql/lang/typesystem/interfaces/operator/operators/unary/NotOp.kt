package org.partiql.lang.typesystem.interfaces.operator.operators.unary

import org.partiql.lang.typesystem.interfaces.operator.OpAlias
import org.partiql.lang.typesystem.interfaces.operator.UnaryOp

/**
 * Used to define [OpAlias.NOT] operator
 */
abstract class NotOp : UnaryOp() {
    override val operatorAlias: OpAlias
        get() = OpAlias.NOT
}
