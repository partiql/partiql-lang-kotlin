package ots

import ots.operator.BinaryConcatOp
import ots.operator.BinaryDivideOp
import ots.operator.BinaryMinusOp
import ots.operator.BinaryModuloOp
import ots.operator.BinaryPlusOp
import ots.operator.BinaryTimesOp
import ots.operator.LikeOp
import ots.operator.NegOp
import ots.operator.NotOp
import ots.operator.PosOp
import ots.operator.ScalarCastOp

/**
 * Used to define a plugin
 */
interface Plugin {
    val scalarCastOp: ScalarCastOp
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
}
