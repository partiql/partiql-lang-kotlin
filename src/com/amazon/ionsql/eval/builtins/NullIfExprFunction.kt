package com.amazon.ionsql.eval.builtins

import com.amazon.ion.*
import com.amazon.ionsql.eval.*

/**
 * NullIf built in function. Takes in two arguments, expr1 and expr2, returns null if expr1 = expr2 otherwise returns expr1
 *
 * ```
 * NULLIF(EXPRESSION, EXPRESSION)
 * ```
 */
internal class NullIfExprFunction(val ion: IonSystem) : ArityCheckingTrait, ExprFunction {
    override val name: String = "nullif"
    override val arity: IntRange = (2..2)

    override fun call(env: Environment, args: List<ExprValue>): ExprValue {
        checkArity(args)

        return when {
            args[0].exprEquals(args[1]) -> nullExprValue(ion)
            else                        -> args[0]
        }
    }
}

