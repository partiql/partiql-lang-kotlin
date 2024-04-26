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

internal class ExprStruct(private val fields: List<Field>) : Operator.Expr {
    @OptIn(PartiQLValueExperimental::class)
    override fun eval(env: Environment): PQLValue {
        val fields = fields.mapNotNull {
            val key = it.key.eval(env).check(PartiQLValueType.STRING) // TODO: Should we allow all text types?
            val value = it.value.eval(env)
            when (value.type) {
                PartiQLValueType.MISSING -> null
                else -> StructField.of(key.stringValue, value)
            }
        }
        return PQLValue.structValue(fields)
    }

    internal class Field(val key: Operator.Expr, val value: Operator.Expr)
}
