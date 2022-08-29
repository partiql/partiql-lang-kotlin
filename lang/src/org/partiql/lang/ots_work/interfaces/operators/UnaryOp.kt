package org.partiql.lang.ots_work.interfaces.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.ots_work.interfaces.ArgTypeValidatable
import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.ScalarOp
import org.partiql.lang.ots_work.interfaces.ScalarOpId
import org.partiql.lang.ots_work.interfaces.TypeInferenceResult

abstract class UnaryOp : ScalarOp, ArgTypeValidatable {
    abstract fun inferType(argType: CompileTimeType): TypeInferenceResult

    abstract fun invoke(value: ExprValue): ExprValue
}

abstract class PosOp : UnaryOp() {
    override val scalarOpId: ScalarOpId = ScalarOpId.Pos
}

abstract class NegOp : UnaryOp() {
    override val scalarOpId: ScalarOpId = ScalarOpId.Neg
}

abstract class NotOp : UnaryOp() {
    override val scalarOpId: ScalarOpId = ScalarOpId.Not
}
