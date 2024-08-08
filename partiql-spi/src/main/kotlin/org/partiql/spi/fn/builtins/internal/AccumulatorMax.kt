package org.partiql.spi.fn.builtins.internal

import org.partiql.eval.value.Datum
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal class AccumulatorMax : Accumulator() {

    var max: Datum = Datum.nullValue()

    /**
     * TODO: When we add a Datum comparator, the inefficient jumping between PartiQLValue and Datum can be removed.
     */
    @OptIn(PartiQLValueExperimental::class)
    override fun nextValue(value: Datum) {
        max = Datum.of(comparisonAccumulator(PartiQLValue.comparator(nullsFirst = true).reversed())(max.toPartiQLValue(), value.toPartiQLValue()))
    }

    override fun value(): Datum = max
}
