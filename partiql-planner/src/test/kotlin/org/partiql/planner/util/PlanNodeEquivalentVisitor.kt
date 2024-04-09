package org.partiql.planner.util

import org.partiql.plan.Catalog
import org.partiql.plan.Identifier
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.visitor.PlanBaseVisitor
import org.partiql.value.PartiQLValueExperimental

// Work around to assert plan equivalence,
// perhaps the easier way is to have an is equivalent method at code generation time
// but this is good enough for the purpose of testing at the moment.
class PlanNodeEquivalentVisitor : PlanBaseVisitor<Boolean, PlanNode>() {
    override fun visit(node: PlanNode, ctx: PlanNode): Boolean = node.accept(this, ctx)

    override fun visitCatalog(node: Catalog, ctx: PlanNode): Boolean {
        if (!super.visitCatalog(node, ctx)) return false
        ctx as Catalog
        if (node.name != ctx.name) return false
        return true
    }

    override fun visitIdentifierSymbol(node: Identifier.Symbol, ctx: PlanNode): Boolean {
        if (!super.visitIdentifierSymbol(node, ctx)) return false
        ctx as Identifier.Symbol
        if (node.symbol != ctx.symbol) return false
        if (node.caseSensitivity != ctx.caseSensitivity) return false
        return true
    }

    override fun visitRex(node: Rex, ctx: PlanNode): Boolean {
        if (!super.visitRex(node, ctx)) return false
        ctx as Rex
        if (node.type != ctx.type) return false
        return true
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun visitRexOpLit(node: Rex.Op.Lit, ctx: PlanNode): Boolean {
        if (!super.visitRexOpLit(node, ctx)) return false
        ctx as Rex.Op.Lit
        if (node.value != ctx.value) return false
        return true
    }

    override fun visitRexOpVar(node: Rex.Op.Var, ctx: PlanNode): Boolean {
        if (!super.visitRexOpVar(node, ctx)) return false
        ctx as Rex.Op.Var
        if (node.depth != ctx.depth) return false
        if (node.ref != ctx.ref) return false
        return true
    }

    override fun visitRexOpPathSymbol(node: Rex.Op.Path.Symbol, ctx: PlanNode): Boolean {
        if (!super.visitRexOpPathSymbol(node, ctx)) return false
        ctx as Rex.Op.Path.Symbol
        if (node.key != ctx.key) return false
        return true
    }

    override fun visitRexOpErr(node: Rex.Op.Err, ctx: PlanNode): Boolean {
        if (!super.visitRexOpErr(node, ctx)) return false
//        ctx as Rex.Op.Err
//        if (node.message != ctx.message) return false
        return true
    }

    override fun visitRelType(node: Rel.Type, ctx: PlanNode): Boolean {
        if (!super.visitRelType(node, ctx)) return false
        ctx as Rel.Type
        if (node.props != ctx.props) return false
        return true
    }

    override fun visitRelOpSortSpec(node: Rel.Op.Sort.Spec, ctx: PlanNode): Boolean {
        if (!super.visitRelOpSortSpec(node, ctx)) return false
        ctx as Rel.Op.Sort.Spec
        if (node.order != ctx.order) return false
        return true
    }

    override fun visitRelOpJoin(node: Rel.Op.Join, ctx: PlanNode): Boolean {
        if (!super.visitRelOpJoin(node, ctx)) return false
        ctx as Rel.Op.Join
        if (node.type != ctx.type) return false
        return true
    }

    override fun visitRelOpAggregate(node: Rel.Op.Aggregate, ctx: PlanNode): Boolean {
        if (!super.visitRelOpAggregate(node, ctx)) return false
        ctx as Rel.Op.Aggregate
        if (node.strategy != ctx.strategy) return false
        return true
    }

    override fun visitRelOpErr(node: Rel.Op.Err, ctx: PlanNode): Boolean {
        if (!super.visitRelOpErr(node, ctx)) return false
//        ctx as Rel.Op.Err
//        if (node.message != ctx.message) return false
        return true
    }

    override fun visitRelBinding(node: Rel.Binding, ctx: PlanNode): Boolean {
        if (!super.visitRelBinding(node, ctx)) return false
        ctx as Rel.Binding
        if (node.name != ctx.name) return false
        if (node.type != ctx.type) return false
        return true
    }

    override fun defaultVisit(node: PlanNode, ctx: PlanNode): Boolean {
        if (ctx.javaClass != node.javaClass) return false
        if (node.children.size != ctx.children.size) return false
        node.children.forEachIndexed { index, planNode ->
            if (!planNode.accept(this, ctx.children[index])) return false
        }
        return true
    }

    override fun defaultReturn(node: PlanNode, ctx: PlanNode): Boolean {
        return false
    }
}
