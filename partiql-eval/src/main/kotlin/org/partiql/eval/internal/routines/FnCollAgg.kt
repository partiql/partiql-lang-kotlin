// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.connector.sql.builtins.internal.Accumulator
import org.partiql.spi.connector.sql.builtins.internal.AccumulatorAnySome
import org.partiql.spi.connector.sql.builtins.internal.AccumulatorAvg
import org.partiql.spi.connector.sql.builtins.internal.AccumulatorCount
import org.partiql.spi.connector.sql.builtins.internal.AccumulatorEvery
import org.partiql.spi.connector.sql.builtins.internal.AccumulatorMax
import org.partiql.spi.connector.sql.builtins.internal.AccumulatorMin
import org.partiql.spi.connector.sql.builtins.internal.AccumulatorSum
import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.BagValue
import org.partiql.value.Datum
import org.partiql.value.PType.Kind
import org.partiql.value.check


internal abstract class Fn_COLL_AGG__BAG__DYNAMIC : Routine {

    abstract fun getAccumulator(): Accumulator

    companion object {
        @JvmStatic
        internal fun createSignature(name: String) = FnSignature(
            name = name,
            returns = PType.Kind.DYNAMIC,
            parameters = listOf(
                FnParameter("value", PType.Kind.BAG),
            ),
            isNullCall = true,
            isNullable = true
        )
    }

    override fun invoke(args: Array<Datum>): Datum {
        val bag = args[0].check<BagValue<*>>()
        val accumulator = getAccumulator()
        bag.forEach { element -> accumulator.next(arrayOf(element)) }
        return accumulator.value()
    }

    object SUM : Routine_COLL_AGG__BAG__DYNAMIC() {
        override val signature = createSignature("coll_sum")
        override fun getAccumulator(): Accumulator = AccumulatorSum()
    }

    object AVG : Routine_COLL_AGG__BAG__DYNAMIC() {
        override val signature = createSignature("coll_avg")
        override fun getAccumulator(): Accumulator = AccumulatorAvg()
    }

    object MIN : Routine_COLL_AGG__BAG__DYNAMIC() {
        override val signature = createSignature("coll_min")
        override fun getAccumulator(): Accumulator = AccumulatorMin()
    }

    object MAX : Routine_COLL_AGG__BAG__DYNAMIC() {
        override val signature = createSignature("coll_max")
        override fun getAccumulator(): Accumulator = AccumulatorMax()
    }

    object COUNT : Routine_COLL_AGG__BAG__DYNAMIC() {
        override val signature = createSignature("coll_count")
        override fun getAccumulator(): Accumulator = AccumulatorCount()
    }

    object EVERY : Routine_COLL_AGG__BAG__DYNAMIC() {
        override val signature = createSignature("coll_every")
        override fun getAccumulator(): Accumulator = AccumulatorEvery()
    }

    object DYNAMIC : Routine_COLL_AGG__BAG__DYNAMIC() {
        override val signature = createSignature("coll_any")
        override fun getAccumulator(): Accumulator = AccumulatorAnySome()
    }

    object SOME : Routine_COLL_AGG__BAG__DYNAMIC() {
        override val signature = createSignature("coll_some")
        override fun getAccumulator(): Accumulator = AccumulatorAnySome()
    }
}
