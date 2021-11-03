package org.partiql.lang.planner

import com.amazon.ionelement.api.ionSymbol
import org.partiql.lang.domains.PartiqlAlgebra
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlAstToPartiqlAlgebraVisitorTransform

fun astToAlgebra(ast: PartiqlAst.Statement): PartiqlAlgebra.Statement =
    AstToAlgebra.transformStatement(ast)

/*
Notes:

- May still need to think about which metas we are going to propagate from the original AST here, i.e. it may be
required to include *only* the source location meta but elide all others.  For now we are just copying all the metas
from the original AST nodes.
- Also related to metas, it would be especially great if https://github.com/partiql/partiql-ir-generator/issues/69 were
completed to avoid bugs where we inadvertently drop metas.
 */


private fun errAstNotNormalized(message: String): Nothing =
    error("$message - have the basic visitor transforms been executed first?")

private object AstToAlgebra : PartiqlAstToPartiqlAlgebraVisitorTransform() {
    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAlgebra.Expr {
        checkForUnsupportedSelectClauses(node)

        var algebra = FromSourceToBindingsExpr.convert(node.from)

        algebra = node.where?.let {
            PartiqlAlgebra.build {
                bindingsTerm(filter(transformExpr(it), algebra, it.metas), it.metas)
            }
        } ?: algebra


        return convertProjectionToMapValues(node, algebra)
    }

    private fun convertProjectionToMapValues(
        node: PartiqlAst.Expr.Select,
        algebra: PartiqlAlgebra.BindingsTerm
    ) = PartiqlAlgebra.build {
        // TODO: support SELECT x.*, y.*, etc.  For now we only support SELECT VALUE and SELECT <expr> AS <alias>,...
        // note that have AST rewrite already which converts:
        //    SELECT * FROM foo AS f, bar as B to
        //    SELECT f.*, b.* FROM foo AS f, bar as B to
        // So, supporting SELECT * here is not necessary.

        mapValues(
            when (val project = node.project) {
                is PartiqlAst.Projection.ProjectValue -> transformExpr(project.value)
                is PartiqlAst.Projection.ProjectList -> {
                    val structFields = project.projectItems.map {
                        when (it) {
                            is PartiqlAst.ProjectItem.ProjectAll -> TODO("Support SELECT <alias>.*")
                            is PartiqlAst.ProjectItem.ProjectExpr -> {
                                val asAliasText = it.asAlias ?: errAstNotNormalized("projectItem.asAlias is null")
                                exprPair(
                                    lit(ionSymbol(asAliasText.text), it.metas),
                                    transformExpr(it.expr),
                                    it.metas
                                )
                            }
                        }
                    }

                    struct(structFields)
                }
                is PartiqlAst.Projection.ProjectStar -> errAstNotNormalized("Expected SELECT * to be removed")
                is PartiqlAst.Projection.ProjectPivot -> TODO("PIVOT ...")
            },
            algebra
        )
    }

    /**
     * Throws [NotImplementedError] if any `SELECT` clauses were used that are not mappable to [PartiqlAlgebra].
     *
     * This function is temporary and will be removed when all the clauses of the `SELECT` expression are mappable
     * to [PartiqlAlgebra].
     */
    private fun checkForUnsupportedSelectClauses(node: PartiqlAst.Expr.Select) {
        when {
            node.fromLet != null -> TODO("support FROM LET")
            node.group != null -> TODO("Support GROUP BY")
            node.order != null -> TODO("Support ORDER BY")
            node.having != null -> TODO("Support HAVING")
            node.limit != null -> TODO("Support LIMIT")
        }

        when (node.setq) {
            null, is PartiqlAst.SetQuantifier.All -> {
                /* do nothing, this is supported (if null, default is SetQuantifier.All) */
            }
            is PartiqlAst.SetQuantifier.Distinct -> TODO("Support SELECT DISTINCT")
        }
    }


    override fun transformStatementDml(node: PartiqlAst.Statement.Dml): PartiqlAlgebra.Statement {
        TODO("support DML in algebra")
    }

    override fun transformStatementDdl(node: PartiqlAst.Statement.Ddl): PartiqlAlgebra.Statement {
        TODO("support DDL in algebra")
    }

    override fun transformStatementExec(node: PartiqlAst.Statement.Exec): PartiqlAlgebra.Statement {
        TODO("support stored procedure calls in algebra")
    }
}

private object FromSourceToBindingsExpr : PartiqlAst.FromSource.Converter<PartiqlAlgebra.BindingsTerm> {

    override fun convertScan(node: PartiqlAst.FromSource.Scan): PartiqlAlgebra.BindingsTerm =
        PartiqlAlgebra.build {
            bindingsTerm(
                scan(
                    AstToAlgebra.transformExpr(node.expr),
                    varDecl_(node.asAlias ?: errAstNotNormalized("node.asAlias is null")),
                    node.atAlias?.let { varDecl_(it) },
                    node.byAlias?.let { varDecl_(it) },
                    node.metas
                ),
                node.metas
            )
        }

    override fun convertUnpivot(node: PartiqlAst.FromSource.Unpivot): PartiqlAlgebra.BindingsTerm {
        TODO("support unpivot in algebra")
    }

    override fun convertJoin(node: PartiqlAst.FromSource.Join): PartiqlAlgebra.BindingsTerm {
        TODO("support join in algebra")
    }

}