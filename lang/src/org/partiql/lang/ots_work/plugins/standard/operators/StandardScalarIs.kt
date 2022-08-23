package org.partiql.lang.ots_work.plugins.standard.operators

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.ots_work.interfaces.CompileTimeType
import org.partiql.lang.ots_work.interfaces.ScalarIs
import org.partiql.lang.ots_work.plugins.standard.plugin.TypedOpBehavior

class StandardScalarIs(
    val typedOpBehavior: TypedOpBehavior = TypedOpBehavior.LEGACY
) : ScalarIs {
    override fun invoke(value: ExprValue, targetType: CompileTimeType): Boolean =
        when (typedOpBehavior) {
            TypedOpBehavior.LEGACY -> value.type == targetType.scalarType.runTimeType
            TypedOpBehavior.HONOR_PARAMETERS -> targetType.scalarType.validateValue(value, targetType.parameters)
        }
}
