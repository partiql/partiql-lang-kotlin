package org.partiql.lang.ots_work.interfaces.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.ots_work.interfaces.*

abstract class BinaryOp: ScalarOp, ArgTypeValidatable {
    abstract fun inferType(lType: CompileTimeType, rType: CompileTimeType): TypeInferenceResult

    abstract fun invoke(lValue: ExprValue, rValue: ExprValue): ExprValue
}

abstract class BinaryPlusOp: BinaryOp() {
    override val scalarOpId: ScalarOpId = ScalarOpId.BinaryPlus
}

abstract class BinaryMinusOp: BinaryOp() {
    override val scalarOpId: ScalarOpId = ScalarOpId.BinaryMinus
}

abstract class BinaryTimesOp: BinaryOp() {
    override val scalarOpId: ScalarOpId = ScalarOpId.BinaryTimes
}

abstract class BinaryDivideOp: BinaryOp() {
    override val scalarOpId: ScalarOpId = ScalarOpId.BinaryDivide
}

abstract class BinaryModuloOp: BinaryOp() {
    override val scalarOpId: ScalarOpId = ScalarOpId.BinaryModulo
}

abstract class BinaryConcatOp: BinaryOp() {
    override val scalarOpId: ScalarOpId = ScalarOpId.BinaryConcat
}