package org.partiql.planner.internal.astPasses

import org.partiql.ast.Binder
import org.partiql.ast.Identifier
import org.partiql.ast.Statement
import org.partiql.ast.normalize.AstPass
import org.partiql.ast.util.AstRewriter
import org.partiql.planner.internal.CaseNormalization
import org.partiql.planner.internal.IdentifierManager

/**
 * Case Normalization behavior:
 *   - UPPERCASE: normalize identifier using uppercase for both lvalue and rvalue
 *   - LOWERCASE: normalize identifier using lowercase for both lvalue and rvalue
 *   - EXACTCASE: normalize identifier by preserving case for both lvalue and rvalue
 *   - NULL: Normalize lvalue by preserving case, no normalization for rvalue.
 */
internal class NormalizeIdentifier(
    caseNormalization: CaseNormalization?
) : AstPass {

    val identifierManager = IdentifierManager(caseNormalization, caseNormalization)
    override fun apply(statement: Statement): Statement = Visitor(identifierManager).visitStatement(statement, Unit) as Statement

    private class Visitor(val identifierManager: IdentifierManager) : AstRewriter<Unit>() {

        override fun visitIdentifierSymbol(node: Identifier.Symbol, ctx: Unit) =
            identifierManager.normalizeRvalue(node)

        override fun visitBinder(node: Binder, ctx: Unit) = identifierManager.normalizeBinder(node)
    }
}
