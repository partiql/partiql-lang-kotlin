package org.partiql.lang.ots_work.interfaces

import org.partiql.lang.ots_work.interfaces.operator.BinaryConcatOp
import org.partiql.lang.ots_work.interfaces.operator.BinaryDivideOp
import org.partiql.lang.ots_work.interfaces.operator.BinaryMinusOp
import org.partiql.lang.ots_work.interfaces.operator.BinaryModuloOp
import org.partiql.lang.ots_work.interfaces.operator.BinaryPlusOp
import org.partiql.lang.ots_work.interfaces.operator.BinaryTimesOp
import org.partiql.lang.ots_work.interfaces.operator.LikeOp
import org.partiql.lang.ots_work.interfaces.operator.NegOp
import org.partiql.lang.ots_work.interfaces.operator.NotOp
import org.partiql.lang.ots_work.interfaces.operator.PosOp
import org.partiql.lang.ots_work.interfaces.operator.ScalarCastOp

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
