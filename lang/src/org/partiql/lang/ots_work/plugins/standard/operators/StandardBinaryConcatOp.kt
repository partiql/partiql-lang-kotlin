package org.partiql.lang.ots_work.plugins.standard.operators

import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.err
import org.partiql.lang.eval.errorContextFrom
import org.partiql.lang.eval.stringValue
import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.Failed
import org.partiql.lang.ots_work.interfaces.Successful
import org.partiql.lang.ots_work.interfaces.TypeInferenceResult
import org.partiql.lang.ots_work.interfaces.operator.BinaryConcatOp
import org.partiql.lang.ots_work.interfaces.type.ScalarType
import org.partiql.lang.ots_work.plugins.standard.types.CharType
import org.partiql.lang.ots_work.plugins.standard.types.StringType
import org.partiql.lang.ots_work.plugins.standard.types.SymbolType
import org.partiql.lang.ots_work.plugins.standard.types.VarcharType
import org.partiql.lang.ots_work.plugins.standard.valueFactory

object StandardBinaryConcatOp : BinaryConcatOp() {
    var currentLocationMeta: SourceLocationMeta? = null

    override val validOperandTypes: List<ScalarType> = ALL_TEXT_TYPES

    override val defaultReturnTypes: List<CompileTimeType> =
        listOf(StringType.compileTimeType)

    override fun inferType(lType: CompileTimeType, rType: CompileTimeType): TypeInferenceResult {
        val leftType = lType.scalarType
        val rightType = rType.scalarType
        if (leftType !in validOperandTypes || rightType !in validOperandTypes) {
            return Failed
        }
        if (leftType === SymbolType || leftType === StringType || rightType === SymbolType || rightType === StringType) {
            return Successful(StringType.compileTimeType)
        }

        // Here only VARCHAR or CHAR exists
        val leftLength = lType.parameters[0]!!
        val rightLength = rType.parameters[0]!!
        val sumLength = leftLength + rightLength
        val returnType = when {
            leftType === CharType && rightType === CharType -> CharType
            else -> VarcharType
        }

        return Successful(
            CompileTimeType(
                scalarType = returnType,
                parameters = listOf(sumLength)
            )
        )
    }

    override fun invoke(lValue: ExprValue, rValue: ExprValue): ExprValue {
        val lType = lValue.type
        val rType = rValue.type

        return when {
            lType.isText && rType.isText -> (lValue.stringValue() + rValue.stringValue()).exprValue(valueFactory)
            else -> err(
                "Wrong argument type for ||",
                ErrorCode.EVALUATOR_CONCAT_FAILED_DUE_TO_INCOMPATIBLE_TYPE,
                errorContextFrom(currentLocationMeta).also {
                    it[Property.ACTUAL_ARGUMENT_TYPES] = listOf(lType, rType).toString()
                },
                internal = false
            )
        }
    }
}
