package org.partiql.ir.rex.visitor

import org.partiql.ir.rex.Rex
import org.partiql.ir.rex.RexNode

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

  public fun visitRexLitCollection(node: Rex.Lit.Collection, ctx: C): R

  public fun visitRexLitScalar(node: Rex.Lit.Scalar, ctx: C): R
}
