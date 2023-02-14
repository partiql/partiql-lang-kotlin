package org.partiql.ir.rex.visitor

import org.partiql.ir.rex.Rex
import org.partiql.ir.rex.RexNode
import org.partiql.ir.rex.StructPart

public interface RexVisitor<R, C> {
  public fun visit(node: RexNode, ctx: C): R

  public fun visitRex(node: Rex, ctx: C): R

  public fun visitRexId(node: Rex.Id, ctx: C): R

  public fun visitRexPath(node: Rex.Path, ctx: C): R

  public fun visitRexUnary(node: Rex.Unary, ctx: C): R

  public fun visitRexBinary(node: Rex.Binary, ctx: C): R

  public fun visitRexCall(node: Rex.Call, ctx: C): R

  public fun visitRexAgg(node: Rex.Agg, ctx: C): R

  public fun visitRexLit(node: Rex.Lit, ctx: C): R

  public fun visitRexCollection(node: Rex.Collection, ctx: C): R

  public fun visitRexStruct(node: Rex.Struct, ctx: C): R

  public fun visitStructPart(node: StructPart, ctx: C): R

  public fun visitStructPartFields(node: StructPart.Fields, ctx: C): R

  public fun visitStructPartField(node: StructPart.Field, ctx: C): R
}
