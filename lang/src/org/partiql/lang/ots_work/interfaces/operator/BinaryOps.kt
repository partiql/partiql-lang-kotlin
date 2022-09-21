package org.partiql.lang.ots_work.interfaces.operator

import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.TypeInferenceResult

abstract class BinaryOp : ScalarOp, ArgTypeValidatable {
    abstract fun inferType(lType: CompileTimeType, rType: CompileTimeType): TypeInferenceResult
}

abstract class BinaryPlusOp : BinaryOp()
abstract class BinaryMinusOp : BinaryOp()
abstract class BinaryTimesOp : BinaryOp()
abstract class BinaryDivideOp : BinaryOp()
abstract class BinaryModuloOp : BinaryOp()
abstract class BinaryConcatOp : BinaryOp()
