package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * Represents a single branch of a <case> or <searched case> expression.
 *
 * @param value
 * @param result
 */
internal class ExprCaseBranch(value: ExprValue, result: ExprValue) {

    // DO NOT USE FINAL
    private var _value = value
    private var _result = result

    /**
     * Evaluate the branch and compare against the match; returning the _value if the match succeeds.
     */
    fun eval(env: Environment): Datum? {
        val v = _value.eval(env)
        if (v.type.code() == PType.BOOL && !v.isNull && !v.isMissing && v.boolean) {
            return _result.eval(env)
        }
        return null
    }

    /**
     * Evaluate the branch and compare against the match; returning the _value if the match succeeds.
     */
    fun eval(env: Environment, match: Datum): Datum? {
        return if (equal(match, _value.eval(env))) _result.eval(env) else null
    }

    private companion object {

        @JvmStatic
        private val comparator = Datum.comparator()

        @JvmStatic
        private fun equal(lhs: Datum, rhs: Datum) = comparator.compare(lhs, rhs) == 0
    }
}
