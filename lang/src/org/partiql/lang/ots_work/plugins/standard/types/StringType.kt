package org.partiql.lang.ots_work.plugins.standard.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots_work.interfaces.ScalarType

object StringType : ScalarType {
    override val id: String
        get() = "string"

    override val runTimeType: ExprValueType
        get() = ExprValueType.STRING
}
