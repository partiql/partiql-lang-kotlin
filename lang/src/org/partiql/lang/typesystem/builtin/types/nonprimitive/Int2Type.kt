package org.partiql.lang.typesystem.builtin.types.nonprimitive

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

/**
 * The standard sql type SMALLINT, whose value ranges from -32768 to +32767
 */
object Int2Type : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("int2", "integer2", "smallint")

    override val exprValueType: ExprValueType
        get() = ExprValueType.INT

    override val isPrimitiveType: Boolean
        get() = false
}
