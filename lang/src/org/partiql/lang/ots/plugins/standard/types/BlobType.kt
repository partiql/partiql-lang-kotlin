package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots.interfaces.ScalarType

object BlobType : ScalarType {
    override val id: String
        get() = "blob"

    override val runTimeType: ExprValueType
        get() = ExprValueType.BLOB
}
