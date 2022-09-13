package org.partiql.lang.ots_work.plugins.standard.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots_work.interfaces.type.ScalarType

object ClobType : ScalarType {
    override val id: String
        get() = "clob"

    override val runTimeType: ExprValueType
        get() = ExprValueType.CLOB
}
