package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.ots.interfaces.ScalarType
import org.partiql.lang.ots.interfaces.TypeParameters

object BlobType : ScalarType {
    override val id: String
        get() = "blob"

    override val runTimeType: ExprValueType
        get() = ExprValueType.BLOB

    override fun createType(parameters: TypeParameters): CompileTimeBlobType = CompileTimeBlobType
}
