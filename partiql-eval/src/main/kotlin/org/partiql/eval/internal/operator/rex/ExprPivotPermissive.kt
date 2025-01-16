package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.ValueUtility.getText
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorException
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field

internal class ExprPivotPermissive(
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
            val keyString = try {
                val k = key.eval(newEnv)
                k.getText()
            } catch (e: PErrorException) {
                if (e.error.code() == PError.TYPE_UNEXPECTED) {
                    continue
                }
                throw e
            }
            val v = value.eval(newEnv)
            fields.add(Field.of(keyString, v))
        }
        input.close()
        return Datum.struct(fields)
    }
}
