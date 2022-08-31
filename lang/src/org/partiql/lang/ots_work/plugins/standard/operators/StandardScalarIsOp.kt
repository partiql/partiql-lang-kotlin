package org.partiql.lang.ots_work.plugins.standard.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.operator.ScalarIsOp
import org.partiql.lang.ots_work.plugins.standard.plugin.TypedOpBehavior

// TODO: remove the whole class once [TypedOpBehavior.LEGACY] is removed
class StandardScalarIsOp(
    val typedOpBehavior: TypedOpBehavior = TypedOpBehavior.LEGACY
) : ScalarIsOp() {
    override fun invoke(value: ExprValue, targetType: CompileTimeType): Boolean =
        when (typedOpBehavior) {
            TypedOpBehavior.LEGACY -> value.type == targetType.scalarType.runTimeType
            TypedOpBehavior.HONOR_PARAMETERS -> targetType.scalarType.validateValue(value, targetType.parameters)
        }
}
