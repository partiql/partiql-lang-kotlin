package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import org.partiql.value.structValue

@OptIn(PartiQLValueExperimental::class)
internal class ExprPivotPermissive(
    private val input: Operator.Relation,
    private val key: Operator.Expr,
    private val value: Operator.Expr,
) : Operator.Expr {

    override fun eval(env: Environment): PartiQLValue {
        input.open(env)
        val fields = mutableListOf<Pair<String, PartiQLValue>>()
        while (input.hasNext()) {
            val row = input.next()
            val newEnv = env.nest(row)
            val k = key.eval(newEnv) as? StringValue ?: continue
            val v = value.eval(newEnv)
            fields.add(k.value!! to v)
        }
        input.close()
        return structValue(fields)
    }
}
