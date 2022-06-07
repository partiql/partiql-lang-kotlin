package org.partiql.lang.typesystem.builtin.types.primitive

import com.amazon.ion.IonType
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

/**
 * Refers to [IonType.CLOB]
 */
object ClobType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("clob")

    override val exprValueType: ExprValueType
        get() = ExprValueType.CLOB

    override val isPrimitiveType: Boolean
        get() = true
}
