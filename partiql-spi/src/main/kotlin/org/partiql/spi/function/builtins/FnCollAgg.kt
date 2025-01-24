// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.function.builtins.internal.Accumulator
import org.partiql.spi.function.builtins.internal.AccumulatorAnySome
import org.partiql.spi.function.builtins.internal.AccumulatorAvgDynamic
import org.partiql.spi.function.builtins.internal.AccumulatorCount
import org.partiql.spi.function.builtins.internal.AccumulatorDistinct
import org.partiql.spi.function.builtins.internal.AccumulatorEvery
import org.partiql.spi.function.builtins.internal.AccumulatorMax
import org.partiql.spi.function.builtins.internal.AccumulatorMin
import org.partiql.spi.function.builtins.internal.AccumulatorSumDynamic
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils

internal abstract class Fn_COLL_AGG__BAG__ANY(
    name: String,
    private var isDistinct: Boolean,
    private var accumulator: () -> Accumulator,
) : FnOverload() {

    private val name: String = FunctionUtils.hide(name)
    private var parameters = arrayOf(Parameter("value", PType.bag()))
    private var returns = PType.dynamic()

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature(
            name,
            parameters.map { it.type }
        )
    }

    override fun getInstance(args: Array<PType>): Fn = instance

    private val instance = Fn.Builder(name)
        .returns(PType.dynamic())
        .addParameters(*parameters)
        .returns(returns)
        .body { args ->
            val bag = args[0]
            val accumulator = when (isDistinct) {
                true -> AccumulatorDistinct(accumulator())
                false -> accumulator()
            }
            bag.forEach { element -> accumulator.next(arrayOf(element)) }
            accumulator.value()
        }
        .build()

    object SUM_ALL : Fn_COLL_AGG__BAG__ANY("coll_sum_all", false, ::AccumulatorSumDynamic)

    object SUM_DISTINCT : Fn_COLL_AGG__BAG__ANY("coll_sum_distinct", true, ::AccumulatorSumDynamic)

    object AVG_ALL : Fn_COLL_AGG__BAG__ANY("coll_avg_all", false, ::AccumulatorAvgDynamic)

    object AVG_DISTINCT : Fn_COLL_AGG__BAG__ANY("coll_avg_distinct", true, ::AccumulatorAvgDynamic)

    object MIN_ALL : Fn_COLL_AGG__BAG__ANY("coll_min_all", false, ::AccumulatorMin)

    object MIN_DISTINCT : Fn_COLL_AGG__BAG__ANY("coll_min_distinct", true, ::AccumulatorMin)

    object MAX_ALL : Fn_COLL_AGG__BAG__ANY("coll_max_all", false, ::AccumulatorMax)

    object MAX_DISTINCT : Fn_COLL_AGG__BAG__ANY("coll_max_distinct", true, ::AccumulatorMax)

    object COUNT_ALL : Fn_COLL_AGG__BAG__ANY("coll_count_all", false, ::AccumulatorCount)

    object COUNT_DISTINCT : Fn_COLL_AGG__BAG__ANY("coll_count_distinct", true, ::AccumulatorCount)

    object EVERY_ALL : Fn_COLL_AGG__BAG__ANY("coll_every_all", false, ::AccumulatorEvery)

    object EVERY_DISTINCT : Fn_COLL_AGG__BAG__ANY("coll_every_distinct", true, ::AccumulatorEvery)

    object ANY_ALL : Fn_COLL_AGG__BAG__ANY("coll_any_all", false, ::AccumulatorAnySome)

    object ANY_DISTINCT : Fn_COLL_AGG__BAG__ANY("coll_any_distinct", true, ::AccumulatorAnySome)

    object SOME_ALL : Fn_COLL_AGG__BAG__ANY("coll_some_all", false, ::AccumulatorAnySome)

    object SOME_DISTINCT : Fn_COLL_AGG__BAG__ANY("coll_some_distinct", true, ::AccumulatorAnySome)
}
