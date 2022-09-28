package OTS.ITF.org.partiql.ots

import OTS.ITF.org.partiql.ots.operator.BinaryConcatOp
import OTS.ITF.org.partiql.ots.operator.BinaryDivideOp
import OTS.ITF.org.partiql.ots.operator.BinaryMinusOp
import OTS.ITF.org.partiql.ots.operator.BinaryModuloOp
import OTS.ITF.org.partiql.ots.operator.BinaryPlusOp
import OTS.ITF.org.partiql.ots.operator.BinaryTimesOp
import OTS.ITF.org.partiql.ots.operator.LikeOp
import OTS.ITF.org.partiql.ots.operator.NegOp
import OTS.ITF.org.partiql.ots.operator.NotOp
import OTS.ITF.org.partiql.ots.operator.PosOp

/**
 * Used to define a plugin
 */
interface Plugin {
    val posOp: PosOp
    val negOp: NegOp
    val binaryPlusOp: BinaryPlusOp
    val binaryMinusOp: BinaryMinusOp
    val binaryTimesOp: BinaryTimesOp
    val binaryDivideOp: BinaryDivideOp
    val binaryModuloOp: BinaryModuloOp
    val binaryConcatOp: BinaryConcatOp
    val notOp: NotOp
    val likeOp: LikeOp

    fun scalarTypeCastInference(sourceType: CompileTimeType, targetType: CompileTimeType): TypeInferenceResult
}
