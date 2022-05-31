package org.partiql.lang.typesystem.builtin.types.nonprimitive

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.builtin.types.primitive.IntType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType
import org.partiql.lang.typesystem.interfaces.type.SqlType

object Int8Type : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("int8", "integer8", "bigint")

    override val exprValueType: ExprValueType
        get() = ExprValueType.INT

    override val parentType: SqlType
        get() = IntType

    override val isPrimitiveType: Boolean
        get() = false
}
