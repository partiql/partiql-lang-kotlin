package OTS.IMP.org.partiql.ots.legacy.plugin

import OTS.IMP.org.partiql.ots.legacy.operators.StandardBinaryConcatOp
import OTS.IMP.org.partiql.ots.legacy.operators.StandardBinaryDivideOp
import OTS.IMP.org.partiql.ots.legacy.operators.StandardBinaryMinusOp
import OTS.IMP.org.partiql.ots.legacy.operators.StandardBinaryModuloOp
import OTS.IMP.org.partiql.ots.legacy.operators.StandardBinaryPlusOp
import OTS.IMP.org.partiql.ots.legacy.operators.StandardBinaryTimesOp
import OTS.IMP.org.partiql.ots.legacy.operators.StandardLikeOp
import OTS.IMP.org.partiql.ots.legacy.operators.StandardNegOp
import OTS.IMP.org.partiql.ots.legacy.operators.StandardNotOp
import OTS.IMP.org.partiql.ots.legacy.operators.StandardPosOp
import OTS.IMP.org.partiql.ots.legacy.operators.StandardScalarCastOp
import OTS.ITF.org.partiql.ots.Plugin
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
import com.amazon.ion.Timestamp
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
