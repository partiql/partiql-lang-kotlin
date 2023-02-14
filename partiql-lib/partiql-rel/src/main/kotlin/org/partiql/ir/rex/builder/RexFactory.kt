package org.partiql.ir.rex.builder

import com.amazon.ionelement.api.AnyElement
import kotlin.String
import kotlin.collections.List
import org.partiql.ir.rex.Rex

public abstract class RexFactory {
  public open fun rexId(name: String) = Rex.Id(name)

  public open fun rexPath(root: Rex) = Rex.Path(root)

  public open fun rexUnary(rex: Rex, op: Rex.Unary.Op) = Rex.Unary(rex, op)

  public open fun rexBinary(
    lhs: Rex,
    rhs: Rex,
    op: Rex.Binary.Op
  ) = Rex.Binary(lhs, rhs, op)

  public open fun rexCall(id: String, args: List<Rex>) = Rex.Call(id, args)

  public open fun rexAgg(
    id: String,
    args: List<Rex>,
    modifier: Rex.Agg.Modifier?
  ) = Rex.Agg(id, args, modifier)

  public open fun rexLitCollection(type: Rex.Lit.Collection.Type, values: List<Rex>) =
      Rex.Lit.Collection(type, values)

  public open fun rexLitScalar(`value`: AnyElement) = Rex.Lit.Scalar(value)

  public companion object {
    public val DEFAULT: RexFactory = object : RexFactory() {}
  }
}
