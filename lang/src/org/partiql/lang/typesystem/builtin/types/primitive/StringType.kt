package org.partiql.lang.typesystem.builtin.types.primitive

import com.amazon.ion.IonType
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

/**
 * Refers to [IonType.STRING]
 */
object StringType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("string")

    override val exprValueType: ExprValueType
        get() = ExprValueType.STRING

    override val isPrimitiveType: Boolean
        get() = true
}
