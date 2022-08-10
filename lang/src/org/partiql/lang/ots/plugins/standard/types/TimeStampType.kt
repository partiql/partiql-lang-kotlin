package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots.interfaces.ScalarType
import org.partiql.lang.ots.interfaces.TypeParameters

object TimeStampType : ScalarType {
    override val id: String
        get() = "timestamp"

    override val runTimeType: ExprValueType
        get() = ExprValueType.TIMESTAMP

    override fun createType(parameters: TypeParameters): CompileTimeTimestampType = CompileTimeTimestampType
}
