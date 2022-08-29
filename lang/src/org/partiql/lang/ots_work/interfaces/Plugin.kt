package org.partiql.lang.ots_work.interfaces

import org.partiql.lang.ots_work.interfaces.operators.BinaryConcatOp
import org.partiql.lang.ots_work.interfaces.operators.BinaryDivideOp
import org.partiql.lang.ots_work.interfaces.operators.BinaryMinusOp
import org.partiql.lang.ots_work.interfaces.operators.BinaryModuloOp
import org.partiql.lang.ots_work.interfaces.operators.BinaryPlusOp
import org.partiql.lang.ots_work.interfaces.operators.BinaryTimesOp
import org.partiql.lang.ots_work.interfaces.operators.LikeOp
import org.partiql.lang.ots_work.interfaces.operators.NegOp
import org.partiql.lang.ots_work.interfaces.operators.NotOp
import org.partiql.lang.ots_work.interfaces.operators.PosOp
import org.partiql.lang.ots_work.interfaces.operators.ScalarCastOp
import org.partiql.lang.ots_work.interfaces.operators.ScalarIsOp

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
