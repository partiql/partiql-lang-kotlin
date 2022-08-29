package org.partiql.lang.ots_work.interfaces

import org.partiql.lang.ots_work.interfaces.operators.*

/**
 * Used to define a plugin
 */
interface Plugin {
    val scalarCastOp: ScalarCastOp
    val scalarIsOp: ScalarIsOp
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
