package org.partiql.lang.typesystem.builtin.types.primitive

import com.amazon.ion.IonType
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

/**
 * Refers to standard sql type INTEGER, whose value rages from -2147483648 to +2147483647
 *
 * Also refers to [IonType.INT]
 */
object IntType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("int", "integer")

    override val exprValueType: ExprValueType
        get() = ExprValueType.INT

    override val isPrimitiveType: Boolean
        get() = true
}
