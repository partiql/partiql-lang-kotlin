package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots.interfaces.ScalarType
import org.partiql.lang.ots.interfaces.TypeParameters

object VarcharType : ScalarType {
    override val id: String
        get() = "varying_character"

    override val runTimeType: ExprValueType
        get() = ExprValueType.STRING

    override fun createType(parameters: TypeParameters): CompileTimeVarcharType {
        require(parameters.size == 1) { "VARCHAR type can have 1 parameter at most when declared" }

        val length = parameters.first()

        return CompileTimeVarcharType(length)
    }
}
