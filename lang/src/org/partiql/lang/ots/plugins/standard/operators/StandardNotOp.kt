package org.partiql.lang.ots.plugins.standard.operators

import org.partiql.lang.ots.interfaces.CompileTimeType
import org.partiql.lang.ots.interfaces.Failed
import org.partiql.lang.ots.interfaces.Successful
import org.partiql.lang.ots.interfaces.TypeInferenceResult
import org.partiql.lang.ots.interfaces.operator.NotOp
import org.partiql.lang.ots.interfaces.type.BoolType
import org.partiql.lang.ots.interfaces.type.ScalarType

object StandardNotOp : NotOp() {
    override val defaultReturnTypes: List<CompileTimeType> =
        listOf(BoolType.compileTimeType)

    override val validOperandTypes: List<ScalarType> =
        listOf(BoolType)

    override fun inferType(argType: CompileTimeType): TypeInferenceResult =
        when (argType.scalarType) {
            BoolType -> Successful(argType)
            else -> Failed
        }
}
