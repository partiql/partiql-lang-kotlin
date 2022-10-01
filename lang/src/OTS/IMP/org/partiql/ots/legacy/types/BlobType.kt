package OTS.IMP.org.partiql.ots.legacy.types

import OTS.ITF.org.partiql.ots.type.NonParametricType
import org.partiql.lang.eval.ExprValueType

object BlobType : NonParametricType() {
    override val typeName = "blob"

    override val aliases = listOf("blob")

    override val runTimeType: ExprValueType
        get() = ExprValueType.BLOB
}
