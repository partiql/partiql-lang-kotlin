package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots.interfaces.ScalarType
import org.partiql.lang.ots.interfaces.TypeParameters

object DateType : ScalarType {
    override val id: String
        get() = "date"

    override val runTimeType: ExprValueType
        get() = ExprValueType.DATE

    override fun createType(parameters: TypeParameters): CompileTimeDateType = CompileTimeDateType
}
