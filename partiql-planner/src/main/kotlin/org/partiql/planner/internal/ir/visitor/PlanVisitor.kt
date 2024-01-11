@file:OptIn(PartiQLValueExperimental::class)

package org.partiql.planner.internal.ir.visitor

import org.partiql.planner.internal.ir.Agg
import org.partiql.planner.internal.ir.Catalog
import org.partiql.planner.internal.ir.Fn
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.PartiQLPlan
import org.partiql.planner.internal.ir.PlanNode
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.Statement
import org.partiql.value.PartiQLValueExperimental

internal interface PlanVisitor<R, C> {
    fun visit(node: PlanNode, ctx: C): R

    fun visitPartiQLPlan(node: PartiQLPlan, ctx: C): R

    fun visitCatalog(node: Catalog, ctx: C): R

    fun visitCatalogSymbol(node: Catalog.Symbol, ctx: C): R

    fun visitCatalogSymbolRef(node: Catalog.Symbol.Ref, ctx: C): R

    fun visitFn(node: Fn, ctx: C): R

    fun visitFnResolved(node: Fn.Resolved, ctx: C): R

    fun visitFnUnresolved(node: Fn.Unresolved, ctx: C): R

    fun visitAgg(node: Agg, ctx: C): R

    fun visitAggResolved(node: Agg.Resolved, ctx: C): R

    fun visitAggUnresolved(node: Agg.Unresolved, ctx: C): R

    fun visitStatement(node: Statement, ctx: C): R

    fun visitStatementQuery(node: Statement.Query, ctx: C): R

    fun visitIdentifier(node: Identifier, ctx: C): R

    fun visitIdentifierSymbol(node: Identifier.Symbol, ctx: C): R

    fun visitIdentifierQualified(node: Identifier.Qualified, ctx: C): R

    fun visitRex(node: Rex, ctx: C): R

    fun visitRexOp(node: Rex.Op, ctx: C): R

    fun visitRexOpLit(node: Rex.Op.Lit, ctx: C): R

    fun visitRexOpVar(node: Rex.Op.Var, ctx: C): R

    fun visitRexOpVarResolved(node: Rex.Op.Var.Resolved, ctx: C): R

    fun visitRexOpVarUnresolved(node: Rex.Op.Var.Unresolved, ctx: C): R

    fun visitRexOpGlobal(node: Rex.Op.Global, ctx: C): R

    fun visitRexOpPath(node: Rex.Op.Path, ctx: C): R

    fun visitRexOpPathIndex(node: Rex.Op.Path.Index, ctx: C): R

    fun visitRexOpPathKey(node: Rex.Op.Path.Key, ctx: C): R

    fun visitRexOpPathSymbol(node: Rex.Op.Path.Symbol, ctx: C): R

    fun visitRexOpCall(node: Rex.Op.Call, ctx: C): R

    fun visitRexOpCallStatic(node: Rex.Op.Call.Static, ctx: C): R

    fun visitRexOpCallDynamic(node: Rex.Op.Call.Dynamic, ctx: C): R

    fun visitRexOpCallDynamicCandidate(node: Rex.Op.Call.Dynamic.Candidate, ctx: C): R

    fun visitRexOpCase(node: Rex.Op.Case, ctx: C): R

    fun visitRexOpCaseBranch(node: Rex.Op.Case.Branch, ctx: C): R

    fun visitRexOpCollection(node: Rex.Op.Collection, ctx: C): R

    fun visitRexOpStruct(node: Rex.Op.Struct, ctx: C): R

    fun visitRexOpStructField(node: Rex.Op.Struct.Field, ctx: C): R

    fun visitRexOpPivot(node: Rex.Op.Pivot, ctx: C): R

    fun visitRexOpSubquery(node: Rex.Op.Subquery, ctx: C): R

    fun visitRexOpSelect(node: Rex.Op.Select, ctx: C): R

    fun visitRexOpTupleUnion(node: Rex.Op.TupleUnion, ctx: C): R

    fun visitRexOpErr(node: Rex.Op.Err, ctx: C): R

    fun visitRel(node: Rel, ctx: C): R

    fun visitRelType(node: Rel.Type, ctx: C): R

    fun visitRelOp(node: Rel.Op, ctx: C): R

    fun visitRelOpScan(node: Rel.Op.Scan, ctx: C): R

    fun visitRelOpScanIndexed(node: Rel.Op.ScanIndexed, ctx: C): R

    fun visitRelOpUnpivot(node: Rel.Op.Unpivot, ctx: C): R

    fun visitRelOpDistinct(node: Rel.Op.Distinct, ctx: C): R

    fun visitRelOpFilter(node: Rel.Op.Filter, ctx: C): R

    fun visitRelOpSort(node: Rel.Op.Sort, ctx: C): R

    fun visitRelOpSortSpec(node: Rel.Op.Sort.Spec, ctx: C): R

    fun visitRelOpUnion(node: Rel.Op.Union, ctx: C): R

    fun visitRelOpIntersect(node: Rel.Op.Intersect, ctx: C): R

    fun visitRelOpExcept(node: Rel.Op.Except, ctx: C): R

    fun visitRelOpLimit(node: Rel.Op.Limit, ctx: C): R

    fun visitRelOpOffset(node: Rel.Op.Offset, ctx: C): R

    fun visitRelOpProject(node: Rel.Op.Project, ctx: C): R

    fun visitRelOpJoin(node: Rel.Op.Join, ctx: C): R

    fun visitRelOpAggregate(node: Rel.Op.Aggregate, ctx: C): R

    fun visitRelOpAggregateCall(node: Rel.Op.Aggregate.Call, ctx: C): R

    fun visitRelOpExclude(node: Rel.Op.Exclude, ctx: C): R

    fun visitRelOpExcludePath(node: Rel.Op.Exclude.Path, ctx: C): R

    fun visitRelOpExcludeStep(node: Rel.Op.Exclude.Step, ctx: C): R

    fun visitRelOpExcludeType(node: Rel.Op.Exclude.Type, ctx: C): R

    fun visitRelOpExcludeTypeStructSymbol(node: Rel.Op.Exclude.Type.StructSymbol, ctx: C): R

    fun visitRelOpExcludeTypeStructKey(node: Rel.Op.Exclude.Type.StructKey, ctx: C): R

    fun visitRelOpExcludeTypeCollIndex(node: Rel.Op.Exclude.Type.CollIndex, ctx: C): R

    fun visitRelOpExcludeTypeStructWildcard(node: Rel.Op.Exclude.Type.StructWildcard, ctx: C):
        R

    fun visitRelOpExcludeTypeCollWildcard(node: Rel.Op.Exclude.Type.CollWildcard, ctx: C): R

    fun visitRelOpErr(node: Rel.Op.Err, ctx: C): R

    fun visitRelBinding(node: Rel.Binding, ctx: C): R
}
