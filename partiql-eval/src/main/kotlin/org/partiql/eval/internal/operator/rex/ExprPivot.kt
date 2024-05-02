package org.partiql.eval.internal.operator.rex

import org.partiql.eval.PQLValue
import org.partiql.eval.StructField
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.ValueUtility.getText
import org.partiql.eval.internal.operator.Operator

internal class ExprPivot(
    private val input: Operator.Relation,
    private val key: Operator.Expr,
    private val value: Operator.Expr,
) : Operator.Expr {

    override fun eval(env: Environment): PQLValue {
        input.open(env)
        val fields = mutableListOf<StructField>()
        while (input.hasNext()) {
            val row = input.next()
            val newEnv = env.push(row)
            val k = key.eval(newEnv)
            val keyString = k.getText()
            val v = value.eval(newEnv)
            fields.add(StructField.of(keyString, v))
        }
        input.close()
        return PQLValue.structValue(fields)
    }
}
