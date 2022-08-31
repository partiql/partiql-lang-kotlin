package org.partiql.lang.ots_work.plugins.standard.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.booleanValue
import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.Failed
import org.partiql.lang.ots_work.interfaces.Successful
import org.partiql.lang.ots_work.interfaces.TypeInferenceResult
import org.partiql.lang.ots_work.interfaces.operator.NotOp
import org.partiql.lang.ots_work.interfaces.type.BoolType
import org.partiql.lang.ots_work.interfaces.type.ScalarType

class StandardNotOp(
    private val valueFactory: ExprValueFactory
) : NotOp() {
    override val defaultReturnTypes: List<CompileTimeType> =
        listOf(BoolType.compileTimeType)

    override val validOperandTypes: List<ScalarType> =
        listOf(BoolType)

    override fun inferType(argType: CompileTimeType): TypeInferenceResult =
        when (argType.scalarType) {
            BoolType -> Successful(argType)
            else -> Failed
        }

    override fun invoke(value: ExprValue): ExprValue {
        return (!value.booleanValue()).exprValue(valueFactory)
    }
}
