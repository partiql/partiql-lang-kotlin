package org.partiql.lang.planner.transforms

import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.ionSymbol
import org.partiql.lang.ast.IsGroupAttributeReferenceMeta
import org.partiql.lang.ast.UniqueNameMeta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlAstToPartiqlLogicalVisitorTransform
import org.partiql.lang.domains.PartiqlLogical
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

        var selectAlgebraPair = transformAggregations(node, algebra) ?: (node to algebra)

        selectAlgebraPair = transformWindowFunctions(selectAlgebraPair.first, selectAlgebraPair.second) ?: selectAlgebraPair

        val select = selectAlgebraPair.first

        algebra = selectAlgebraPair.second

        algebra = select.order?.let { orderBy ->
            val sortSpecs = orderBy.sortSpecs.map { sortSpec -> transformSortSpec(sortSpec) }
            PartiqlLogical.build { sort(algebra, sortSpecs, orderBy.metas) }
        } ?: algebra

        algebra = select.offset?.let {
            PartiqlLogical.build { offset(transformExpr(it), algebra, select.offset!!.metas) }
        } ?: algebra

        algebra = select.limit?.let {
            PartiqlLogical.build { limit(transformExpr(it), algebra, select.limit!!.metas) }
        } ?: algebra

        val expr = transformProjection(select, algebra)

        // SELECT DISTINCT ...
        if (node.setq != null && node.setq is PartiqlAst.SetQuantifier.Distinct) {
            return PartiqlLogical.build { call("filter_distinct", expr) }
        }

        return expr
    }

    /**
     * Transforms the input [node] into a pair of two items:
     *  1. An AST (where all [PartiqlAst.Expr.CallWindow]'s in the input projection list are replaced with references
     *   ([PartiqlAst.Expr.Id]) to new [PartiqlLogical.VarDecl]'s)
     *  2. A modified algebra such that each window function call in the input projection list corresponding to one window operator
     *  TODO: if multiple window functions are operating on the same window, we can potentially add them in a single window operator to increase performance
     *
     * Transforms a query like:
     * ```
     * SELECT LAG(t.a) OVER (...), LEAD(t.a) OVER (...)
     * FROM t AS t
     * ```
     * into a logical plan similar to:
     * ```
     * bindings_to_values
     *   struct ...
     *   window
     *      window
     *          scan
     *          over
     *          window_expression
     *              $__partiql_window_function_0
     *              LAG
     *              t.a
     *      over
     *      window_expression
     *          $__partiql_window_function_1
     *          LEAD
     *          t.a
     * ```
     */
    private fun transformWindowFunctions(node: PartiqlAst.Expr.Select, algebra: PartiqlLogical.Bexpr): Pair<PartiqlAst.Expr.Select, PartiqlLogical.Bexpr>? {
        val windowReplacer = CurrentProjectionListWindowFunctionTransform()

        val transformedNode = windowReplacer.transformExprSelect(node) as PartiqlAst.Expr.Select

        val windowExpressions = windowReplacer.getWindowFuncs()

        if (windowExpressions.isEmpty()) {
            return null
        }

        var modifiedAlgebra = algebra
        windowExpressions.forEach { callWindow ->
            val callWindowNode = callWindow.second
            val windowFuncGeneratedName = callWindow.first
            modifiedAlgebra =
                PartiqlLogical.build {
                    window(
                        modifiedAlgebra,
                        transformOver(callWindowNode.over),
                        PartiqlLogical.build {
                            windowExpression(
                                varDecl(windowFuncGeneratedName),
                                callWindowNode.funcName.text,
                                callWindowNode.args.map { arg ->
                                    transformExpr(arg)
                                },
                                metas = node.project.metas
                            )
                        }
                    )
                }
        }
        return transformedNode to modifiedAlgebra
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
        val aggregationReplacer = CallAggregationsProjectionReplacer()
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
        return PartiqlLogical.build {
            when (val project = node.project) {
                is PartiqlAst.Projection.ProjectValue -> {
                    bindingsToValues(
                        exp = transformExpr(project.value),
                        query = algebra,
                        metas = project.metas
                    )
                }
                is PartiqlAst.Projection.ProjectList -> {
                    bindingsToValues(
                        exp = transformProjectList(project),
                        query = algebra,
                        metas = project.metas
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
                        metas = project.metas
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
                    is PartiqlAst.ConflictAction.DoUpdate -> {
                        when (conflictAction.value) {
                            PartiqlAst.OnConflictValue.Excluded() -> PartiqlLogical.build {
                                dml(
                                    target = dmlOp.target.toDmlTargetId(),
                                    operation = dmlUpdate(),
                                    rows = transformExpr(dmlOp.values),
                                    metas = node.metas
                                )
                            }
                            else -> TODO("Only `DO UPDATE EXCLUDED` is supported in logical plan at the moment.")
                        }
                    }
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

    override fun transformExprCallWindow(node: PartiqlAst.Expr.CallWindow): PartiqlLogical.Expr =
        error("Call window node is not transformed (This shall never happend)")

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
 * Given a [PartiqlAst.Expr.Select], transforms all [PartiqlAst.Expr.CallWindow]'s within the projection list to
 * [PartiqlAst.Expr.Id]'s that reference the new [PartiqlLogical.VarDecl].
 * We only want this to convert the window function in the current projection list without recuse into any sub-query.
 *
 * For example:
 * Consider:
 * SELECT
 *  aWinFunc
 * FROM (
 *  SELECT
 *      anotherWinFunc
 *  FROM
 *  ...
 * )
 *
 * The transformation Order is as follows:
 * FROM <-- This is the outer FROM
 *      FROM <- This is the inner FROM
 *          CurrentProjectionListWindowFunctionTransform (1)
 *      SELECT <- This is the inner SELECT
 *          anotherWinFunc <- transformed By CurrentProjectionListWindowFunctionTransform (1)
 *          Transform Projection
 * CurrentProjectionListWindowFunctionTransform (2)
 * SELECT <- This is the outer SELECT
 *      aWindFunc <- transformed By CurrentProjectionListWindowFunctionTransform (2)
 * TransformProjection
 */
private class CurrentProjectionListWindowFunctionTransform(var level: Int = 0) : VisitorTransformBase() {
    val callWindowFunctionVisitorTransform = CallWindowReplacer()

    override fun transformProjectItemProjectExpr_expr(node: PartiqlAst.ProjectItem.ProjectExpr): PartiqlAst.Expr {
        return callWindowFunctionVisitorTransform.transformExpr(node.expr)
    }

    override fun transformProjectionProjectValue_value(node: PartiqlAst.Projection.ProjectValue): PartiqlAst.Expr {
        return callWindowFunctionVisitorTransform.transformExpr(node.value)
    }

    // we don't want to nested in sub-queries, otherwise the inner window function get transformed multiple times.
    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr =
        if (level == 0) {
            level += 1
            super.transformExprSelect(node)
        } else {
            node
        }

    fun getWindowFuncs() = callWindowFunctionVisitorTransform.windowFunctions
}

/**
 * Created to be invoked by the [CurrentProjectionListWindowFunctionTransform] to transform all encountered [PartiqlAst.Expr.CallWindow]'s to
 * [PartiqlAst.Expr.Id]'s. This class is designed to be called directly on [PartiqlAst.Projection]'s and does NOT recurse
 * into [PartiqlAst.Expr.Select]'s if the projection list contains a Select Node.
 */
private class CallWindowReplacer : VisitorTransformBase() {
    private var varDeclIncrement = 0
    val windowFunctions = mutableSetOf<Pair<String, PartiqlAst.Expr.CallWindow>>()
    override fun transformExprCallWindow(node: PartiqlAst.Expr.CallWindow): PartiqlAst.Expr {
        val name = getAggregationIdName()
        windowFunctions.add(name to node)
        return PartiqlAst.build {
            id(
                name = name,
                case = caseInsensitive(),
                qualifier = unqualified(),
                metas = node.metas
            )
        }
    }

    // If this function is called, then the projection list contains a Select Node.
    // Regardless whether that select node's projection list contains window function
    // we do not want to transform it at the moment
    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
        return node
    }

    /**
     * Returns unique (per [PartiqlAst.Expr.Select]) strings for each window function.
     */
    private fun getAggregationIdName(): String = "\$__partiql_window_function_${varDeclIncrement++}"
}

/**
 * Given a [PartiqlAst.Expr.Select], transforms all [PartiqlAst.Expr.CallAgg]'s within the projection list to
 * [PartiqlAst.Expr.Id]'s that reference the new [PartiqlLogical.VarDecl].
 * Does not recurse into more than 1 [PartiqlAst.Expr.Select]. Designed to be invoked directly on a
 * [PartiqlAst.Expr.Select] using [transformExprSelect].
 */
private class CallAggregationsProjectionReplacer(var level: Int = 0) : VisitorTransformBase() {
    val callAggregationVisitorTransform = CallAggregationReplacer()

    override fun transformProjectItemProjectExpr_expr(node: PartiqlAst.ProjectItem.ProjectExpr): PartiqlAst.Expr {
        return callAggregationVisitorTransform.transformExpr(node.expr)
    }

    override fun transformProjectionProjectValue_value(node: PartiqlAst.Projection.ProjectValue): PartiqlAst.Expr {
        return callAggregationVisitorTransform.transformExpr(node.value)
    }

    override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
        return if (level++ == 0) super.transformExprSelect(node) else node
    }

    fun getAggregations() = callAggregationVisitorTransform.aggregations
}

/**
 * Created to be invoked by the [CallAggregationsProjectionReplacer] to transform all encountered [PartiqlAst.Expr.CallAgg]'s to
 * [PartiqlAst.Expr.Id]'s. This class is designed to be called directly on [PartiqlAst.Projection]'s and does NOT recurse
 * into [PartiqlAst.Expr.Select]'s. This class is designed to be instantiated once per aggregation scope. The class collects
 * all unique-per-scope aggregation variable declaration names and exposes it to the calling class.
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
private class CallAggregationReplacer() : VisitorTransformBase() {
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

/**
 * Asserts whether each found [PartiqlAst.Expr.Id] is a Group Key or Group As reference by checking for the
 * existence of a [IsGroupAttributeReferenceMeta]. Does not recurse into more than 1 [PartiqlAst.Expr.Select]. Designed
 * to be called directly on a single [PartiqlAst.Expr.Select].
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
