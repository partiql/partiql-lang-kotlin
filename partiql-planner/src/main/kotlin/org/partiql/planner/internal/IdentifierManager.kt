package org.partiql.planner.internal

import org.partiql.ast.Binder
import org.partiql.ast.Identifier
import org.partiql.ast.binder
import org.partiql.ast.identifierSymbol

internal class IdentifierManager(
    val casePreservation: Boolean,
    val rValue: RValue
) {
    fun normalizeBinder(binder: Binder): Binder =
        when (binder.caseSensitivity) {
            Binder.CaseSensitivity.SENSITIVE -> binder
            Binder.CaseSensitivity.INSENSITIVE -> {
                if (casePreservation) {
                    binder(binder.symbol, Binder.CaseSensitivity.SENSITIVE)
                }
                // This is a conscious decision
                // Even though the case preservation is off, we still need a way to store the binder
                // To make this compatible with the current implementation,
                // the decision here is: we normalize by preserving case.
                else {
                    binder(binder.symbol, Binder.CaseSensitivity.SENSITIVE)
                }
            }
        }

    fun normalizeRvalue(id: Identifier.Symbol): Identifier.Symbol =
        when (id.caseSensitivity) {
            Identifier.CaseSensitivity.SENSITIVE -> id
            Identifier.CaseSensitivity.INSENSITIVE -> when (rValue) {
                RValue.FOLDING_UP -> identifierSymbol(id.symbol.uppercase(), Identifier.CaseSensitivity.SENSITIVE)
                RValue.FOLDING_DOWN -> identifierSymbol(id.symbol.lowercase(), Identifier.CaseSensitivity.SENSITIVE)
                RValue.SENSITIVE -> identifierSymbol(id.symbol, Identifier.CaseSensitivity.SENSITIVE)
                RValue.INSENSITIVE -> identifierSymbol(id.symbol, Identifier.CaseSensitivity.INSENSITIVE)
            }
        }
}
