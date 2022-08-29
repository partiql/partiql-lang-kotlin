package org.partiql.lang.ots_work.plugins.standard.plugin

import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.ots_work.interfaces.Plugin
import org.partiql.lang.ots_work.interfaces.operators.*
import org.partiql.lang.ots_work.plugins.standard.operators.*
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
