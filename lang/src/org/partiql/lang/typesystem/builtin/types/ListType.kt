package org.partiql.lang.typesystem.builtin.types

import com.amazon.ion.IonType
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

/**
 * The return type of SELECT clause with ORDER BY
 *
 * Also refers to [IonType.LIST]
 */
object ListType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("list")

    override val exprValueType: ExprValueType
        get() = ExprValueType.LIST
}
