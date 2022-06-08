package org.partiql.lang.typesystem.builtin.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

/**
 * An unordered list, the return type of SELECT clauses without ORDER BY
 */
object BagType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("bag")

    override val exprValueType: ExprValueType
        get() = ExprValueType.BAG
}
