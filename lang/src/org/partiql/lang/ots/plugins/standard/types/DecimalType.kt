package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots.interfaces.ScalarType
import org.partiql.lang.ots.interfaces.TypeParameters

object DecimalType : ScalarType {
    override val id: String
        get() = "decimal"

    override val runTimeType: ExprValueType
        get() = ExprValueType.DECIMAL

    override fun createType(parameters: TypeParameters): CompileTimeDecimalType {
        require(parameters.size <= 2) { "DECIMAL type can have 2 parameters at most when declared" }

        val precision = parameters.firstOrNull()
        val scale = parameters.getOrNull(1) ?: 0

        return CompileTimeDecimalType(precision, scale)
    }
}
