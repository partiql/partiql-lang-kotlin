package org.partiql.lang.typesystem.builtin.types.primitive

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

object SexpType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("sexp")

    override val exprValueType: ExprValueType
        get() = ExprValueType.SEXP

    override val isPrimitiveType: Boolean
        get() = true
}
