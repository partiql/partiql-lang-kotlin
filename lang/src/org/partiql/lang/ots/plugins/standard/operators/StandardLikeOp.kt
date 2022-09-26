package org.partiql.lang.ots.plugins.standard.operators

import org.partiql.lang.ots.interfaces.CompileTimeType
import org.partiql.lang.ots.interfaces.Failed
import org.partiql.lang.ots.interfaces.Successful
import org.partiql.lang.ots.interfaces.TypeInferenceResult
import org.partiql.lang.ots.interfaces.Uncertain
import org.partiql.lang.ots.interfaces.operator.LikeOp
import org.partiql.lang.ots.interfaces.type.BoolType
import org.partiql.lang.ots.interfaces.type.ScalarType

object StandardLikeOp : LikeOp() {
    override val validOperandTypes: List<ScalarType> = ALL_TEXT_TYPES

    override fun inferType(value: CompileTimeType, pattern: CompileTimeType, escape: CompileTimeType?): TypeInferenceResult =
        when {
            value.scalarType !in validOperandTypes || pattern.scalarType !in validOperandTypes -> Failed
            escape === null -> Successful(BoolType.compileTimeType)
            escape.scalarType in validOperandTypes -> Uncertain(BoolType.compileTimeType)
            else -> Failed
        }
}
