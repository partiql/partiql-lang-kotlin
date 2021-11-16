package org.partiql.lang.planner.transforms

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlAstToPartiqlAstVarDeclVisitorTransform
import org.partiql.lang.domains.PartiqlAstVarDecl
import org.partiql.lang.eval.extractColumnAlias
import org.partiql.pig.runtime.SymbolPrimitive
import org.partiql.pig.runtime.asPrimitive

fun PartiqlAst.Statement.toAstVarDecl(): PartiqlAstVarDecl.Statement =
    ToAstVarDeclVisitorTransform().transformStatement(this)

/**
 * `PartiqlAstVarlDecl` changes:
 *  - All aliases (select-list items and from sources) are now non-nulalble.
 *  - From-source aliases are now represented with a `VarDecl` node instead of a symbol.
 *
 *  Any `AS` alias not specified by the query author is synthesized using [extractColumnAlias].
 */
private class ToAstVarDeclVisitorTransform(
) : PartiqlAstToPartiqlAstVarDeclVisitorTransform() {

    private var selectListItemCount = 0
    private var fromSourceAliasCount = 0

    private var varDeclCount = 0

    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAstVarDecl.Expr {
        // Save these fields for later in case we are in a nested query.
        // varDecl count is intentionally excluded from this list as each variable should be
        // allocated a unique index.
        val savedSelectListItemCount = selectListItemCount
        val savedFromSourceAliasCount = fromSourceAliasCount

        selectListItemCount = 0
        fromSourceAliasCount = 0

        return super.transformExprSelect(node).also {
            selectListItemCount = savedSelectListItemCount
            fromSourceAliasCount = savedFromSourceAliasCount
        }
    }

    private fun createVarDecl(name: SymbolPrimitive): PartiqlAstVarDecl.VarDecl = PartiqlAstVarDecl.build {
        varDecl_(name, (varDeclCount++).asPrimitive())
    }

    override fun transformProjectItemProjectExpr(node: PartiqlAst.ProjectItem.ProjectExpr): PartiqlAstVarDecl.ProjectItem =
        PartiqlAstVarDecl.build {
            projectExpr_(
                transformExpr(node.expr),
                node.asAlias ?: node.expr.extractColumnAlias(selectListItemCount).asPrimitive()
            )
        }.also {
            selectListItemCount++
        }


    override fun transformFromSourceScan(node: PartiqlAst.FromSource.Scan): PartiqlAstVarDecl.FromSource =
        PartiqlAstVarDecl.build {
            scan(
                transformExpr(node.expr),
                createVarDecl(node.asAlias ?: node.expr.extractColumnAlias(fromSourceAliasCount).asPrimitive()),
                node.atAlias?.let { createVarDecl(it) },
                node.byAlias?.let { createVarDecl(it) }
            )
        }.also {
            fromSourceAliasCount++
        }


    override fun transformFromSourceUnpivot(node: PartiqlAst.FromSource.Unpivot): PartiqlAstVarDecl.FromSource =
        PartiqlAstVarDecl.build {
            unpivot(
                transformExpr(node.expr),
                createVarDecl(node.asAlias ?: node.expr.extractColumnAlias(fromSourceAliasCount).asPrimitive()),
                node.atAlias?.let { createVarDecl(it) },
                node.byAlias?.let { createVarDecl(it) }
            )
        }.also {
            fromSourceAliasCount++
        }

    override fun transformLetBinding(node: PartiqlAst.LetBinding): PartiqlAstVarDecl.LetBinding =
        PartiqlAstVarDecl.build {
            letBinding(
                expr = transformExpr(node.expr),
                name = createVarDecl(node.name),
                metas = node.metas
            )
        }

}



