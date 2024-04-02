package org.partiql.spi.connector.sql

/* ktlint-disable no-wildcard-imports */
import org.partiql.spi.connector.sql.exts.*
import org.partiql.spi.fn.FnExperimental

/**
 * PartiQL built-in functions that are not in the SQL-92 spec
 */
@OptIn(FnExperimental::class)
internal object PartiQLExts {
    @JvmStatic
    val builtins = listOf(
        Fn_EXISTS__BAG__BOOL,
        Fn_EXISTS__LIST__BOOL,
        Fn_EXISTS__STRUCT__BOOL,
        Fn_SIZE__BAG__INT32,
        Fn_SIZE__LIST__INT32,
        Fn_SIZE__STRUCT__INT32
    )
}
