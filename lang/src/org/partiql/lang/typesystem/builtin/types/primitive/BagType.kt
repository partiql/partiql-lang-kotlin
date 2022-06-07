package org.partiql.lang.typesystem.builtin.types.primitive

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

object BagType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("bag")

    override val exprValueType: ExprValueType
        get() = ExprValueType.BAG

    override val isPrimitiveType: Boolean
        get() = true
}
