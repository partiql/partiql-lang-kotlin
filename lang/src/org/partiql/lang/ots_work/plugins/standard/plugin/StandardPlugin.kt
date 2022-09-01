package org.partiql.lang.ots_work.plugins.standard.plugin

import com.amazon.ion.Timestamp
import org.partiql.lang.ots_work.interfaces.Plugin
import org.partiql.lang.ots_work.interfaces.function.ScalarFunction
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
import org.partiql.lang.ots_work.plugins.standard.functions.Ceil
import org.partiql.lang.ots_work.plugins.standard.functions.Ceiling
import org.partiql.lang.ots_work.plugins.standard.functions.CharLength
import org.partiql.lang.ots_work.plugins.standard.functions.CharacterLength
import org.partiql.lang.ots_work.plugins.standard.functions.DateAdd
import org.partiql.lang.ots_work.plugins.standard.functions.DateDiff
import org.partiql.lang.ots_work.plugins.standard.functions.Extract
import org.partiql.lang.ots_work.plugins.standard.functions.Floor
import org.partiql.lang.ots_work.plugins.standard.functions.FromUnixTime
import org.partiql.lang.ots_work.plugins.standard.functions.Lower
import org.partiql.lang.ots_work.plugins.standard.functions.MakeDate
import org.partiql.lang.ots_work.plugins.standard.functions.MakeTime
import org.partiql.lang.ots_work.plugins.standard.functions.Substring
import org.partiql.lang.ots_work.plugins.standard.functions.ToString
import org.partiql.lang.ots_work.plugins.standard.functions.ToTimestamp
import org.partiql.lang.ots_work.plugins.standard.functions.Trim
import org.partiql.lang.ots_work.plugins.standard.functions.UnixTimestamp
import org.partiql.lang.ots_work.plugins.standard.functions.Upper
import org.partiql.lang.ots_work.plugins.standard.functions.UtcNow
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
import java.time.ZoneOffset

data class StandardPlugin(
    val typedOpBehavior: TypedOpBehavior = TypedOpBehavior.LEGACY,
    val behaviorWhenDivisorIsZero: BehaviorWhenDivisorIsZero? = null,
    val defaultTimezoneOffset: ZoneOffset = ZoneOffset.UTC,
    val now: Timestamp = Timestamp.nowZ()
) : Plugin {
    override val scalarCastOp = StandardScalarCastOp(
        typedOpBehavior = typedOpBehavior,
        defaultTimezoneOffset = defaultTimezoneOffset
    )
    override val scalarIsOp: ScalarIsOp = StandardScalarIsOp(
        typedOpBehavior = typedOpBehavior
    )
    override val binaryPlusOp: BinaryPlusOp = StandardBinaryPlusOp
    override val binaryMinusOp: BinaryMinusOp = StandardBinaryMinusOp
    override val binaryTimesOp: BinaryTimesOp = StandardBinaryTimesOp
    override val binaryDivideOp: BinaryDivideOp = StandardBinaryDivideOp(
        behaviorWhenDivisorIsZero = behaviorWhenDivisorIsZero,
    )
    override val binaryModuloOp: BinaryModuloOp = StandardBinaryModuloOp
    override val posOp: PosOp = StandardPosOp
    override val negOp: NegOp = StandardNegOp
    override val binaryConcatOp: BinaryConcatOp = StandardBinaryConcatOp
    override val notOp: NotOp = StandardNotOp
    override val likeOp: LikeOp = StandardLikeOp
    override val scalarFunctions: List<ScalarFunction> =
        listOf(
            CharacterLength,
            CharLength,
            DateAdd,
            DateDiff,
            Extract,
            FromUnixTime,
            Lower,
            MakeDate,
            MakeTime,
            Substring,
            ToString,
            ToTimestamp,
            Trim,
            Upper,
            UnixTimestamp(now),
            UtcNow(now),
            Ceil,
            Ceiling,
            Floor
        )
}
