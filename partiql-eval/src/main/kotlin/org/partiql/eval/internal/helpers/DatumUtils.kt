package org.partiql.eval.internal.helpers

import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal object DatumUtils {

    /**
     * Calls [Datum.lower] if the datum is a variant, otherwise returns the datum. If you don't know whether the value
     * is of type [PType.VARIANT], you should use [Datum.lowerSafe] before invoking whatever methods you intend to use.
     * This is essentially a workaround for the fact that we currently don't know whether a particular expression will be
     * [PType.VARIANT] or not. The planner/plan can eventually be optimized to accommodate this.
     */
    internal fun Datum.lowerSafe(): Datum {
        return when (this.type.code()) {
            PType.VARIANT -> this.lower()
            else -> this
        }
    }
}
