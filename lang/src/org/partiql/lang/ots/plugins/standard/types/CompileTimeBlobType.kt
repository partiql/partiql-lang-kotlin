package org.partiql.lang.ots.plugins.standard.types

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.ots.interfaces.CompileTimeType
import org.partiql.lang.ots.interfaces.ScalarType

object CompileTimeBlobType : CompileTimeType {
    override val type: ScalarType = BlobType

    override fun validateValue(value: ExprValue): Boolean =
        value.type == type.runTimeType
}
