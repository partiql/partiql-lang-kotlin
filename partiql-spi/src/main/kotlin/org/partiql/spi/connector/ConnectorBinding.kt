package org.partiql.spi.connector

import org.partiql.eval.value.Datum

/**
 * TODO REMOVE ME IN FAVOR OF EXTENSIONS TO TABLE
 */
public interface ConnectorBinding {

    /**
     * Return the datum for this binding.
     */
    public fun getDatum(): Datum
}
