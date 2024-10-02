package org.partiql.planner.internal.transforms

import org.partiql.errors.Problem
import org.partiql.errors.ProblemCallback
import org.partiql.plan.AggregateCall
import org.partiql.plan.Collation
import org.partiql.plan.ExcludePath
import org.partiql.plan.ExcludeStep
import org.partiql.plan.JoinType
import org.partiql.plan.Plan
import org.partiql.plan.rel.RelType
import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexCase
import org.partiql.plan.rex.RexStruct
import org.partiql.plan.rex.RexVar
import org.partiql.planner.internal.PlannerFlag
import org.partiql.planner.internal.ProblemGenerator
import org.partiql.planner.internal.ir.SetQuantifier
import org.partiql.planner.internal.ir.visitor.PlanBaseVisitor
import org.partiql.spi.value.Datum
import org.partiql.types.Field
import org.partiql.types.PType
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
internal class PlanTransform(private val flags: Set<PlannerFlag>) {

    /**
     * Transform the internal IR to the public plan interfaces.
     *
     * @param internal
     * @param onProblem
     * @return
     */
    fun transform(internal: IPlan, onProblem: ProblemCallback): Plan {
        val signal = flags.contains(PlannerFlag.SIGNAL_MODE)
        val query = (internal.statement as IStatement.Query)
        val visitor = Visitor(onProblem, signal)
        val root = visitor.visitRex(query.root, query.root.type)
        // TODO replace with standard implementations (or just remove plan transform altogether when possible).
        return object : Plan {
            override fun getOperation(): org.partiql.plan.Operation = object : org.partiql.plan.Operation.Query {
                override fun getRoot(): Rex = root
            }
        }
    }

    private class Visitor(
        private val onProblem: ProblemCallback,
        private val signal: Boolean,
    ) : PlanBaseVisitor<Any, PType>() {

        private val factory = org.partiql.plan.builder.PlanFactory.STANDARD

        override fun defaultReturn(node: INode, ctx: PType): Any {
            TODO("Translation not supported for ${node::class.simpleName}")
        }

        // ERRORS

        override fun visitRexOpMissing(node: IRex.Op.Missing, ctx: PType): Any {
            val trace = node.causes.map { visitRexOp(it, ctx) }
            return when (signal) {
                true -> {
                    onProblem.invoke(ProblemGenerator.asError(node.problem))
                    err(node.problem, trace)
                }
                false -> {
                    onProblem.invoke(ProblemGenerator.asWarning(node.problem))
                    factory.rexMissing(node.problem.toString(), trace)
                }
            }
        }

        override fun visitRexOpErr(node: IRex.Op.Err, ctx: PType): Any {
            val message = node.problem.toString()
            val trace = node.causes.map { visitRexOp(it, ctx) }
            onProblem(ProblemGenerator.asError(node.problem))
            return factory.rexError(message, trace)
        }

        override fun visitRelOpErr(node: org.partiql.planner.internal.ir.Rel.Op.Err, ctx: PType): Any {
            val message = node.message
            onProblem(ProblemGenerator.compilerError(message))
            return org.partiql.plan.rel.RelError(message)
        }

        // EXPRESSIONS

        override fun visitRex(node: IRex, ctx: PType): Rex = super.visitRexOp(node.op, node.type) as Rex

        override fun visitRexOp(node: IRex.Op, ctx: PType): Rex = super.visitRexOp(node, ctx) as Rex

        override fun visitRexOpTupleUnion(node: IRex.Op.TupleUnion, ctx: PType): Any {
            val args = node.args.map { visitRex(it, ctx) }
            return factory.rexSpread(args)
        }

        override fun visitRexOpSelect(node: IRex.Op.Select, ctx: PType): Any {
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
        override fun visitRexOpSubquery(node: IRex.Op.Subquery, ctx: PType): Any {
            val input = visitRel(node.rel, ctx)
            val constructor = visitRex(node.constructor, ctx)
            val isScalar = node.coercion == IRex.Op.Subquery.Coercion.SCALAR
            return factory.rexSubquery(input, constructor, isScalar)
        }

        override fun visitRexOpPivot(node: IRex.Op.Pivot, ctx: PType): Any {
            val input = visitRel(node.rel, ctx)
            val key = visitRex(node.key, ctx)
            val value = visitRex(node.value, ctx)
            return factory.rexPivot(input, key, value)
        }

        override fun visitRexOpStruct(node: IRex.Op.Struct, ctx: PType): Any {
            val fields = node.fields.map { field(it) }
            return factory.rexStruct(fields)
        }

        override fun visitRexOpCollection(node: IRex.Op.Collection, ctx: PType): Any {
            val values = node.values.map { visitRex(it, ctx) }
            return when (ctx.kind) {
                PType.Kind.ARRAY -> factory.rexArray(values)
                PType.Kind.BAG -> factory.rexBag(values)
                else -> error("Expected bag or array, found ${ctx.kind.name.lowercase()}")
            }
        }

        override fun visitRexOpCoalesce(node: IRex.Op.Coalesce, ctx: PType): Any {
            val args = node.args.map { visitRex(it, ctx) }
            return factory.rexCoalesce(args)
        }

        override fun visitRexOpNullif(node: IRex.Op.Nullif, ctx: PType): Any {
            val value = visitRex(node.value, ctx)
            val nullifier = visitRex(node.nullifier, ctx)
            return factory.rexNullIf(value, nullifier)
        }

        override fun visitRexOpCase(node: IRex.Op.Case, ctx: PType): Any {
            val branches = node.branches.map { branch(it) }
            val default = visitRex(node.default, ctx)
            return factory.rexCase(branches, default)
        }

        override fun visitRexOpCallDynamic(node: IRex.Op.Call.Dynamic, ctx: PType): Any {
            // TODO add argument types and move to plan typer!!
            val fns = node.candidates.map { it.fn.signature.getInstance(emptyArray()) }
            val args = node.args.map { visitRex(it, ctx) }
            // TODO assert on function name in plan typer .. here is not the place.
            return factory.rexCallDynamic("unknown", fns, args)
        }

        override fun visitRexOpCallStatic(node: IRex.Op.Call.Static, ctx: PType): Any {
            // TODO add argument types and move to PlanTyper!!
            val fn = node.fn.signature.getInstance(emptyArray())
            val args = node.args.map { visitRex(it, ctx) }
            return factory.rexCall(fn, args)
        }

        override fun visitRexOpCallUnresolved(node: IRex.Op.Call.Unresolved, ctx: PType): Any {
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
        override fun visitRexOpCastUnresolved(node: IRex.Op.Cast.Unresolved, ctx: PType): Any {
            val problem = ProblemGenerator.undefinedCast(node.arg.type, node.target)
            return when (signal) {
                true -> {
                    onProblem.invoke(problem)
                    err(problem, emptyList())
                }
                false -> {
                    onProblem.invoke(ProblemGenerator.asWarning(problem))
                    factory.rexMissing(problem.toString(), emptyList())
                }
            }
        }

        override fun visitRexOpCastResolved(node: IRex.Op.Cast.Resolved, ctx: PType): Any {
            val operand = visitRex(node.arg, ctx)
            val target = node.cast.target
            return factory.rexCast(operand, target)
        }

        override fun visitRexOpPathSymbol(node: IRex.Op.Path.Symbol, ctx: PType): Any {
            val operand = visitRex(node.root, ctx)
            val symbol = node.key
            return factory.rexPathSymbol(operand, symbol)
        }

        override fun visitRexOpPathKey(node: IRex.Op.Path.Key, ctx: PType): Any {
            val operand = visitRex(node.root, ctx)
            val key = visitRex(node.key, ctx)
            return factory.rexPathKey(operand, key)
        }

        override fun visitRexOpPathIndex(node: IRex.Op.Path.Index, ctx: PType): Any {
            val operand = visitRex(node.root, ctx)
            val index = visitRex(node.key, ctx)
            return factory.rexPathIndex(operand, index)
        }

        override fun visitRexOpVarGlobal(node: IRex.Op.Var.Global, ctx: PType): Any {
            return factory.rexTable(node.ref.table)
        }

        override fun visitRexOpVarUnresolved(node: IRex.Op.Var.Unresolved, ctx: PType): Any {
            error("The Internal Plan Node Rex.Op.Var.Unresolved should be converted to an MISSING Node during type resolution if resolution failed")
        }

        override fun visitRexOpVarLocal(node: IRex.Op.Var.Local, ctx: PType): Any {
            return factory.rexVar(depth = node.depth, offset = node.ref)
        }

        @OptIn(PartiQLValueExperimental::class)
        override fun visitRexOpLit(node: IRex.Op.Lit, ctx: PType): Any {
            return factory.rexLit(Datum.of(node.value))
        }

        // RELATION OPERATORS

        override fun visitRel(node: IRel, ctx: PType): org.partiql.plan.rel.Rel = super.visitRelOp(node.op, ctx) as org.partiql.plan.rel.Rel

        override fun visitRelOp(node: IRel.Op, ctx: PType): org.partiql.plan.rel.Rel = super.visitRelOp(node, ctx) as org.partiql.plan.rel.Rel

        override fun visitRelOpAggregate(node: IRel.Op.Aggregate, ctx: PType): Any {
            val input = visitRel(node.input, ctx)
            val calls = node.calls.map { visitRelOpAggregateCall(it, ctx) as AggregateCall }
            val groups = node.groups.map { visitRex(it, ctx) }
            return factory.relAggregate(input, calls, groups)
        }

        override fun visitRelOpAggregateCallUnresolved(node: IRel.Op.Aggregate.Call.Unresolved, ctx: PType): Any {
            error("Unresolved aggregate call $node")
        }

        override fun visitRelOpAggregateCallResolved(node: IRel.Op.Aggregate.Call.Resolved, ctx: PType): Any {
            val agg = node.agg.signature
            val args = node.args.map { visitRex(it, ctx) }
            val isDistinct = node.setq == SetQuantifier.DISTINCT
            return factory.relAggregateCall(agg, args, isDistinct)
        }

        override fun visitRelOpJoin(node: IRel.Op.Join, ctx: PType): Any {
            val lhs = visitRel(node.lhs, ctx)
            val rhs = visitRel(node.rhs, ctx)
            val condition = visitRex(node.rex, ctx)

            // TODO CLEANUP JOIN SCHEMA
            val lhsType = toSchema(node.lhs.type)
            val rhsType = toSchema(node.rhs.type)

            val type = when (node.type) {
                IRel.Op.Join.Type.INNER -> JoinType.INNER
                IRel.Op.Join.Type.LEFT -> JoinType.LEFT
                IRel.Op.Join.Type.RIGHT -> JoinType.RIGHT
                IRel.Op.Join.Type.FULL -> JoinType.FULL
            }
            return factory.relJoin(lhs, rhs, condition, type, lhsType, rhsType)
        }

        override fun visitRelOpExclude(node: IRel.Op.Exclude, ctx: PType): Any {
            val input = visitRel(node.input, ctx)
            val paths = node.paths.map { visitRelOpExcludePath(it, ctx) as ExcludePath }
            return factory.relExclude(input, paths)
        }

        override fun visitRelOpExcludePath(node: IRel.Op.Exclude.Path, ctx: PType): Any {
            val root = visitRexOp(node.root, ctx) as RexVar
            val steps = node.steps.map { visitRelOpExcludeStep(it, ctx) as ExcludeStep }
            return ExcludePath.of(root, steps)
        }

        override fun visitRelOpExcludeStep(node: IRel.Op.Exclude.Step, ctx: PType): Any {
            val substeps = node.substeps.map { visitRelOpExcludeStep(it, ctx) as ExcludeStep }
            return when (node.type) {
                is IRel.Op.Exclude.Type.CollIndex -> ExcludeStep.index(node.type.index, substeps)
                is IRel.Op.Exclude.Type.CollWildcard -> ExcludeStep.collection(substeps)
                is IRel.Op.Exclude.Type.StructKey -> ExcludeStep.key(node.type.key, substeps)
                is IRel.Op.Exclude.Type.StructSymbol -> ExcludeStep.symbol(node.type.symbol, substeps)
                is IRel.Op.Exclude.Type.StructWildcard -> ExcludeStep.struct(substeps)
            }
        }

        override fun visitRelOpProject(node: IRel.Op.Project, ctx: PType): Any {
            val input = visitRel(node.input, ctx)
            val projections = node.projections.map { visitRex(it, ctx) }
            return factory.relProject(input, projections)
        }

        override fun visitRelOpOffset(node: IRel.Op.Offset, ctx: PType): Any {
            val input = visitRel(node.input, ctx)
            val offset = visitRex(node.offset, ctx)
            return factory.relOffset(input, offset)
        }

        override fun visitRelOpLimit(node: IRel.Op.Limit, ctx: PType): Any {
            val input = visitRel(node.input, ctx)
            val limit = visitRex(node.limit, ctx)
            return factory.relLimit(input, limit)
        }

        override fun visitRelOpIntersect(node: IRel.Op.Intersect, ctx: PType): Any {
            val lhs = visitRel(node.lhs, ctx)
            val rhs = visitRel(node.rhs, ctx)
            val isAll = node.setq == SetQuantifier.ALL
            return factory.relIntersect(lhs, rhs, isAll)
        }

        override fun visitRelOpUnion(node: IRel.Op.Union, ctx: PType): Any {
            val lhs = visitRel(node.lhs, ctx)
            val rhs = visitRel(node.rhs, ctx)
            val isAll = node.setq == SetQuantifier.ALL
            return factory.relUnion(lhs, rhs, isAll)
        }

        override fun visitRelOpExcept(node: IRel.Op.Except, ctx: PType): Any {
            val lhs = visitRel(node.lhs, ctx)
            val rhs = visitRel(node.rhs, ctx)
            val isAll = node.setq == SetQuantifier.ALL
            return factory.relExcept(lhs, rhs, isAll)
        }

        override fun visitRelOpSort(node: IRel.Op.Sort, ctx: PType): Any {
            val input = visitRel(node.input, ctx)
            val collations = node.specs.map { collation(it) }
            return factory.relSort(input, collations)
        }

        override fun visitRelOpFilter(node: IRel.Op.Filter, ctx: PType): Any {
            val input = visitRel(node.input, ctx)
            val condition = visitRex(node.predicate, ctx)
            return factory.relFilter(input, condition)
        }

        override fun visitRelOpDistinct(node: IRel.Op.Distinct, ctx: PType): Any {
            val input = visitRel(node.input, ctx)
            return factory.relDistinct(input)
        }

        override fun visitRelOpUnpivot(node: IRel.Op.Unpivot, ctx: PType): Any {
            val input = visitRex(node.rex, ctx)
            return factory.relUnpivot(input)
        }

        override fun visitRelOpScanIndexed(node: IRel.Op.ScanIndexed, ctx: PType): Any {
            val input = visitRex(node.rex, ctx)
            return factory.relIterate(input)
        }

        override fun visitRelOpScan(node: IRel.Op.Scan, ctx: PType): Any {
            val input = visitRex(node.rex, ctx)
            return factory.relScan(input)
        }

        // HELPERS

        /**
         * TODO STANDARD COLLATION IMPLEMENTATION.
         */
        private fun collation(spec: IRel.Op.Sort.Spec): Collation {
            val rex = visitRex(spec.rex, spec.rex.type)
            val (order, nulls) = when (spec.order) {
                IRel.Op.Sort.Order.ASC_NULLS_LAST -> Collation.Order.ASC to Collation.Nulls.LAST
                IRel.Op.Sort.Order.ASC_NULLS_FIRST -> Collation.Order.ASC to Collation.Nulls.FIRST
                IRel.Op.Sort.Order.DESC_NULLS_LAST -> Collation.Order.DESC to Collation.Nulls.LAST
                IRel.Op.Sort.Order.DESC_NULLS_FIRST -> Collation.Order.DESC to Collation.Nulls.FIRST
            }
            return object : Collation {
                override fun getRex(): Rex = rex
                override fun getOrder(): Collation.Order = order
                override fun getNulls(): Collation.Nulls = nulls
            }
        }

        private fun field(field: IRex.Op.Struct.Field): RexStruct.Field {
            val key = visitRex(field.k, field.k.type)
            val value = visitRex(field.v, field.v.type)
            return RexStruct.Field(key, value)
        }

        private fun branch(branch: IRex.Op.Case.Branch): RexCase.Branch {
            val condition = visitRex(branch.condition, branch.condition.type)
            val result = visitRex(branch.rex, branch.rex.type)
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

        /**
         * TODO TEMPORARY!
         */
        private fun toSchema(type: IRel.Type): RelType = object : RelType {
            private val fields = type.schema.map { Field.of(it.name, it.type) }
            override fun getFields(): List<Field> = fields
            override fun getField(name: String): Field = fields.first { it.name == name }
        }
    }
}
