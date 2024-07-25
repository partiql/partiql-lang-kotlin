package org.partiql.planner.internal

import org.partiql.ast.Binder
import org.partiql.ast.Identifier
import org.partiql.ast.binder
import org.partiql.ast.identifierSymbol

// The identifier manager class takes in two parameters,
// in case we want to decouple case preservation and case normalization behavior in the future.
internal class IdentifierManager(
    private val casePreservation: CaseNormalization?,
    private val caseNormalization: CaseNormalization?
) {
    fun normalizeBinder(binder: Binder): Binder =
        when (binder.isRegular) {
            true -> {
                when (casePreservation) {
                    CaseNormalization.UPPERCASE -> binder(binder.symbol.uppercase(), false)
                    CaseNormalization.LOWERCASE -> binder(binder.symbol.lowercase(), false)
                    CaseNormalization.EXACTCASE -> binder(binder.symbol, false)
                    null -> binder(binder.symbol, false)
                }
            }
            false -> binder
        }

    fun normalizeRvalue(id: Identifier.Symbol): Identifier.Symbol =
        when (id.caseSensitivity) {
            Identifier.CaseSensitivity.SENSITIVE -> id
            Identifier.CaseSensitivity.INSENSITIVE -> when (caseNormalization) {
                CaseNormalization.UPPERCASE -> identifierSymbol(id.symbol.uppercase(), Identifier.CaseSensitivity.SENSITIVE)
                CaseNormalization.LOWERCASE -> identifierSymbol(id.symbol.lowercase(), Identifier.CaseSensitivity.SENSITIVE)
                CaseNormalization.EXACTCASE -> identifierSymbol(id.symbol, Identifier.CaseSensitivity.SENSITIVE)
                null -> id
            }
        }
}
