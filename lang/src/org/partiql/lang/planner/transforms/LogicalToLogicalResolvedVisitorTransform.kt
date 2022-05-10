package org.partiql.lang.planner.transforms

import com.amazon.ionelement.api.ionSymbol
import org.partiql.lang.ast.sourceLocation
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.domains.PartiqlLogicalToPartiqlLogicalResolvedVisitorTransform
import org.partiql.lang.domains.toBindingCase
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.builtins.DYNAMIC_LOOKUP_FUNCTION_NAME
import org.partiql.lang.planner.GlobalBindings
import org.partiql.lang.planner.ResolutionResult
import org.partiql.pig.runtime.asPrimitive

/**
 * Resolves all variables by rewriting `(id <name> <case-sensitivity> <scope-qualifier>)` to
 * `(id <name> <unique-index>)`) or `(global_id <name> <unique-id>)`.  The latter is usually a reference to
 * a database table.  `<unique-id>` is supplied by the integrating PartiQL service by means of the [globals] specified
 * by callers of this function.  Note that in general, all `(scan (global_id ...) ...)` operators will later be
 * rewritten to an optimized physical read operator.
 *
 * The [problemHandler] is notified of any undefined variables.  Resolution does not stop on the first error, rather
 * we keep going to provide the end user any additional error messaging, unless [ProblemHandler.handleProblem] throws
 * an exception when an error is logged.  **If any undefined variables are detected, in order to allow traversal to
 * continue, a fake index value is used in place of a real one and the resolved logical plan returned by this function
 * is guaranteed to be invalid.** **Therefore, it is the responsibility therefore of callers to check if any problems
 * have been logged with [org.partiql.lang.errors.ProblemSeverity.ERROR] and to abort further query planning if
 * necessary.**
 *
 * Local variables are resolved independently within this pass, but we rely on [globals] to resolve global variables.
 *
 * Ths works in two passes:
 * 1. All [PartiqlLogical.VarDecl] nodes are allocated unique indexes (which is stored in a meta).
 * 2. Then, during the transform from the `partiql_logical` domain to the `partiql_logical_resolved` domain, we
 * determine if the `id` node refers to a global variable or local variable.  For global variables, the `id` node is
 * replaced with `(global_id <name> <unique-id>)`.  For local variables, the original `id` node is replaced with a
 * `(id <name> <unique-index>)`), where `<unique-index>` is the index of the corresponding `var_decl`.
 */
internal fun PartiqlLogical.Plan.toResolvedPlan(
    problemHandler: ProblemHandler,
    globals: GlobalBindings,
    allowUndefinedVariables: Boolean = false
): PartiqlLogicalResolved.Plan {
    // Allocate a unique id for each `VarDecl`
    val (planWithAllocatedVariables, allLocals) = this.allocateVariableIds()

    // Transform to `partiql_logical_resolved` while resolving variables.
    val resolvedSt = LogicalToLogicalResolvedVisitorTransform(allowUndefinedVariables, problemHandler, globals)
        .transformPlan(planWithAllocatedVariables)
        .copy(locals = allLocals)

    return resolvedSt
}

private fun PartiqlLogical.Expr.Id.asGlobalId(uniqueId: String): PartiqlLogicalResolved.Expr.GlobalId =
    PartiqlLogicalResolved.build {
        globalId_(
            name = name,
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

/**
 * A local scope is a list of variable declarations that are produced by a relational operator and an optional
 * reference to a parent scope.  This is handled separately from global variables.
 *
 * This is a [List] of [PartiqlLogical.VarDecl] and not a [Map] or some other more efficient data structure
 * because most variable lookups are case-insensitive, which makes storing them in a [Map] and benefiting from it hard.
 */
private data class LocalScope(val varDecls: List<PartiqlLogical.VarDecl>)

private data class LogicalToLogicalResolvedVisitorTransform(
    /** If set to `true`, do not log errors about undefined variables. Rewrite such variables to a `dynamic_id` node. */
    val allowUndefinedVariables: Boolean,
    /** Where to send error reports. */
    private val problemHandler: ProblemHandler,
    /** If a variable is not found using [inputScope], we will attempt to locate the binding here instead. */
    private val globals: GlobalBindings,

) : PartiqlLogicalToPartiqlLogicalResolvedVisitorTransform() {
    /** The current [LocalScope]. */
    private var inputScope: LocalScope = LocalScope(emptyList())

    private enum class VariableLookupStrategy {
        LOCALS_THEN_GLOBALS,
        GLOBALS_THEN_LOCALS
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
     * Returns [ResolutionResult.LocalVariable] if [bindingName] refers to a local variable.
     *
     * Otherwise, returns [ResolutionResult.Undefined].  (Elsewhere, [globals] will be checked next.)
     */
    private fun lookupLocalVariable(bindingName: BindingName): ResolutionResult {
        val found = this.inputScope.varDecls.firstOrNull { bindingName.isEquivalentTo(it.name.text) }
        return if (found == null) {
            ResolutionResult.Undefined
        } else {
            ResolutionResult.LocalVariable(found.indexMeta)
        }
    }

    /**
     * Resolves the logical `(id ...)` node node to a `(local_id ...)`, `(global_id ...)`, or dynamic `(id...)`
     * variable.
     */
    override fun transformExprId(node: PartiqlLogical.Expr.Id): PartiqlLogicalResolved.Expr {
        val bindingName = BindingName(node.name.text, node.case.toBindingCase())

        val resolutionResult = if (
            this.currentVariableLookupStrategy == VariableLookupStrategy.GLOBALS_THEN_LOCALS &&
            node.qualifier is PartiqlLogical.ScopeQualifier.Unqualified
        ) {
            // look up variable in globals first, then locals
            when (val globalResolutionResult = globals.resolve(bindingName)) {
                ResolutionResult.Undefined -> lookupLocalVariable(bindingName)
                else -> globalResolutionResult
            }
        } else {
            // look up variable in locals first, then globals.
            when (val localResolutionResult = lookupLocalVariable(bindingName)) {
                ResolutionResult.Undefined -> globals.resolve(bindingName)
                else -> localResolutionResult
            }
        }
        return when (resolutionResult) {
            is ResolutionResult.GlobalVariable -> {
                node.asGlobalId(resolutionResult.uniqueId)
            }
            is ResolutionResult.LocalVariable -> {
                node.asLocalId(resolutionResult.index)
            }
            ResolutionResult.Undefined -> {
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
                                node.metas.sourceLocation ?: error("MetaContainer is missing SourceLocationMeta"),
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

    override fun transformBexprFilter_predicate(node: PartiqlLogical.Bexpr.Filter): PartiqlLogicalResolved.Expr {
        val bindings = getOutputScope(node.source)
        return withInputScope(bindings) {
            this.transformExpr(node.predicate)
        }
    }

    override fun transformBexprJoin_predicate(node: PartiqlLogical.Bexpr.Join): PartiqlLogicalResolved.Expr? {
        val bindings = getOutputScope(node)
        return withInputScope(bindings) {
            node.predicate?.let { this.transformExpr(it) }
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
            val loweredVariableName = varDecl.name.text.toLowerCase()
            if (usedVariableNames.contains(loweredVariableName)) {
                this.problemHandler.handleProblem(
                    Problem(
                        varDecl.metas.sourceLocation ?: error("VarDecl was missing source location meta"),
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
            is PartiqlLogical.Bexpr.Scan -> {
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
        }

    private fun LocalScope.concatenate(other: LocalScope): LocalScope =
        this.concatenate(other.varDecls)

    private fun LocalScope.concatenate(other: List<PartiqlLogical.VarDecl>): LocalScope {
        val concatenatedScopeVariables = this.varDecls + other
        return LocalScope(concatenatedScopeVariables)
    }

    private fun PartiqlLogical.Expr.Id.asDynamicLookupCallsite(
        search: List<PartiqlLogicalResolved.Expr>
    ): PartiqlLogicalResolved.Expr {
        val caseSensitivityString = when (case) {
            is PartiqlLogical.CaseSensitivity.CaseInsensitive -> "case_insensitive"
            is PartiqlLogical.CaseSensitivity.CaseSensitive -> "case_sensitive"
        }
        return PartiqlLogicalResolved.build {
            call(
                funcName = DYNAMIC_LOOKUP_FUNCTION_NAME,
                args = listOf(
                    lit(name.toIonElement()),
                    lit(ionSymbol(caseSensitivityString)),
                    lit(ionSymbol(currentVariableLookupStrategy.toString().toLowerCase())),
                ) + search,
                metas = this@asDynamicLookupCallsite.metas
            )
        }
    }
}

/** Marks a variable for dynamic resolution--i.e. if undefined, this vardecl will be included in any dynamic_id lookup. */
fun PartiqlLogical.VarDecl.markForDynamicResolution() = this.withMeta("\$include_in_dynamic_resolution", Unit)
/** Returns true of the [VarDecl] has been marked to participate in unqualified field resolution */
val PartiqlLogical.VarDecl.includeInDynamicResolution get() = this.metas.containsKey("\$include_in_dynamic_resolution")
