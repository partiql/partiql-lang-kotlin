package org.partiql.lang.typesystem.builtin.types.nonprimitive

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.builtin.types.primitive.IntType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType
import org.partiql.lang.typesystem.interfaces.type.SqlType

object Int2Type : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("int2", "integer2", "smallint")

    override val exprValueType: ExprValueType
        get() = ExprValueType.INT

    override val parentType: SqlType
        get() = IntType

    override val isPrimitiveType: Boolean
        get() = false
}
