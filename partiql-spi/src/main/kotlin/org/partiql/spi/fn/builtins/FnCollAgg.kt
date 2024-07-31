// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.spi.fn.builtins.internal.Accumulator
import org.partiql.spi.fn.builtins.internal.AccumulatorAnySome
import org.partiql.spi.fn.builtins.internal.AccumulatorAvg
import org.partiql.spi.fn.builtins.internal.AccumulatorCount
import org.partiql.spi.fn.builtins.internal.AccumulatorEvery
import org.partiql.spi.fn.builtins.internal.AccumulatorMax
import org.partiql.spi.fn.builtins.internal.AccumulatorMin
import org.partiql.spi.fn.builtins.internal.AccumulatorSum
import org.partiql.value.BagValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.check

@OptIn(PartiQLValueExperimental::class)
internal abstract class Fn_COLL_AGG__BAG__ANY : Fn {

    abstract fun getAccumulator(): Agg.Accumulator

    companion object {
        @JvmStatic
        internal fun createSignature(name: String) = FnSignature(
            name = name,
            returns = PartiQLValueType.ANY,
            parameters = listOf(
                FnParameter("value", PartiQLValueType.BAG),
            ),
            isNullCall = true,
            isNullable = true
        )
    }

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val bag = args[0].check<BagValue<*>>()
        val accumulator = getAccumulator()
        bag.forEach { element -> accumulator.next(arrayOf(element)) }
        return accumulator.value()
    }

    object SUM : Fn_COLL_AGG__BAG__ANY() {
        override val signature = createSignature("coll_sum")
        override fun getAccumulator(): Accumulator = AccumulatorSum()
    }

    object AVG : Fn_COLL_AGG__BAG__ANY() {
        override val signature = createSignature("coll_avg")
        override fun getAccumulator(): Accumulator = AccumulatorAvg()
    }

    object MIN : Fn_COLL_AGG__BAG__ANY() {
        override val signature = createSignature("coll_min")
        override fun getAccumulator(): Accumulator = AccumulatorMin()
    }

    object MAX : Fn_COLL_AGG__BAG__ANY() {
        override val signature = createSignature("coll_max")
        override fun getAccumulator(): Accumulator = AccumulatorMax()
    }

    object COUNT : Fn_COLL_AGG__BAG__ANY() {
        override val signature = createSignature("coll_count")
        override fun getAccumulator(): Accumulator = AccumulatorCount()
    }

    object EVERY : Fn_COLL_AGG__BAG__ANY() {
        override val signature = createSignature("coll_every")
        override fun getAccumulator(): Accumulator = AccumulatorEvery()
    }

    object ANY : Fn_COLL_AGG__BAG__ANY() {
        override val signature = createSignature("coll_any")
        override fun getAccumulator(): Accumulator = AccumulatorAnySome()
    }

    object SOME : Fn_COLL_AGG__BAG__ANY() {
        override val signature = createSignature("coll_some")
        override fun getAccumulator(): Accumulator = AccumulatorAnySome()
    }
}
