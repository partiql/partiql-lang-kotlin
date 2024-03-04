package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.MissingValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.TextValue
import org.partiql.value.check
import org.partiql.value.structValue

internal class ExprStruct(val fields: List<Field>) : Operator.Expr {
    @OptIn(PartiQLValueExperimental::class)
    override fun eval(env: Environment): PartiQLValue {
        val fields = fields.mapNotNull {
            val key = it.key.eval(env).check<TextValue<String>>()
            when (val value = it.value.eval(env)) {
                is MissingValue -> null
                else -> key.value!! to value
            }
        }
        return structValue(fields)
    }

    internal class Field(val key: Operator.Expr, val value: Operator.Expr)
}
