// ktlint-disable filename
@file:Suppress("ClassName", "unused")

package org.partiql.eval.internal.routines

import org.partiql.eval.internal.Aggregation

internal object AGG_MAX__TINYINT__TINYINT : Aggregation {

    override fun getKey(): String = "AGG_MAX__TINYINT___TINYINT"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_MAX__SMALLINT__SMALLINT : Aggregation {

    override fun getKey(): String = "AGG_MAX__SMALLINT___SMALLINT"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_MAX__INT__INT : Aggregation {

    override fun getKey(): String = "AGG_MAX__INT___INT"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_MAX__BIGINT__BIGINT : Aggregation {

    override fun getKey(): String = "AGG_MAX__BIGINT___BIGINT"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_MAX__NUMERIC__INT : Aggregation {

    override fun getKey(): String = "AGG_MAX__NUMERIC___NUMERIC"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_MAX__DECIMAL__DECIMAL : Aggregation {

    override fun getKey(): String = "AGG_MAX__NUMERIC___NUMERIC"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_MAX__REAL__REAL : Aggregation {

    override fun getKey(): String = "AGG_MAX__REAL___REAL"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_MAX__DOUBLE__DOUBLE : Aggregation {

    override fun getKey(): String = "AGG_MAX__SMALLINT___SMALLINT"

    override fun accumulator() = TODO("Accumulator not implemented")
}

internal object AGG_MAX__DYNAMIC__DYNAMIC : Aggregation {

    override fun getKey(): String = "AGG_MAX__SMALLINT___SMALLINT"

    override fun accumulator() = TODO("Accumulator not implemented")
}
