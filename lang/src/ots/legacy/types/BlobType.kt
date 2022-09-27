package ots.legacy.types

import org.partiql.lang.eval.ExprValueType
import ots.type.ScalarType

object BlobType : ScalarType {
    override val id: String
        get() = "blob"

    override val runTimeType: ExprValueType
        get() = ExprValueType.BLOB
}
