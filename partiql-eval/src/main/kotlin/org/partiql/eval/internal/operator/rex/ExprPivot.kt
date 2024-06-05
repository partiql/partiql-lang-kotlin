package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.ValueUtility.getText
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.eval.value.Field

internal class ExprPivot(
    private val input: Operator.Relation,
    private val key: Operator.Expr,
    private val value: Operator.Expr,
) : Operator.Expr {

    override fun eval(env: Environment): Datum {
        input.open(env)
        val fields = mutableListOf<Field>()
        while (input.hasNext()) {
            val row = input.next()
            val newEnv = env.push(row)
            val k = key.eval(newEnv)
            val keyString = k.getText()
            val v = value.eval(newEnv)
            fields.add(Field.of(keyString, v))
        }
        input.close()
        return Datum.structValue(fields)
    }
}
