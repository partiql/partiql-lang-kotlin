package org.partiql.lang.eval.visitors

import org.partiql.lang.domains.extractSourceLocation

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.extractColumnAlias
import org.partiql.pig.runtime.SymbolPrimitive

/**
 * Assigns aliases to any unspecified [PartiqlAst.FromSource.Scan]/[PartiqlAst.FromSource.Unpivot] that does not
 * already have one.
 *
 * For example: `SELECT * FROM foo` gets rewritten as `SELECT * from foo as foo`.
 * Path expressions:  `SELECT * FROM foo.bar.bat` gets rewritten as `SELECT * from foo.bar.bat as bat`
 *
 * If provided with a query that has all of the from source aliases already specified, an exact clone is returned.
 */
class FromSourceAliasVisitorTransform : PartiqlAst.VisitorTransform() {

    private class InnerFromSourceAliasVisitorTransform : PartiqlAst.VisitorTransform() {
        private var fromSourceCounter = 0

        override fun transformFromSourceScan_asAlias(node: PartiqlAst.FromSource.Scan): SymbolPrimitive? {
            val thisFromSourceIndex = fromSourceCounter++
            return node.asAlias ?:
                transformSymbolPrimitive(
                    SymbolPrimitive(node.expr.extractColumnAlias(thisFromSourceIndex), node.extractSourceLocation()))
        }

        override fun transformFromSourceUnpivot_asAlias(node: PartiqlAst.FromSource.Unpivot): SymbolPrimitive? {
            val thisFromSourceIndex = fromSourceCounter++
            return node.asAlias ?:
                transformSymbolPrimitive(
                    SymbolPrimitive(node.expr.extractColumnAlias(thisFromSourceIndex), node.extractSourceLocation()))
        }

        // Need use a different [fromSourceCounter] for sub-queries.
        override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr =
            FromSourceAliasVisitorTransform().transformExprSelect(node)
    }

    override fun transformFromSource(node: PartiqlAst.FromSource): PartiqlAst.FromSource =
        InnerFromSourceAliasVisitorTransform().transformFromSource(node)
}
