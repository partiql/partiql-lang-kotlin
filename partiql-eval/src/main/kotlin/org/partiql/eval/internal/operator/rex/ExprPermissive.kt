package org.partiql.eval.internal.operator.rex

import org.partiql.errors.CardinalityViolation
import org.partiql.errors.TypeCheckException
import org.partiql.eval.PQLValue
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator

internal class ExprPermissive(
    val target: Operator.Expr
) : Operator.Expr {

    override fun eval(env: Environment): PQLValue {
        return try {
            target.eval(env)
        } catch (e: TypeCheckException) {
            PQLValue.missingValue()
        } catch (e: CardinalityViolation) {
            PQLValue.missingValue()
        }
    }
}
