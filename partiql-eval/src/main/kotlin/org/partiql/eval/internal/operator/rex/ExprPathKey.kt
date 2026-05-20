package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.eval.internal.helpers.ValueUtility.checkStruct
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal class ExprPathKey(
    @JvmField val root: ExprValue,
    @JvmField val key: ExprValue
) : ExprValue {

    override fun eval(env: Environment): Datum {
        val rootEvaluated = root.eval(env)
        val keyEvaluated = key.eval(env).check(PType.string())
        if (rootEvaluated.isNull || keyEvaluated.isNull) {
            return Datum.nullValue()
        }
        // MAP key access
        if (rootEvaluated.type.code() == PType.MAP) {
            return rootEvaluated.get(keyEvaluated)
        }
        val checkedRoot = rootEvaluated.checkStruct()
        val keyString = keyEvaluated.string
        return checkedRoot.get(keyString) ?: throw PErrors.pathKeyFailureException()
    }
}
