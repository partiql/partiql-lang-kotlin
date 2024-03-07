package org.partiql.lang.eval.physical

import org.partiql.lang.eval.physical.operators.ValueExpression

/**
 * A compiled variable binding.
 *
 * @property setFunc The function to be invoked at evaluation-time to set the value of the variable.
 * @property expr The function to be invoked at evaluation-time to compute the value of the variable.
 */
@Deprecated("To be removed in the next major version.", replaceWith = ReplaceWith("VariableBindingAsync"))
class VariableBinding(
    val setFunc: SetVariableFunc,
    val expr: ValueExpression
)
