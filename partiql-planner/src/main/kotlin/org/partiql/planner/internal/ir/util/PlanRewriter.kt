@file:Suppress(
    "UNUSED_PARAMETER",
    "UNUSED_VARIABLE",
) @file:OptIn(PartiQLValueExperimental::class)

package org.partiql.planner.internal.ir.util

import org.partiql.planner.internal.ir.Agg
import org.partiql.planner.internal.ir.Fn
import org.partiql.planner.internal.ir.Global
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.PartiQLPlan
import org.partiql.planner.internal.ir.PlanNode
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.Statement
import org.partiql.planner.internal.ir.visitor.PlanBaseVisitor
import org.partiql.value.PartiQLValueExperimental

internal abstract class PlanRewriter<C> : PlanBaseVisitor<PlanNode, C>() {

    override fun defaultReturn(node: PlanNode, ctx: C): PlanNode = node

    private inline fun <reified T> _visitList(
        nodes: List<T>,
        ctx: C,
        method: (node: T, ctx: C) -> PlanNode,
    ): List<T> {
        if (nodes.isEmpty()) return nodes
        var diff = false
        val transformed = ArrayList<T>(nodes.size)
        nodes.forEach {
            val n = method(it, ctx) as T
            if (it !== n) diff = true
            transformed.add(n)
        }
        return if (diff) transformed else nodes
    }

    private inline fun <reified T> _visitListNull(
        nodes: List<T?>,
        ctx: C,
        method: (node: T, ctx: C) -> PlanNode,
    ): List<T?> {
        if (nodes.isEmpty()) return nodes
        var diff = false
        val transformed = ArrayList<T?>(nodes.size)
        nodes.forEach {
            val n = if (it == null) null else method(it, ctx) as T
            if (it !== n) diff = true
            transformed.add(n)
        }
        return if (diff) transformed else nodes
    }

    private inline fun <reified T> _visitSet(
        nodes: Set<T>,
        ctx: C,
        method: (node: T, ctx: C) -> PlanNode,
    ): Set<T> {
        if (nodes.isEmpty()) return nodes
        var diff = false
        val transformed = HashSet<T>(nodes.size)
        nodes.forEach {
            val n = method(it, ctx) as T
            if (it !== n) diff = true
            transformed.add(n)
        }
        return if (diff) transformed else nodes
    }

    private inline fun <reified T> _visitSetNull(
        nodes: Set<T?>,
        ctx: C,
        method: (node: T, ctx: C) -> PlanNode,
    ): Set<T?> {
        if (nodes.isEmpty()) return nodes
        var diff = false
        val transformed = HashSet<T?>(nodes.size)
        nodes.forEach {
            val n = if (it == null) null else method(it, ctx) as T
            if (it !== n) diff = true
            transformed.add(n)
        }
        return if (diff) transformed else nodes
    }

    override fun visitPartiQLPlan(node: PartiQLPlan, ctx: C): PlanNode {
        val version = node.version
        val globals = _visitList(node.globals, ctx, ::visitGlobal)
        val statement = visitStatement(node.statement, ctx) as Statement
        return if (version !== node.version || globals !== node.globals || statement !== node.statement) {
            PartiQLPlan(version, globals, statement)
        } else {
            node
        }
    }

    override fun visitGlobal(node: Global, ctx: C): PlanNode {
        val path = visitIdentifierQualified(node.path, ctx) as Identifier.Qualified
        val type = node.type
        return if (path !== node.path || type !== node.type) {
            Global(path, type)
        } else {
            node
        }
    }

    override fun visitFnResolved(node: Fn.Resolved, ctx: C): PlanNode {
        val signature = node.signature
        return node
    }

    override fun visitFnUnresolved(node: Fn.Unresolved, ctx: C): PlanNode {
        val identifier = visitIdentifier(node.identifier, ctx) as Identifier
        val isHidden = node.isHidden
        return if (identifier !== node.identifier || isHidden !== node.isHidden) {
            Fn.Unresolved(identifier, isHidden)
        } else {
            node
        }
    }

    override fun visitAggResolved(node: Agg.Resolved, ctx: C): PlanNode {
        val signature = node.signature
        return node
    }

    override fun visitAggUnresolved(node: Agg.Unresolved, ctx: C): PlanNode {
        val identifier = visitIdentifier(node.identifier, ctx) as Identifier
        return if (identifier !== node.identifier) {
            Agg.Unresolved(identifier)
        } else {
            node
        }
    }

    override fun visitStatementQuery(node: Statement.Query, ctx: C): PlanNode {
        val root = visitRex(node.root, ctx) as Rex
        return if (root !== node.root) {
            Statement.Query(root)
        } else {
            node
        }
    }

    override fun visitIdentifierSymbol(node: Identifier.Symbol, ctx: C): PlanNode {
        val symbol = node.symbol
        val caseSensitivity = node.caseSensitivity
        return node
    }

    override fun visitIdentifierQualified(node: Identifier.Qualified, ctx: C): PlanNode {
        val root = visitIdentifierSymbol(node.root, ctx) as Identifier.Symbol
        val steps = _visitList(node.steps, ctx, ::visitIdentifierSymbol)
        return if (root !== node.root || steps !== node.steps) {
            Identifier.Qualified(root, steps)
        } else {
            node
        }
    }

    override fun visitRex(node: Rex, ctx: C): PlanNode {
        val type = node.type
        val op = visitRexOp(node.op, ctx) as Rex.Op
        return if (type !== node.type || op !== node.op) {
            Rex(type, op)
        } else {
            node
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun visitRexOpLit(node: Rex.Op.Lit, ctx: C): PlanNode {
        val value = node.value
        return node
    }

    override fun visitRexOpVarResolved(node: Rex.Op.Var.Resolved, ctx: C): PlanNode {
        val ref = node.ref
        return node
    }

    override fun visitRexOpVarUnresolved(node: Rex.Op.Var.Unresolved, ctx: C): PlanNode {
        val identifier = visitIdentifier(node.identifier, ctx) as Identifier
        val scope = node.scope
        return if (identifier !== node.identifier || scope !== node.scope) {
            Rex.Op.Var.Unresolved(identifier, scope)
        } else {
            node
        }
    }

    override fun visitRexOpGlobal(node: Rex.Op.Global, ctx: C): PlanNode {
        val ref = node.ref
        return node
    }

    override fun visitRexOpPath(node: Rex.Op.Path, ctx: C): PlanNode {
        val root = visitRex(node.root, ctx) as Rex
        val steps = _visitList(node.steps, ctx, ::visitRexOpPathStep)
        return if (root !== node.root || steps !== node.steps) {
            Rex.Op.Path(root, steps)
        } else {
            node
        }
    }

    override fun visitRexOpPathStepIndex(node: Rex.Op.Path.Step.Index, ctx: C): PlanNode {
        val key = visitRex(node.key, ctx) as Rex
        return if (key !== node.key) {
            Rex.Op.Path.Step.Index(key)
        } else {
            node
        }
    }

    override fun visitRexOpPathStepSymbol(node: Rex.Op.Path.Step.Symbol, ctx: C): PlanNode {
        val identifier = visitIdentifierSymbol(node.identifier, ctx) as Identifier.Symbol
        return if (identifier !== node.identifier) {
            Rex.Op.Path.Step.Symbol(identifier)
        } else {
            node
        }
    }

    override fun visitRexOpPathStepWildcard(node: Rex.Op.Path.Step.Wildcard, ctx: C): PlanNode = node

    override fun visitRexOpPathStepUnpivot(node: Rex.Op.Path.Step.Unpivot, ctx: C): PlanNode = node

    override fun visitRexOpCallStatic(node: Rex.Op.Call.Static, ctx: C): PlanNode {
        val fn = visitFn(node.fn, ctx) as Fn
        val args = _visitList(node.args, ctx, ::visitRex)
        return if (fn !== node.fn || args !== node.args) {
            Rex.Op.Call.Static(fn, args)
        } else {
            node
        }
    }

    override fun visitRexOpCallDynamic(node: Rex.Op.Call.Dynamic, ctx: C): PlanNode {
        val args = _visitList(node.args, ctx, ::visitRex)
        val candidates = _visitList(node.candidates, ctx, ::visitRexOpCallDynamicCandidate)
        return if (args !== node.args || candidates !== node.candidates) {
            Rex.Op.Call.Dynamic(args, candidates)
        } else {
            node
        }
    }

    override fun visitRexOpCallDynamicCandidate(node: Rex.Op.Call.Dynamic.Candidate, ctx: C): PlanNode {
        val fn = visitFnResolved(node.fn, ctx) as Fn.Resolved
        val coercions = _visitListNull(node.coercions, ctx, ::visitFnResolved)
        return if (fn !== node.fn || coercions !== node.coercions) {
            Rex.Op.Call.Dynamic.Candidate(fn, coercions)
        } else {
            node
        }
    }

    override fun visitRexOpCase(node: Rex.Op.Case, ctx: C): PlanNode {
        val branches = _visitList(node.branches, ctx, ::visitRexOpCaseBranch)
        val default = visitRex(node.default, ctx) as Rex
        return if (branches !== node.branches || default !== node.default) {
            Rex.Op.Case(branches, default)
        } else {
            node
        }
    }

    override fun visitRexOpCaseBranch(node: Rex.Op.Case.Branch, ctx: C): PlanNode {
        val condition = visitRex(node.condition, ctx) as Rex
        val rex = visitRex(node.rex, ctx) as Rex
        return if (condition !== node.condition || rex !== node.rex) {
            Rex.Op.Case.Branch(condition, rex)
        } else {
            node
        }
    }

    override fun visitRexOpCollection(node: Rex.Op.Collection, ctx: C): PlanNode {
        val values = _visitList(node.values, ctx, ::visitRex)
        return if (values !== node.values) {
            Rex.Op.Collection(values)
        } else {
            node
        }
    }

    override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: C): PlanNode {
        val fields = _visitList(node.fields, ctx, ::visitRexOpStructField)
        return if (fields !== node.fields) {
            Rex.Op.Struct(fields)
        } else {
            node
        }
    }

    override fun visitRexOpStructField(node: Rex.Op.Struct.Field, ctx: C): PlanNode {
        val k = visitRex(node.k, ctx) as Rex
        val v = visitRex(node.v, ctx) as Rex
        return if (k !== node.k || v !== node.v) {
            Rex.Op.Struct.Field(k, v)
        } else {
            node
        }
    }

    override fun visitRexOpPivot(node: Rex.Op.Pivot, ctx: C): PlanNode {
        val key = visitRex(node.key, ctx) as Rex
        val value = visitRex(node.value, ctx) as Rex
        val rel = visitRel(node.rel, ctx) as Rel
        return if (key !== node.key || value !== node.value || rel !== node.rel) {
            Rex.Op.Pivot(key, value, rel)
        } else {
            node
        }
    }

    override fun visitRexOpSubquery(node: Rex.Op.Subquery, ctx: C): PlanNode {
        val select = visitRexOpSelect(node.select, ctx) as Rex.Op.Select
        val coercion = node.coercion
        return if (select !== node.select || coercion !== node.coercion) {
            Rex.Op.Subquery(select, coercion)
        } else {
            node
        }
    }

    override fun visitRexOpSelect(node: Rex.Op.Select, ctx: C): PlanNode {
        val constructor = visitRex(node.constructor, ctx) as Rex
        val rel = visitRel(node.rel, ctx) as Rel
        return if (constructor !== node.constructor || rel !== node.rel) {
            Rex.Op.Select(constructor, rel)
        } else {
            node
        }
    }

    override fun visitRexOpTupleUnion(node: Rex.Op.TupleUnion, ctx: C): PlanNode {
        val args = _visitList(node.args, ctx, ::visitRex)
        return if (args !== node.args) {
            Rex.Op.TupleUnion(args)
        } else {
            node
        }
    }

    override fun visitRexOpErr(node: Rex.Op.Err, ctx: C): PlanNode {
        val message = node.message
        return node
    }

    override fun visitRel(node: Rel, ctx: C): PlanNode {
        val type = visitRelType(node.type, ctx) as Rel.Type
        val op = visitRelOp(node.op, ctx) as Rel.Op
        return if (type !== node.type || op !== node.op) {
            Rel(type, op)
        } else {
            node
        }
    }

    override fun visitRelType(node: Rel.Type, ctx: C): PlanNode {
        val schema = _visitList(node.schema, ctx, ::visitRelBinding)
        val props = node.props
        return if (schema !== node.schema || props !== node.props) {
            Rel.Type(schema, props)
        } else {
            node
        }
    }

    override fun visitRelOpScan(node: Rel.Op.Scan, ctx: C): PlanNode {
        val rex = visitRex(node.rex, ctx) as Rex
        return if (rex !== node.rex) {
            Rel.Op.Scan(rex)
        } else {
            node
        }
    }

    override fun visitRelOpScanIndexed(node: Rel.Op.ScanIndexed, ctx: C): PlanNode {
        val rex = visitRex(node.rex, ctx) as Rex
        return if (rex !== node.rex) {
            Rel.Op.ScanIndexed(rex)
        } else {
            node
        }
    }

    override fun visitRelOpUnpivot(node: Rel.Op.Unpivot, ctx: C): PlanNode {
        val rex = visitRex(node.rex, ctx) as Rex
        return if (rex !== node.rex) {
            Rel.Op.Unpivot(rex)
        } else {
            node
        }
    }

    override fun visitRelOpDistinct(node: Rel.Op.Distinct, ctx: C): PlanNode {
        val input = visitRel(node.input, ctx) as Rel
        return if (input !== node.input) {
            Rel.Op.Distinct(input)
        } else {
            node
        }
    }

    override fun visitRelOpFilter(node: Rel.Op.Filter, ctx: C): PlanNode {
        val input = visitRel(node.input, ctx) as Rel
        val predicate = visitRex(node.predicate, ctx) as Rex
        return if (input !== node.input || predicate !== node.predicate) {
            Rel.Op.Filter(input, predicate)
        } else {
            node
        }
    }

    override fun visitRelOpSort(node: Rel.Op.Sort, ctx: C): PlanNode {
        val input = visitRel(node.input, ctx) as Rel
        val specs = _visitList(node.specs, ctx, ::visitRelOpSortSpec)
        return if (input !== node.input || specs !== node.specs) {
            Rel.Op.Sort(input, specs)
        } else {
            node
        }
    }

    override fun visitRelOpSortSpec(node: Rel.Op.Sort.Spec, ctx: C): PlanNode {
        val rex = visitRex(node.rex, ctx) as Rex
        val order = node.order
        return if (rex !== node.rex || order !== node.order) {
            Rel.Op.Sort.Spec(rex, order)
        } else {
            node
        }
    }

    override fun visitRelOpUnion(node: Rel.Op.Union, ctx: C): PlanNode {
        val lhs = visitRel(node.lhs, ctx) as Rel
        val rhs = visitRel(node.rhs, ctx) as Rel
        return if (lhs !== node.lhs || rhs !== node.rhs) {
            Rel.Op.Union(lhs, rhs)
        } else {
            node
        }
    }

    override fun visitRelOpIntersect(node: Rel.Op.Intersect, ctx: C): PlanNode {
        val lhs = visitRel(node.lhs, ctx) as Rel
        val rhs = visitRel(node.rhs, ctx) as Rel
        return if (lhs !== node.lhs || rhs !== node.rhs) {
            Rel.Op.Intersect(lhs, rhs)
        } else {
            node
        }
    }

    override fun visitRelOpExcept(node: Rel.Op.Except, ctx: C): PlanNode {
        val lhs = visitRel(node.lhs, ctx) as Rel
        val rhs = visitRel(node.rhs, ctx) as Rel
        return if (lhs !== node.lhs || rhs !== node.rhs) {
            Rel.Op.Except(lhs, rhs)
        } else {
            node
        }
    }

    override fun visitRelOpLimit(node: Rel.Op.Limit, ctx: C): PlanNode {
        val input = visitRel(node.input, ctx) as Rel
        val limit = visitRex(node.limit, ctx) as Rex
        return if (input !== node.input || limit !== node.limit) {
            Rel.Op.Limit(input, limit)
        } else {
            node
        }
    }

    override fun visitRelOpOffset(node: Rel.Op.Offset, ctx: C): PlanNode {
        val input = visitRel(node.input, ctx) as Rel
        val offset = visitRex(node.offset, ctx) as Rex
        return if (input !== node.input || offset !== node.offset) {
            Rel.Op.Offset(input, offset)
        } else {
            node
        }
    }

    override fun visitRelOpProject(node: Rel.Op.Project, ctx: C): PlanNode {
        val input = visitRel(node.input, ctx) as Rel
        val projections = _visitList(node.projections, ctx, ::visitRex)
        return if (input !== node.input || projections !== node.projections) {
            Rel.Op.Project(input, projections)
        } else {
            node
        }
    }

    override fun visitRelOpJoin(node: Rel.Op.Join, ctx: C): PlanNode {
        val lhs = visitRel(node.lhs, ctx) as Rel
        val rhs = visitRel(node.rhs, ctx) as Rel
        val rex = visitRex(node.rex, ctx) as Rex
        val type = node.type
        return if (lhs !== node.lhs || rhs !== node.rhs || rex !== node.rex || type !== node.type) {
            Rel.Op.Join(lhs, rhs, rex, type)
        } else {
            node
        }
    }

    override fun visitRelOpAggregate(node: Rel.Op.Aggregate, ctx: C): PlanNode {
        val input = visitRel(node.input, ctx) as Rel
        val strategy = node.strategy
        val calls = _visitList(node.calls, ctx, ::visitRelOpAggregateCall)
        val groups = _visitList(node.groups, ctx, ::visitRex)
        return if (input !== node.input || strategy !== node.strategy || calls !== node.calls || groups !== node.groups) {
            Rel.Op.Aggregate(input, strategy, calls, groups)
        } else {
            node
        }
    }

    override fun visitRelOpAggregateCall(node: Rel.Op.Aggregate.Call, ctx: C): PlanNode {
        val agg = visitAgg(node.agg, ctx) as Agg
        val args = _visitList(node.args, ctx, ::visitRex)
        return if (agg !== node.agg || args !== node.args) {
            Rel.Op.Aggregate.Call(agg, args)
        } else {
            node
        }
    }

    override fun visitRelOpExclude(node: Rel.Op.Exclude, ctx: C): PlanNode {
        val input = visitRel(node.input, ctx) as Rel
        val items = _visitList(node.items, ctx, ::visitRelOpExcludeItem)
        return if (input !== node.input || items !== node.items) {
            Rel.Op.Exclude(input, items)
        } else {
            node
        }
    }

    override fun visitRelOpExcludeItem(node: Rel.Op.Exclude.Item, ctx: C): PlanNode {
        val root = visitIdentifierSymbol(node.root, ctx) as Identifier.Symbol
        val steps = _visitList(node.steps, ctx, ::visitRelOpExcludeStep)
        return if (root !== node.root || steps !== node.steps) {
            Rel.Op.Exclude.Item(root, steps)
        } else {
            node
        }
    }

    override fun visitRelOpExcludeStepAttr(node: Rel.Op.Exclude.Step.Attr, ctx: C): PlanNode {
        val symbol = visitIdentifierSymbol(node.symbol, ctx) as Identifier.Symbol
        return if (symbol !== node.symbol) {
            Rel.Op.Exclude.Step.Attr(symbol)
        } else {
            node
        }
    }

    override fun visitRelOpExcludeStepPos(node: Rel.Op.Exclude.Step.Pos, ctx: C): PlanNode {
        val index = node.index
        return node
    }

    override fun visitRelOpExcludeStepStructWildcard(
        node: Rel.Op.Exclude.Step.StructWildcard,
        ctx: C,
    ): PlanNode = node

    override fun visitRelOpExcludeStepCollectionWildcard(
        node: Rel.Op.Exclude.Step.CollectionWildcard,
        ctx: C,
    ): PlanNode = node

    override fun visitRelOpErr(node: Rel.Op.Err, ctx: C): PlanNode {
        val message = node.message
        return node
    }

    override fun visitRelBinding(node: Rel.Binding, ctx: C): PlanNode {
        val name = node.name
        val type = node.type
        return node
    }
}
