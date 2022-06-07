package org.partiql.lang.typesystem.builtin.types.primitive

import com.amazon.ion.IonType
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

/**
 * The standard SQL type BOOLEAN
 *
 * Also refers to [IonType.BOOL]
 */
object BooleanType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("bool", "boolean")

    override val exprValueType: ExprValueType
        get() = ExprValueType.BOOL

    override val isPrimitiveType: Boolean
        get() = true
}
