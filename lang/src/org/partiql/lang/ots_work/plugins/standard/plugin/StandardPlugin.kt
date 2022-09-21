package org.partiql.lang.ots_work.plugins.standard.plugin

import com.amazon.ion.Timestamp
import org.partiql.lang.ots_work.interfaces.Plugin
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
import org.partiql.lang.ots_work.plugins.standard.operators.StandardBinaryConcatOp
import org.partiql.lang.ots_work.plugins.standard.operators.StandardBinaryDivideOp
import org.partiql.lang.ots_work.plugins.standard.operators.StandardBinaryMinusOp
import org.partiql.lang.ots_work.plugins.standard.operators.StandardBinaryModuloOp
import org.partiql.lang.ots_work.plugins.standard.operators.StandardBinaryPlusOp
import org.partiql.lang.ots_work.plugins.standard.operators.StandardBinaryTimesOp
import org.partiql.lang.ots_work.plugins.standard.operators.StandardLikeOp
import org.partiql.lang.ots_work.plugins.standard.operators.StandardNegOp
import org.partiql.lang.ots_work.plugins.standard.operators.StandardNotOp
import org.partiql.lang.ots_work.plugins.standard.operators.StandardPosOp
import org.partiql.lang.ots_work.plugins.standard.operators.StandardScalarCastOp
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
