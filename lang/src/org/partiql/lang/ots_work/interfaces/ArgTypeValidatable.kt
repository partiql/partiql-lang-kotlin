package org.partiql.lang.ots_work.interfaces

interface ArgTypeValidatable {
    /**
     * Used to check data type mismatch error for any operand.
     */
    // TODO: Will be removed after we support function overloading
    val validOperandTypes: List<ScalarType>
}