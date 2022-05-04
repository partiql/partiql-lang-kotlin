package org.partiql.lang.typesystem.interfaces.operator.operators.unary

import org.partiql.lang.typesystem.interfaces.operator.OpAlias
import org.partiql.lang.typesystem.interfaces.operator.UnaryOp

/**
 * Used to define [OpAlias.NEG] operator
 */
abstract class NegOp: UnaryOp() {
    override fun getOperatorAlias(): OpAlias = OpAlias.NEG
}