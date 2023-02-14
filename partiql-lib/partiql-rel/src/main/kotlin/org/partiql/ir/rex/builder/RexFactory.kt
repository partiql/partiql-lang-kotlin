package org.partiql.ir.rex.builder

import com.amazon.ionelement.api.IonElement
import kotlin.String
import kotlin.collections.List
import org.partiql.ir.rex.Rex
import org.partiql.ir.rex.StructPart

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
    modifier: Rex.Agg.Modifier
  ) = Rex.Agg(id, args, modifier)

  public open fun rexLit(`value`: IonElement) = Rex.Lit(value)

  public open fun rexCollection(type: Rex.Collection.Type, values: List<Rex>) = Rex.Collection(type,
      values)

  public open fun rexStruct(fields: List<StructPart>) = Rex.Struct(fields)

  public open fun structPartFields(rex: Rex) = StructPart.Fields(rex)

  public open fun structPartField(name: Rex, rex: Rex) = StructPart.Field(name, rex)

  public companion object {
    public val DEFAULT: RexFactory = object : RexFactory() {}
  }
}
