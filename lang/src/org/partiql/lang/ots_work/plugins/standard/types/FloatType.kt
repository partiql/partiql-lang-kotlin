package org.partiql.lang.ots_work.plugins.standard.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.type.ScalarType

object FloatType : ScalarType {
    val compileTimeType: CompileTimeType = CompileTimeType(this, emptyList())

    override val id: String
        get() = "float"

    override val runTimeType: ExprValueType
        get() = ExprValueType.FLOAT
}
