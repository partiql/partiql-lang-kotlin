package org.partiql.spi.connector.sql.info

import org.partiql.shape.PShape
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * Basic interface for an INFORMATION_SCHEMA table view.
 */
public interface InfoView {

    public val schema: PShape

    @OptIn(PartiQLValueExperimental::class)
    public fun value(): PartiQLValue
}
