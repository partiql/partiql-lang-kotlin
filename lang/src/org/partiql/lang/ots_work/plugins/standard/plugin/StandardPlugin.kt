package org.partiql.lang.ots_work.plugins.standard.plugin

import org.partiql.lang.eval.ExprValueFactory
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
import org.partiql.lang.ots_work.interfaces.operator.ScalarIsOp
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
import org.partiql.lang.ots_work.plugins.standard.operators.StandardScalarIsOp
import org.partiql.lang.ots_work.plugins.standard.operators.ion
import java.time.ZoneOffset

data class StandardPlugin(
    val typedOpBehavior: TypedOpBehavior = TypedOpBehavior.LEGACY,
    val behaviorWhenDivisorIsZero: BehaviorWhenDivisorIsZero?,
    val defaultTimezoneOffset: ZoneOffset = ZoneOffset.UTC,
    val valueFactory: ExprValueFactory = ExprValueFactory.standard(ion)
) : Plugin {
    override val scalarCastOp = StandardScalarCastOp(
        typedOpBehavior = typedOpBehavior,
        defaultTimezoneOffset = defaultTimezoneOffset,
        valueFactory = valueFactory
    )

    override val scalarIsOp: ScalarIsOp = StandardScalarIsOp(
        typedOpBehavior = typedOpBehavior
    )

    override val binaryPlusOp: BinaryPlusOp = StandardBinaryPlusOp(
        valueFactory = valueFactory
    )

    override val binaryMinusOp: BinaryMinusOp = StandardBinaryMinusOp(
        valueFactory = valueFactory
    )

    override val binaryTimesOp: BinaryTimesOp = StandardBinaryTimesOp(
        valueFactory = valueFactory
    )

    override val binaryDivideOp: BinaryDivideOp = StandardBinaryDivideOp(
        behaviorWhenDivisorIsZero = behaviorWhenDivisorIsZero,
        valueFactory = valueFactory
    )

    override val binaryModuloOp: BinaryModuloOp = StandardBinaryModuloOp(
        valueFactory = valueFactory
    )

    override val posOp: PosOp = StandardPosOp
    override val negOp: NegOp = StandardNegOp(
        valueFactory = valueFactory
    )

    override val binaryConcatOp: BinaryConcatOp = StandardBinaryConcatOp(
        valueFactory = valueFactory
    )

    override val notOp: NotOp = StandardNotOp(
        valueFactory = valueFactory
    )

    override val likeOp: LikeOp = StandardLikeOp(
        valueFactory = valueFactory
    )
}
