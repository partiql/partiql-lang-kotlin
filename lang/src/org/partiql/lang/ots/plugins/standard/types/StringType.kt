package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots.interfaces.CompileTimeType
import org.partiql.lang.ots.interfaces.type.ScalarType

object StringType : ScalarType {
    val compileTimeType: CompileTimeType = CompileTimeType(this, emptyList())

    override val id: String
        get() = "string"

    override val runTimeType: ExprValueType
        get() = ExprValueType.STRING
}
