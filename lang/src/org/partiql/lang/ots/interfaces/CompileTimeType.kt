package org.partiql.lang.ots.interfaces

import org.partiql.lang.eval.ExprValue

interface CompileTimeType {
    val type: ScalarType

    /**
     * used to validate a value of this type
     */
    fun validateValue(value: ExprValue): Boolean
}
