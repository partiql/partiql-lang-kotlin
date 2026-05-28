package org.partiql.eval;

import org.partiql.plan.Plan;

/**
 * An opaque, immutable, thread-safe execution plan.
 * <p>
 * Produced by {@link org.partiql.eval.compiler.PartiQLCompiler#compile(Plan)}.
 * Executed by {@link PartiQLVM#execute(ExecutionPlan, Mode, org.partiql.spi.catalog.ExecutionCatalog[])}.
 * <p>
 * This class has no public methods — internal structure may change without notice.
 *
 * <h2>Migration Guide</h2>
 * <pre>
 * OLD PATH (deprecated):
 *   PartiQLPlanner.standard().plan(ast, session)        → Plan (embedded objects)
 *   PartiQLCompiler.standard().prepare(plan, mode)      → Statement
 *   statement.execute()                                 → Datum
 *
 * NEW PATH (thread-safe):
 *   PartiQLPlanner.builder().useRefs().build().plan(ast, session) → Plan + SymbolTable
 *   PartiQLCompiler.standard().compile(plan)                      → ExecutionPlan
 *   PartiQLVM.standard().execute(execPlan, mode, catalogs)        → Datum
 * </pre>
 */
public final class ExecutionPlan {

    private final Plan plan;

    /**
     * Internal use only. Do not call directly.
     * @param plan validated plan
     */
    public ExecutionPlan(Plan plan) {
        this.plan = plan;
    }

    /**
     * Internal use only. Do not call directly.
     * @return the underlying plan
     */
    public Plan getPlan() {
        return plan;
    }
}
