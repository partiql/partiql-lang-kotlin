package org.partiql.eval.internal

import org.partiql.spi.value.Datum

/**
 * Statement execution parameters; make public Java class in later PR.
 */
internal class Parameters(private val values: Array<Datum>) {

    /**
     * Get n-th parameter (0-indexed).
     *
     * @param idx
     * @return
     */
    fun get(idx: Int): Datum = values[idx]

    internal companion object {

        /**
         * Empty parameters -> [].
         */
        @JvmStatic
        internal val EMPTY = Parameters(emptyArray())
    }
}
