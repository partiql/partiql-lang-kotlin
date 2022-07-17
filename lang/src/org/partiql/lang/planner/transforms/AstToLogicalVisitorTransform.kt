package org.partiql.lang.planner.transforms

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlAstToPartiqlLogicalVisitorTransform
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.errors.Problem
import org.partiql.lang.eval.physical.sourceLocationMetaOrUnknown
import org.partiql.lang.planner.PlanningProblemDetails
import org.partiql.lang.planner.abortQueryPlanning
import org.partiql.lang.planner.abortUnimplementedFeature

/**
 * Transforms an instance of [PartiqlAst.Statement] to [PartiqlLogical.Statement].  This representation of the query
 * expresses the intent of the query author in terms of PartiQL's relational algebra instead of it its AST.
 *
 * Performs no semantic checks.
 *
 * This conversion (and the logical algebra) are early in their lifecycle and so only a limited subset of SFW queries
 * are transformable.  See `AstToLogicalVisitorTransformTests` to see which queries are transformable.
 */
internal fun PartiqlAst.Statement.toLogicalPlan(): PartiqlLogical.Plan =
    PartiqlLogical.build {
        plan(
            AstToLogicalVisitorTransform.transformStatement(this@toLogicalPlan),
            version = PLAN_VERSION_NUMBER
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
            node.group != null -> abortUnimplementedFeature(node, "GROUP BY")
            node.order != null -> abortUnimplementedFeature(node, "ORDER BY")
            node.having != null -> abortUnimplementedFeature(node, "HAVING")
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
        // `INSERT` and `DELETE` statements are all that's needed for the current effort--and it just so
        // happens that these never utilize more than one DML operation anyway.  We don't need to
        // support more than one DML operation until we start supporting UPDATE statements.
        if (node.operations.ops.size != 1) {
            abortUnimplementedFeature(node, "more than one DML operation")
        }
        val dmlOp = node.operations.ops.single()

        // All
        return when (dmlOp) {
            is PartiqlAst.DmlOp.Insert -> {
                node.from?.let { TODO("FROM / INSERT ") }
                // Check for and block `INSERT INTO <tbl> VALUES (...)`  This is *no* way to support this
                // within PartiQL itself since this flavor requires schema which we do not yet have.
                // We block this by identifying (bag (list ...) ...) nodes which  is how the parser represents the
                // VALUES constructor.  Since parser uses the same nodes for the alternate syntactic representations
                // `<< [ ... ] ... >>` and `BAG(LIST(...), ...)` those get blocked too.  This is probably just as well.
                if (dmlOp.values is PartiqlAst.Expr.Bag) {
                    dmlOp.values.values.firstOrNull { it is PartiqlAst.Expr.List }?.let {
                        abortQueryPlanning(
                            Problem(
                                it.metas.sourceLocationMetaOrUnknown,
                                PlanningProblemDetails.InsertValuesDisallowed
                            )
                        )
                    }
                }

                PartiqlLogical.build {
                    dml(
                        target = dmlTarget(transformExpr(dmlOp.target)),
                        operation = dmlInsert(),
                        rows = transformExpr(dmlOp.values),
                        metas = node.metas
                    )
                }
            }
            // INSERT single row with VALUE is disallowed use INSERT INTO <target> << <value>, <value> >> instead.
            is PartiqlAst.DmlOp.InsertValue -> {
                abortQueryPlanning(
                    Problem(
                        node.metas.sourceLocationMetaOrUnknown,
                        PlanningProblemDetails.InsertValueDisallowed
                    )
                )
            }
            is PartiqlAst.DmlOp.Delete -> {
                val relation = node.from?.let { FromSourceToBexpr.convert(it) }
                if (node.from == null) {
                    // unfortunately, the AST allows malformations such as this however the parser should
                    // never actually create an AST for a DELETE statement without a FROM clause.
                    error("Malformed AST: DELETE without FROM (this should never happen)")
                } else {
                    val scan = relation as? PartiqlLogical.Bexpr.Scan
                        ?: abortQueryPlanning(
                            Problem(
                                node.metas.sourceLocationMetaOrUnknown,
                                PlanningProblemDetails.InvalidDmlTarget
                            )
                        )

                    val predicate = node.where?.let { transformExpr(it) }
                    val rows = if (predicate == null) scan else PartiqlLogical.build { filter(predicate, scan) }

                    PartiqlLogical.build {
                        dml(
                            target = dmlTarget(scan.expr),
                            operation = dmlDelete(),
                            rows = bindingsToValues(
                                // This returns entire rows which are to be deleted, which is very naive but
                                // is unavoidable without knowledge of schema. PartiQL embedders may apply a
                                // pass over the resolved logical (oor later) plan that changes this to only include
                                // the primary keys of the rows to be deleted.
                                exp = id(scan.asDecl.name.text, caseSensitive(), unqualified()),
                                query = rows,
                            ),
                            metas = node.metas
                        )
                    }
                }
            }
            is PartiqlAst.DmlOp.Remove -> {
                abortQueryPlanning(
                    Problem(node.metas.sourceLocationMetaOrUnknown, PlanningProblemDetails.InvalidUseOfRemove)
                )
            }
            is PartiqlAst.DmlOp.Set -> {
                abortQueryPlanning(
                    Problem(node.metas.sourceLocationMetaOrUnknown, PlanningProblemDetails.InvalidUseOfSet)
                )
            }
        }
    }

    override fun transformStatementDdl(node: PartiqlAst.Statement.Ddl): PartiqlLogical.Statement = abortQueryPlanning(
        Problem(node.metas.sourceLocationMetaOrUnknown, PlanningProblemDetails.DdlUnsupported)
    )

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
        abortUnimplementedFeature(node, "UNPIVOT")
    }

    override fun convertJoin(node: PartiqlAst.FromSource.Join): PartiqlLogical.Bexpr =
        PartiqlLogical.build {
            join(
                joinType = AstToLogicalVisitorTransform.transformJoinType(node.type),
                left = convert(node.left),
                right = convert(node.right),
                predicate = node.predicate?.let { AstToLogicalVisitorTransform.transformExpr(it) },
                node.metas
            )
        }

    override fun convertGraphMatch(node: PartiqlAst.FromSource.GraphMatch): PartiqlLogical.Bexpr =
        abortUnimplementedFeature(node, "MATCH")
}
