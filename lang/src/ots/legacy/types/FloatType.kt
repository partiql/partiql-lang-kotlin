package ots.legacy.types

import org.partiql.lang.eval.ExprValueType
import ots.CompileTimeType
import ots.type.ScalarType

object FloatType : ScalarType {
    val compileTimeType: CompileTimeType = CompileTimeType(this, emptyList())

    override val id: String
        get() = "float"

    override val runTimeType: ExprValueType
        get() = ExprValueType.FLOAT
}
