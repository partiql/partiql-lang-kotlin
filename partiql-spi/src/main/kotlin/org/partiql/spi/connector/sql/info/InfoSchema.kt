package org.partiql.spi.connector.sql.info

import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.Index

/**
 * Provides the INFORMATION_SCHEMA views over internal database symbols.
 */
public class InfoSchema @OptIn(FnExperimental::class) constructor(
    public val functions: Index<Fn>,
    public val aggregations: Index<Agg>
) {

    /**
     * INFORMATION_SCHEMA.ROUTINES
     */
    @OptIn(FnExperimental::class)
    private val routines: InfoView = InfoViewRoutines(functions)

    public fun get(table: String): InfoView? = when (table) {
        "routines" -> routines
        else -> null
    }

    public companion object {

        @OptIn(FnExperimental::class)
        @JvmStatic
        public fun default(): InfoSchema {
            val functions = Index.fnBuilder()
                // .addAll(SqlBuiltins.builtins)
                .build()
            val aggregations = Index.aggBuilder()
                // .addAll(SqlBuiltins.aggregations)
                .build()
            return InfoSchema(functions, aggregations)
        }

        @OptIn(FnExperimental::class)
        public fun ext(): InfoSchema {
            val functions = Index.fnBuilder()
                // .addAll(SqlBuiltins.builtins + PartiQLExts.builtins)
                .build()
            val aggregations = Index.aggBuilder()
                // .addAll(SqlBuiltins.aggregations)
                .build()
            return InfoSchema(functions, aggregations)
        }
    }
}
