package org.partiql.planner.internal.utils

import org.partiql.plan.Identifier

internal object PlanUtils {

    /**
     * Transforms an identifier to a human-readable string.
     *
     * Example output: aCaseInsensitiveCatalog."aCaseSensitiveSchema".aCaseInsensitiveTable
     */
    fun identifierToString(node: Identifier): String = when (node) {
        is Identifier.Symbol -> identifierSymbolToString(node)
        is Identifier.Qualified -> {
            val toJoin = listOf(node.root) + node.steps
            toJoin.joinToString(separator = ".") { ident ->
                identifierSymbolToString(ident)
            }
        }
    }

    private fun identifierSymbolToString(node: Identifier.Symbol) = when (node.caseSensitivity) {
        Identifier.CaseSensitivity.SENSITIVE -> "\"${node.symbol}\""
        Identifier.CaseSensitivity.INSENSITIVE -> node.symbol
    }
}
