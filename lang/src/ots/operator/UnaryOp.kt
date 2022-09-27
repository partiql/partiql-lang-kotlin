package ots.operator

import ots.CompileTimeType
import ots.TypeInferenceResult

abstract class UnaryOp : ScalarOp, ArgTypeValidatable {
    abstract fun inferType(argType: CompileTimeType): TypeInferenceResult
}

abstract class PosOp : UnaryOp()
abstract class NegOp : UnaryOp()
abstract class NotOp : UnaryOp()
