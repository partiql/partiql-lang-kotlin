package org.partiql.ir.rex.visitor

import org.partiql.ir.rex.Rex
import org.partiql.ir.rex.RexNode
import org.partiql.ir.rex.StructPart

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
    is Rex.Collection -> visitRexCollection(node, ctx)
    is Rex.Struct -> visitRexStruct(node, ctx)
  }

  public override fun visitRexId(node: Rex.Id, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexPath(node: Rex.Path, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexUnary(node: Rex.Unary, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexBinary(node: Rex.Binary, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexCall(node: Rex.Call, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexAgg(node: Rex.Agg, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexLit(node: Rex.Lit, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexCollection(node: Rex.Collection, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitRexStruct(node: Rex.Struct, ctx: C): R = defaultVisit(node, ctx)

  public override fun visitStructPart(node: StructPart, ctx: C): R = when (node) {
    is StructPart.Fields -> visitStructPartFields(node, ctx)
    is StructPart.Field -> visitStructPartField(node, ctx)
  }

  public override fun visitStructPartFields(node: StructPart.Fields, ctx: C): R = defaultVisit(node,
      ctx)

  public override fun visitStructPartField(node: StructPart.Field, ctx: C): R = defaultVisit(node,
      ctx)

  public open fun defaultVisit(node: RexNode, ctx: C): R {
    for (child in node.children) {
      child.accept(this, ctx)
    }
    return defaultReturn(node, ctx)
  }

  public abstract fun defaultReturn(node: RexNode, ctx: C): R
}
