package org.partiql.lang.typesystem.builtin.types

import com.amazon.ion.IonType
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.BuiltInType

/**
 * Refers to [IonType.BLOB]
 */
object BlobType : BuiltInType() {
    override val typeAliases: List<String>
        get() = listOf("blob")

    override val exprValueType: ExprValueType
        get() = ExprValueType.BLOB
}
