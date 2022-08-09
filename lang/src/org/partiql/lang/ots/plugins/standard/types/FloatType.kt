package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots.interfaces.ScalarType
import org.partiql.lang.ots.interfaces.TypeParameters

object FloatType : ScalarType {
    override val id: String
        get() = "float"

    override val runTimeType: ExprValueType
        get() = ExprValueType.FLOAT

    override fun createType(parameters: TypeParameters): CompileTimeFloatType = CompileTimeFloatType
}
