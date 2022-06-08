package org.partiql.lang.typesystem.builtin.types

import com.amazon.ion.IonType
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

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
}
