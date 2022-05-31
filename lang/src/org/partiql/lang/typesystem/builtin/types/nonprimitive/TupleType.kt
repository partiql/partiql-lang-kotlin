package org.partiql.lang.typesystem.builtin.types.nonprimitive

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.builtin.types.primitive.StructType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType
import org.partiql.lang.typesystem.interfaces.type.SqlType

object TupleType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("tuple")

    override val exprValueType: ExprValueType
        get() = ExprValueType.STRUCT

    override val parentType: SqlType
        get() = StructType

    override val isPrimitiveType: Boolean
        get() = false
}
