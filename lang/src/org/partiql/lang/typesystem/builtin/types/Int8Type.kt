package org.partiql.lang.typesystem.builtin.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.typesystem.interfaces.type.ScalarType

/**
 * The standard sql type BIGINT, whose value ranges from -9223372036854775808 to +9223372036854775807
 */
object Int8Type : ScalarType {
    override val typeAliases: List<String>
        get() = listOf("int8", "integer8", "bigint")

    override val exprValueType: ExprValueType
        get() = ExprValueType.INT
}
