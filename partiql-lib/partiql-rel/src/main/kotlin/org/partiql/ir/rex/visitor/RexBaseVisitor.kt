package org.partiql.ir.rex.visitor

import org.partiql.ir.rex.Rex
import org.partiql.ir.rex.RexNode

public abstract class RexBaseVisitor<R, C> : RexVisitor<R, C> {
  public override fun visit(node: RexNode, ctx: C): R = node.accept(this, ctx)

  public override fun visitRex(node: Rex, ctx: C): R = when (node) {
    is Rex.Id -> visitRexId(node, ctx)
    is Rex.Path -> visitRexPath(node, ctx)
    is Rex.Unary -> visitRexUnary(node, ctx)
    is Rex.Binary -> visitRexBinary(node, ctx)
    is Rex.Call -> visitRexCall(node, ctx)
    is Rex.Agg -> visitRexAgg(node, ctx)
    is Rex.Lit -> visitRexLit(node, ctx)
  }

  public override fun visitRexId(node: Rex.Id, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexPath(node: Rex.Path, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexUnary(node: Rex.Unary, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexBinary(node: Rex.Binary, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexCall(node: Rex.Call, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexAgg(node: Rex.Agg, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexLit(node: Rex.Lit, ctx: C): R = when (node) {
    is Rex.Lit.Collection -> visitRexLitCollection(node, ctx)
    is Rex.Lit.Scalar -> visitRexLitScalar(node, ctx)
  }

  public override fun visitRexLitCollection(node: Rex.Lit.Collection, ctx: C): R =
      defaultVisit(node, ctx)

  public override fun visitRexLitScalar(node: Rex.Lit.Scalar, ctx: C): R = defaultVisit(node, ctx)

  public open fun defaultVisit(node: RexNode, ctx: C): R {
    for (child in node.children) {
      child.accept(this, ctx)
    }
    return defaultReturn(node, ctx)
  }

  public abstract fun defaultReturn(node: RexNode, ctx: C): R
}
