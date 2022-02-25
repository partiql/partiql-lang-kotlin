package org.partiql.lang.types

import org.partiql.lang.eval.ExprValue

interface Type {
    /**
     * Represents the type signature for this type.
     */
    val typeSignature: TypeSignature

    /**
     * A type name is unique across all PartiQL's global types.
     */
    fun getDisplayName(): String

    /**
     * Converts the Object [value] to [ExprValue]
     */
    fun writeExprValue(value: Any): ExprValue

    /**
     * Read ExprValue [value] and convert it into an [Any]
     */
    fun readExprValue(value: ExprValue): Any

    /**
     * Cast/convert an ExprValue [value] to output a value of this type and convert it to an [ExprValue].
     * This function will be called by PartiQL's evaluator for CASTing a value to a type.
     * For e.g., CAST(<exprValue> AS <type>)
     */
    fun cast(value: ExprValue): ExprValue

    /**
     * Checks if the given exprvalue is of this type. The function is used by PartiQL's evaluator for IS operator.
     */
    fun matches(value: ExprValue): Boolean
}