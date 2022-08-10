package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots.interfaces.ScalarType
import org.partiql.lang.ots.interfaces.TypeParameters

object StringType : ScalarType {
    override val id: String
        get() = "string"

    override val runTimeType: ExprValueType
        get() = ExprValueType.STRING

    override fun createType(parameters: TypeParameters): CompileTimeStringType = CompileTimeStringType
}
