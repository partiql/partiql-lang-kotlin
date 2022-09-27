package org.partiql.lang.ots.interfaces.operator

import org.partiql.lang.ots.interfaces.CompileTimeType
import org.partiql.lang.ots.interfaces.TypeInferenceResult

abstract class UnaryOp : ScalarOp, ArgTypeValidatable {
    abstract fun inferType(argType: CompileTimeType): TypeInferenceResult
}

abstract class PosOp : UnaryOp()
abstract class NegOp : UnaryOp()
abstract class NotOp : UnaryOp()
