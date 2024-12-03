package org.partiql.system.builtins

import org.partiql.types.PType

internal object DefaultNumeric {
    // TODO: Once all functions are converted to use the new function modeling, this can be removed.
    val NUMERIC: PType = PType.numeric(38, 19)
}
