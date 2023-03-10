package org.partiql.lang.eval.visitors

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.extractSourceLocation
import org.partiql.lang.eval.extractColumnAlias
import org.partiql.pig.runtime.SymbolPrimitive

/**
 * Assigns aliases to any unspecified [PartiqlAst.FromSource.Scan]/[PartiqlAst.FromSource.Unpivot] that does not
 * already have one.
 *
 * For example: `SELECT * FROM foo` gets transformed into `SELECT * from foo as foo`.
 * Path expressions:  `SELECT * FROM foo.bar.bat` gets transformed into `SELECT * from foo.bar.bat as bat`
 *
 * If provided with a query that has all of the from source aliases already specified, an exact clone is returned.
 */
class FromSourceAliasVisitorTransform : VisitorTransformBase() {
    // When this visitor reaches a top-level FromSource, it transforms it with a fresh instance
    // of the helper visitor below.
    // Currently, there are two places where a top-level FromSource can occur: in a SELECT and in a DML statement.

    override fun transformExprSelect_from(node: PartiqlAst.Expr.Select): PartiqlAst.FromSource {
        val newFrom = super.transformExprSelect_from(node) // this applies the transformation to any eligible subqueries
        return InnerFromSourceAliasVisitorTransform().transformFromSource(newFrom)
    }

    override fun transformStatementDml_from(node: PartiqlAst.Statement.Dml): PartiqlAst.FromSource? {
        val newFrom = super.transformStatementDml_from(node)
        return newFrom?.let { InnerFromSourceAliasVisitorTransform().transformFromSource(it) }
    }

    /** The helper visitor is responsible for traversing one top-level FromSource and creating aliases in it.
     *  Only the [transformFromSource] method is intended to be useful as an entry point.
     *  The visitor is stateful, maintaining a counter for synthetic aliases,
     *  so a separate instance is needed for each top level FromSource.
     */
    private class InnerFromSourceAliasVisitorTransform : VisitorTransformBase() {
        private var fromSourceCounter = 0

        override fun transformFromSourceScan_asAlias(node: PartiqlAst.FromSource.Scan): SymbolPrimitive {
            val thisFromSourceIndex = fromSourceCounter++
            return node.asAlias
                ?: SymbolPrimitive(node.expr.extractColumnAlias(thisFromSourceIndex), node.extractSourceLocation())
        }

        override fun transformFromSourceUnpivot_asAlias(node: PartiqlAst.FromSource.Unpivot): SymbolPrimitive {
            val thisFromSourceIndex = fromSourceCounter++
            return node.asAlias
                ?: SymbolPrimitive(node.expr.extractColumnAlias(thisFromSourceIndex), node.extractSourceLocation())
        }

        // Do not traverse into subexpressions of a [FromSource].
        // All relevant subqueries are reached by the host [FromSourceAliasVisitorTransform].
        override fun transformExpr(node: PartiqlAst.Expr): PartiqlAst.Expr {
            return node
        }
    }
}
