package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.ValueUtility.getText
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.PQLValue
import org.partiql.eval.value.StructField

internal class ExprPivotPermissive(
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
            val keyString = try {
                val k = key.eval(newEnv)
                k.getText()
            } catch (_: TypeCheckException) {
                continue
            }
            val v = value.eval(newEnv)
            fields.add(StructField.of(keyString, v))
        }
        input.close()
        return PQLValue.structValue(fields)
    }
}
