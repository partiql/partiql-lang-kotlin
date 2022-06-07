package org.partiql.lang.typesystem.builtin.types.primitive

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType
import com.amazon.ion.IonType

/**
 * Refers to sql standard type NULL
 *
 * Also refers to [IonType.NULL]
 */
object NullType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("null")

    override val exprValueType: ExprValueType
        get() = ExprValueType.NULL

    override val isPrimitiveType: Boolean
        get() = true
}
