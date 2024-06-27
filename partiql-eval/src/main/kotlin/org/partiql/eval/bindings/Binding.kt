package org.partiql.eval.bindings

import org.partiql.eval.value.Datum

public interface Binding {

    /**
     * Return the datum for this binding.
     */
    public fun getDatum(): Datum
}
