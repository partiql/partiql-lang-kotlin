package org.partiql.planner.transforms

import org.partiql.errors.ProblemHandler
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.planner.GlobalVariableResolver
import org.partiql.planner.impl.allocateVariableIds

// TODO: Migrate the tests from :partiql-lang.
class LogicalToLogicalResolvedVisitorTransformTests {

    /**
     * TODO: Currently, since the VisitorTransform tests requires the Pig AST parser (which is only within :partiql-lang),
     *  we need to keep the VisitorTransform tests within :partiql-lang while allowing the tests to see the
     *  previously-internal constants (below). By exposing them using a "testArtifacts" configuration, we can avoid making
     *  these things public.
     */
    companion object {
        /**
         * Resolves all variables by rewriting `(id <name> <case-sensitivity> <scope-qualifier>)` to
         * `(id <unique-index>)`) or `(global_id <name> <unique-id>)`, or a `$__dynamic_lookup__` call site (if enabled).
         *
         * Local variables are resolved independently within this pass, but we rely on [resolver] to resolve global variables.
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
         * [org.partiql.errors.ProblemSeverity.ERROR] and to abort further query planning if so.**
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
        fun PartiqlLogical.Plan.toResolvedPlan(
            problemHandler: ProblemHandler,
            resolver: GlobalVariableResolver,
            allowUndefinedVariables: Boolean = false
        ): PartiqlLogicalResolved.Plan {
            // Allocate a unique id for each `VarDecl`
            val (planWithAllocatedVariables, allLocals) = this.allocateVariableIds()

            // Transform to `partiql_logical_resolved` while resolving variables.
            val resolvedSt = LogicalToLogicalResolvedVisitorTransform(allowUndefinedVariables, problemHandler, resolver)
                .transformPlan(planWithAllocatedVariables)
                .copy(locals = allLocals)

            return resolvedSt
        }
    }
}
