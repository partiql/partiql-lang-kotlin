package org.partiql.lang.typesystem.builtin.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

/**
 * Refers to the standard SQL type DATE, e.g. `1999-01-08`
 */
object DateType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("date")

    override val exprValueType: ExprValueType
        get() = ExprValueType.DATE
}
