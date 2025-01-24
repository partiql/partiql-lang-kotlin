package org.partiql.spi.function.builtins

import org.partiql.spi.types.PType

internal object DefaultDecimal {
    // TODO: Once all functions are converted to use the new function modeling, this can be removed.
    val DECIMAL: PType = PType.decimal(38, 19)
}
