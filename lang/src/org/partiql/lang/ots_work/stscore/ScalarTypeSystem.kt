package org.partiql.lang.ots_work.stscore

import com.amazon.ionelement.api.MetaContainer
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.Plugin
import org.partiql.lang.ots_work.interfaces.operator.ArgTypeValidatable
import org.partiql.lang.ots_work.interfaces.operator.BinaryConcatOp
import org.partiql.lang.ots_work.interfaces.operator.BinaryDivideOp
import org.partiql.lang.ots_work.interfaces.operator.BinaryMinusOp
import org.partiql.lang.ots_work.interfaces.operator.BinaryModuloOp
import org.partiql.lang.ots_work.interfaces.operator.BinaryPlusOp
import org.partiql.lang.ots_work.interfaces.operator.BinaryTimesOp
import org.partiql.lang.ots_work.interfaces.operator.LikeOp
import org.partiql.lang.ots_work.interfaces.operator.NegOp
import org.partiql.lang.ots_work.interfaces.operator.NotOp
import org.partiql.lang.ots_work.interfaces.operator.PosOp
import org.partiql.lang.ots_work.interfaces.operator.ScalarCastOp
import org.partiql.lang.ots_work.interfaces.operator.ScalarIsOp
import org.partiql.lang.ots_work.interfaces.operator.ScalarOp
import org.partiql.lang.ots_work.interfaces.operator.ScalarOpId
import org.partiql.lang.ots_work.interfaces.type.ScalarType
import org.partiql.lang.ots_work.plugins.standard.operators.StandardBinaryConcatOp
import org.partiql.lang.ots_work.plugins.standard.operators.StandardBinaryDivideOp
import org.partiql.lang.ots_work.plugins.standard.operators.StandardLikeOp
import org.partiql.lang.ots_work.plugins.standard.operators.StandardScalarCastOp

/**
 * [plugin] is the plugin that a PartiQL scalar type system uses. For now, let's assume there is only one plugin existed in the type system.
 */
class ScalarTypeSystem(
    // TODO remove dependencies of the following field from `org.partiql.lang` package and make it private
    val plugin: Plugin
) {
    private fun getScalarOp(scalarOpId: ScalarOpId): ScalarOp = when (scalarOpId) {
        ScalarOpId.ScalarCast -> plugin.scalarCastOp
        ScalarOpId.ScalarIs -> plugin.scalarIsOp
        ScalarOpId.Pos -> plugin.posOp
        ScalarOpId.Neg -> plugin.negOp
        ScalarOpId.BinaryPlus -> plugin.binaryPlusOp
        ScalarOpId.BinaryMinus -> plugin.binaryMinusOp
        ScalarOpId.BinaryTimes -> plugin.binaryTimesOp
        ScalarOpId.BinaryDivide -> plugin.binaryDivideOp
        ScalarOpId.BinaryModulo -> plugin.binaryModuloOp
        ScalarOpId.BinaryConcat -> plugin.binaryConcatOp
        ScalarOpId.Not -> plugin.notOp
        ScalarOpId.Like -> plugin.likeOp
    }

    internal fun inferReturnType(scalarOpId: ScalarOpId, vararg argsType: CompileTimeType) =
        inferReturnType(scalarOpId, argsType.toList())

    internal fun inferReturnType(scalarOpId: ScalarOpId, argsType: List<CompileTimeType>) =
        when (val scalarOp = getScalarOp(scalarOpId)) {
            is ScalarCastOp -> {
                require(argsType.size == 2) { "Scalar CAST operator expects receiving 2 args" }
                scalarOp.inferType(argsType[0], argsType[1])
            }
            is ScalarIsOp -> error("IS operator has well defined semantics which returns BOOLEAN type")
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

    internal fun defaultReturnTypeOfScalarOp(scalarOpId: ScalarOpId): List<CompileTimeType> =
        getScalarOp(scalarOpId).defaultReturnTypes

    /**
     * Used to check data type mismatch error for each operand.
     */
    // TODO: Will be removed after we support function overloading
    internal fun validateOperandType(scalarOpId: ScalarOpId, opScalarType: ScalarType): Boolean =
        when (val scalarOp = getScalarOp(scalarOpId)) {
            is ArgTypeValidatable -> opScalarType in scalarOp.validOperandTypes
            else -> true
        }

    // TODO: Remove `invokeCastOp()` and make it compatible `invokeScalarOp()`
    internal fun invokeScalarCastOp(value: ExprValue, targetType: CompileTimeType, locationMeta: SourceLocationMeta?): ExprValue? {
        // TODO: remove the hard-coded cast below and properly catch the error and add location meta to that error and throw it again.
        (plugin.scalarCastOp as StandardScalarCastOp).currentLocationMeta = locationMeta

        return plugin.scalarCastOp.invoke(value, targetType)
    }

    // TODO: remove this method once [TypedOpBehavior.Legacy] is removed
    internal fun invokeIsOp(value: ExprValue, targetType: CompileTimeType): Boolean =
        plugin.scalarIsOp.invoke(value, targetType)

    // TODO: Merge this method into `invokeScalarOp` once we deal with metas in error handling correctly
    internal fun invokeLikeOp(
        value: ExprValue,
        pattern: ExprValue,
        escape: ExprValue?,
        metas: MetaContainer?,
        patternLocationMeta: SourceLocationMeta?,
        escapeLocationMeta: SourceLocationMeta?,
    ): ExprValue {
        val likeOp = (plugin.likeOp as StandardLikeOp)

        likeOp.metas = metas
        likeOp.patternLocationMeta = patternLocationMeta
        likeOp.escapeLocationMeta = escapeLocationMeta

        return plugin.likeOp.invoke(value, pattern, escape)
    }

    internal fun invokeScalarOp(opId: ScalarOpId, vararg args: ExprValue, locationMeta: SourceLocationMeta? = null): ExprValue =
        invokeScalarOp(opId, args.toList(), locationMeta)

    internal fun invokeScalarOp(opId: ScalarOpId, args: List<ExprValue>, locationMeta: SourceLocationMeta? = null): ExprValue =
        when (val scalarOp = getScalarOp(opId)) {
            is ScalarCastOp -> error("Please call `invokeCastOp()` for now")
            is ScalarIsOp -> error("Please call `invokeIsOp()` for now")
            is BinaryPlusOp -> {
                require(args.size == 2) { "Binary PLUS operator expects receiving 2 args" }
                scalarOp.invoke(args[0], args[1])
            }
            is BinaryMinusOp -> {
                require(args.size == 2) { "Binary MINUS operator expects receiving 2 args" }
                scalarOp.invoke(args[0], args[1])
            }
            is BinaryTimesOp -> {
                require(args.size == 2) { "Binary TIMES operator expects receiving 2 args" }
                scalarOp.invoke(args[0], args[1])
            }
            is BinaryDivideOp -> {
                require(args.size == 2) { "Binary DIVIDE operator expects receiving 2 args" }

                // TODO: remove the hard-coded cast below and properly catch the error and add location meta to that error and throw it again.
                (plugin.binaryDivideOp as StandardBinaryDivideOp).currentLocationMeta = locationMeta

                scalarOp.invoke(args[0], args[1])
            }
            is BinaryModuloOp -> {
                require(args.size == 2) { "Binary MODULO operator expects receiving 2 args" }
                scalarOp.invoke(args[0], args[1])
            }
            is PosOp -> {
                require(args.size == 1) { "POS operator expects receiving 1 arg" }
                scalarOp.invoke(args[0])
            }
            is NegOp -> {
                require(args.size == 1) { "POS operator expects receiving 1 arg" }
                scalarOp.invoke(args[0])
            }
            is BinaryConcatOp -> {
                require(args.size == 2) { "CONCAT operator expects receiving 2 args" }

                // TODO: remove the hard-coded cast below and properly catch the error and add location meta to that error and throw it again.
                (plugin.binaryConcatOp as StandardBinaryConcatOp).currentLocationMeta = locationMeta

                scalarOp.invoke(args[0], args[1])
            }
            is NotOp -> {
                require(args.size == 1) { "NOT operator expects receiving 1 arg" }
                scalarOp.invoke(args[0])
            }
            is LikeOp -> TODO("Unsupported for now")
            else -> error("Unrecognized scalar operator: $scalarOp")
        }

    internal val scalarFunctions = plugin.scalarFunctions
}
