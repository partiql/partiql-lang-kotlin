// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.Function
import org.partiql.spi.fn.Parameter
import org.partiql.spi.fn.builtins.internal.Accumulator
import org.partiql.spi.fn.builtins.internal.AccumulatorAnySome
import org.partiql.spi.fn.builtins.internal.AccumulatorAvg
import org.partiql.spi.fn.builtins.internal.AccumulatorCount
import org.partiql.spi.fn.builtins.internal.AccumulatorDistinct
import org.partiql.spi.fn.builtins.internal.AccumulatorEvery
import org.partiql.spi.fn.builtins.internal.AccumulatorMax
import org.partiql.spi.fn.builtins.internal.AccumulatorMin
import org.partiql.spi.fn.builtins.internal.AccumulatorSum
import org.partiql.types.PType

internal abstract class Fn_COLL_AGG__BAG__ANY(
    name: String,
    private val isDistinct: Boolean,
    private val accumulator: () -> Accumulator,
) : Function {

    private fun getAccumulator(): Accumulator = when (isDistinct) {
        true -> AccumulatorDistinct(accumulator.invoke())
        false -> accumulator.invoke()
    }

    override val signature: FnSignature = createSignature(name)

    companion object {
        @JvmStatic
        internal fun createSignature(name: String) = FnSignature(
            name = name,
            returns = PType.dynamic(),
            parameters = listOf(
                Parameter("value", PType.bag()),
            ),
            isNullCall = true,
            isNullable = true
        )
    }

    override fun invoke(args: Array<Datum>): Datum {
        val bag = args[0]
        val accumulator = getAccumulator()
        bag.forEach { element -> accumulator.next(arrayOf(element)) }
        return accumulator.value()
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
