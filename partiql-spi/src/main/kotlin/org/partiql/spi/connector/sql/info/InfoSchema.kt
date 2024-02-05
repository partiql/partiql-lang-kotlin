package org.partiql.spi.connector.sql.info

import org.partiql.spi.connector.sql.SqlBuiltins
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnIndex

/**
 * Provides the INFORMATION_SCHEMA views over internal database symbols.
 */
public class InfoSchema(
    public val functions: FnIndex,
) {

    /**
     * INFORMATION_SCHEMA.ROUTINES
     */
    private val routines: InfoView = InfoViewRoutines(functions)

    public fun get(table: String): InfoView? = when (table) {
        "routines" -> routines
        else -> null
    }

    public companion object {

        @OptIn(FnExperimental::class)
        @JvmStatic
        public fun default(): InfoSchema {
            val functions = FnIndex.builder()
                .addAll(SqlBuiltins.builtins)
                .build()
            return InfoSchema(functions)
        }
    }
}
