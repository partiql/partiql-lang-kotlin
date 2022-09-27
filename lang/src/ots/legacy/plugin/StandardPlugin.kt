package ots.legacy.plugin

import com.amazon.ion.Timestamp
import ots.Plugin
import ots.legacy.operators.StandardBinaryConcatOp
import ots.legacy.operators.StandardBinaryDivideOp
import ots.legacy.operators.StandardBinaryMinusOp
import ots.legacy.operators.StandardBinaryModuloOp
import ots.legacy.operators.StandardBinaryPlusOp
import ots.legacy.operators.StandardBinaryTimesOp
import ots.legacy.operators.StandardLikeOp
import ots.legacy.operators.StandardNegOp
import ots.legacy.operators.StandardNotOp
import ots.legacy.operators.StandardPosOp
import ots.legacy.operators.StandardScalarCastOp
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
import java.time.ZoneOffset

data class StandardPlugin(
    val defaultTimezoneOffset: ZoneOffset = ZoneOffset.UTC,
    val now: Timestamp = Timestamp.nowZ()
) : Plugin {
    override val scalarCastOp = StandardScalarCastOp(
        defaultTimezoneOffset = defaultTimezoneOffset
    )
    override val binaryPlusOp: BinaryPlusOp = StandardBinaryPlusOp
    override val binaryMinusOp: BinaryMinusOp = StandardBinaryMinusOp
    override val binaryTimesOp: BinaryTimesOp = StandardBinaryTimesOp
    override val binaryDivideOp: BinaryDivideOp = StandardBinaryDivideOp
    override val binaryModuloOp: BinaryModuloOp = StandardBinaryModuloOp
    override val posOp: PosOp = StandardPosOp
    override val negOp: NegOp = StandardNegOp
    override val binaryConcatOp: BinaryConcatOp = StandardBinaryConcatOp
    override val notOp: NotOp = StandardNotOp
    override val likeOp: LikeOp = StandardLikeOp
}
