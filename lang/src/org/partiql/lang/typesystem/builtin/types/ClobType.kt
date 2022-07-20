package org.partiql.lang.typesystem.builtin.types

import com.amazon.ion.IonType
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.ScalarType

/**
 * Refers to [IonType.CLOB]
 */
object ClobType : ScalarType {
    override val typeAliases: List<String>
        get() = listOf("clob")

    override val exprValueType: ExprValueType
        get() = ExprValueType.CLOB
}
