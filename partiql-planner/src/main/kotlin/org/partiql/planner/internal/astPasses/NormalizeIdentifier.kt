package org.partiql.planner.internal.astPasses

import org.partiql.ast.Binder
import org.partiql.ast.Identifier
import org.partiql.ast.Statement
import org.partiql.ast.normalize.AstPass
import org.partiql.ast.util.AstRewriter
import org.partiql.planner.internal.IdentifierManager
import org.partiql.planner.internal.RValue

/**
 * Case Preservation:
 *   - If on, turn case-insensitive binder to case-sensitive binder, with symbol preserve the original case
 *   - If off, normalize the binder.
 *      - The normalization function in the spec perhaps will end up being implementation defined.
 *      - Therefore, for now we normalize by preserving case.
 *      - This is to stay consistent with the current behavior, and reduce one moving element during testing....
 *
 * Identifier Normalization:
 *   - Determines the normalization for identifier (variable look up)
 *   - Folding down: Using lower case text to look up
 *   - Folding up: Using upper case text to look up.
 *   - Case Sensitive: Using original text to look up
 *   - Case Insensitive: Matching behavior, string comparison with case ignored.
 */
internal class NormalizeIdentifier(
    casePreservation: Boolean,
    rValue: RValue
) : AstPass {

    val identifierManager = IdentifierManager(casePreservation, rValue)
    override fun apply(statement: Statement): Statement = Visitor(identifierManager).visitStatement(statement, Unit) as Statement

    private class Visitor(val identifierManager: IdentifierManager) : AstRewriter<Unit>() {

        override fun visitIdentifierSymbol(node: Identifier.Symbol, ctx: Unit) =
            identifierManager.normalizeRvalue(node)

        override fun visitBinder(node: Binder, ctx: Unit) = identifierManager.normalizeBinder(node)
    }
}
