package org.partiql.lang.typesystem.builtin.types

import com.amazon.ion.IonType
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.ScalarType

/**
 * Refers to the standard SQL type TIMESTAMP. e.g. '2004-10-19 10:23:54'
 *
 * Also refers to [IonType.TIMESTAMP]
 */
object TimestampType : ScalarType {
    override val typeAliases: List<String>
        get() = listOf("timestamp")

    override val exprValueType: ExprValueType
        get() = ExprValueType.TIMESTAMP
}
