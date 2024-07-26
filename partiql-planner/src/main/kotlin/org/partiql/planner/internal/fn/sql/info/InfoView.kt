package org.partiql.planner.internal.fn.sql.info

import org.partiql.types.StaticType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * Basic interface for an INFORMATION_SCHEMA table view.
 */
public interface InfoView {

    public val schema: StaticType

    @OptIn(PartiQLValueExperimental::class)
    public fun value(): PartiQLValue
}
