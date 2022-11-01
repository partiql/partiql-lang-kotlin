package OTS.ITF.org.partiql.ots

import OTS.ITF.org.partiql.ots.operator.ScalarOp
import OTS.ITF.org.partiql.ots.type.ScalarType

/**
 * Used to define a plugin
 */
interface Plugin {
    /**
     * All the scalar types
     */
    val scalarTypes: List<ScalarType>

    val posOp: ScalarOp
    val negOp: ScalarOp
    val binaryPlusOp: ScalarOp
    val binaryMinusOp: ScalarOp
    val binaryTimesOp: ScalarOp
    val binaryDivideOp: ScalarOp
    val binaryModuloOp: ScalarOp
    val binaryConcatOp: ScalarOp
    val notOp: ScalarOp
    val likeOp: ScalarOp

    fun scalarTypeCastInference(sourceType: CompileTimeType, targetType: CompileTimeType): TypeInferenceResult
}
