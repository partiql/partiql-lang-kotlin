package org.partiql.lang.typesystem.builtin.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

/**
 * The same as [StructType]
 */
object TupleType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("tuple")

    override val exprValueType: ExprValueType
        get() = ExprValueType.STRUCT
}
