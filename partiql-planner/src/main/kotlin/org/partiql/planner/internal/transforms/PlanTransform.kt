package org.partiql.planner.internal.transforms

import org.partiql.errors.ProblemCallback
import org.partiql.plan.PlanNode
import org.partiql.plan.partiQLPlan
import org.partiql.plan.rex
import org.partiql.plan.rexOpLit
import org.partiql.plan.rexOpPathStepKey
import org.partiql.plan.rexOpPathStepSymbol
import org.partiql.planner.internal.ir.Agg
import org.partiql.planner.internal.ir.Fn
import org.partiql.planner.internal.ir.Global
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.PartiQLPlan
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.Statement
import org.partiql.planner.internal.ir.visitor.PlanBaseVisitor
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.stringValue

/**
 * This is an internal utility to translate from the internal unresolved plan used for typing to the public plan IR.
 * At the moment, these data structures are very similar sans-unresolved variants. The internal unresolved plan
 * continues to undergo frequent changes as we improve our typing model. This indirection enables a more stable public
 * consumable API while guaranteeing resolution safety.
 *
 * Ideally this class becomes very small as the internal IR will be a thin wrapper over the public API.
 */
internal object PlanTransform : PlanBaseVisitor<PlanNode, ProblemCallback>() {

    override fun defaultReturn(node: org.partiql.planner.internal.ir.PlanNode, ctx: ProblemCallback): PlanNode {
        error("Not implemented")
    }

    override fun visitPartiQLPlan(node: PartiQLPlan, ctx: ProblemCallback): org.partiql.plan.PartiQLPlan {
        val globals = node.globals.map { visitGlobal(it, ctx) }
        val statement = visitStatement(node.statement, ctx)
        return partiQLPlan(globals, statement)
    }

    override fun visitGlobal(node: Global, ctx: ProblemCallback): org.partiql.plan.Global {
        val path = visitIdentifierQualified(node.path, ctx)
        val type = node.type
        return org.partiql.plan.global(path, type)
    }

    override fun visitFnResolved(node: Fn.Resolved, ctx: ProblemCallback) = org.partiql.plan.fn(node.signature)

    override fun visitFnUnresolved(node: Fn.Unresolved, ctx: ProblemCallback): org.partiql.plan.Rex.Op {
        return org.partiql.plan.Rex.Op.Err("Unresolved function")
    }

    override fun visitAgg(node: Agg, ctx: ProblemCallback) = super.visitAgg(node, ctx) as org.partiql.plan.Agg

    override fun visitAggResolved(node: Agg.Resolved, ctx: ProblemCallback) = org.partiql.plan.Agg(node.signature)

    override fun visitAggUnresolved(node: Agg.Unresolved, ctx: ProblemCallback): org.partiql.plan.Rex.Op {
        return org.partiql.plan.Rex.Op.Err("Unresolved aggregation")
    }

    override fun visitStatement(node: Statement, ctx: ProblemCallback) =
        super.visitStatement(node, ctx) as org.partiql.plan.Statement

    override fun visitStatementQuery(node: Statement.Query, ctx: ProblemCallback): org.partiql.plan.Statement.Query {
        val root = visitRex(node.root, ctx)
        return org.partiql.plan.Statement.Query(root)
    }

    override fun visitIdentifier(node: Identifier, ctx: ProblemCallback) =
        super.visitIdentifier(node, ctx) as org.partiql.plan.Identifier

    override fun visitIdentifierSymbol(node: Identifier.Symbol, ctx: ProblemCallback) =
        org.partiql.plan.Identifier.Symbol(
            symbol = node.symbol,
            caseSensitivity = when (node.caseSensitivity) {
                Identifier.CaseSensitivity.SENSITIVE -> org.partiql.plan.Identifier.CaseSensitivity.SENSITIVE
                Identifier.CaseSensitivity.INSENSITIVE -> org.partiql.plan.Identifier.CaseSensitivity.INSENSITIVE
            }
        )

    override fun visitIdentifierQualified(node: Identifier.Qualified, ctx: ProblemCallback) =
        org.partiql.plan.Identifier.Qualified(
            root = visitIdentifierSymbol(node.root, ctx),
            steps = node.steps.map { visitIdentifierSymbol(it, ctx) }
        )

    // EXPRESSIONS

    override fun visitRex(node: Rex, ctx: ProblemCallback): org.partiql.plan.Rex {
        val type = node.type
        val op = visitRexOp(node.op, ctx)
        return org.partiql.plan.Rex(type, op)
    }

    override fun visitRexOp(node: Rex.Op, ctx: ProblemCallback) = super.visitRexOp(node, ctx) as org.partiql.plan.Rex.Op

    @OptIn(PartiQLValueExperimental::class)
    override fun visitRexOpLit(node: Rex.Op.Lit, ctx: ProblemCallback) = org.partiql.plan.rexOpLit(node.value)

    override fun visitRexOpVar(node: Rex.Op.Var, ctx: ProblemCallback) =
        super.visitRexOpVar(node, ctx) as org.partiql.plan.Rex.Op

    override fun visitRexOpVarResolved(node: Rex.Op.Var.Resolved, ctx: ProblemCallback) =
        org.partiql.plan.Rex.Op.Var(node.ref)

    override fun visitRexOpVarUnresolved(node: Rex.Op.Var.Unresolved, ctx: ProblemCallback) =
        org.partiql.plan.Rex.Op.Err("Unresolved variable $node")

    override fun visitRexOpGlobal(node: Rex.Op.Global, ctx: ProblemCallback) = org.partiql.plan.Rex.Op.Global(node.ref)

    override fun visitRexOpPath(node: Rex.Op.Path, ctx: ProblemCallback): org.partiql.plan.Rex.Op.Path {
        val root = visitRex(node.root, ctx)
        val steps = node.steps.map { visitRexOpPathStep(it, ctx) }
        return org.partiql.plan.Rex.Op.Path(root, steps)
    }

    override fun visitRexOpPathStep(node: Rex.Op.Path.Step, ctx: ProblemCallback) =
        super.visit(node, ctx) as org.partiql.plan.Rex.Op.Path.Step

    override fun visitRexOpPathStepIndex(node: Rex.Op.Path.Step.Index, ctx: ProblemCallback) =
        org.partiql.plan.Rex.Op.Path.Step.Index(
            key = visitRex(node.key, ctx),
        )

    @OptIn(PartiQLValueExperimental::class)
    override fun visitRexOpPathStepSymbol(node: Rex.Op.Path.Step.Symbol, ctx: ProblemCallback) = when (node.identifier.caseSensitivity) {
        Identifier.CaseSensitivity.SENSITIVE -> rexOpPathStepKey(rex(StaticType.STRING, rexOpLit(stringValue(node.identifier.symbol))))
        Identifier.CaseSensitivity.INSENSITIVE -> rexOpPathStepSymbol(node.identifier.symbol)
    }

    override fun visitRexOpPathStepKey(node: Rex.Op.Path.Step.Key, ctx: ProblemCallback): PlanNode = rexOpPathStepKey(
        key = visitRex(node.key, ctx)
    )

    override fun visitRexOpPathStepWildcard(node: Rex.Op.Path.Step.Wildcard, ctx: ProblemCallback) =
        org.partiql.plan.Rex.Op.Path.Step.Wildcard()

    override fun visitRexOpPathStepUnpivot(node: Rex.Op.Path.Step.Unpivot, ctx: ProblemCallback) =
        org.partiql.plan.Rex.Op.Path.Step.Unpivot()

    override fun visitRexOpCall(node: Rex.Op.Call, ctx: ProblemCallback) =
        super.visitRexOpCall(node, ctx) as org.partiql.plan.Rex.Op

    override fun visitRexOpCallStatic(node: Rex.Op.Call.Static, ctx: ProblemCallback): org.partiql.plan.Rex.Op {
        val fn = visitFn(node.fn, ctx)
        val args = node.args.map { visitRex(it, ctx) }
        return when (fn) {
            is org.partiql.plan.Fn -> {
                org.partiql.plan.Rex.Op.Call.Static(fn, args)
            }
            is org.partiql.plan.Rex.Op -> {
                // had error
                fn
            }
            else -> {
                error("Expected Fn or Err, found $fn")
            }
        }
    }

    override fun visitRexOpCallDynamic(node: Rex.Op.Call.Dynamic, ctx: ProblemCallback): PlanNode {
        val candidates = node.candidates.map {
            val c = visitRexOpCallDynamicCandidate(it, ctx)
            if (c is org.partiql.plan.Rex.Op.Err) return c
            c as org.partiql.plan.Rex.Op.Call.Dynamic.Candidate
        }
        return org.partiql.plan.Rex.Op.Call.Dynamic(
            candidates = candidates,
            args = node.args.map { visitRex(it, ctx) }
        )
    }

    override fun visitRexOpCallDynamicCandidate(node: Rex.Op.Call.Dynamic.Candidate, ctx: ProblemCallback): PlanNode {
        val fn = visitFn(node.fn, ctx)
        if (fn is org.partiql.plan.Rex.Op.Err) return fn
        fn as org.partiql.plan.Fn
        val coercions = node.coercions.map {
            it?.let {
                val c = visitFn(it, ctx)
                if (c is org.partiql.plan.Rex.Op.Err) return c
                c as org.partiql.plan.Fn
            }
        }
        return org.partiql.plan.Rex.Op.Call.Dynamic.Candidate(fn, coercions)
    }

    override fun visitRexOpCase(node: Rex.Op.Case, ctx: ProblemCallback) = org.partiql.plan.Rex.Op.Case(
        branches = node.branches.map { visitRexOpCaseBranch(it, ctx) }, default = visitRex(node.default, ctx)
        )

        override fun visitRexOpCaseBranch(node: Rex.Op.Case.Branch, ctx: ProblemCallback) =
            org.partiql.plan.Rex.Op.Case.Branch(
                condition = visitRex(node.condition, ctx), rex = visitRex(node.rex, ctx)
            )

        override fun visitRexOpCollection(node: Rex.Op.Collection, ctx: ProblemCallback) =
            org.partiql.plan.Rex.Op.Collection(values = node.values.map { visitRex(it, ctx) })

        override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: ProblemCallback) =
            org.partiql.plan.Rex.Op.Struct(fields = node.fields.map { visitRexOpStructField(it, ctx) })

        override fun visitRexOpStructField(node: Rex.Op.Struct.Field, ctx: ProblemCallback) =
            org.partiql.plan.Rex.Op.Struct.Field(
                k = visitRex(node.k, ctx),
                v = visitRex(node.v, ctx),
            )

        override fun visitRexOpPivot(node: Rex.Op.Pivot, ctx: ProblemCallback) = org.partiql.plan.Rex.Op.Pivot(
            key = visitRex(node.key, ctx),
            value = visitRex(node.value, ctx),
            rel = visitRel(node.rel, ctx),
        )

        override fun visitRexOpSubquery(node: Rex.Op.Subquery, ctx: ProblemCallback) = org.partiql.plan.Rex.Op.Subquery(
            select = visitRexOpSelect(node.select, ctx),
            coercion = when (node.coercion) {
                Rex.Op.Subquery.Coercion.SCALAR -> org.partiql.plan.Rex.Op.Subquery.Coercion.SCALAR
                Rex.Op.Subquery.Coercion.ROW -> org.partiql.plan.Rex.Op.Subquery.Coercion.ROW
            }
        )

        override fun visitRexOpSelect(node: Rex.Op.Select, ctx: ProblemCallback) = org.partiql.plan.Rex.Op.Select(
            constructor = visitRex(node.constructor, ctx),
            rel = visitRel(node.rel, ctx),
        )

        override fun visitRexOpTupleUnion(node: Rex.Op.TupleUnion, ctx: ProblemCallback) =
            org.partiql.plan.Rex.Op.TupleUnion(args = node.args.map { visitRex(it, ctx) })

        override fun visitRexOpErr(node: Rex.Op.Err, ctx: ProblemCallback) = org.partiql.plan.Rex.Op.Err(node.message)

        // RELATION OPERATORS

        override fun visitRel(node: Rel, ctx: ProblemCallback) = org.partiql.plan.Rel(
            type = visitRelType(node.type, ctx),
            op = visitRelOp(node.op, ctx),
        )

        override fun visitRelType(node: Rel.Type, ctx: ProblemCallback) =
            org.partiql.plan.Rel.Type(
                schema = node.schema.map { visitRelBinding(it, ctx) },
                props = node.props.map {
                    when (it) {
                        Rel.Prop.ORDERED -> org.partiql.plan.Rel.Prop.ORDERED
                    }
                }.toSet()

            )

        override fun visitRelOp(node: Rel.Op, ctx: ProblemCallback) = super.visitRelOp(node, ctx) as org.partiql.plan.Rel.Op

        override fun visitRelOpScan(node: Rel.Op.Scan, ctx: ProblemCallback) = org.partiql.plan.Rel.Op.Scan(
            rex = visitRex(node.rex, ctx),
        )

        override fun visitRelOpScanIndexed(node: Rel.Op.ScanIndexed, ctx: ProblemCallback) =
            org.partiql.plan.Rel.Op.ScanIndexed(
                rex = visitRex(node.rex, ctx),
            )

        override fun visitRelOpUnpivot(node: Rel.Op.Unpivot, ctx: ProblemCallback) = org.partiql.plan.Rel.Op.Unpivot(
            rex = visitRex(node.rex, ctx),
        )

        override fun visitRelOpDistinct(node: Rel.Op.Distinct, ctx: ProblemCallback) = org.partiql.plan.Rel.Op.Distinct(
            input = visitRel(node.input, ctx),
        )

        override fun visitRelOpFilter(node: Rel.Op.Filter, ctx: ProblemCallback) = org.partiql.plan.Rel.Op.Filter(
            input = visitRel(node.input, ctx),
            predicate = visitRex(node.predicate, ctx),
        )

        override fun visitRelOpSort(node: Rel.Op.Sort, ctx: ProblemCallback) =
            org.partiql.plan.Rel.Op.Sort(
                input = visitRel(node.input, ctx),
                specs = node.specs.map { visitRelOpSortSpec(it, ctx) }
            )

        override fun visitRelOpSortSpec(node: Rel.Op.Sort.Spec, ctx: ProblemCallback) = org.partiql.plan.Rel.Op.Sort.Spec(
            rex = visitRex(node.rex, ctx),
            order = when (node.order) {
                Rel.Op.Sort.Order.ASC_NULLS_LAST -> org.partiql.plan.Rel.Op.Sort.Order.ASC_NULLS_LAST
                Rel.Op.Sort.Order.ASC_NULLS_FIRST -> org.partiql.plan.Rel.Op.Sort.Order.ASC_NULLS_FIRST
                Rel.Op.Sort.Order.DESC_NULLS_LAST -> org.partiql.plan.Rel.Op.Sort.Order.DESC_NULLS_LAST
                Rel.Op.Sort.Order.DESC_NULLS_FIRST -> org.partiql.plan.Rel.Op.Sort.Order.DESC_NULLS_FIRST
            }
        )

        override fun visitRelOpUnion(node: Rel.Op.Union, ctx: ProblemCallback) = org.partiql.plan.Rel.Op.Union(
            lhs = visitRel(node.lhs, ctx),
            rhs = visitRel(node.rhs, ctx),
        )

        override fun visitRelOpIntersect(node: Rel.Op.Intersect, ctx: ProblemCallback) = org.partiql.plan.Rel.Op.Intersect(
            lhs = visitRel(node.lhs, ctx),
            rhs = visitRel(node.rhs, ctx),
        )

        override fun visitRelOpExcept(node: Rel.Op.Except, ctx: ProblemCallback) = org.partiql.plan.Rel.Op.Except(
            lhs = visitRel(node.lhs, ctx),
            rhs = visitRel(node.rhs, ctx),
        )

        override fun visitRelOpLimit(node: Rel.Op.Limit, ctx: ProblemCallback) = org.partiql.plan.Rel.Op.Limit(
            input = visitRel(node.input, ctx),
            limit = visitRex(node.limit, ctx),
        )

        override fun visitRelOpOffset(node: Rel.Op.Offset, ctx: ProblemCallback) = org.partiql.plan.Rel.Op.Offset(
            input = visitRel(node.input, ctx),
            offset = visitRex(node.offset, ctx),
        )

        override fun visitRelOpProject(node: Rel.Op.Project, ctx: ProblemCallback) = org.partiql.plan.Rel.Op.Project(
            input = visitRel(node.input, ctx),
            projections = node.projections.map { visitRex(it, ctx) },
        )

        override fun visitRelOpJoin(node: Rel.Op.Join, ctx: ProblemCallback) = org.partiql.plan.Rel.Op.Join(
            lhs = visitRel(node.lhs, ctx),
            rhs = visitRel(node.rhs, ctx),
            rex = visitRex(node.rex, ctx),
            type = when (node.type) {
                Rel.Op.Join.Type.INNER -> org.partiql.plan.Rel.Op.Join.Type.INNER
                Rel.Op.Join.Type.LEFT -> org.partiql.plan.Rel.Op.Join.Type.LEFT
                Rel.Op.Join.Type.RIGHT -> org.partiql.plan.Rel.Op.Join.Type.RIGHT
                Rel.Op.Join.Type.FULL -> org.partiql.plan.Rel.Op.Join.Type.FULL
            }
        )

        override fun visitRelOpAggregate(node: Rel.Op.Aggregate, ctx: ProblemCallback) = org.partiql.plan.Rel.Op.Aggregate(
            input = visitRel(node.input, ctx),
            strategy = when (node.strategy) {
                Rel.Op.Aggregate.Strategy.FULL -> org.partiql.plan.Rel.Op.Aggregate.Strategy.FULL
                Rel.Op.Aggregate.Strategy.PARTIAL -> org.partiql.plan.Rel.Op.Aggregate.Strategy.PARTIAL
            },
            calls = node.calls.map { visitRelOpAggregateCall(it, ctx) },
            groups = node.groups.map { visitRex(it, ctx) },
        )

        override fun visitRelOpAggregateCall(node: Rel.Op.Aggregate.Call, ctx: ProblemCallback) =
            org.partiql.plan.Rel.Op.Aggregate.Call(
                agg = visitAgg(node.agg, ctx),
                args = node.args.map { visitRex(it, ctx) },
            )

        override fun visitRelOpExclude(node: Rel.Op.Exclude, ctx: ProblemCallback) = org.partiql.plan.Rel.Op.Exclude(
            input = visitRel(node.input, ctx),
            items = node.items.map { visitRelOpExcludeItem(it, ctx) },
        )

        override fun visitRelOpExcludeItem(node: Rel.Op.Exclude.Item, ctx: ProblemCallback) =
            org.partiql.plan.Rel.Op.Exclude.Item(
                root = visitIdentifierSymbol(node.root, ctx),
                steps = node.steps.map { visitRelOpExcludeStep(it, ctx) },
            )

        override fun visitRelOpExcludeStep(node: Rel.Op.Exclude.Step, ctx: ProblemCallback) =
            super.visit(node, ctx) as org.partiql.plan.Rel.Op.Exclude.Step

        override fun visitRelOpExcludeStepAttr(node: Rel.Op.Exclude.Step.Attr, ctx: ProblemCallback) =
            org.partiql.plan.Rel.Op.Exclude.Step.Attr(
                symbol = visitIdentifierSymbol(node.symbol, ctx),
            )

        override fun visitRelOpExcludeStepPos(node: Rel.Op.Exclude.Step.Pos, ctx: ProblemCallback) =
            org.partiql.plan.Rel.Op.Exclude.Step.Pos(
                index = node.index,
            )

        override fun visitRelOpExcludeStepStructWildcard(
            node: Rel.Op.Exclude.Step.StructWildcard,
            ctx: ProblemCallback,
        ) = org.partiql.plan.Rel.Op.Exclude.Step.StructWildcard()

        override fun visitRelOpExcludeStepCollectionWildcard(
            node: Rel.Op.Exclude.Step.CollectionWildcard,
            ctx: ProblemCallback,
        ) = org.partiql.plan.Rel.Op.Exclude.Step.CollectionWildcard()

        override fun visitRelOpErr(node: Rel.Op.Err, ctx: ProblemCallback) = org.partiql.plan.Rel.Op.Err(node.message)

        override fun visitRelBinding(node: Rel.Binding, ctx: ProblemCallback) = org.partiql.plan.Rel.Binding(
            name = node.name,
            type = node.type,
        )
    }
    