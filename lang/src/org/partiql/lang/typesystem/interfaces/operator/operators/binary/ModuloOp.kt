package org.partiql.lang.typesystem.interfaces.operator.operators.binary

import org.partiql.lang.typesystem.interfaces.operator.BinaryOp
import org.partiql.lang.typesystem.interfaces.operator.OpAlias

/**
 * Used to define [OpAlias.MODULO] operator
 */
abstract class ModuloOp : BinaryOp() {
    override val operatorAlias: OpAlias
        get() = OpAlias.MODULO
}
