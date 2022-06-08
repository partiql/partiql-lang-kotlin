package org.partiql.lang.typesystem.builtin.types

import com.amazon.ion.IonType
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

/**
 * Refers to one row of a table
 *
 * Also refers to [IonType.STRUCT]
 */
object StructType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("struct")

    override val exprValueType: ExprValueType
        get() = ExprValueType.STRUCT
}
