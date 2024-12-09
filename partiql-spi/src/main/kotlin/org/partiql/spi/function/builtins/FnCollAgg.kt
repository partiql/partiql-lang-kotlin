// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.Accumulator
import org.partiql.spi.function.builtins.internal.AccumulatorAnySome
import org.partiql.spi.function.builtins.internal.AccumulatorAvg
import org.partiql.spi.function.builtins.internal.AccumulatorCount
import org.partiql.spi.function.builtins.internal.AccumulatorDistinct
import org.partiql.spi.function.builtins.internal.AccumulatorEvery
import org.partiql.spi.function.builtins.internal.AccumulatorMax
import org.partiql.spi.function.builtins.internal.AccumulatorMin
import org.partiql.spi.function.builtins.internal.AccumulatorSum
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal abstract class Fn_COLL_AGG__BAG__ANY(
    private var name: String,
    private var isDistinct: Boolean,
    private var accumulator: () -> Accumulator,
) : Function {

    private var parameters = arrayOf(Parameter("value", PType.bag()))
    private var returns = PType.dynamic()

    override fun getName(): String = name
    override fun getParameters(): Array<Parameter> = parameters
    override fun getReturnType(args: Array<PType>): PType = returns
    override fun getInstance(args: Array<PType>): Function.Instance = instance

    private val instance = object : Function.Instance(
        name,
        parameters = arrayOf(PType.bag()),
        returns = PType.dynamic(),
    ) {
        override fun invoke(args: Array<Datum>): Datum {
            val bag = args[0]
            val accumulator = when (isDistinct) {
                true -> AccumulatorDistinct(accumulator())
                false -> accumulator()
            }
            bag.forEach { element -> accumulator.next(arrayOf(element)) }
            return accumulator.value()
        }
    }

    object SUM_ALL : Fn_COLL_AGG__BAG__ANY("coll_sum_all", false, ::AccumulatorSum)

    object SUM_DISTINCT : Fn_COLL_AGG__BAG__ANY("coll_sum_distinct", true, ::AccumulatorSum)

    object AVG_ALL : Fn_COLL_AGG__BAG__ANY("coll_avg_all", false, ::AccumulatorAvg)

    object AVG_DISTINCT : Fn_COLL_AGG__BAG__ANY("coll_avg_distinct", true, ::AccumulatorAvg)

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
