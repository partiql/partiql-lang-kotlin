package org.partiql.lang.planner.transforms

import com.amazon.ionelement.api.ionSymbol
import org.partiql.lang.domains.PartiqlAlgebraUnindexed
import org.partiql.lang.domains.PartiqlAstVarDecl
import org.partiql.lang.domains.PartiqlAstVarDeclToPartiqlAlgebraUnindexedVisitorTransform

fun PartiqlAstVarDecl.Statement.toUnindexedAlgebra(): PartiqlAlgebraUnindexed.Statement =
    ToUnindexedAlgebra.transformStatement(this)

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

private object ToUnindexedAlgebra : PartiqlAstVarDeclToPartiqlAlgebraUnindexedVisitorTransform() {

    override fun transformExprSelect(node: PartiqlAstVarDecl.Expr.Select): PartiqlAlgebraUnindexed.Expr {
        checkForUnsupportedSelectClauses(node)

        var algebra = FromSourceToBindingsExpr.convert(node.from)

        algebra = node.where?.let {
            PartiqlAlgebraUnindexed.build {
                bindingsTerm(filter(transformExpr(it), algebra, it.metas), it.metas)
            }
        } ?: algebra

        return convertProjectionToMapValues(node, algebra)
    }

    private fun convertProjectionToMapValues(
        node: PartiqlAstVarDecl.Expr.Select,
        algebra: PartiqlAlgebraUnindexed.BindingsTerm
    ) = PartiqlAlgebraUnindexed.build {
        // TODO: support SELECT x.*, y.*, etc.  For now we only support SELECT VALUE and SELECT <expr> AS <alias>,...
        // note that have AST rewrite already which converts:
        //    SELECT * FROM foo AS f, bar as B to
        //    SELECT f.*, b.* FROM foo AS f, bar as B to
        // So, supporting SELECT * here is not necessary.

        mapValues(
            when (val project = node.project) {
                is PartiqlAstVarDecl.Projection.ProjectValue -> transformExpr(project.value)
                is PartiqlAstVarDecl.Projection.ProjectList -> {
                    val structFields = project.projectItems.map {
                        when (it) {
                            is PartiqlAstVarDecl.ProjectItem.ProjectAll -> TODO("Support SELECT <alias>.*")
                            is PartiqlAstVarDecl.ProjectItem.ProjectExpr -> {
                                exprPair(
                                    lit(ionSymbol(it.asName.text), it.metas),
                                    transformExpr(it.expr),
                                    it.metas
                                )
                            }
                        }
                    }

                    struct(structFields)
                }
                is PartiqlAstVarDecl.Projection.ProjectStar -> errAstNotNormalized("Expected SELECT * to be removed")
                is PartiqlAstVarDecl.Projection.ProjectPivot -> TODO("PIVOT ...")
            },
            algebra
        )
    }

    /**
     * Throws [NotImplementedError] if any `SELECT` clauses were used that are not mappable to [PartiqlAlgebraUnindexed].
     *
     * This function is temporary and will be removed when all the clauses of the `SELECT` expression are mappable
     * to [PartiqlAlgebraUnindexed].
     */
    private fun checkForUnsupportedSelectClauses(node: PartiqlAstVarDecl.Expr.Select) {
        when {
            node.fromLet != null -> TODO("support FROM LET")
            node.group != null -> TODO("Support GROUP BY")
            node.order != null -> TODO("Support ORDER BY")
            node.having != null -> TODO("Support HAVING")
            node.limit != null -> TODO("Support LIMIT")
        }

        when (node.setq) {
            null, is PartiqlAstVarDecl.SetQuantifier.All -> {
                /* do nothing, this is supported (if null, default is SetQuantifier.All) */
            }
            is PartiqlAstVarDecl.SetQuantifier.Distinct -> TODO("Support SELECT DISTINCT")
        }
    }

    override fun transformStatementDml(node: PartiqlAstVarDecl.Statement.Dml): PartiqlAlgebraUnindexed.Statement {
        TODO("support DML in algebra")
    }

    override fun transformStatementDdl(node: PartiqlAstVarDecl.Statement.Ddl): PartiqlAlgebraUnindexed.Statement {
        TODO("support DDL in algebra")
    }

    override fun transformStatementExec(node: PartiqlAstVarDecl.Statement.Exec): PartiqlAlgebraUnindexed.Statement {
        TODO("support stored procedure calls in algebra")
    }
}

private object FromSourceToBindingsExpr : PartiqlAstVarDecl.FromSource.Converter<PartiqlAlgebraUnindexed.BindingsTerm> {

    override fun convertScan(node: PartiqlAstVarDecl.FromSource.Scan): PartiqlAlgebraUnindexed.BindingsTerm =
        PartiqlAlgebraUnindexed.build {
            bindingsTerm(
                scan(
                    ToUnindexedAlgebra.transformExpr(node.expr),
                    ToUnindexedAlgebra.transformVarDecl(node.asDecl),
                    node.atDecl?.let { ToUnindexedAlgebra.transformVarDecl(it) },
                    node.byDecl?.let { ToUnindexedAlgebra.transformVarDecl(it) },
                    node.metas
                ),
                node.metas
            )
        }

    override fun convertUnpivot(node: PartiqlAstVarDecl.FromSource.Unpivot): PartiqlAlgebraUnindexed.BindingsTerm {
        TODO("support unpivot in algebra")
    }

    override fun convertJoin(node: PartiqlAstVarDecl.FromSource.Join): PartiqlAlgebraUnindexed.BindingsTerm =
        when (node.type) {
            is PartiqlAstVarDecl.JoinType.Inner ->
                if(node.predicate == null) {
                    PartiqlAlgebraUnindexed.build {
                        bindingsTerm(
                            crossJoin(
                                super.convert(node.left),
                                super.convert(node.right),
                                node.metas
                            )
                        )
                    }
                }
                else {
                    TODO("support inner join in algebra")
                }

            is PartiqlAstVarDecl.JoinType.Full -> TODO("support full join in algebra")
            is PartiqlAstVarDecl.JoinType.Left -> TODO("support join in algebra")
            is PartiqlAstVarDecl.JoinType.Right -> TODO("support join in algebra")
        }
}