package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.ValueUtility.getText
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field

internal class ExprPivot(
    private val input: ExprRelation,
    private val key: ExprValue,
    private val value: ExprValue,
) : ExprValue {

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
        return Datum.struct(fields)
    }
}
