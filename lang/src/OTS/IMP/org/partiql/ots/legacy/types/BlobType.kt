package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.type.ScalarType
import org.partiql.lang.eval.ExprValueType

object BlobType : ScalarType {
    override val id = "blob"

    override val names = listOf("blob")

    override val runTimeType: ExprValueType
        get() = ExprValueType.BLOB
}
