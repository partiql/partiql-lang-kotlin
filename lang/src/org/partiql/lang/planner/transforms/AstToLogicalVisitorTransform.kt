package org.partiql.lang.planner.transforms

import com.amazon.ionelement.api.ionBool
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
internal fun PartiqlAst.Statement.toLogicalPlan(): PartiqlLogical.Plan =
    PartiqlLogical.build {
        plan(
            AstToLogicalVisitorTransform.transformStatement(this@toLogicalPlan),
            version = PLAN_VERSION_NUMBER.toLong()
        )
    }

private object AstToLogicalVisitorTransform : PartiqlAstToPartiqlLogicalVisitorTransform() {

    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlLogical.Expr {
        checkForUnsupportedSelectClauses(node)

        var algebra: PartiqlLogical.Bexpr = FromSourceToBexpr.convert(node.from)

        algebra = node.fromLet?.let { fromLet ->
            PartiqlLogical.build {
                let(algebra, fromLet.letBindings.map { transformLetBinding(it) }, node.fromLet.metas)
            }
        } ?: algebra

        algebra = node.where?.let {
            PartiqlLogical.build { filter(transformExpr(it), algebra, it.metas) }
        } ?: algebra

        algebra = node.offset?.let {
            PartiqlLogical.build { offset(transformExpr(it), algebra, node.offset.metas) }
        } ?: algebra

        algebra = node.limit?.let {
            PartiqlLogical.build { limit(transformExpr(it), algebra, node.limit.metas) }
        } ?: algebra

        return convertProjectionToBindingsToValues(node, algebra)
    }

    private fun convertProjectionToBindingsToValues(node: PartiqlAst.Expr.Select, algebra: PartiqlLogical.Bexpr) =
        PartiqlLogical.build {
            bindingsToValues(
                when (val project = node.project) {
                    is PartiqlAst.Projection.ProjectValue -> transformExpr(project.value)
                    is PartiqlAst.Projection.ProjectList -> {
                        struct(
                            List(project.projectItems.size) { idx ->
                                when (val projectItem = project.projectItems[idx]) {
                                    is PartiqlAst.ProjectItem.ProjectExpr ->
                                        structField(
                                            lit(
                                                projectItem.asAlias?.toIonElement()
                                                    ?: errAstNotNormalized("SELECT-list item alias not specified")
                                            ),
                                            transformExpr(projectItem.expr),
                                        )
                                    is PartiqlAst.ProjectItem.ProjectAll -> {
                                        structFields(transformExpr(projectItem.expr), projectItem.metas)
                                    }
                                }
                            }
                        )
                    }
                    is PartiqlAst.Projection.ProjectStar ->
                        // `SELECT * FROM bar AS b` is rewritten to `SELECT b.* FROM bar as b` by
                        // [SelectStarVisitorTransform]. Therefore, there is no need to support `SELECT *` here.
                        errAstNotNormalized("Expected SELECT * to be removed")

                    is PartiqlAst.Projection.ProjectPivot -> TODO("PIVOT ...")
                },
                algebra,
                node.project.metas
            )
        }.let { q ->
            // in case of SELECT DISTINCT, wrap bindingsToValues in call to filter_distinct
            when (node.setq) {
                null, is PartiqlAst.SetQuantifier.All -> q
                is PartiqlAst.SetQuantifier.Distinct -> PartiqlLogical.build { call("filter_distinct", q) }
            }
        }

    /**
     * Throws [NotImplementedError] if any `SELECT` clauses were used that are not mappable to [PartiqlLogical].
     *
     * This function is temporary and will be removed when all the clauses of the `SELECT` expression are mappable
     * to [PartiqlLogical].
     */
    private fun checkForUnsupportedSelectClauses(node: PartiqlAst.Expr.Select) {
        when {
            node.group != null -> TODO("Support for GROUP BY")
            node.order != null -> TODO("Support for ORDER BY")
            node.having != null -> TODO("Support for HAVING")
        }
    }

    override fun transformLetBinding(node: PartiqlAst.LetBinding): PartiqlLogical.LetBinding =
        PartiqlLogical.build {
            letBinding(
                transformExpr(node.expr),
                varDecl_(node.name, node.name.metas),
                node.metas
            )
        }

    override fun transformStatementDml(node: PartiqlAst.Statement.Dml): PartiqlLogical.Statement {
        TODO("Support for DML")
    }

    override fun transformStatementDdl(node: PartiqlAst.Statement.Ddl): PartiqlLogical.Statement {
        TODO("Support for DDL")
    }

    override fun transformExprStruct(node: PartiqlAst.Expr.Struct): PartiqlLogical.Expr =
        PartiqlLogical.build {
            struct(
                node.fields.map {
                    structField(
                        transformExpr(it.first),
                        transformExpr(it.second)
                    )
                },
                metas = node.metas
            )
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

    override fun convertJoin(node: PartiqlAst.FromSource.Join): PartiqlLogical.Bexpr =
        PartiqlLogical.build {
            join(
                joinType = AstToLogicalVisitorTransform.transformJoinType(node.type),
                left = convert(node.left),
                right = convert(node.right),
                predicate = node.predicate?.let { AstToLogicalVisitorTransform.transformExpr(it) } ?: lit(ionBool(true)),
                node.metas
            )
        }
}
