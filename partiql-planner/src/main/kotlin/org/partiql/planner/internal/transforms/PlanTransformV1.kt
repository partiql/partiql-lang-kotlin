package org.partiql.planner.internal.transforms

import org.partiql.errors.Problem
import org.partiql.errors.ProblemCallback
import org.partiql.eval.value.Datum
import org.partiql.plan.v1.PartiQLPlan
import org.partiql.plan.v1.PartiQLPlanImpl
import org.partiql.plan.v1.Statement
import org.partiql.plan.v1.builder.PlanFactory
import org.partiql.plan.v1.operator.rel.Rel
import org.partiql.plan.v1.operator.rel.RelCollation
import org.partiql.plan.v1.operator.rel.RelError
import org.partiql.plan.v1.operator.rel.RelJoinType
import org.partiql.plan.v1.operator.rex.Rex
import org.partiql.plan.v1.operator.rex.RexCase
import org.partiql.plan.v1.operator.rex.RexStruct
import org.partiql.planner.internal.PlannerFlag
import org.partiql.planner.internal.ProblemGenerator
import org.partiql.planner.internal.ir.visitor.PlanBaseVisitor
import org.partiql.spi.fn.SqlFnProvider
import org.partiql.value.PartiQLValueExperimental
import org.partiql.planner.internal.ir.PartiQLPlan as IPlan
import org.partiql.planner.internal.ir.PlanNode as INode
import org.partiql.planner.internal.ir.Rel as IRel
import org.partiql.planner.internal.ir.Rex as IRex
import org.partiql.planner.internal.ir.Statement as IStatement

/**
 * This produces a V1 plan from the internal plan IR.
 *
 * TODO types and schemas!
 */
internal class PlanTransformV1(private val flags: Set<PlannerFlag>) {

    /**
     * Transform the internal IR to the public plan interfaces.
     *
     * @param internal
     * @param onProblem
     * @return
     */
    fun transform(internal: IPlan, onProblem: ProblemCallback): PartiQLPlan {
        val signal = flags.contains(PlannerFlag.SIGNAL_MODE)
        val query = (internal.statement as IStatement.Query)
        val visitor = Visitor(onProblem, signal)
        val root = visitor.visitRex(query.root, Unit)
        return PartiQLPlanImpl(object : Statement.Query {
            override fun getRoot(): Rex = root
        })
    }

    private class Visitor(
        private val onProblem: ProblemCallback,
        private val signal: Boolean,
    ) : PlanBaseVisitor<Any, Unit>() {

        private val factory = PlanFactory.STANDARD

        override fun defaultReturn(node: INode, ctx: Unit): Any {
            TODO("Translation not supported for ${node::class.simpleName}")
        }

        // ERRORS

        override fun visitRexOpMissing(node: IRex.Op.Missing, ctx: Unit): Any {
            val trace = node.causes.map { visitRexOp(it, ctx) }
            return err(node.problem, trace)
        }

        override fun visitRexOpErr(node: IRex.Op.Err, ctx: Unit): Any {
            val message = node.problem.toString()
            val trace = node.causes.map { visitRexOp(it, ctx) }
            onProblem(ProblemGenerator.asError(node.problem))
            return factory.rexError(message, trace)
        }

        override fun visitRelOpErr(node: org.partiql.planner.internal.ir.Rel.Op.Err, ctx: Unit): Any {
            val message = node.message
            onProblem(ProblemGenerator.compilerError(message))
            return RelError(message)
        }

        // EXPRESSIONS

        override fun visitRex(node: IRex, ctx: Unit): Rex = super.visitRexOp(node.op, ctx) as Rex

        override fun visitRexOp(node: IRex.Op, ctx: Unit): Rex = super.visitRexOp(node, ctx) as Rex

        override fun visitRexOpTupleUnion(node: IRex.Op.TupleUnion, ctx: Unit): Any {
            val args = node.args.map { visitRex(it, ctx) }
            return factory.rexSpread(args)
        }

        override fun visitRexOpSelect(node: IRex.Op.Select, ctx: Unit): Any {
            val input = visitRel(node.rel, ctx)
            val constructor = visitRex(node.constructor, ctx)
            return factory.rexSelect(input, constructor)
        }

        /**
         * TODO proper handling of subqueries in the planner.
         *
         * @param node
         * @param ctx
         * @return
         */
        override fun visitRexOpSubquery(node: IRex.Op.Subquery, ctx: Unit): Any {
            val input = visitRel(node.rel, ctx)
            val constructor = visitRex(node.constructor, ctx)
            val select = factory.relProject(input, listOf(constructor))
            return factory.rexSubquery(select)
        }

        override fun visitRexOpPivot(node: IRex.Op.Pivot, ctx: Unit): Any {
            val input = visitRel(node.rel, ctx)
            val key = visitRex(node.key, ctx)
            val value = visitRex(node.value, ctx)
            return factory.rexPivot(input, key, value)
        }

        override fun visitRexOpStruct(node: IRex.Op.Struct, ctx: Unit): Any {
            val fields = node.fields.map { field(it) }
            return factory.rexStruct(fields)
        }

        override fun visitRexOpCollection(node: IRex.Op.Collection, ctx: Unit): Any {
            val args = node.values.map { visitRex(it, ctx) }
            return factory.rexCollection(args)
        }

        override fun visitRexOpCoalesce(node: IRex.Op.Coalesce, ctx: Unit): Any {
            val args = node.args.map { visitRex(it, ctx) }
            return factory.rexCoalesce(args)
        }

        override fun visitRexOpNullif(node: IRex.Op.Nullif, ctx: Unit): Any {
            TODO("NULLIF should be a scalar function")
        }

        override fun visitRexOpCase(node: IRex.Op.Case, ctx: Unit): Any {
            val branches = node.branches.map { branch(it) }
            val default = visitRex(node.default, ctx)
            return factory.rexCase(branches, default)
        }

        override fun visitRexOpCallDynamic(node: IRex.Op.Call.Dynamic, ctx: Unit): Any {
            TODO("Dynamic function not supported")
        }

        override fun visitRexOpCallStatic(node: IRex.Op.Call.Static, ctx: Unit): Any {
            val fn = SqlFnProvider.getFn(node.fn.signature.specific)
                ?: error("Function not found: ${node.fn.signature.specific}")
            val args = node.args.map { visitRex(it, ctx) }
            return factory.rexCall(fn, args)
        }

        override fun visitRexOpCallUnresolved(node: IRex.Op.Call.Unresolved, ctx: Unit): Any {
            error("The Internal Node Rex.Op.Call.Unresolved should be converted to an Err Node during type resolution if resolution failed")
        }

        /**
         * TODO the following comment comes from the existing implementation, but how does it apply to CAST ??
         *
         * See PartiQL Specification [Section 4.1.1](https://partiql.org/partiql-lang/#sec:schema-in-tuple-path).
         * While it talks about pathing into a tuple, it provides guidance on expressions that always return missing:
         *
         * > In a more important and common case, an PartiQL implementation can utilize the input data schema to prove
         * > that a path expression always returns MISSING and thus throw a compile-time error.
         *
         * This is accomplished via the signaling mode below.
         */
        override fun visitRexOpCastUnresolved(node: IRex.Op.Cast.Unresolved, ctx: Unit): Any {
            val problem = ProblemGenerator.undefinedCast(node.arg.type, node.target)
            return err(problem, emptyList())
        }

        override fun visitRexOpCastResolved(node: IRex.Op.Cast.Resolved, ctx: Unit): Any {
            val operand = visitRex(node.arg, ctx)
            val target = node.cast.target
            return factory.rexCast(operand, target)
        }

        override fun visitRexOpPathSymbol(node: IRex.Op.Path.Symbol, ctx: Unit): Any {
            TODO("Path symbol")
        }

        override fun visitRexOpPathKey(node: IRex.Op.Path.Key, ctx: Unit): Any {
            TODO("Path key")
        }

        override fun visitRexOpPathIndex(node: IRex.Op.Path.Index, ctx: Unit): Any {
            TODO("Path index")
        }

        override fun visitRexOpVarUnresolved(node: IRex.Op.Var.Unresolved, ctx: Unit): Any {
            error("The Internal Plan Node Rex.Op.Var.Unresolved should be converted to an MISSING Node during type resolution if resolution failed")
        }

        override fun visitRexOpVarLocal(node: IRex.Op.Var.Local, ctx: Unit): Any {
            return factory.rexCol(depth = node.depth, offset = node.ref)
        }

        @OptIn(PartiQLValueExperimental::class)
        override fun visitRexOpLit(node: IRex.Op.Lit, ctx: Unit): Any {
            return factory.rexLit(Datum.of(node.value))
        }

        // RELATION OPERATORS

        override fun visitRel(node: IRel, ctx: Unit): Rel = super.visitRelOp(node.op, ctx) as Rel

        override fun visitRelOp(node: IRel.Op, ctx: Unit): Rel = super.visitRelOp(node, ctx) as Rel

        override fun visitRelOpJoin(node: IRel.Op.Join, ctx: Unit): Any {
            val lhs = visitRel(node.lhs, ctx)
            val rhs = visitRel(node.rhs, ctx)
            val condition = visitRex(node.rex, ctx)
            val type = when (node.type) {
                IRel.Op.Join.Type.INNER -> RelJoinType.INNER
                IRel.Op.Join.Type.LEFT -> RelJoinType.LEFT
                IRel.Op.Join.Type.RIGHT -> RelJoinType.RIGHT
                IRel.Op.Join.Type.FULL -> RelJoinType.FULL
            }
            return factory.relJoin(lhs, rhs, condition, type)
        }

        override fun visitRelOpProject(node: IRel.Op.Project, ctx: Unit): Any {
            val input = visitRel(node.input, ctx)
            val projections = node.projections.map { visitRex(it, ctx) }
            return factory.relProject(input, projections)
        }

        override fun visitRelOpOffset(node: IRel.Op.Offset, ctx: Unit): Any {
            val input = visitRel(node.input, ctx)
            val offset = visitRex(node.offset, ctx)
            return factory.relOffset(input, offset)
        }

        override fun visitRelOpLimit(node: IRel.Op.Limit, ctx: Unit): Any {
            val input = visitRel(node.input, ctx)
            val limit = visitRex(node.limit, ctx)
            return factory.relOffset(input, limit)
        }

        override fun visitRelOpSetIntersect(node: IRel.Op.Set.Intersect, ctx: Unit): Any {
            val lhs = visitRel(node.lhs, ctx)
            val rhs = visitRel(node.rhs, ctx)
            val isAll = node.quantifier == IRel.Op.Set.Quantifier.ALL
            return factory.relIntersect(lhs, rhs, isAll)
        }

        override fun visitRelOpSetUnion(node: IRel.Op.Set.Union, ctx: Unit): Any {
            val lhs = visitRel(node.lhs, ctx)
            val rhs = visitRel(node.rhs, ctx)
            val isAll = node.quantifier == IRel.Op.Set.Quantifier.ALL
            return factory.relUnion(lhs, rhs, isAll)
        }

        override fun visitRelOpSort(node: IRel.Op.Sort, ctx: Unit): Any {
            val input = visitRel(node.input, ctx)
            val collations = node.specs.map { collation(it) }
            return factory.relSort(input, collations)
        }

        override fun visitRelOpFilter(node: IRel.Op.Filter, ctx: Unit): Any {
            val input = visitRel(node.input, ctx)
            val condition = visitRex(node.predicate, ctx)
            return factory.relFilter(input, condition)
        }

        override fun visitRelOpDistinct(node: IRel.Op.Distinct, ctx: Unit): Any {
            val input = visitRel(node.input, ctx)
            return factory.relDistinct(input)
        }

        override fun visitRelOpUnpivot(node: IRel.Op.Unpivot, ctx: Unit): Any {
            val input = visitRex(node.rex, ctx)
            return factory.relUnpivot(input)
        }

        override fun visitRelOpScanIndexed(node: IRel.Op.ScanIndexed, ctx: Unit): Any {
            val input = visitRex(node.rex, ctx)
            return factory.relIterate(input)
        }

        override fun visitRelOpScan(node: IRel.Op.Scan, ctx: Unit): Any {
            val input = visitRex(node.rex, ctx)
            return factory.relScan(input)
        }

        // HELPERS

        /**
         * TODO STANDARD COLLATION IMPLEMENTATION.
         */
        private fun collation(spec: IRel.Op.Sort.Spec): RelCollation {
            val rex = visitRex(spec.rex, Unit)
            val (order, nulls) = when (spec.order) {
                IRel.Op.Sort.Order.ASC_NULLS_LAST -> RelCollation.Order.ASC to RelCollation.Nulls.LAST
                IRel.Op.Sort.Order.ASC_NULLS_FIRST -> RelCollation.Order.ASC to RelCollation.Nulls.FIRST
                IRel.Op.Sort.Order.DESC_NULLS_LAST -> RelCollation.Order.DESC to RelCollation.Nulls.LAST
                IRel.Op.Sort.Order.DESC_NULLS_FIRST -> RelCollation.Order.DESC to RelCollation.Nulls.FIRST
            }
            return object : RelCollation {
                override fun getRex(): Rex = rex
                override fun getOrder(): RelCollation.Order = order
                override fun getNulls(): RelCollation.Nulls = nulls
            }
        }

        private fun field(field: IRex.Op.Struct.Field): RexStruct.Field {
            val key = visitRex(field.k, Unit)
            val value = visitRex(field.v, Unit)
            return object : RexStruct.Field {
                override fun getKey(): Rex = key
                override fun getValue(): Rex = value
            }
        }

        private fun branch(branch: IRex.Op.Case.Branch): RexCase.Branch {
            val condition = visitRex(branch.condition, Unit)
            val result = visitRex(branch.rex, Unit)
            return object : RexCase.Branch {
                override fun getCondition(): Rex = condition
                override fun getResult(): Rex = result
            }
        }

        private fun err(problem: Problem, trace: List<Rex>): Rex = when (signal) {
            true -> {
                onProblem(ProblemGenerator.asError(problem))
                factory.rexError(message = problem.toString(), trace)
            }
            false -> {
                onProblem(ProblemGenerator.asWarning(problem))
                factory.rexMissing(message = problem.toString(), trace)
            }
        }
    }
}
