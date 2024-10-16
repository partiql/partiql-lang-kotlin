package org.partiql.eval.internal.operator.rex

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.helpers.ValueUtility.getText
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field

/**
 * Note that PIVOT introduces new bindings so it holds a reference to the stack.
 *
 * @property env
 * @property input
 * @property key
 * @property value
 */
internal class ExprPivot(
    private val env: Environment,
    private val input: Operator.Relation,
    private val key: Operator.Expr,
    private val value: Operator.Expr,
) : Operator.Expr {

    override fun eval(): Datum {
        input.open()
        val fields = mutableListOf<Field>()
        for (row in input) {
            env.scope(row) {
                val k = key.eval().getText()
                val v = value.eval()
                fields.add(Field.of(k, v))
            }
        }
        input.close()
        return Datum.struct(fields)
    }
}

/**
 * Same as PIVOT, but ignore keys which are not text.
 *
 * @property env
 * @property input
 * @property key
 * @property value
 */
internal class ExprPivotPermissive(
    private val env: Environment,
    private val input: Operator.Relation,
    private val key: Operator.Expr,
    private val value: Operator.Expr,
) : Operator.Expr {

    override fun eval(): Datum {
        input.open()
        val fields = mutableListOf<Field>()
        for (row in input) {
            env.scope(row) {
                try {
                    val k = key.eval().getText()
                    val v = value.eval()
                    fields.add(Field.of(k, v))
                } catch (_: TypeCheckException) {
                    // ignore
                }
            }
        }
        input.close()
        return Datum.struct(fields)
    }
}
