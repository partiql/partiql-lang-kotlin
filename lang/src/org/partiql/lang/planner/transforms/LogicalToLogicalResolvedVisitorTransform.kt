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
import org.partiql.lang.planner.UniqueIdResolver
import org.partiql.lang.planner.ResolutionResult
import org.partiql.pig.runtime.asPrimitive

/**
 * Resolves all variables by rewriting `(id <name> <case-sensitivity> <scope-qualifier>)` to
 * `(id <unique-index>)`) or `(global_id <name> <unique-id>)`, or a `$__dynamic_lookup__` call site (if enabled).
 *
 * Local variables are resolved independently within this pass, but we rely on [globals] to resolve global variables.
 *
 * There are actually two passes here:
 * 1. All [PartiqlLogical.VarDecl] nodes are allocated unique indexes (which is stored in a meta).  This pass is
 * relatively simple.
 * 2. Then, during the transform from the `partiql_logical` domain to the `partiql_logical_resolved` domain, we
 * determine if the `id` node refers to a global variable, local variable or undefined variable.  For global variables,
 * the `id` node is replaced with `(global_id <name> <unique-id>)`.  For local variables, the original `id` node is
 * replaced with a `(id <name> <unique-index>)`), where `<unique-index>` is the index of the corresponding `var_decl`.
 *
 * When [allowUndefinedVariables] is `false`, the [problemHandler] is notified of any undefined variables.  Resolution
 * does not stop on the first undefined variable, rather we keep going to provide the end user any additional error
 * messaging, unless [ProblemHandler.handleProblem] throws an exception when an error is logged.  **If any undefined
 * variables are detected, in order to allow traversal to continue, a fake index value (-1) is used in place of a real
 * one and the resolved logical plan returned by this function is guaranteed to be invalid.** **Therefore, it is the
 * responsibility of callers to check if any problems have been logged with
 * [org.partiql.lang.errors.ProblemSeverity.ERROR] and to abort further query planning if so.**
 *
 * When [allowUndefinedVariables] is `true`, undefined variables are transformed into a dynamic lookup call site, which
 * is semantically equivalent to the behavior of the AST evaluator in the same scenario.  For example, `name` in the
 * query below is undefined:
 *
 * ```sql
 * SELECT name
 * FROM foo AS f, bar AS b
 * ```
 * Is effectively rewritten to:
 *
 * ```sql
 * SELECT "$__dynamic_lookup__"('name', 'locals_then_globals', 'case_insensitive', f, b)
 * FROM foo AS f, bar AS b
 * ```
 *
 * When `$__dynamic_lookup__` is invoked it will look for the value of `name` in the following locations: (All field
 * / variable name comparisons are case-insensitive in this example, although we could have also specified
 * `case_sensitive`.)
 *
 * - The fields of `f` if it is a struct.
 * - The fields of `b` if it is a struct.
 * - The global scope.
 *
 * The first value found is returned and the others are ignored.  Local variables are searched first
 * (`locals_then_globals`) because of the context of the undefined variable.  (`name` is not within a `FROM` source.)
 * However, to support SQL's FROM-clause semantics this pass specifies `globals_then_locals` when the variable is within
 * a `FROM` source, which causes globals to be searched first.
 *
 * This behavior is backward compatible with the legacy AST evaluator.  Furthermore, this rewrite allows us to avoid
 * having to support this kind of dynamic lookup within the plan evaluator, thereby reducing its complexity.  This
 * rewrite can also be disabled entirely by setting [allowUndefinedVariables] to `false`, in which case undefined
 * variables to result in a plan-time error instead.
 */
internal fun PartiqlLogical.Plan.toResolvedPlan(
    problemHandler: ProblemHandler,
    globals: UniqueIdResolver,
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
    private val globals: UniqueIdResolver,

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
        val variableLookupStrategy = when(currentVariableLookupStrategy) {
            // If we are not in a FROM source, ignore the scope qualifier
            VariableLookupStrategy.LOCALS_THEN_GLOBALS -> VariableLookupStrategy.LOCALS_THEN_GLOBALS
            // If we are in a FROM source, allow scope qualifier to override the current variable lookup strategy.
            VariableLookupStrategy.GLOBALS_THEN_LOCALS -> when(this.qualifier) {
                is PartiqlLogical.ScopeQualifier.LocalsFirst -> VariableLookupStrategy.LOCALS_THEN_GLOBALS
                is PartiqlLogical.ScopeQualifier.Unqualified -> VariableLookupStrategy.GLOBALS_THEN_LOCALS
            }
        }.toString().toLowerCase()
        return PartiqlLogicalResolved.build {
            call(
                funcName = DYNAMIC_LOOKUP_FUNCTION_NAME,
                args = listOf(
                    lit(name.toIonElement()),
                    lit(ionSymbol(caseSensitivityString)),
                    lit(ionSymbol(variableLookupStrategy)),
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
