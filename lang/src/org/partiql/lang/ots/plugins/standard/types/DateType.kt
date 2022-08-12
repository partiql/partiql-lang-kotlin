package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots.interfaces.ScalarType

object DateType : ScalarType {
    override val id: String
        get() = "date"

    override val runTimeType: ExprValueType
        get() = ExprValueType.DATE
}
