package org.partiql.lang.planner.transforms

import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.ionSymbol
import org.partiql.lang.ast.IsGroupAttributeReferenceMeta
import org.partiql.lang.ast.IsOrderedMeta
import org.partiql.lang.ast.UniqueNameMeta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlAstToPartiqlLogicalVisitorTransform
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.metaContainerOf
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.errorContextFrom
import org.partiql.lang.eval.physical.sourceLocationMetaOrUnknown
import org.partiql.lang.eval.visitors.VisitorTransformBase
import org.partiql.lang.planner.PlanningProblemDetails
import org.partiql.lang.planner.handleUnimplementedFeature
import org.partiql.pig.runtime.SymbolPrimitive
import org.partiql.pig.runtime.toIonElement

internal fun PartiqlAst.Statement.toLogicalPlan(problemHandler: ProblemHandler): PartiqlLogical.Plan =
    PartiqlLogical.build {
        plan(
            AstToLogicalVisitorTransform(problemHandler).transformStatement(this@toLogicalPlan),
            version = PLAN_VERSION_NUMBER
        )
    }

/**
 * Transforms an instance of [PartiqlAst.Statement] to [PartiqlLogical.Statement].  This representation of the query
 * expresses the intent of the query author in terms of PartiQL's relational algebra instead of its AST.
 *
 * Performs no semantic checks.
 *
 * This conversion (and the logical algebra) are early in their lifecycle and so only a limited subset of SFW queries
 * are transformable.  See `AstToLogicalVisitorTransformTests` to see which queries are transformable.
 */
internal class AstToLogicalVisitorTransform(
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

        var (select, algebraAfterAggregation) = transformAggregations(node, algebra) ?: node to algebra

        algebraAfterAggregation = select.order?.let { orderBy ->
            val sortSpecs = orderBy.sortSpecs.map { sortSpec -> transformSortSpec(sortSpec) }
            PartiqlLogical.build { sort(algebraAfterAggregation, sortSpecs, orderBy.metas) }
        } ?: algebraAfterAggregation

        algebraAfterAggregation = select.offset?.let {
            PartiqlLogical.build { offset(transformExpr(it), algebraAfterAggregation, select.offset!!.metas) }
        } ?: algebraAfterAggregation

        algebraAfterAggregation = select.limit?.let {
            PartiqlLogical.build { limit(transformExpr(it), algebraAfterAggregation, select.limit!!.metas) }
        } ?: algebraAfterAggregation

        val expr = transformProjection(select, algebraAfterAggregation)

        // SELECT DISTINCT ...
        if (node.setq != null && node.setq is PartiqlAst.SetQuantifier.Distinct) {
            return PartiqlLogical.build { call("filter_distinct", expr) }
        }

        return expr
    }

    /**
     * Transforms the input [node] into a pair of two items:
     *  1. An AST (where all [PartiqlAst.Expr.CallAgg]'s in the input projection list are replaced with references
     *   ([PartiqlAst.Expr.Id]) to new [PartiqlLogical.VarDecl]'s)
     *  2. A [PartiqlLogical.Bexpr.Aggregate] with the necessary [PartiqlLogical.GroupKey]'s and
     *   [PartiqlLogical.AggregateFunction]'s (taken from the input [node]). Also, each [PartiqlLogical.GroupKey] and
     *   [PartiqlLogical.AggregateFunction] creates a [PartiqlLogical.VarDecl] that is used to create the aforementioned
     *   references (via [PartiqlAst.Expr.Id])
     *
     * Transforms a query like:
     * ```
     * SELECT newA, MAX(t.b) AS maxB
     * FROM t AS t
     * GROUP BY t.a AS newA GROUP AS g
     * ```
     * * into a logical plan similar to:
     * * ```
     * FROM
     *   - t AS t
     * AGGREGATION
     *   - Group Keys: t.a AS newA
     *   - Functions: (GROUP_AS AS g), (MAX AS aggregation_function_0)
     * PROJECT
     *   - newA
     *   - aggregation_function_0 AS maxB
     * ```
     */
    private fun transformAggregations(
        node: PartiqlAst.Expr.Select,
        source: PartiqlLogical.Bexpr
    ): Pair<PartiqlAst.Expr.Select, PartiqlLogical.Bexpr.Aggregate>? {
        val aggregationReplacer = ReplaceAllCallAggregationsVisitorTransform()
        val transformedNode = aggregationReplacer.transformExprSelect(node) as PartiqlAst.Expr.Select

        // Check if aggregation is necessary
        if (node.group == null && aggregationReplacer.getAggregations().isEmpty()) {
            return null
        }

        // Assert that projection identifiers all reference grouping attributes
        CheckProjectionsForGroups().walkExprSelect(node)

        return transformedNode to PartiqlLogical.build {
            val groupAsFunction = node.group?.groupAsAlias?.let { convertGroupAsAlias(it, node.from) }
            val aggFunctions = aggregationReplacer.getAggregations().map { (name, callAgg) -> convertCallAgg(callAgg, name) }
            aggregate(
                source = source,
                strategy = node.group?.strategy?.let { transformGroupingStrategy(it) } ?: groupFull(),
                groupList = groupKeyList(node.group?.keyList?.keys?.map { transformGroupKey(it) } ?: emptyList()),
                functionList = aggregateFunctionList(listOfNotNull(groupAsFunction) + aggFunctions),
                metas = node.group?.metas ?: emptyMetaContainer()
            )
        }
    }

    override fun transformGroupKey(node: PartiqlAst.GroupKey): PartiqlLogical.GroupKey {
        val thiz = this
        val varDeclName = node.asAlias?.metas?.get(UniqueNameMeta.TAG) as? UniqueNameMeta
        return PartiqlLogical.build {
            groupKey(
                expr = thiz.transformExpr(node.expr),
                asVar = varDecl(
                    name = varDeclName?.uniqueName ?: errAstNotNormalized(
                        "The group key should have encountered a unique name. This is typically added by the GroupByItemAliasVisitorTransform."
                    ),
                    metas = node.asAlias.metas
                )
            )
        }
    }

    private fun convertGroupAsAlias(node: SymbolPrimitive, from: PartiqlAst.FromSource) = PartiqlLogical.build {
        val sourceAliases = getSourceAliases(from)
        val structFields = sourceAliases.map { alias ->
            val aliasText = alias?.text ?: errAstNotNormalized("All FromSources should have aliases")
            structField(
                lit(aliasText.toIonElement()),
                id(aliasText, caseInsensitive(), unqualified())
            )
        }
        aggregateFunction(
            quantifier = all(),
            name = "group_as",
            arg = struct(structFields),
            asVar = varDecl_(node, node.metas),
            metas = node.metas
        )
    }

    private fun getSourceAliases(node: PartiqlAst.FromSource): List<SymbolPrimitive?> = when (node) {
        is PartiqlAst.FromSource.Scan -> listOf(node.asAlias ?: errAstNotNormalized("Scan should have alias initialized."))
        is PartiqlAst.FromSource.Join -> getSourceAliases(node.left).plus(getSourceAliases(node.right))
        is PartiqlAst.FromSource.GraphMatch -> listOf()
        is PartiqlAst.FromSource.Unpivot -> listOf(node.asAlias ?: errAstNotNormalized("Unpivot should have alias initialized."))
    }

    private fun convertCallAgg(node: PartiqlAst.Expr.CallAgg, name: String): PartiqlLogical.AggregateFunction = PartiqlLogical.build {
        aggregateFunction(
            quantifier = transformSetQuantifier(node.setq),
            name = node.funcName.text,
            arg = transformExpr(node.arg),
            asVar = varDecl(name, node.metas),
            metas = node.metas
        )
    }

    private fun transformProjection(node: PartiqlAst.Expr.Select, algebra: PartiqlLogical.Bexpr): PartiqlLogical.Expr {
        val project = node.project
        val metas = when (node.order) {
            null -> project.metas
            else -> project.metas + metaContainerOf(IsOrderedMeta.instance)
        }
        return PartiqlLogical.build {
            when (project) {
                is PartiqlAst.Projection.ProjectValue -> {
                    bindingsToValues(
                        exp = transformExpr(project.value),
                        query = algebra,
                        metas = metas
                    )
                }
                is PartiqlAst.Projection.ProjectList -> {
                    bindingsToValues(
                        exp = transformProjectList(project),
                        query = algebra,
                        metas = metas
                    )
                }
                is PartiqlAst.Projection.ProjectStar -> {
                    // `SELECT * FROM bar AS b` is rewritten to `SELECT b.* FROM bar as b` by
                    // [SelectStarVisitorTransform]. Therefore, there is no need to support `SELECT *` here.
                    errAstNotNormalized("Expected SELECT * to be removed")
                }
                is PartiqlAst.Projection.ProjectPivot -> {
                    pivot(
                        input = algebra,
                        key = transformExpr(project.key),
                        value = transformExpr(project.value),
                        metas = metas
                    )
                }
            }
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
                // within without the optional comma separated list of columns that precedes `VALUES` since doing so
                // requires
                // We block this by identifying (bag (list ...) ...) nodes which is how the parser represents the
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

                when (val conflictAction = dmlOp.conflictAction) {
                    null -> {
                        PartiqlLogical.build {
                            dml(
                                target = dmlOp.target.toDmlTargetId(),
                                operation = dmlInsert(),
                                rows = transformExpr(dmlOp.values),
                                metas = node.metas
                            )
                        }
                    }
                    is PartiqlAst.ConflictAction.DoReplace -> {
                        when (conflictAction.value) {
                            PartiqlAst.OnConflictValue.Excluded() -> PartiqlLogical.build {
                                dml(
                                    target = dmlOp.target.toDmlTargetId(),
                                    operation = dmlReplace(),
                                    rows = transformExpr(dmlOp.values),
                                    metas = node.metas
                                )
                            } else -> TODO("Only `DO REPLACE EXCLUDED` is supported in logical plan at the moment.")
                        }
                    }
                    is PartiqlAst.ConflictAction.DoUpdate -> TODO(
                        "`ON CONFLICT DO UPDATE` is not supported in logical plan yet."
                    )
                    is PartiqlAst.ConflictAction.DoNothing -> TODO(
                        "`ON CONFLICT DO NOTHING` is not supported in logical plan yet."
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
                                    target = node.from.expr.toDmlTargetId(),
                                    operation = dmlDelete(),
                                    // This query returns entire rows which are to be deleted, which is unfortunate
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

    private fun PartiqlAst.Expr.toDmlTargetId(): PartiqlLogical.Identifier {
        val dmlTargetId = when (this) {
            is PartiqlAst.Expr.Id -> PartiqlLogical.build {
                identifier_(name, transformCaseSensitivity(case), metas)
            }
            else -> {
                problemHandler.handleProblem(
                    Problem(
                        metas.sourceLocationMetaOrUnknown,
                        PlanningProblemDetails.InvalidDmlTarget
                    )
                )
                INVALID_DML_TARGET_ID
            }
        }
        return dmlTargetId
    }

    override fun transformStatementDdl(node: PartiqlAst.Statement.Ddl): PartiqlLogical.Statement {
        // It is an open question whether the planner will support DDL statements directly or if they must be handled by
        // some other construct.  For now, we just submit an error with problem details indicating these statements
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

    private fun transformProjectList(node: PartiqlAst.Projection.ProjectList): PartiqlLogical.Expr =
        PartiqlLogical.build {
            struct(
                List(node.projectItems.size) { idx ->
                    when (val projectItem = node.projectItems[idx]) {
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

    override fun convertUnpivot(node: PartiqlAst.FromSource.Unpivot): PartiqlLogical.Bexpr {
        val asAlias = node.asAlias ?: errAstNotNormalized("Expected as alias to be non-null")
        return PartiqlLogical.build {
            unpivot(
                toLogicalTransform.transformExpr(node.expr),
                varDecl_(asAlias, asAlias.metas),
                node.atAlias?.let { varDecl_(it, it.metas) },
                node.byAlias?.let { varDecl_(it, it.metas) },
                node.metas
            )
        }
    }

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
}

/**
 * Transforms [PartiqlAst.Expr.CallAgg]'s to [PartiqlAst.Expr.Id]'s that reference the new [PartiqlLogical.VarDecl].
 * Does not recurse into more than 1 [PartiqlAst.Expr.Select].
 */
private class ReplaceAllCallAggregationsVisitorTransform(var level: Int = 0) : VisitorTransformBase() {
    val callAggregationVisitorTransform = CallAggToIdVisitorTransform()
    val findAggregationsVisitor = FindAggregationsVisitor()

    override fun transformProjectItemProjectExpr_expr(node: PartiqlAst.ProjectItem.ProjectExpr): PartiqlAst.Expr {
        findAggregationsVisitor.walkProjectItem(node)
        val transformed = when (findAggregationsVisitor.containsCallAggregations) {
            true -> callAggregationVisitorTransform.transformExpr(node.expr)
            false -> super.transformProjectItemProjectExpr_expr(node)
        }
        findAggregationsVisitor.containsCallAggregations = false
        return transformed
    }

    override fun transformProjectionProjectValue_value(node: PartiqlAst.Projection.ProjectValue): PartiqlAst.Expr {
        findAggregationsVisitor.walkProjectionProjectValue(node)
        val transformed = when (findAggregationsVisitor.containsCallAggregations) {
            true -> callAggregationVisitorTransform.transformExpr(node.value)
            false -> super.transformProjectionProjectValue_value(node)
        }
        findAggregationsVisitor.containsCallAggregations = false
        return transformed
    }

    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
        return if (level++ == 0) super.transformExprSelect(node) else node
    }

    fun getAggregations() = callAggregationVisitorTransform.aggregations
}

/**
 * Determines whether the AST contains [PartiqlAst.Expr.CallAgg]s
 */
private class FindAggregationsVisitor() : PartiqlAst.Visitor() {
    var containsCallAggregations = false
    override fun visitExprCallAgg(node: PartiqlAst.Expr.CallAgg) {
        containsCallAggregations = true
    }
}

/**
 * Asserts whether each found [PartiqlAst.Expr.Id] is a Group Key or Group As reference by checking for the
 * existence of a [IsGroupAttributeReferenceMeta]. Does not recurse into more than 1 [PartiqlAst.Expr.Select].
 */
private class CheckProjectionsForGroups(var level: Int = 0) : PartiqlAst.Visitor() {

    override fun walkExprSelect(node: PartiqlAst.Expr.Select) {
        if (level == 0) {
            level++
            super.walkProjection(node.project)
        }
    }

    override fun visitExprId(node: PartiqlAst.Expr.Id) {
        if (node.metas.containsKey(IsGroupAttributeReferenceMeta.TAG).not()) {
            throw EvaluationException(
                "Variable not in GROUP BY or aggregation function: ${node.name.text}",
                ErrorCode.EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY,
                errorContextFrom(node.metas).also {
                    it[Property.BINDING_NAME] = node.name.text
                },
                internal = false
            )
        }
    }

    override fun walkExprCallAgg(node: PartiqlAst.Expr.CallAgg) {
        return
    }
}

/**
 * Created to be invoked by the [ReplaceAllCallAggregationsVisitorTransform] to transform projection items and assert
 * that any remaining variable references are either GROUP BY KEYS, GROUP BY KEY ALIASES, or GROUP AS ALIASES.
 *
 * Also transforms CallAgg with a reference to the result identifier.
 *
 * As an example, this transforms:
 * ```
 * SELECT g, SUM(t.b) AS sumB
 * FROM t
 * GROUP BY t.a AS g GROUP AS h
 * ```
 *
 * into:
 *
 * ```
 * SELECT g, t.b, h, $__partiql_aggregation_0 AS sumB
 * FROM t
 * GROUP BY t.a AS g, t.b GROUP AS h
 * ```
 *
 */
private class CallAggToIdVisitorTransform() : VisitorTransformBase() {
    private var varDeclIncrement = 0
    val aggregations = mutableSetOf<Pair<String, PartiqlAst.Expr.CallAgg>>()

    override fun transformExprCallAgg(node: PartiqlAst.Expr.CallAgg): PartiqlAst.Expr {
        val name = getAggregationIdName()
        aggregations.add(name to node)
        return PartiqlAst.build {
            id(
                name = name,
                case = caseInsensitive(),
                qualifier = unqualified(),
                metas = node.metas
            )
        }
    }

    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
        return node
    }

    /**
     * Returns unique (per [PartiqlAst.Expr.Select]) strings for each aggregation.
     */
    private fun getAggregationIdName(): String = "\$__partiql_aggregation_${varDeclIncrement++}"
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

private val INVALID_DML_TARGET_ID = PartiqlLogical.build {
    identifier("this is a placeholder for an invalid DML target - do not run", caseInsensitive())
}
