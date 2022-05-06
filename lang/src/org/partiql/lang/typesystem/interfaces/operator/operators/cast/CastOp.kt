package org.partiql.lang.typesystem.interfaces.operator.operators.cast

import org.partiql.lang.typesystem.interfaces.operator.AbstractCastOp
import org.partiql.lang.typesystem.interfaces.operator.OpAlias
import org.partiql.lang.typesystem.interfaces.type.Type

/**
 * Used to define [OpAlias.CAST] operator
 */
abstract class CastOp : AbstractCastOp() {
    override val operatorAlias: OpAlias
        get() = OpAlias.CAST

    override val returnType: Type = targetType
}
