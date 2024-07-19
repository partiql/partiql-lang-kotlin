package org.partiql.planner.internal.functions.sql

import org.partiql.planner.catalog.Function

/**
 * A simple lookup for the SQL-99 builtins.
 */
internal object SqlFunctions {

    private val functions = mutableMapOf<String, SqlFunction>()

    init {
        createFunction(SqlBetween)
        createFunction(SqlConcat)
        createFunction(SqlUpper)
        createFunction(SqlLower)
    }

    fun getFunctions(name: String): Collection<Function.Scalar> = functions[name]?.getVariants() ?: emptyList()

    private fun createFunction(function: SqlFunction) {
        functions[function.getName()] = function
    }
}
