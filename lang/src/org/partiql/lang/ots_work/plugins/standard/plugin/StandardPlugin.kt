package org.partiql.lang.ots_work.plugins.standard.plugin

import org.partiql.lang.ots_work.interfaces.Plugin
import org.partiql.lang.ots_work.plugins.standard.operators.StandardScalarCast
import java.time.ZoneOffset

class StandardPlugin(
    val typedOpBehavior: TypedOpBehavior = TypedOpBehavior.LEGACY,
    val defaultTimezoneOffset: ZoneOffset = ZoneOffset.UTC,
) : Plugin {
    override val scalarCast = StandardScalarCast(
        typedOpBehavior = typedOpBehavior,
        defaultTimezoneOffset = defaultTimezoneOffset
    )
}
