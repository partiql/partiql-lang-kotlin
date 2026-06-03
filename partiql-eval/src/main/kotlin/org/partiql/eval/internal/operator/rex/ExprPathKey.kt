package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal class ExprPathKey(
    @JvmField val root: ExprValue,
    @JvmField val key: ExprValue
) : ExprValue {

    private val mapOp = ExprPathKeyMap(root, key)
    private val structOp = ExprPathKeyStruct(root, key)

    override fun eval(env: Environment): Datum {
        val input = root.eval(env)
        return when (input.type.code()) {
            PType.MAP -> mapOp.evalWithInput(input, env)
            else -> structOp.evalWithInput(input, env)
        }
    }
}
