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

    fun externalize(node: org.partiql.planner.internal.ir.Identifier): Identifier = when (node) {
        is org.partiql.planner.internal.ir.Identifier.Symbol -> externalize(node)
        is org.partiql.planner.internal.ir.Identifier.Qualified -> externalize(node)
    }

    private fun externalize(node: org.partiql.planner.internal.ir.Identifier.Symbol): Identifier.Symbol {
        val symbol = node.symbol
        val case = externalize(node.caseSensitivity)
        return Identifier.Symbol(symbol, case)
    }

    private fun externalize(node: org.partiql.planner.internal.ir.Identifier.Qualified): Identifier.Qualified {
        val root = externalize(node.root)
        val steps = node.steps.map { externalize(it) }
        return Identifier.Qualified(root, steps)
    }

    private fun externalize(node: org.partiql.planner.internal.ir.Identifier.CaseSensitivity): Identifier.CaseSensitivity {
        return when (node) {
            org.partiql.planner.internal.ir.Identifier.CaseSensitivity.SENSITIVE -> Identifier.CaseSensitivity.SENSITIVE
            org.partiql.planner.internal.ir.Identifier.CaseSensitivity.INSENSITIVE -> Identifier.CaseSensitivity.INSENSITIVE
        }
    }
}
