package org.partiql.eval.internal

import org.partiql.eval.value.Datum

internal interface Routine {

    /**
     * Invoke the routine with the given arguments.
     *
     * @param args
     * @return
     */
    fun invoke(args: Array<Datum>): Datum
}
