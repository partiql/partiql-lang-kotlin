package org.partiql.planner.internal.transforms

import org.partiql.errors.ProblemCallback
import org.partiql.plan.PlanNode
import org.partiql.plan.partiQLPlan
import org.partiql.plan.rexOpCast
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.PartiQLPlan
import org.partiql.planner.internal.ir.Ref
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.Statement
import org.partiql.planner.internal.ir.visitor.PlanBaseVisitor
import org.partiql.value.PartiQLValueExperimental

/**
 * This is an internal utility to translate from the internal unresolved plan used for typing to the public plan IR.
 * At the moment, these data structures are very similar sans-unresolved variants. The internal unresolved plan
 * continues to undergo frequent changes as we improve our typing model. This indirection enables a more stable public
 * consumable API while guaranteeing resolution safety.
 *
 * Ideally this class becomes very small as the internal IR will be a thin wrapper over the public API.
 */
internal object PlanTransform {

    fun transform(node: PartiQLPlan, onProblem: ProblemCallback): org.partiql.plan.PartiQLPlan {
        val symbols = Symbols.empty()
        val visitor = Visitor(symbols, onProblem)
        val statement = visitor.visitStatement(node.statement, Unit)
        return partiQLPlan(
            catalogs = symbols.build(),
            statement = statement,
        )
    }

    private class Visitor(
        private val symbols: Symbols,
        private val onProblem: ProblemCallback,
    ) : PlanBaseVisitor<PlanNode, Unit>() {

        /**
         * Vi
         *
         * @param node
         * @param ctx
         * @return
         */
        override fun visitPartiQLPlan(node: PartiQLPlan, ctx: Unit): org.partiql.plan.PartiQLPlan {
            val statement = visitStatement(node.statement, ctx)
            return partiQLPlan(emptyList(), statement)
        }

        override fun defaultReturn(node: org.partiql.planner.internal.ir.PlanNode, ctx: Unit): PlanNode {
            error("Not implemented")
        }

        override fun visitRef(node: Ref, ctx: Unit) = super.visitRef(node, ctx) as org.partiql.plan.Ref

        /**
         * Insert into symbol table, returning the public reference.
         */
        override fun visitRefObj(node: Ref.Obj, ctx: Unit) = symbols.insert(node)

        /**
         * Insert into symbol table, returning the public reference.
         */
        override fun visitRefFn(node: Ref.Fn, ctx: Unit) = symbols.insert(node)

        @OptIn(PartiQLValueExperimental::class)
        override fun visitRefCast(node: Ref.Cast, ctx: Unit) = org.partiql.plan.refCast(node.input, node.target)

        override fun visitStatement(node: Statement, ctx: Unit) =
            super.visitStatement(node, ctx) as org.partiql.plan.Statement

        override fun visitStatementQuery(node: Statement.Query, ctx: Unit): org.partiql.plan.Statement.Query {
            val root = visitRex(node.root, ctx)
            return org.partiql.plan.Statement.Query(root)
        }

        override fun visitIdentifier(node: Identifier, ctx: Unit) =
            super.visitIdentifier(node, ctx) as org.partiql.plan.Identifier

        override fun visitIdentifierSymbol(node: Identifier.Symbol, ctx: Unit) = org.partiql.plan.Identifier.Symbol(
            symbol = node.symbol,
            caseSensitivity = when (node.caseSensitivity) {
                Identifier.CaseSensitivity.SENSITIVE -> org.partiql.plan.Identifier.CaseSensitivity.SENSITIVE
                Identifier.CaseSensitivity.INSENSITIVE -> org.partiql.plan.Identifier.CaseSensitivity.INSENSITIVE
            }
        )

        override fun visitIdentifierQualified(node: Identifier.Qualified, ctx: Unit) =
            org.partiql.plan.Identifier.Qualified(
                root = visitIdentifierSymbol(node.root, ctx),
                steps = node.steps.map { visitIdentifierSymbol(it, ctx) }
            )

        // EXPRESSIONS

        override fun visitRex(node: Rex, ctx: Unit): org.partiql.plan.Rex {
            val type = node.type
            val op = visitRexOp(node.op, ctx)
            return org.partiql.plan.Rex(type, op)
        }

        override fun visitRexOp(node: Rex.Op, ctx: Unit) = super.visitRexOp(node, ctx) as org.partiql.plan.Rex.Op

        @OptIn(PartiQLValueExperimental::class)
        override fun visitRexOpLit(node: Rex.Op.Lit, ctx: Unit) = org.partiql.plan.rexOpLit(node.value)

        override fun visitRexOpVar(node: Rex.Op.Var, ctx: Unit) =
            super.visitRexOpVar(node, ctx) as org.partiql.plan.Rex.Op

        override fun visitRexOpVarResolved(node: Rex.Op.Var.Resolved, ctx: Unit) = org.partiql.plan.Rex.Op.Var(node.ref)

        override fun visitRexOpVarUnresolved(node: Rex.Op.Var.Unresolved, ctx: Unit) =
            org.partiql.plan.Rex.Op.Err("Unresolved variable $node")

        override fun visitRexOpGlobal(node: Rex.Op.Global, ctx: Unit) = org.partiql.plan.Rex.Op.Global(
            ref = visitRef(node.ref, ctx)
        )

        override fun visitRexOpPathIndex(node: Rex.Op.Path.Index, ctx: Unit): PlanNode {
            val root = visitRex(node.root, ctx)
            val key = visitRex(node.key, ctx)
            return org.partiql.plan.Rex.Op.Path.Index(root, key)
        }

        override fun visitRexOpPathKey(node: Rex.Op.Path.Key, ctx: Unit): PlanNode {
            val root = visitRex(node.root, ctx)
            val key = visitRex(node.key, ctx)
            return org.partiql.plan.Rex.Op.Path.Key(root, key)
        }

        override fun visitRexOpPathSymbol(node: Rex.Op.Path.Symbol, ctx: Unit): PlanNode {
            val root = visitRex(node.root, ctx)
            return org.partiql.plan.Rex.Op.Path.Symbol(root, node.key)
        }

        override fun visitRexOpCall(node: Rex.Op.Call, ctx: Unit) =
            super.visitRexOpCall(node, ctx) as org.partiql.plan.Rex.Op

        override fun visitRexOpPath(node: Rex.Op.Path, ctx: Unit) =
            super.visitRexOpPath(node, ctx) as org.partiql.plan.Rex.Op.Path

        override fun visitRexOpCast(node: Rex.Op.Cast, ctx: Unit) =
            super.visitRexOpCast(node, ctx) as org.partiql.plan.Rex.Op.Cast

        override fun visitRexOpCastUnresolved(node: Rex.Op.Cast.Unresolved, ctx: Unit): PlanNode {
            error("Unresolved cast $node")
        }

        override fun visitRexOpCastResolved(node: Rex.Op.Cast.Resolved, ctx: Unit): PlanNode {
            val cast = visitRefCast(node.cast, ctx)
            val arg = visitRex(node.arg, ctx)
            return rexOpCast(cast, arg)
        }

        override fun visitRexOpCallUnresolved(node: Rex.Op.Call.Unresolved, ctx: Unit): PlanNode {
            error("Unresolved function ${node.identifier}")
        }

        override fun visitRexOpCallStatic(node: Rex.Op.Call.Static, ctx: Unit): org.partiql.plan.Rex.Op {
            val fn = visitRef(node.fn, ctx)
            val args = node.args.map { visitRex(it, ctx) }
            return org.partiql.plan.rexOpCallStatic(fn, args)
        }

        override fun visitRexOpCallDynamic(node: Rex.Op.Call.Dynamic, ctx: Unit): PlanNode {
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

        override fun visitRexOpCallDynamicCandidate(node: Rex.Op.Call.Dynamic.Candidate, ctx: Unit): PlanNode {
            val fn = visitRef(node.fn, ctx)
            val coercions = node.coercions.map { it?.let { visitRefCast(it, ctx) } }
            return org.partiql.plan.Rex.Op.Call.Dynamic.Candidate(fn, coercions)
        }

        override fun visitRexOpCase(node: Rex.Op.Case, ctx: Unit) = org.partiql.plan.Rex.Op.Case(
            branches = node.branches.map { visitRexOpCaseBranch(it, ctx) }, default = visitRex(node.default, ctx)
            )

            override fun visitRexOpCaseBranch(node: Rex.Op.Case.Branch, ctx: Unit) = org.partiql.plan.Rex.Op.Case.Branch(
                condition = visitRex(node.condition, ctx), rex = visitRex(node.rex, ctx)
            )

            override fun visitRexOpCollection(node: Rex.Op.Collection, ctx: Unit) =
                org.partiql.plan.Rex.Op.Collection(values = node.values.map { visitRex(it, ctx) })

            override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: Unit) =
                org.partiql.plan.Rex.Op.Struct(fields = node.fields.map { visitRexOpStructField(it, ctx) })

            override fun visitRexOpStructField(node: Rex.Op.Struct.Field, ctx: Unit) = org.partiql.plan.Rex.Op.Struct.Field(
                k = visitRex(node.k, ctx),
                v = visitRex(node.v, ctx),
            )

            override fun visitRexOpPivot(node: Rex.Op.Pivot, ctx: Unit) = org.partiql.plan.Rex.Op.Pivot(
                key = visitRex(node.key, ctx),
                value = visitRex(node.value, ctx),
                rel = visitRel(node.rel, ctx),
            )

            override fun visitRexOpSubquery(node: Rex.Op.Subquery, ctx: Unit) = org.partiql.plan.Rex.Op.Subquery(
                select = visitRexOpSelect(node.select, ctx),
                coercion = when (node.coercion) {
                    Rex.Op.Subquery.Coercion.SCALAR -> org.partiql.plan.Rex.Op.Subquery.Coercion.SCALAR
                    Rex.Op.Subquery.Coercion.ROW -> org.partiql.plan.Rex.Op.Subquery.Coercion.ROW
                }
            )

            override fun visitRexOpSelect(node: Rex.Op.Select, ctx: Unit) = org.partiql.plan.Rex.Op.Select(
                constructor = visitRex(node.constructor, ctx),
                rel = visitRel(node.rel, ctx),
            )

            override fun visitRexOpTupleUnion(node: Rex.Op.TupleUnion, ctx: Unit) =
                org.partiql.plan.Rex.Op.TupleUnion(args = node.args.map { visitRex(it, ctx) })

            override fun visitRexOpErr(node: Rex.Op.Err, ctx: Unit) = org.partiql.plan.Rex.Op.Err(node.message)

            // RELATION OPERATORS

            override fun visitRel(node: Rel, ctx: Unit) = org.partiql.plan.Rel(
                type = visitRelType(node.type, ctx),
                op = visitRelOp(node.op, ctx),
            )

            override fun visitRelType(node: Rel.Type, ctx: Unit) =
                org.partiql.plan.Rel.Type(
                    schema = node.schema.map { visitRelBinding(it, ctx) },
                    props = node.props.map {
                        when (it) {
                            Rel.Prop.ORDERED -> org.partiql.plan.Rel.Prop.ORDERED
                        }
                    }.toSet()

                )

            override fun visitRelOp(node: Rel.Op, ctx: Unit) = super.visitRelOp(node, ctx) as org.partiql.plan.Rel.Op

            override fun visitRelOpScan(node: Rel.Op.Scan, ctx: Unit) = org.partiql.plan.Rel.Op.Scan(
                rex = visitRex(node.rex, ctx),
            )

            override fun visitRelOpScanIndexed(node: Rel.Op.ScanIndexed, ctx: Unit) = org.partiql.plan.Rel.Op.ScanIndexed(
                rex = visitRex(node.rex, ctx),
            )

            override fun visitRelOpUnpivot(node: Rel.Op.Unpivot, ctx: Unit) = org.partiql.plan.Rel.Op.Unpivot(
                rex = visitRex(node.rex, ctx),
            )

            override fun visitRelOpDistinct(node: Rel.Op.Distinct, ctx: Unit) = org.partiql.plan.Rel.Op.Distinct(
                input = visitRel(node.input, ctx),
            )

            override fun visitRelOpFilter(node: Rel.Op.Filter, ctx: Unit) = org.partiql.plan.Rel.Op.Filter(
                input = visitRel(node.input, ctx),
                predicate = visitRex(node.predicate, ctx),
            )

            override fun visitRelOpSort(node: Rel.Op.Sort, ctx: Unit) =
                org.partiql.plan.Rel.Op.Sort(
                    input = visitRel(node.input, ctx),
                    specs = node.specs.map { visitRelOpSortSpec(it, ctx) }
                )

            override fun visitRelOpSortSpec(node: Rel.Op.Sort.Spec, ctx: Unit) = org.partiql.plan.Rel.Op.Sort.Spec(
                rex = visitRex(node.rex, ctx),
                order = when (node.order) {
                    Rel.Op.Sort.Order.ASC_NULLS_LAST -> org.partiql.plan.Rel.Op.Sort.Order.ASC_NULLS_LAST
                    Rel.Op.Sort.Order.ASC_NULLS_FIRST -> org.partiql.plan.Rel.Op.Sort.Order.ASC_NULLS_FIRST
                    Rel.Op.Sort.Order.DESC_NULLS_LAST -> org.partiql.plan.Rel.Op.Sort.Order.DESC_NULLS_LAST
                    Rel.Op.Sort.Order.DESC_NULLS_FIRST -> org.partiql.plan.Rel.Op.Sort.Order.DESC_NULLS_FIRST
                }
            )

            override fun visitRelOpUnion(node: Rel.Op.Union, ctx: Unit) = org.partiql.plan.Rel.Op.Union(
                lhs = visitRel(node.lhs, ctx),
                rhs = visitRel(node.rhs, ctx),
            )

            override fun visitRelOpIntersect(node: Rel.Op.Intersect, ctx: Unit) = org.partiql.plan.Rel.Op.Intersect(
                lhs = visitRel(node.lhs, ctx),
                rhs = visitRel(node.rhs, ctx),
            )

            override fun visitRelOpExcept(node: Rel.Op.Except, ctx: Unit) = org.partiql.plan.Rel.Op.Except(
                lhs = visitRel(node.lhs, ctx),
                rhs = visitRel(node.rhs, ctx),
            )

            override fun visitRelOpLimit(node: Rel.Op.Limit, ctx: Unit) = org.partiql.plan.Rel.Op.Limit(
                input = visitRel(node.input, ctx),
                limit = visitRex(node.limit, ctx),
            )

            override fun visitRelOpOffset(node: Rel.Op.Offset, ctx: Unit) = org.partiql.plan.Rel.Op.Offset(
                input = visitRel(node.input, ctx),
                offset = visitRex(node.offset, ctx),
            )

            override fun visitRelOpProject(node: Rel.Op.Project, ctx: Unit) = org.partiql.plan.Rel.Op.Project(
                input = visitRel(node.input, ctx),
                projections = node.projections.map { visitRex(it, ctx) },
            )

            override fun visitRelOpJoin(node: Rel.Op.Join, ctx: Unit) = org.partiql.plan.Rel.Op.Join(
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

            override fun visitRelOpAggregate(node: Rel.Op.Aggregate, ctx: Unit) = org.partiql.plan.Rel.Op.Aggregate(
                input = visitRel(node.input, ctx),
                strategy = when (node.strategy) {
                    Rel.Op.Aggregate.Strategy.FULL -> org.partiql.plan.Rel.Op.Aggregate.Strategy.FULL
                    Rel.Op.Aggregate.Strategy.PARTIAL -> org.partiql.plan.Rel.Op.Aggregate.Strategy.PARTIAL
                },
                calls = node.calls.map { visitRelOpAggregateCall(it, ctx) },
                groups = node.groups.map { visitRex(it, ctx) },
            )

            override fun visitRelOpAggregateCall(node: Rel.Op.Aggregate.Call, ctx: Unit) =
                super.visitRelOpAggregateCall(node, ctx) as org.partiql.plan.Rel.Op.Aggregate.Call

            override fun visitRelOpAggregateCallUnresolved(node: Rel.Op.Aggregate.Call.Unresolved, ctx: Unit): PlanNode {
                error("Unresolved aggregate call $node")
            }

            override fun visitRelOpAggregateCallResolved(node: Rel.Op.Aggregate.Call.Resolved, ctx: Unit): PlanNode {
                val agg = node.agg.name
                val args = node.args.map { visitRex(it, ctx) }
                return org.partiql.plan.relOpAggregateCall(node.agg.name, args)
            }

            override fun visitRelOpExclude(node: Rel.Op.Exclude, ctx: Unit) = org.partiql.plan.Rel.Op.Exclude(
                input = visitRel(node.input, ctx),
                items = node.items.map { visitRelOpExcludeItem(it, ctx) },
            )

            override fun visitRelOpExcludeItem(
                node: Rel.Op.Exclude.Item,
                ctx: Unit,
            ): org.partiql.plan.Rel.Op.Exclude.Item {
                val root = when (node.root) {
                    is Rex.Op.Var.Resolved -> visitRexOpVar(node.root, ctx) as org.partiql.plan.Rex.Op.Var
                    is Rex.Op.Var.Unresolved -> org.partiql.plan.Rex.Op.Var(-1) // unresolved in `PlanTyper` results in error
                }
                return org.partiql.plan.Rel.Op.Exclude.Item(
                    root = root,
                    steps = node.steps.map { visitRelOpExcludeStep(it, ctx) },
                )
            }

            override fun visitRelOpExcludeStep(node: Rel.Op.Exclude.Step, ctx: Unit) =
                super.visit(node, ctx) as org.partiql.plan.Rel.Op.Exclude.Step

            override fun visitRelOpExcludeStepStructField(node: Rel.Op.Exclude.Step.StructField, ctx: Unit) =
                org.partiql.plan.Rel.Op.Exclude.Step.StructField(
                    symbol = visitIdentifierSymbol(node.symbol, ctx),
                )

            override fun visitRelOpExcludeStepCollIndex(node: Rel.Op.Exclude.Step.CollIndex, ctx: Unit) =
                org.partiql.plan.Rel.Op.Exclude.Step.CollIndex(
                    index = node.index,
                )

            override fun visitRelOpExcludeStepStructWildcard(
                node: Rel.Op.Exclude.Step.StructWildcard,
                ctx: Unit,
            ) = org.partiql.plan.Rel.Op.Exclude.Step.StructWildcard()

            override fun visitRelOpExcludeStepCollWildcard(
                node: Rel.Op.Exclude.Step.CollWildcard,
                ctx: Unit,
            ) = org.partiql.plan.Rel.Op.Exclude.Step.CollWildcard()

            override fun visitRelOpErr(node: Rel.Op.Err, ctx: Unit) = org.partiql.plan.Rel.Op.Err(node.message)

            override fun visitRelBinding(node: Rel.Binding, ctx: Unit) = org.partiql.plan.Rel.Binding(
                name = node.name,
                type = node.type,
            )
        }
    }
    