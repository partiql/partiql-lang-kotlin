package org.partiql.eval

import org.partiql.eval.internal.plan.ExecutionPlanImpl

/**
 * An opaque, immutable, thread-safe execution plan.
 *
 * Produced by [org.partiql.eval.compiler.PartiQLCompiler.compile].
 * Executed by [PartiQLVM.execute].
 *
 * This class has no public methods — internal structure may change without notice.
 */
public class ExecutionPlan internal constructor(internal val impl: ExecutionPlanImpl)
