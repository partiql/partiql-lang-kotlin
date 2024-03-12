package org.partiql.lang.eval.physical

import org.partiql.lang.eval.physical.operators.ValueExpressionAsync

/**
 * A compiled variable binding.
 *
 * @property setFunc The function to be invoked at evaluation-time to set the value of the variable.
 * @property expr The function to be invoked at evaluation-time to compute the value of the variable.
 */
class VariableBindingAsync(
    val setFunc: SetVariableFunc,
    val expr: ValueExpressionAsync
)
