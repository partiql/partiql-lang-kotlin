package org.partiql.planner.transforms

import com.amazon.ionelement.api.IonElement
import org.partiql.lang.ast.sourceLocation
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.domains.PartiqlLogicalToPartiqlLogicalResolvedVisitorTransform
import org.partiql.lang.domains.toBindingCase
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.eval.BindingName
import org.partiql.pig.runtime.asPrimitive
import org.partiql.planner.GlobalBindings
import org.partiql.planner.ResolutionResult

/**
 * Resolves all variables by rewriting `(id <name> <case-sensitivity> <scope-qualifier>)` to
 * `(id <name> <unique-index>)`) or `(global_id <name> <unique-id>)`.  The latter is usually a reference to
 * a database table.  `<unique-id>` is supplied by the integrating PartiQL service by means of the [globals] specified
 * by callers of this function.  Note that in general, all `(scan (global_id ...) ...)` operators will later be
 * rewritten to an optimized physical read operator.
 *
 * The [problemHandler] is notified of any undefined variables.  Resolution does not stop on the first error, rather
 * we keep going to provide the end user any additional error messaging, unless [ProblemHandler.handleProblem] throws
 * an exception when an error is logged.  **If any undefined variables are detected, a fake index value is used in
 * place of a real one and the resolved logical plan returned by this function is guaranteed to be invalid.**
 * **Therefore, it is the responsibility therefore of callers to check if any problems have been logged with
 * [org.partiql.lang.errors.ProblemSeverity.ERROR] and to abort further query planning if necessary.**
 *
 * Local variables are resolved independently here, but we rely on [globals] to resolve global variables.
 *
 * Ths works in two passes:
 * 1. All [PartiqlLogical.VarDecl] nodes are allocated unique indexes (which is stored in a meta).
 * 2. Then, during the transform from the `partiql_logical` domain to the `partiql_logical_resolved` domain, we
 * determine if the `id` node refers to a global variable or local variable.  For global variables, the `id` node is
 * replaced with `(global_id <name> <unique-id>)`.  For local variables, the original `id` node is replaced with a
 * `(id <name> <unique-index>)`), where `<unique-index>` is the index of the corresponding `var_decl`.
 */
internal fun PartiqlLogical.Statement.toResolved(
    problemHandler: ProblemHandler,
    globals: GlobalBindings
): PartiqlLogicalResolved.Statement {
    // Allocate a unique id for each `VarDecl`
    val planWithAllocatedVariables = VariableIdAllocator().transformStatement(this)

    // Transform to `partiql_logical_resolved` while resolving variables.
    return LogicalToLogicalResolvedVisitorTransform(problemHandler, globals).transformStatement(planWithAllocatedVariables)
}

private const val VARIABLE_ID_META_TAG = "\$variable_id"

private val PartiqlLogical.VarDecl.indexMeta
    get() = this.metas[VARIABLE_ID_META_TAG] as? Int ?: error("Meta $VARIABLE_ID_META_TAG was not present")

/**
 * Allocates a unique index to every `var_decl` in the logical plan.  We use metas for this step to avoid a having
 * create another permuted domain.
 */
private class VariableIdAllocator : PartiqlLogical.VisitorTransform() {
    private var nextVariableId = 0
    override fun transformVarDecl(node: PartiqlLogical.VarDecl): PartiqlLogical.VarDecl =
        node.withMeta(VARIABLE_ID_META_TAG, nextVariableId++)
}

private fun PartiqlLogical.Expr.Id.asGlobalId(uniqueId: IonElement): PartiqlLogicalResolved.Expr.GlobalId =
    PartiqlLogicalResolved.build {
        globalId_(
            name = name,
            uniqueId = uniqueId,
            metas = metas
        )
    }

private fun PartiqlLogical.Expr.Id.asLocalId(index: Int): PartiqlLogicalResolved.Expr =
    PartiqlLogicalResolved.build {
        id_(name, index.asPrimitive())
    }

private fun PartiqlLogical.Expr.Id.asErrorId(): PartiqlLogicalResolved.Expr =
    PartiqlLogicalResolved.build {
        id_(name, (-1).asPrimitive())
    }

/**
 * A local scope is a list of variable declarations that are produced by a relational operator and an optional
 * reference to a parent scope.  This is handled separately from global variables.
 *
 * This is a [List] of [PartiqlLogical.VarDecl] and not a [Map] or some other more efficient data structure
 * because most variable lookups are case-insensitive, which makes storing them in a [Map] and benefiting from it hard.
 */
private data class LocalScope(val varDecls: List<PartiqlLogical.VarDecl>, val parent: LocalScope?)

private data class LogicalToLogicalResolvedVisitorTransform(
    /** Where to send error reports. */
    private val problemHandler: ProblemHandler,
    /** If a variable is not found using [currentScope], we will attempt to locate the binding here instead. */
    private val globals: GlobalBindings,
    /** The current [LocalScope]. */
    private val currentScope: LocalScope = LocalScope(emptyList(), parent = null),
    /** This should only be set for the `<expr>` in `(scan <expr> ...)` nodes. */
    private val lookupUnqualifiedInGlobalsFirst: Boolean = false
) : PartiqlLogicalToPartiqlLogicalResolvedVisitorTransform() {

    /**
     * Creates a new instance of this class bound to a nested lexical scope.
     * Should be invoked at any place in the logical algebra where we descend into new lexical scope.
     */
    private fun nest(nextScope: LocalScope) = this.copy(currentScope = nextScope)

    private fun superTransformBexprScanExpr(node: PartiqlLogical.Bexpr.Scan): PartiqlLogicalResolved.Expr =
        super.transformBexprScan_expr(node)

    override fun transformBexprScan_expr(node: PartiqlLogical.Bexpr.Scan): PartiqlLogicalResolved.Expr =
        // Have to call in to super.transformBexprScan_expr to avoid infinitely looping...
        this.copy(lookupUnqualifiedInGlobalsFirst = true).superTransformBexprScanExpr(node)

    override fun transformExprMapValues(node: PartiqlLogical.Expr.MapValues): PartiqlLogicalResolved.Expr {
        if(this.lookupUnqualifiedInGlobalsFirst) {
            TODO("Support for sub-queries")
        } else {
            return super.transformExprMapValues(node)
        }
    }

    /**
     * Grabs the index meta added by [VariableIdAllocator] and stores it as an element in
     * [PartiqlLogicalResolved.VarDecl].
     */
    override fun transformVarDecl(node: PartiqlLogical.VarDecl): PartiqlLogicalResolved.VarDecl =
        PartiqlLogicalResolved.build {
            varDecl_(node.name, node.indexMeta.asPrimitive())
        }

    /**
     * Returns [ResolutionResult.LocalVariable] if [bindingName] refers to a local variable.
     *
     * Otherwise, returns [ResolutionResult.Undefined].  (Elsewhere, [globals] will be checked next.)
     */
    private fun lookupLocalVariable(bindingName: BindingName): ResolutionResult {
        tailrec fun findBindings(scope: LocalScope): ResolutionResult {

            val found = scope.varDecls.firstOrNull { bindingName.isEquivalentTo(it.name.text) }
            return if (found == null) {
                // we didn't find the binding...
                if (scope.parent == null) {
                    // there are no more parent scopes to search, the variable is not a local variable.
                    ResolutionResult.Undefined
                }
                else findBindings(scope.parent)
            } else {
                // We found at least one, just return the first one.
                ResolutionResult.LocalVariable(found.indexMeta)
            }
        }

        return findBindings(currentScope)
    }

    /** Resolves the `(id ...)` node to a local or global variable. */
    override fun transformExprId(node: PartiqlLogical.Expr.Id): PartiqlLogicalResolved.Expr {
        val bindingName = BindingName(node.name.text, node.case.toBindingCase())

        val resolutionResult = if (lookupUnqualifiedInGlobalsFirst) {
            if (node.qualifier is PartiqlLogical.ScopeQualifier.LocalsFirst) {
                // this isn't needed until we support JOINs
                TODO("Support for looking up locally qualified (`@<variable>`) variables.")
            }
            when (val globalResolutionResult = globals.resolve(bindingName)) {
                ResolutionResult.Undefined -> lookupLocalVariable(bindingName)
                else -> globalResolutionResult
            }
        } else {
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
                node.asErrorId().also {
                    problemHandler.handleProblem(
                        Problem(
                            node.metas.sourceLocation ?: error("MetaContainer is missing SourceLocationMeta"),
                            PlanningProblemDetails.UndefinedVariable(
                                node.name.text,
                                node.case is PartiqlLogical.CaseSensitivity.CaseSensitive)
                        )
                    )
                }
            }
        }
    }

    override fun transformExprMapValues_exp(node: PartiqlLogical.Expr.MapValues): PartiqlLogicalResolved.Expr {
        val bindings = getNestedScope(node.query, currentScope)
        return nest(bindings).transformExpr(node.exp)
    }
    override fun transformBexprFilter_predicate(node: PartiqlLogical.Bexpr.Filter): PartiqlLogicalResolved.Expr {
        val bindings = getNestedScope(node.source, currentScope)
        val nested = this.copy(currentScope = bindings)
        return nested.transformExpr(node.predicate)
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
            if(usedVariableNames.contains(loweredVariableName)) {
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

    private fun getNestedScope(bindingsTerm: PartiqlLogical.Bexpr, parent: LocalScope?): LocalScope =
        when(bindingsTerm) {
            is PartiqlLogical.Bexpr.Scan -> {
                LocalScope(
                    listOfNotNull(bindingsTerm.asDecl, bindingsTerm.atDecl, bindingsTerm.byDecl).also {
                        checkForDuplicateVariables(it)
                    },
                    parent
                )
            }
            is PartiqlLogical.Bexpr.Filter -> {
                getNestedScope(bindingsTerm.source, parent)
                // note: `(filter ...)` does not itself produce new bindings so no need to check for duplicates.
            }
        }
}

