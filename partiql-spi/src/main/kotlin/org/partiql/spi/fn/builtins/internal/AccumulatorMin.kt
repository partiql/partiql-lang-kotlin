package org.partiql.spi.fn.builtins.internal

import org.partiql.eval.value.Datum
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal class AccumulatorMin : Accumulator() {

    var min: Datum = Datum.nullValue()

    /**
     * TODO: When we add a Datum comparator, the inefficient jumping between PartiQLValue and Datum can be removed.
     */
    @OptIn(PartiQLValueExperimental::class)
    override fun nextValue(value: Datum) {
        min = Datum.of(comparisonAccumulator(PartiQLValue.comparator(nullsFirst = false))(min.toPartiQLValue(), value.toPartiQLValue()))
    }

    override fun value(): Datum = min
}
