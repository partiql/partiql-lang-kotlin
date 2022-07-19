package org.partiql.lang.planner.transforms

import com.amazon.ionelement.api.ionSymbol
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlAstToPartiqlLogicalVisitorTransform
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.eval.physical.sourceLocationMetaOrUnknown
import org.partiql.lang.planner.PlanningProblemDetails
import org.partiql.lang.planner.handleUnimplementedFeature

/**
 * Transforms an instance of [PartiqlAst.Statement] to [PartiqlLogical.Statement].  This representation of the query
 * expresses the intent of the query author in terms of PartiQL's relational algebra instead of it its AST.
 *
 * Performs no semantic checks.
 *
 * This conversion (and the logical algebra) are early in their lifecycle and so only a limited subset of SFW queries
 * are transformable.  See `AstToLogicalVisitorTransformTests` to see which queries are transformable.
 */
internal fun PartiqlAst.Statement.toLogicalPlan(problemHandler: ProblemHandler): PartiqlLogical.Plan =
    PartiqlLogical.build {
        plan(
            AstToLogicalVisitorTransform(problemHandler).transformStatement(this@toLogicalPlan),
            version = PLAN_VERSION_NUMBER
        )
    }

private class AstToLogicalVisitorTransform(
    val problemHandler: ProblemHandler
) : PartiqlAstToPartiqlLogicalVisitorTransform() {

    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlLogical.Expr {
        checkForUnsupportedSelectClauses(node)

        var algebra: PartiqlLogical.Bexpr = node.from.toBexpr(this, problemHandler)

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

                    is PartiqlAst.Projection.ProjectPivot -> {
                        problemHandler.handleUnimplementedFeature(node, "PIVOT")
                        INVALID_EXPR
                    }
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
            node.group != null -> problemHandler.handleUnimplementedFeature(node.group, "GROUP BY")
            node.order != null -> problemHandler.handleUnimplementedFeature(node.order, "ORDER BY")
            node.having != null -> problemHandler.handleUnimplementedFeature(node.having, "HAVING")
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
        require(node.operations.ops.isNotEmpty())

        // `INSERT` and `DELETE` statements are all that's needed for the current effort--and it just so
        // happens that these never utilize more than one DML operation anyway.  We don't need to
        // support more than one DML operation until we start supporting UPDATE statements.
        if (node.operations.ops.size > 1) {
            problemHandler.handleUnimplementedFeature(node, "more than one DML operation")
        }

        return when (val dmlOp = node.operations.ops.first()) {
            is PartiqlAst.DmlOp.Insert -> {
                node.from?.let { problemHandler.handleUnimplementedFeature(dmlOp, "UPDATE / INSERT") }
                // Check for and block `INSERT INTO <tbl> VALUES (...)`  This is *no* way to support this
                // within PartiQL itself since this flavor requires schema which we do not yet have.
                // We block this by identifying (bag (list ...) ...) nodes which  is how the parser represents the
                // VALUES constructor.  Since parser uses the same nodes for the alternate syntactic representations
                // `<< [ ... ] ... >>` and `BAG(LIST(...), ...)` those get blocked too.  This is probably just as well.
                if (dmlOp.values is PartiqlAst.Expr.Bag) {
                    dmlOp.values.values.firstOrNull { it is PartiqlAst.Expr.List }?.let {
                        problemHandler.handleProblem(
                            Problem(
                                node.metas.sourceLocationMetaOrUnknown,
                                PlanningProblemDetails.InsertValuesDisallowed
                            )
                        )
                    }
                }

                PartiqlLogical.build {
                    dml(
                        target = transformExpr(dmlOp.target),
                        operation = dmlInsert(),
                        rows = transformExpr(dmlOp.values),
                        metas = node.metas
                    )
                }
            }
            // INSERT single row with VALUE is disallowed. (This variation of INSERT might be removed in a future
            // release of PartiQL.)
            is PartiqlAst.DmlOp.InsertValue -> {
                problemHandler.handleProblem(
                    Problem(
                        node.metas.sourceLocationMetaOrUnknown,
                        PlanningProblemDetails.InsertValueDisallowed
                    )
                )
                INVALID_STATEMENT
            }
            is PartiqlAst.DmlOp.Delete -> {
                if (node.from == null) {
                    // unfortunately, the AST allows malformations such as this however the parser should
                    // never actually create an AST for a DELETE statement without a FROM clause.
                    error("Malformed AST: DELETE without FROM (this should never happen)")
                } else {
                    when (node.from) {
                        is PartiqlAst.FromSource.Scan -> {
                            val rowsSource = node.from.toBexpr(this, problemHandler) as PartiqlLogical.Bexpr.Scan
                            val predicate = node.where?.let { transformExpr(it) }
                            val rows = if (predicate == null) {
                                rowsSource
                            } else {
                                PartiqlLogical.build { filter(predicate, rowsSource) }
                            }

                            PartiqlLogical.build {
                                dml(
                                    target = transformExpr(node.from.expr),
                                    operation = dmlDelete(),
                                    // This query returns entire rows which are to be deleted, which is very but
                                    // unavoidable without knowledge of schema. PartiQL embedders may apply a
                                    // pass over the resolved logical (or later) plan that changes this to only
                                    // include the primary keys of the rows to be deleted.
                                    rows = bindingsToValues(
                                        exp = id(rowsSource.asDecl.name.text, caseSensitive(), unqualified()),
                                        query = rows,
                                    ),
                                    metas = node.metas
                                )
                            }
                        }
                        else -> {
                            problemHandler.handleProblem(
                                Problem(
                                    node.from.metas.sourceLocationMetaOrUnknown,
                                    PlanningProblemDetails.InvalidDmlTarget
                                )
                            )
                            INVALID_STATEMENT
                        }
                    }
                }
            }
            is PartiqlAst.DmlOp.Remove -> {
                problemHandler.handleProblem(
                    Problem(dmlOp.metas.sourceLocationMetaOrUnknown, PlanningProblemDetails.UnimplementedFeature("REMOVE"))
                )
                INVALID_STATEMENT
            }
            is PartiqlAst.DmlOp.Set -> {
                problemHandler.handleProblem(
                    Problem(dmlOp.metas.sourceLocationMetaOrUnknown, PlanningProblemDetails.UnimplementedFeature("SET"))
                )
                INVALID_STATEMENT
            }
        }
    }

    override fun transformStatementDdl(node: PartiqlAst.Statement.Ddl): PartiqlLogical.Statement {
        // It is an open question whether the planner will support DDL statements directly or if they must be handled by
        // some other construct.  For now, we just abort the query with problem details indicating these statements
        // are not implemented.
        problemHandler.handleProblem(
            Problem(
                node.metas.sourceLocationMetaOrUnknown,
                PlanningProblemDetails.UnimplementedFeature(
                    when (node.op) {
                        is PartiqlAst.DdlOp.CreateIndex -> "CREATE INDEX"
                        is PartiqlAst.DdlOp.CreateTable -> "CREATE TABLE"
                        is PartiqlAst.DdlOp.DropIndex -> "DROP INDEX"
                        is PartiqlAst.DdlOp.DropTable -> "DROP TABLE"
                    }
                )
            )
        )
        return INVALID_STATEMENT
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

private fun PartiqlAst.FromSource.toBexpr(
    toLogicalTransform: AstToLogicalVisitorTransform,
    problemHandler: ProblemHandler
) =
    FromSourceToBexpr(toLogicalTransform, problemHandler).convert(this)

private class FromSourceToBexpr(
    val toLogicalTransform: AstToLogicalVisitorTransform,
    val problemHandler: ProblemHandler
) : PartiqlAst.FromSource.Converter<PartiqlLogical.Bexpr> {

    override fun convertScan(node: PartiqlAst.FromSource.Scan): PartiqlLogical.Bexpr {
        val asAlias = node.asAlias ?: errAstNotNormalized("Expected as alias to be non-null")
        return PartiqlLogical.build {
            scan(
                toLogicalTransform.transformExpr(node.expr),
                varDecl_(asAlias, asAlias.metas),
                node.atAlias?.let { varDecl_(it, it.metas) },
                node.byAlias?.let { varDecl_(it, it.metas) },
                node.metas
            )
        }
    }

    override fun convertUnpivot(node: PartiqlAst.FromSource.Unpivot): PartiqlLogical.Bexpr =
        INVALID_BEXPR.also { problemHandler.handleUnimplementedFeature(node, "UNPIVOT") }

    override fun convertJoin(node: PartiqlAst.FromSource.Join): PartiqlLogical.Bexpr =
        PartiqlLogical.build {
            join(
                joinType = toLogicalTransform.transformJoinType(node.type),
                left = convert(node.left),
                right = convert(node.right),
                predicate = node.predicate?.let { toLogicalTransform.transformExpr(it) },
                node.metas
            )
        }

    override fun convertGraphMatch(node: PartiqlAst.FromSource.GraphMatch): PartiqlLogical.Bexpr =
        INVALID_BEXPR.also { problemHandler.handleUnimplementedFeature(node, "MATCH") }
}

private val INVALID_STATEMENT = PartiqlLogical.build {
    query(lit(ionSymbol("this is a placeholder for an invalid statement - do not run")))
}

private val INVALID_BEXPR = PartiqlLogical.build {
    scan(lit(ionSymbol("this is a placeholder for an invalid relation - do not run")), varDecl("invalid"))
}

private val INVALID_EXPR = PartiqlLogical.build {
    lit(ionSymbol("this is a placeholder for an invalid expression - do not run"))
}
