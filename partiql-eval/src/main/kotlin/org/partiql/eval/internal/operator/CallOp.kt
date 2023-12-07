package org.partiql.eval.internal.operator

import org.partiql.errors.TypeCheckException
import org.partiql.eval.Expression
import org.partiql.eval.Record
import org.partiql.eval.internal.helpers.toNull
import org.partiql.spi.function.PartiQLFunction
import org.partiql.value.PartiQLValue
import org.partiql.value.missingValue

typealias ScalarInvoke = (Array<PartiQLValue>) -> PartiQLValue

class CallOp(
    private val fn: PartiQLFunction.Scalar,
    private val inputs: Array<Expression>,
) : Expression {

    /**
     * Memoize creation of
     */
    private val nil = fn.signature.returns.toNull()

    override fun evaluate(record: Record): PartiQLValue = try {
        // Evaluate arguments
        val args = inputs.map { input ->
            val r = input.evaluate(record)
            if (r.isNull && fn.signature.isNullCall) return nil()
            r
        }.toTypedArray()
        fn.invoke(args)
    } catch (ex: TypeCheckException) {
        missingValue()
    }
}
