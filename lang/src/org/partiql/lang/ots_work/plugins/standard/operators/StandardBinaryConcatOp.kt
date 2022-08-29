package org.partiql.lang.ots_work.plugins.standard.operators

import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.*
import org.partiql.lang.eval.err
import org.partiql.lang.ots_work.interfaces.*
import org.partiql.lang.ots_work.interfaces.operators.BinaryConcatOp
import org.partiql.lang.ots_work.plugins.standard.types.*

class StandardBinaryConcatOp(
    val valueFactory: ExprValueFactory,
    var currentLocationMeta: SourceLocationMeta? = null
) : BinaryConcatOp() {
    override val validOperandTypes: List<ScalarType> = ALL_TEXT_TYPES

    override val defaultReturnTypes: List<CompileTimeType> =
        listOf(StringType.compileTimeType)

    override fun inferType(lType: CompileTimeType, rType: CompileTimeType): TypeInferenceResult {
        val leftType = lType.scalarType
        val rightType = rType.scalarType
        if (leftType !in validOperandTypes || rightType !in validOperandTypes){
            return Failed
        }
        if (leftType === SymbolType || leftType === StringType || rightType === SymbolType || rightType === StringType){
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