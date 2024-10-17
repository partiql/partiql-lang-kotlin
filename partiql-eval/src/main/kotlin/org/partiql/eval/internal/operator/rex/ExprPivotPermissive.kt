package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.Environment
import org.partiql.eval.internal.helpers.ValueUtility.getText
import org.partiql.eval.operator.Expression
import org.partiql.eval.operator.Relation
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field

internal class ExprPivotPermissive(
    private val input: Relation,
    private val key: Expression,
    private val value: Expression,
) : Expression {

    override fun eval(env: Environment): Datum {
        input.open(env)
        val fields = mutableListOf<Field>()
        while (input.hasNext()) {
            val row = input.next()
            val newEnv = env.push(row)
            val keyString = try {
                val k = key.eval(newEnv)
                k.getText()
            } catch (_: TypeCheckException) {
                continue
            }
            val v = value.eval(newEnv)
            fields.add(Field.of(keyString, v))
        }
        input.close()
        return Datum.struct(fields)
    }
}
