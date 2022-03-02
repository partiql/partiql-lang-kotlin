package org.partiql.planner.transforms

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlAstToPartiqlLogicalVisitorTransform
import org.partiql.lang.domains.PartiqlLogical

/**
 * Transforms an instance of [PartiqlAst.Statement] to [PartiqlLogical.Statement].
 *
 * Performs no semantic checks.
 *
 * This conversion (and the logical algebra) are early in their lifecycle and so only a very limited subset of
 * SFW queries are transformable.  See tests for this class to see which queries are transformable.
 */
internal fun PartiqlAst.Statement.toLogical(): PartiqlLogical.Statement =
    AstToLogicalVisitorTransform.transformStatement(this)

private object AstToLogicalVisitorTransform : PartiqlAstToPartiqlLogicalVisitorTransform() {

    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlLogical.Expr {
        checkForUnsupportedSelectClauses(node)

        var algebra = FromSourceToBexpr.convert(node.from)

        algebra = node.where?.let {
            PartiqlLogical.build { filter(transformExpr(it), algebra, it.metas) }
        } ?: algebra

        return convertProjectionToMapValues(node, algebra)
    }

    private fun convertProjectionToMapValues(node: PartiqlAst.Expr.Select, algebra: PartiqlLogical.Bexpr) =
        PartiqlLogical.build {
            mapValues(
                when (val project = node.project) {
                    is PartiqlAst.Projection.ProjectValue -> transformExpr(project.value)
                    is PartiqlAst.Projection.ProjectList -> {
                        if(project.projectItems.size > 1) {
                            TODO("Support for more than one projectItem")
                        } else {
                            when(val projectItem = project.projectItems.first()) {
                                is PartiqlAst.ProjectItem.ProjectExpr -> TODO("Support for <expr> AS <alias> in select list")
                                is PartiqlAst.ProjectItem.ProjectAll -> {
                                    transformExpr(projectItem.expr)
                                }
                            }
                        }
                    }
                    is PartiqlAst.Projection.ProjectStar ->
                        // `SELECT * FROM bar AS b` is rewritten to `SELECT b.* FROM bar as b` by
                        // [SelectStarVisitorTransform]. Therefore, there is no need to support `SELECT *` here.
                        errAstNotNormalized("Expected SELECT * to be removed")

                    is PartiqlAst.Projection.ProjectPivot -> TODO("PIVOT ...")
                },
                algebra
            )
        }

    /**
     * Throws [NotImplementedError] if any `SELECT` clauses were used that are not mappable to [PartiqlLogical].
     *
     * This function is temporary and will be removed when all the clauses of the `SELECT` expression are mappable
     * to [PartiqlLogical].
     */
    private fun checkForUnsupportedSelectClauses(node: PartiqlAst.Expr.Select) {
        when {
            node.fromLet != null -> TODO("Support for FROM LET")
            node.group != null -> TODO("Support for GROUP BY")
            node.order != null -> TODO("Support for ORDER BY")
            node.having != null -> TODO("Support for HAVING")
            node.offset != null -> TODO("Support for OFFSET")
            node.limit != null -> TODO("Support for LIMIT")
        }

        when (node.setq) {
            null, is PartiqlAst.SetQuantifier.All -> {
                /* do nothing, this is supported (if null, default is SetQuantifier.All) */
            }
            is PartiqlAst.SetQuantifier.Distinct -> TODO("Support for SELECT DISTINCT")
        }
    }

    override fun transformStatementDml(node: PartiqlAst.Statement.Dml): PartiqlLogical.Statement {
        TODO("Support for DML")
    }

    override fun transformStatementDdl(node: PartiqlAst.Statement.Ddl): PartiqlLogical.Statement {
        TODO("Support for DDL")
    }

    override fun transformStatementExec(node: PartiqlAst.Statement.Exec): PartiqlLogical.Statement {
        TODO("Support for EXEC")
    }
}

private object FromSourceToBexpr : PartiqlAst.FromSource.Converter<PartiqlLogical.Bexpr> {

    override fun convertScan(node: PartiqlAst.FromSource.Scan): PartiqlLogical.Bexpr {
        val asAlias = node.asAlias ?: errAstNotNormalized("Expected as alias to be non-null")
        return PartiqlLogical.build {
            scan(
                AstToLogicalVisitorTransform.transformExpr(node.expr),
                varDecl_(asAlias, asAlias.metas),
                node.atAlias?.let { varDecl_(it, it.metas) },
                node.byAlias?.let { varDecl_(it, it.metas) },
                node.metas
            )
        }
    }

    override fun convertUnpivot(node: PartiqlAst.FromSource.Unpivot): PartiqlLogical.Bexpr {
        TODO("Support for UNPIVOT")
    }

    override fun convertJoin(node: PartiqlAst.FromSource.Join): PartiqlLogical.Bexpr {
        TODO("Support for JOINs")
    }
}

