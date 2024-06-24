package org.partiql.planner.internal

import org.partiql.ast.normalize.normalize
import org.partiql.errors.Problem
import org.partiql.errors.ProblemCallback
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.transforms.AstToPlan
import org.partiql.planner.internal.transforms.PlanTransform
import org.partiql.planner.metadata.Namespace
import org.partiql.planner.metadata.Session
import org.partiql.planner.metadata.System
import org.partiql.planner.validate.SqlValidator
import org.partiql.ast.Statement as AST
import org.partiql.plan.PartiQLPlan as PLAN
import org.partiql.planner.internal.ir.Statement as IR

/**
 * Default PartiQL logical query planner.
 *
 * Several intermediate representations have conflicting names, so they've been aliased.
 *
 *  - AST   -> Statement of the Abstract Syntax Tree.       public
 *  - IR    -> Statement of the Internal Representation.    private
 *  - PLAN  -> The public-facing PartiQLPlan                public
 */
internal class SqlPlanner(
    private val system: System,
    private val flags: Set<PlannerFlag>,
) : PartiQLPlanner {

    /**
     * Basic logical query planning is broken down into four phases:
     *
     *  1. Normalization         - AST -> IR
     *  2. Semantic Analysis     - IR  -> IR
     *  3. Factorization         - IR  -> IR
     *  4. Translation           - IR  -> PLAN
     */
    override fun plan(
        statement: AST,
        session: PartiQLPlanner.Session,
        onProblem: ProblemCallback,
    ): PartiQLPlanner.Result {
        var ir = normalize(statement)
        ir = validate(ir, session(), session)
        ir = factorize(ir)
        // Translate to the external plan.
        val plan = translate(ir)
        val problems = emptyList<Problem>()
        return PartiQLPlanner.Result(plan, problems)
    }

    /**
     * Phase 1.
     *
     * Apply normalization rules to the AST and translate to the internal, algebraic IR.
     *
     * This is current done in two passes (1) normalization and (2) translation, but could be done in one.
     */
    private fun normalize(ast: AST): IR {
        val normalized = ast.normalize()
        return AstToPlan.apply(normalized)
    }

    /**
     * Phase 2.
     *
     * This phase types all IR nodes and resolves names (tables, variables, routines).
     */
    private fun validate(ir: IR, session: Session, sessionLegacy: PartiQLPlanner.Session): IR =
        SqlValidator(system, session, sessionLegacy).validate(ir)

    /**
     * Phase 3.
     *
     * No-Op — Not implemented.
     */
    private fun factorize(ir: IR): IR = ir

    /**
     * Phase 4.
     *
     * Transform the internal plan to a public plan.
     */
    private fun translate(ir: IR): PLAN {
        val flags = emptySet<PlannerFlag>()
        val internal = org.partiql.planner.internal.ir.PartiQLPlan(ir)
        return PlanTransform(flags).transform(internal) {}
    }

    private companion object {

        /**
         * TODO REMOVE ME ONCE SESSION IS FIGURED OUT.
         */
        private fun session(): Session {
            return Session.Base(
                namespace = emptyScope(),
                path = emptyList(),
            )
        }

        /**
         * TODO REMOVE ME ONCE SESSION IS FIGURED OUT.
         */
        private fun emptyScope(): Namespace {
            return object : Namespace {
                override fun getName(): String = "empty"
            }
        }
    }
}
