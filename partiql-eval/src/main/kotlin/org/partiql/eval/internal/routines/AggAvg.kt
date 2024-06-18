// ktlint-disable filename
@file:Suppress("ClassName", "unused")

package org.partiql.eval.internal.routines

import org.partiql.eval.internal.Aggregation

// internal class AccumulatorAvg(
//     private val targetType: PartiQLValueType = PartiQLValueType.DYNAMIC
// ) : Accumulator() {
//
//     var sum: Number = 0.0
//     var count: Long = 0L
//
//     override fun nextValue(value: Datum) {
//         checkIsNumberType(funcName = "AVG", value = value)
//         this.sum += value.numberValue()
//         this.count += 1L
//     }
//
//     override fun value(): Datum = when (count) {
//         0L -> nullToTargetType(targetType)
//         else -> (sum / bigDecimalOf(count)).toTargetType(targetType)
//     }
// }

internal object AGG_AVG__TINYINT__TINYINT : Aggregation {

    override fun getKey(): String = "AGG_AVG__TINYINT___TINYINT"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_AVG__SMALLINT__SMALLINT : Aggregation {

    override fun getKey(): String = "AGG_AVG__SMALLINT___SMALLINT"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_AVG__INT__INT : Aggregation {

    override fun getKey(): String = "AGG_AVG__INT___INT"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_AVG__BIGINT__BIGINT : Aggregation {

    override fun getKey(): String = "AGG_AVG__BIGINT___BIGINT"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_AVG__NUMERIC__INT : Aggregation {

    override fun getKey(): String = "AGG_AVG__NUMERIC___NUMERIC"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_AVG__DECIMAL__DECIMAL : Aggregation {

    override fun getKey(): String = "AGG_AVG__NUMERIC___NUMERIC"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_AVG__REAL__REAL : Aggregation {

    override fun getKey(): String = "AGG_AVG__REAL___REAL"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_AVG__DOUBLE__DOUBLE : Aggregation {

    override fun getKey(): String = "AGG_AVG__SMALLINT___SMALLINT"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_AVG__DYNAMIC__DYNAMIC : Aggregation {

    override fun getKey(): String = "AGG_AVG__SMALLINT___SMALLINT"

    override fun accumulator() = TODO("Accumulator not implemented")
}
