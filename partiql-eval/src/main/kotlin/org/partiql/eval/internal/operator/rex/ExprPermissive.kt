package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.spi.errors.CardinalityViolation
import org.partiql.spi.errors.DataException
import org.partiql.spi.errors.TypeCheckException
import org.partiql.spi.value.Datum
import org.partiql.spi.value.InvalidOperationException

internal class ExprPermissive(private var expr: ExprValue) :
    ExprValue {

    override fun eval(env: Environment): Datum {
        return try {
            expr.eval(env)
        } catch (e: TypeCheckException) {
            Datum.missing()
        } catch (e: CardinalityViolation) {
            Datum.missing()
        } catch (e: InvalidOperationException) {
            Datum.missing()
        } catch (e: DataException) {
            Datum.missing()
        }
    }
}
