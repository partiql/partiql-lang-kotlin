package org.partiql.lang.util.ots_work

import org.partiql.lang.ots.interfaces.CompileTimeType
import org.partiql.lang.ots.interfaces.Plugin
import org.partiql.lang.ots.interfaces.operator.ArgTypeValidatable
import org.partiql.lang.ots.interfaces.operator.BinaryConcatOp
import org.partiql.lang.ots.interfaces.operator.BinaryDivideOp
import org.partiql.lang.ots.interfaces.operator.BinaryMinusOp
import org.partiql.lang.ots.interfaces.operator.BinaryModuloOp
import org.partiql.lang.ots.interfaces.operator.BinaryPlusOp
import org.partiql.lang.ots.interfaces.operator.BinaryTimesOp
import org.partiql.lang.ots.interfaces.operator.LikeOp
import org.partiql.lang.ots.interfaces.operator.NegOp
import org.partiql.lang.ots.interfaces.operator.NotOp
import org.partiql.lang.ots.interfaces.operator.PosOp
import org.partiql.lang.ots.interfaces.operator.ScalarCastOp
import org.partiql.lang.ots.interfaces.operator.ScalarOp
import org.partiql.lang.ots.interfaces.type.ScalarType

internal fun Plugin.getScalarOp(scalarOpId: ScalarOpId): ScalarOp = when (scalarOpId) {
    ScalarOpId.ScalarCast -> scalarCastOp
    ScalarOpId.Pos -> posOp
    ScalarOpId.Neg -> negOp
    ScalarOpId.BinaryPlus -> binaryPlusOp
    ScalarOpId.BinaryMinus -> binaryMinusOp
    ScalarOpId.BinaryTimes -> binaryTimesOp
    ScalarOpId.BinaryDivide -> binaryDivideOp
    ScalarOpId.BinaryModulo -> binaryModuloOp
    ScalarOpId.BinaryConcat -> binaryConcatOp
    ScalarOpId.Not -> notOp
    ScalarOpId.Like -> likeOp
}

internal fun Plugin.inferReturnType(scalarOpId: ScalarOpId, vararg argsType: CompileTimeType) =
    inferReturnType(scalarOpId, argsType.toList())

internal fun Plugin.inferReturnType(scalarOpId: ScalarOpId, argsType: List<CompileTimeType>) =
    when (val scalarOp = getScalarOp(scalarOpId)) {
        is ScalarCastOp -> {
            require(argsType.size == 2) { "Scalar CAST operator expects receiving 2 args" }
            scalarOp.inferType(argsType[0], argsType[1])
        }
        is BinaryPlusOp -> {
            require(argsType.size == 2) { "Binary PLUS operator expects receiving 2 args" }
            scalarOp.inferType(argsType[0], argsType[1])
        }
        is BinaryMinusOp -> {
            require(argsType.size == 2) { "Binary MINUS operator expects receiving 2 args" }
            scalarOp.inferType(argsType[0], argsType[1])
        }
        is BinaryTimesOp -> {
            require(argsType.size == 2) { "Binary TIMES operator expects receiving 2 args" }
            scalarOp.inferType(argsType[0], argsType[1])
        }
        is BinaryDivideOp -> {
            require(argsType.size == 2) { "Binary DIVIDE operator expects receiving 2 args" }
            scalarOp.inferType(argsType[0], argsType[1])
        }
        is BinaryModuloOp -> {
            require(argsType.size == 2) { "Binary MODULO operator expects receiving 2 args" }
            scalarOp.inferType(argsType[0], argsType[1])
        }
        is PosOp -> {
            require(argsType.size == 1) { "POS operator expects receiving 1 arg type" }
            scalarOp.inferType(argsType[0])
        }
        is NegOp -> {
            require(argsType.size == 1) { "POS operator expects receiving 1 arg type" }
            scalarOp.inferType(argsType[0])
        }
        is BinaryConcatOp -> {
            require(argsType.size == 2) { "CONCAT operator expects receiving 2 arg types" }
            scalarOp.inferType(argsType[0], argsType[1])
        }
        is NotOp -> {
            require(argsType.size == 1) { "NOT operator expects receiving 1 arg type" }
            scalarOp.inferType(argsType[0])
        }
        is LikeOp -> {
            require(argsType.size in 2..3) { "NOT operator expects receiving 2~3 arg type" }
            scalarOp.inferType(argsType[0], argsType[1], argsType.getOrNull(2))
        }
        else -> TODO()
    }

internal fun Plugin.defaultReturnTypeOfScalarOp(scalarOpId: ScalarOpId): List<CompileTimeType> =
    getScalarOp(scalarOpId).defaultReturnTypes

/**
 * Used to check data type mismatch error for each operand.
 */
// TODO: Will be removed after we support function overloading
internal fun Plugin.validateOperandType(scalarOpId: ScalarOpId, opScalarType: ScalarType): Boolean =
    when (val scalarOp = getScalarOp(scalarOpId)) {
        is ArgTypeValidatable -> opScalarType in scalarOp.validOperandTypes
        else -> true
    }
