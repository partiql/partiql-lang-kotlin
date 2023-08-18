package org.partiql.planner.transforms

import com.amazon.ionelement.api.ionSymbol
import org.partiql.errors.Problem
import org.partiql.errors.ProblemHandler
import org.partiql.lang.ast.sourceLocation
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.domains.PartiqlLogicalToPartiqlLogicalResolvedVisitorTransform
import org.partiql.pig.runtime.asPrimitive
import org.partiql.planner.GlobalResolutionResult
import org.partiql.planner.GlobalVariableResolver
import org.partiql.planner.PlanningProblemDetails
import org.partiql.planner.impl.indexMeta
import org.partiql.planner.impl.sourceLocationMetaOrUnknown
import org.partiql.planner.impl.toBindingCase
import org.partiql.spi.BindingName

/** This class's subclasses represent the possible outcomes from an attempt to resolve a variable. */
private sealed class ResolvedVariable {

    /**
     * A success case, indicates the [uniqueId] of the match to the [BindingName] in the global scope.
     * Typically, this is defined by the storage layer.
     *
     * In the future, this will likely contain much more than just a unique id.  It might include detailed schema
     * information about global variables.
     */
    data class Global(val uniqueId: String) : ResolvedVariable()

    /**
     * A success case, indicates the [index] of the only possible match to the [BindingName] in a local lexical scope.
     * This is `internal` because [index] is an implementation detail that shouldn't be accessible outside of this
     * library.
     */
    data class LocalVariable(val index: Int) : ResolvedVariable()

    /** A failure case, indicates that resolution did not match any variable. */
    object Undefined : ResolvedVariable()
}

/**
 * Converts the public [GlobalResolutionResult] (which cannot represent local variables) to the private [ResolvedVariable],
 * which can represent local variables.
 */
private fun GlobalResolutionResult.toResolvedVariable() =
    when (this) {
        is GlobalResolutionResult.GlobalVariable -> ResolvedVariable.Global(this.uniqueId)
        GlobalResolutionResult.Undefined -> ResolvedVariable.Undefined
    }

/**
 * A local scope is a list of variable declarations that are produced by a relational operator and an optional
 * reference to a parent scope.  This is handled separately from global variables.
 *
 * This is a [List] of [PartiqlLogical.VarDecl] and not a [Map] or some other more efficient data structure
 * because most variable lookups are case-insensitive, which makes storing them in a [Map] and benefiting from it hard.
 */
private data class LocalScope(val varDecls: List<PartiqlLogical.VarDecl>)

internal data class LogicalToLogicalResolvedVisitorTransform(
    /** If set to `true`, do not log errors about undefined variables. Rewrite such variables to a `dynamic_id` node. */
    val allowUndefinedVariables: Boolean,
    /** Where to send error reports. */
    private val problemHandler: ProblemHandler,
    /** If a variable is not found using [inputScope], we will attempt to locate the binding here instead. */
    private val globals: GlobalVariableResolver,

) : PartiqlLogicalToPartiqlLogicalResolvedVisitorTransform() {
    /** The current [LocalScope]. */
    private var inputScope: LocalScope = LocalScope(emptyList())

    private enum class VariableLookupStrategy {
        LOCALS_THEN_GLOBALS,
        GLOBALS_THEN_LOCALS
    }

    companion object {

        /**
         * The name of this function is [DYNAMIC_LOOKUP_FUNCTION_NAME], which includes a unique prefix and suffix so as to
         * avoid clashes with user-defined functions.
         */
        internal const val DYNAMIC_LOOKUP_FUNCTION_NAME = "\$__dynamic_lookup__"
    }

    /**
     * This is set to [VariableLookupStrategy.GLOBALS_THEN_LOCALS] for the `<expr>` in `(scan <expr> ...)` nodes and
     * [VariableLookupStrategy.LOCALS_THEN_GLOBALS] for everything else.  This is we resolve globals first within
     * a `FROM`.
     */
    private var currentVariableLookupStrategy: VariableLookupStrategy = VariableLookupStrategy.LOCALS_THEN_GLOBALS

    private fun <T> withVariableLookupStrategy(nextVariableLookupStrategy: VariableLookupStrategy, block: () -> T): T {
        val lastVariableLookupStrategy = this.currentVariableLookupStrategy
        this.currentVariableLookupStrategy = nextVariableLookupStrategy
        return block().also {
            this.currentVariableLookupStrategy = lastVariableLookupStrategy
        }
    }

    private fun <T> withInputScope(nextScope: LocalScope, block: () -> T): T {
        val lastScope = inputScope
        inputScope = nextScope
        return block().also {
            inputScope = lastScope
        }
    }

    private fun PartiqlLogical.Expr.Id.asGlobalId(uniqueId: String): PartiqlLogicalResolved.Expr.GlobalId =
        PartiqlLogicalResolved.build {
            globalId_(
                uniqueId = uniqueId.asPrimitive(),
                metas = this@asGlobalId.metas
            )
        }

    private fun PartiqlLogical.Expr.Id.asLocalId(index: Int): PartiqlLogicalResolved.Expr =
        PartiqlLogicalResolved.build {
            localId_(index.asPrimitive(), this@asLocalId.metas)
        }

    private fun PartiqlLogical.Expr.Id.asErrorId(): PartiqlLogicalResolved.Expr =
        PartiqlLogicalResolved.build {
            localId_((-1).asPrimitive(), this@asErrorId.metas)
        }

    override fun transformPlan(node: PartiqlLogical.Plan): PartiqlLogicalResolved.Plan =
        PartiqlLogicalResolved.build {
            plan_(
                stmt = transformStatement(node.stmt),
                version = node.version,
                locals = emptyList(), // NOTE: locals will be populated by caller
                metas = node.metas
            )
        }

    override fun transformBexprScan_expr(node: PartiqlLogical.Bexpr.Scan): PartiqlLogicalResolved.Expr =
        withVariableLookupStrategy(VariableLookupStrategy.GLOBALS_THEN_LOCALS) {
            super.transformBexprScan_expr(node)
        }

    override fun transformBexprJoin_right(node: PartiqlLogical.Bexpr.Join): PartiqlLogicalResolved.Bexpr {
        // No need to change the current scope of the node.left.  Node.right gets the current scope +
        // the left output scope.
        val leftOutputScope = getOutputScope(node.left)
        val rightInputScope = inputScope.concatenate(leftOutputScope)
        return withInputScope(rightInputScope) {
            this.transformBexpr(node.right)
        }
    }

    override fun transformBexprLet(node: PartiqlLogical.Bexpr.Let): PartiqlLogicalResolved.Bexpr {
        val thiz = this
        return PartiqlLogicalResolved.build {
            let(
                source = transformBexpr(node.source),
                bindings = withInputScope(getOutputScope(node.source)) {
                    // This "wonderful" (depending on your definition of the term) bit of code performs a fold
                    // combined with a map... The accumulator is a Pair<List<PartiqlLogicalResolved.LetBinding>,
                    // LocalScope>.
                    // accumulator.first:  the current list of let bindings that have been transformed so far
                    // accumulator.second:  an instance of LocalScope that includes all the variables defined up to
                    // this point, not including the current let binding.
                    val initial = emptyList<PartiqlLogicalResolved.LetBinding>() to thiz.inputScope
                    val (newBindings: List<PartiqlLogicalResolved.LetBinding>, _: LocalScope) =
                        node.bindings.fold(initial) { accumulator, current ->
                            // Each let binding's expression should be resolved within the scope of the *last*
                            // let binding (or the current scope if this is the first let binding).
                            val resolvedValueExpr = withInputScope(accumulator.second) {
                                thiz.transformExpr(current.value)
                            }
                            val nextScope = LocalScope(listOf(current.decl)).concatenate(accumulator.second)
                            val transformedLetBindings = accumulator.first + PartiqlLogicalResolved.build {
                                letBinding(resolvedValueExpr, transformVarDecl(current.decl))
                            }
                            transformedLetBindings to nextScope
                        }
                    newBindings
                }
            )
        }
    }

    override fun transformBexprWindow_windowSpecification(node: PartiqlLogical.Bexpr.Window): PartiqlLogicalResolved.Over {
        val bindings = getOutputScope(node).concatenate(this.inputScope)
        return withInputScope(bindings) {
            node.windowSpecification.let {
                this.transformOver(it)
            }
        }
    }

    override fun transformBexprWindow_windowExpressionList(node: PartiqlLogical.Bexpr.Window): List<PartiqlLogicalResolved.WindowExpression> {
        val bindings = getOutputScope(node).concatenate(this.inputScope)
        return withInputScope(bindings) {
            node.windowExpressionList.map {
                this.transformWindowExpression(it)
            }
        }
    }

    // We are currently using bindings_to_values to denote a sub-query, which works for all the use cases we are
    // presented with today, as every SELECT statement is replaced with `bindings_to_values at the top level.
    override fun transformExprBindingsToValues(node: PartiqlLogical.Expr.BindingsToValues): PartiqlLogicalResolved.Expr =
        // If we are in the expr of a scan node, we need to reset the lookup strategy
        withVariableLookupStrategy(VariableLookupStrategy.LOCALS_THEN_GLOBALS) {
            super.transformExprBindingsToValues(node)
        }

    /**
     * Grabs the index meta added by [VariableIdAllocator] and stores it as an element in
     * [PartiqlLogicalResolved.VarDecl].
     */
    override fun transformVarDecl(node: PartiqlLogical.VarDecl): PartiqlLogicalResolved.VarDecl =
        PartiqlLogicalResolved.build {
            varDecl(node.indexMeta.toLong())
        }

    /**
     * Returns [GlobalResolutionResult.LocalVariable] if [bindingName] refers to a local variable.
     *
     * Otherwise, returns [GlobalResolutionResult.Undefined].  (Elsewhere, [globals] will be checked next.)
     */
    private fun resolveLocalVariable(bindingName: BindingName): ResolvedVariable {
        val found = this.inputScope.varDecls.firstOrNull { bindingName.isEquivalentTo(it.name.text) }
        return if (found == null) {
            ResolvedVariable.Undefined
        } else {
            ResolvedVariable.LocalVariable(found.indexMeta)
        }
    }

    /**
     * Resolves the logical `(id ...)` node node to a `(local_id ...)`, `(global_id ...)`, or dynamic `(id...)`
     * variable.
     */
    override fun transformExprId(node: PartiqlLogical.Expr.Id): PartiqlLogicalResolved.Expr {
        val bindingName = BindingName(node.name.text, node.case.toBindingCase())

        val globalResolutionResult = if (
            this.currentVariableLookupStrategy == VariableLookupStrategy.GLOBALS_THEN_LOCALS &&
            node.qualifier is PartiqlLogical.ScopeQualifier.Unqualified
        ) {
            // look up variable in globals first, then locals
            when (val resolvedVariable = globals.resolveGlobal(bindingName)) {
                GlobalResolutionResult.Undefined -> resolveLocalVariable(bindingName)
                else -> resolvedVariable.toResolvedVariable()
            }
        } else {
            // look up variable in locals first, then globals.
            when (val localResolutionResult = resolveLocalVariable(bindingName)) {
                ResolvedVariable.Undefined -> globals.resolveGlobal(bindingName).toResolvedVariable()
                else -> localResolutionResult
            }
        }
        return when (globalResolutionResult) {
            is ResolvedVariable.Global -> {
                node.asGlobalId(globalResolutionResult.uniqueId)
            }
            is ResolvedVariable.LocalVariable -> {
                node.asLocalId(globalResolutionResult.index)
            }
            ResolvedVariable.Undefined -> {
                if (this.allowUndefinedVariables) {
                    node.asDynamicLookupCallsite(
                        currentDynamicResolutionCandidates()
                            .map {
                                PartiqlLogicalResolved.build {
                                    localId(it.indexMeta.toLong())
                                }
                            }
                    )
                } else {
                    node.asErrorId().also {
                        problemHandler.handleProblem(
                            Problem(
                                (node.metas.sourceLocation ?: error("MetaContainer is missing SourceLocationMeta")).toProblemLocation(),
                                PlanningProblemDetails.UndefinedVariable(
                                    node.name.text,
                                    node.case is PartiqlLogical.CaseSensitivity.CaseSensitive
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    override fun transformStatementDml(node: PartiqlLogical.Statement.Dml): PartiqlLogicalResolved.Statement {
        // We only support DML targets that are global variables.
        val bindingName = BindingName(node.target.name.text, node.target.case.toBindingCase())
        val tableUniqueId = when (val resolvedVariable = globals.resolveGlobal(bindingName)) {
            is GlobalResolutionResult.GlobalVariable -> resolvedVariable.uniqueId
            GlobalResolutionResult.Undefined -> {
                problemHandler.handleProblem(
                    Problem(
                        node.metas.sourceLocationMetaOrUnknown.toProblemLocation(),
                        PlanningProblemDetails.UndefinedDmlTarget(
                            node.target.name.text,
                            node.target.case is PartiqlLogical.CaseSensitivity.CaseSensitive
                        )
                    )
                )
                "undefined DML target: ${node.target.name.text} - do not run"
            }
        }
        return PartiqlLogicalResolved.build {
            dml(
                uniqueId = tableUniqueId,
                operation = transformDmlOperation(node.operation),
                rows = transformExpr(node.rows),
                metas = node.metas
            )
        }
    }

    override fun transformDmlOperationDmlInsert(node: PartiqlLogical.DmlOperation.DmlInsert): PartiqlLogicalResolved.DmlOperation {
        return withInputScope(this.inputScope.concatenate(node.targetAlias)) {
            super.transformDmlOperationDmlInsert(node)
        }
    }

    override fun transformDmlOperationDmlReplace(node: PartiqlLogical.DmlOperation.DmlReplace): PartiqlLogicalResolved.DmlOperation {
        val scopeWithTarget = this.inputScope.concatenate(node.targetAlias)
        val inputScope = node.rowAlias?.let { scopeWithTarget.concatenate(it) } ?: scopeWithTarget
        return withInputScope(inputScope) {
            super.transformDmlOperationDmlReplace(node)
        }
    }

    override fun transformDmlOperationDmlUpdate(node: PartiqlLogical.DmlOperation.DmlUpdate): PartiqlLogicalResolved.DmlOperation {
        val scopeWithTarget = this.inputScope.concatenate(node.targetAlias)
        val inputScope = node.rowAlias?.let { scopeWithTarget.concatenate(it) } ?: scopeWithTarget
        return withInputScope(inputScope) {
            super.transformDmlOperationDmlUpdate(node)
        }
    }

    /**
     * Returns a list of variables accessible from the current scope which contain variables that may contain
     * an unqualified variable, in the order that they should be searched.
     */
    fun currentDynamicResolutionCandidates(): List<PartiqlLogical.VarDecl> =
        inputScope.varDecls.filter { it.includeInDynamicResolution }

    override fun transformExprBindingsToValues_exp(node: PartiqlLogical.Expr.BindingsToValues): PartiqlLogicalResolved.Expr {
        val bindings = getOutputScope(node.query).concatenate(this.inputScope)
        return withInputScope(bindings) {
            this.transformExpr(node.exp)
        }
    }

    override fun transformBexprSort_sortSpecs(node: PartiqlLogical.Bexpr.Sort): List<PartiqlLogicalResolved.SortSpec> {
        val bindings = getOutputScope(node.source).concatenate(this.inputScope)
        return withInputScope(bindings) {
            node.sortSpecs.map {
                this.transformSortSpec(it)
            }
        }
    }

    override fun transformBexprFilter_predicate(node: PartiqlLogical.Bexpr.Filter): PartiqlLogicalResolved.Expr {
        val bindings = getOutputScope(node.source)
        return withInputScope(bindings) {
            this.transformExpr(node.predicate)
        }
    }

    override fun transformBexprAggregate(node: PartiqlLogical.Bexpr.Aggregate): PartiqlLogicalResolved.Bexpr {
        val scope = getOutputScope(node.source).concatenate(this.inputScope)
        return PartiqlLogicalResolved.build {
            aggregate(
                source = transformBexpr(node.source),
                strategy = transformBexprAggregate_strategy(node),
                groupList = withInputScope(scope) { transformBexprAggregate_groupList(node) },
                functionList = withInputScope(scope) { transformBexprAggregate_functionList(node) },
                metas = transformBexprAggregate_metas(node)
            )
        }
    }

    override fun transformBexprJoin_predicate(node: PartiqlLogical.Bexpr.Join): PartiqlLogicalResolved.Expr? {
        val bindings = getOutputScope(node)
        return withInputScope(bindings) {
            node.predicate?.let { this.transformExpr(it) }
        }
    }

    /**
     * Rewrites PIVOT with resolved variables of the relevant scope
     */
    override fun transformExprPivot(node: PartiqlLogical.Expr.Pivot): PartiqlLogicalResolved.Expr {
        val scope = getOutputScope(node.input).concatenate(this.inputScope)
        return PartiqlLogicalResolved.build {
            pivot(
                input = transformBexpr(node.input),
                key = withInputScope(scope) { transformExpr(node.key) },
                value = withInputScope(scope) { transformExpr(node.value) },
                metas = transformMetas(node.metas)
            )
        }
    }

    /**
     * This should be called any time we create a [LocalScope] with more than one variable to prevent duplicate
     * variable names.  When checking for duplication, the letter case of the variable names is not considered.
     *
     * Example:
     *
     * ```
     * SELECT * FROM foo AS X AT x
     *       duplicate variable: ^
     * ```
     */
    private fun checkForDuplicateVariables(varDecls: List<PartiqlLogical.VarDecl>) {
        val usedVariableNames = hashSetOf<String>()
        varDecls.forEach { varDecl ->
            val loweredVariableName = varDecl.name.text.lowercase()
            if (usedVariableNames.contains(loweredVariableName)) {
                this.problemHandler.handleProblem(
                    Problem(
                        varDecl.metas.sourceLocation?.toProblemLocation() ?: error("VarDecl was missing source location meta"),
                        PlanningProblemDetails.VariablePreviouslyDefined(varDecl.name.text)
                    )
                )
            }
            usedVariableNames.add(loweredVariableName)
        }
    }

    /**
     * Computes a [LocalScope] for containing all of the variables that are output from [bexpr].
     */
    private fun getOutputScope(bexpr: PartiqlLogical.Bexpr): LocalScope =
        when (bexpr) {
            is PartiqlLogical.Bexpr.Filter -> getOutputScope(bexpr.source)
            is PartiqlLogical.Bexpr.Limit -> getOutputScope(bexpr.source)
            is PartiqlLogical.Bexpr.Offset -> getOutputScope(bexpr.source)
            is PartiqlLogical.Bexpr.Sort -> getOutputScope(bexpr.source)
            is PartiqlLogical.Bexpr.Aggregate -> {
                val keyVariables = bexpr.groupList.keys.map { it.asVar }
                val functionVariables = bexpr.functionList.functions.map { it.asVar }
                LocalScope(keyVariables + functionVariables)
            }
            is PartiqlLogical.Bexpr.Scan -> {
                LocalScope(
                    listOfNotNull(bexpr.asDecl.markForDynamicResolution(), bexpr.atDecl, bexpr.byDecl).also {
                        checkForDuplicateVariables(it)
                    }
                )
            }
            is PartiqlLogical.Bexpr.Unpivot -> {
                LocalScope(
                    listOfNotNull(bexpr.asDecl.markForDynamicResolution(), bexpr.atDecl, bexpr.byDecl).also {
                        checkForDuplicateVariables(it)
                    }
                )
            }
            is PartiqlLogical.Bexpr.Join -> {
                val (leftBexpr, rightBexpr) = when (bexpr.joinType) {
                    is PartiqlLogical.JoinType.Full,
                    is PartiqlLogical.JoinType.Inner,
                    is PartiqlLogical.JoinType.Left -> bexpr.left to bexpr.right
                    // right join is same as left join but right and left operands are swapped.
                    is PartiqlLogical.JoinType.Right -> bexpr.right to bexpr.left
                }
                val leftScope = getOutputScope(leftBexpr)
                val rightScope = getOutputScope(rightBexpr)
                // right scope is first to allow RHS variables to "shadow" LHS variables.
                rightScope.concatenate(leftScope)
            }
            is PartiqlLogical.Bexpr.Let -> {
                val sourceScope = getOutputScope(bexpr.source)
                // Note that .reversed() is important here to ensure that variable shadowing works correctly.
                val letVariables = bexpr.bindings.reversed().map { it.decl }
                sourceScope.concatenate(letVariables)
            }

            is PartiqlLogical.Bexpr.Window -> {
                val sourceScope = getOutputScope(bexpr.source)
                val windowVariable = bexpr.windowExpressionList.map { it.decl }
                sourceScope.concatenate(windowVariable)
            }
        }

    private fun LocalScope.concatenate(other: LocalScope): LocalScope =
        this.concatenate(other.varDecls)

    private fun LocalScope.concatenate(other: List<PartiqlLogical.VarDecl>): LocalScope {
        val concatenatedScopeVariables = this.varDecls + other
        return LocalScope(concatenatedScopeVariables)
    }

    private fun LocalScope.concatenate(other: PartiqlLogical.VarDecl): LocalScope {
        val concatenatedScopeVariables = this.varDecls + listOf(other)
        return LocalScope(concatenatedScopeVariables)
    }

    private fun PartiqlLogical.Expr.Id.asDynamicLookupCallsite(
        search: List<PartiqlLogicalResolved.Expr>
    ): PartiqlLogicalResolved.Expr {
        val caseSensitivityString = when (case) {
            is PartiqlLogical.CaseSensitivity.CaseInsensitive -> "case_insensitive"
            is PartiqlLogical.CaseSensitivity.CaseSensitive -> "case_sensitive"
        }
        val variableLookupStrategy = when (currentVariableLookupStrategy) {
            // If we are not in a FROM source, ignore the scope qualifier
            VariableLookupStrategy.LOCALS_THEN_GLOBALS -> VariableLookupStrategy.LOCALS_THEN_GLOBALS
            // If we are in a FROM source, allow scope qualifier to override the current variable lookup strategy.
            VariableLookupStrategy.GLOBALS_THEN_LOCALS -> when (this.qualifier) {
                is PartiqlLogical.ScopeQualifier.LocalsFirst -> VariableLookupStrategy.LOCALS_THEN_GLOBALS
                is PartiqlLogical.ScopeQualifier.Unqualified -> VariableLookupStrategy.GLOBALS_THEN_LOCALS
            }
        }.toString().lowercase()
        return PartiqlLogicalResolved.build {
            call(
                funcName = DYNAMIC_LOOKUP_FUNCTION_NAME,
                args = listOf(
                    lit(name.toIonElement()),
                    lit(ionSymbol(caseSensitivityString)),
                    lit(ionSymbol(variableLookupStrategy)),
                    list(search)
                ),
                metas = this@asDynamicLookupCallsite.metas
            )
        }
    }
}

/** Marks a variable for dynamic resolution--i.e. if undefined, this vardecl will be included in any dynamic_id lookup. */
private fun PartiqlLogical.VarDecl.markForDynamicResolution() = this.withMeta("\$include_in_dynamic_resolution", Unit)

/** Returns true of the [VarDecl] has been marked to participate in unqualified field resolution */
private val PartiqlLogical.VarDecl.includeInDynamicResolution get() = this.metas.containsKey("\$include_in_dynamic_resolution")
