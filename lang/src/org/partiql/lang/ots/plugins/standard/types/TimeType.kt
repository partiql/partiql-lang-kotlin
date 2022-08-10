package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots.interfaces.ScalarType
import org.partiql.lang.ots.interfaces.TypeParameters

object TimeType : ScalarType {
    override val id: String
        get() = "time"

    override val runTimeType: ExprValueType
        get() = ExprValueType.TIME

    override fun createType(parameters: TypeParameters): CompileTimeTimeType {
        require(parameters.size <= 1) { "TIME type can have 1 parameter at most when declared" }

        val precision = parameters.firstOrNull()

        return CompileTimeTimeType(precision)
    }
}
