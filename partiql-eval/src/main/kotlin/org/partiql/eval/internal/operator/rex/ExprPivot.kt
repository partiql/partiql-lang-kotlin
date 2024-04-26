package org.partiql.eval.internal.operator.rex

import org.partiql.eval.PQLValue
import org.partiql.eval.StructField
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.ValueUtility.check
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.check
import org.partiql.value.structValue

@OptIn(PartiQLValueExperimental::class)
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
            val k = key.eval(newEnv).check(PartiQLValueType.STRING)
            val v = value.eval(newEnv)
            fields.add(StructField.of(k.stringValue, v))
        }
        input.close()
        return PQLValue.structValue(fields)
    }
}
