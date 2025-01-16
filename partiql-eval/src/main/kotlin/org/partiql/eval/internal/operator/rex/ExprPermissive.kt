package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorException
import org.partiql.spi.value.Datum
import org.partiql.spi.value.InvalidOperationException

internal class ExprPermissive(private var expr: ExprValue) :
    ExprValue {

    override fun eval(env: Environment): Datum {
        return try {
            expr.eval(env)
        } catch (e: PErrorException) {
            val code = e.error.code()
            when (code) {
                PError.FUNCTION_NOT_FOUND,
                PError.FUNCTION_TYPE_MISMATCH,
                PError.CARDINALITY_VIOLATION,
                PError.NUMERIC_VALUE_OUT_OF_RANGE,
                PError.PATH_INDEX_NEVER_SUCCEEDS,
                PError.PATH_SYMBOL_NEVER_SUCCEEDS,
                PError.PATH_KEY_NEVER_SUCCEEDS -> Datum.missing()
                else -> throw e
            }
        } catch (e: InvalidOperationException) {
            Datum.missing()
        }
    }
}
