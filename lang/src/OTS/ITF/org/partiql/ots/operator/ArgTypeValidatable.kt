package OTS.ITF.org.partiql.ots.operator

import OTS.ITF.org.partiql.ots.type.ScalarType

interface ArgTypeValidatable {
    /**
     * Used to check data type mismatch error for any operand.
     */
    // TODO: Will be removed after we support function overloading
    val validOperandTypes: List<ScalarType>
}
