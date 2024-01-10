package org.partiql.spi.internal.views

import org.partiql.types.StaticType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * Basic interface for an INFORMATION_SCHEMA table view.
 */
internal interface InfoView {

    val schema: StaticType

    @OptIn(PartiQLValueExperimental::class)
    fun value(): PartiQLValue
}
