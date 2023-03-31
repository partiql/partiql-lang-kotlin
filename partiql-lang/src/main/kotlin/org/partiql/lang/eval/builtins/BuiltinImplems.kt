package org.partiql.lang.eval.builtins

import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.stringValue

internal fun characterLength(value: ExprValue): Int {
    val str = value.stringValue()
    return str.codePointCount(0, str.length)
}
