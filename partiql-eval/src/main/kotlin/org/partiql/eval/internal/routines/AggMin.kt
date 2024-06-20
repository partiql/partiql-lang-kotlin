// ktlint-disable filename
@file:Suppress("ClassName", "unused")

package org.partiql.eval.internal.routines

import org.partiql.eval.internal.Aggregation

internal object AGG_MIN__TINYINT__TINYINT : Aggregation {

    override fun getKey(): String = "AGG_MIN__TINYINT___TINYINT"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_MIN__SMALLINT__SMALLINT : Aggregation {

    override fun getKey(): String = "AGG_MIN__SMALLINT___SMALLINT"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_MIN__INT__INT : Aggregation {

    override fun getKey(): String = "AGG_MIN__INT___INT"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_MIN__BIGINT__BIGINT : Aggregation {

    override fun getKey(): String = "AGG_MIN__BIGINT___BIGINT"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_MIN__NUMERIC__INT : Aggregation {

    override fun getKey(): String = "AGG_MIN__NUMERIC___NUMERIC"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_MIN__DECIMAL__DECIMAL : Aggregation {

    override fun getKey(): String = "AGG_MIN__NUMERIC___NUMERIC"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_MIN__REAL__REAL : Aggregation {

    override fun getKey(): String = "AGG_MIN__REAL___REAL"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_MIN__DOUBLE__DOUBLE : Aggregation {

    override fun getKey(): String = "AGG_MIN__SMALLINT___SMALLINT"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_MIN__DYNAMIC__DYNAMIC : Aggregation {

    override fun getKey(): String = "AGG_MIN__SMALLINT___SMALLINT"

    override fun accumulator() = TODO("Accumulator not implemented")
}
