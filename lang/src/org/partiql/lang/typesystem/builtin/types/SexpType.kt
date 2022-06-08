package org.partiql.lang.typesystem.builtin.types

import com.amazon.ion.IonType
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

/**
 * Refers to [IonType.SEXP]
 */
object SexpType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("sexp")

    override val exprValueType: ExprValueType
        get() = ExprValueType.SEXP
}
