package OTS.ITF.org.partiql.ots.operator

import OTS.ITF.org.partiql.ots.CompileTimeType
import OTS.ITF.org.partiql.ots.TypeInferenceResult
import OTS.ITF.org.partiql.ots.type.ScalarType

interface ScalarOp {
    /**
     * Used to check data type mismatch error for any operand.
     */
    // TODO: Will be removed after we support function overloading
    val validOperandTypes: List<ScalarType>

    /**
     * Return type when we cannot decide the return type (type inference result is [Failed])
     *
     * Empty list leads to type inference result as MISSING
     */
    val defaultReturnTypes: List<CompileTimeType>

    /**
     * Infer the return type
     */
    fun inferReturnType(argsType: List<CompileTimeType>): TypeInferenceResult
}
