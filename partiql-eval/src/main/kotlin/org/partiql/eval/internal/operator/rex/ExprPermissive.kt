package org.partiql.eval.internal.operator.rex

import org.partiql.errors.CardinalityViolation
import org.partiql.errors.DataException
import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum

internal class ExprPermissive(private var expr: Operator.Expr) : Operator.Expr {

    override fun eval(env: Environment): Datum {
        return try {
            expr.eval(env)
        } catch (e: TypeCheckException) {
            Datum.missing()
        } catch (e: CardinalityViolation) {
            Datum.missing()
        } catch (e: UnsupportedOperationException) {
            Datum.missing()
        } catch (e: DataException) {
            Datum.missing()
        }
    }
}
