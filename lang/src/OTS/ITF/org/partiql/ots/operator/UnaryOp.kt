package OTS.ITF.org.partiql.ots.operator

import OTS.ITF.org.partiql.ots.CompileTimeType
import OTS.ITF.org.partiql.ots.TypeInferenceResult

abstract class UnaryOp : ScalarOp, ArgTypeValidatable {
    abstract fun inferType(argType: CompileTimeType): TypeInferenceResult
}

abstract class PosOp : UnaryOp()
abstract class NegOp : UnaryOp()
abstract class NotOp : UnaryOp()
