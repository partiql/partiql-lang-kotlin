package org.partiql.ir.rel.builder

import kotlin.Any
import kotlin.Long
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.Set
import org.partiql.ir.rel.Binding
import org.partiql.ir.rel.Common
import org.partiql.ir.rel.Property
import org.partiql.ir.rel.Rel
import org.partiql.ir.rex.RexNode

public abstract class RelFactory {
  public open fun common(
    schema: Map<String, Rel.Join.Type>,
    properties: Set<Property>,
    metas: Map<String, Any>
  ) = Common(schema, properties, metas)

  public open fun relScan(
    common: Common,
    `value`: RexNode,
    alias: String?,
    at: String?,
    `by`: String?
  ) = Rel.Scan(common, value, alias, at, by)

  public open fun relCross(
    common: Common,
    lhs: Rel,
    rhs: Rel
  ) = Rel.Cross(common, lhs, rhs)

  public open fun relFilter(
    common: Common,
    input: Rel,
    condition: RexNode
  ) = Rel.Filter(common, input, condition)

  public open fun relSort(
    common: Common,
    rex: RexNode,
    dir: Rel.Sort.Dir,
    nulls: Rel.Sort.Nulls
  ) = Rel.Sort(common, rex, dir, nulls)

  public open fun relBag(
    common: Common,
    lhs: Rel,
    rhs: Rel,
    op: Rel.Bag.Op
  ) = Rel.Bag(common, lhs, rhs, op)

  public open fun relFetch(
    common: Common,
    input: Rel,
    limit: Long,
    offset: Long
  ) = Rel.Fetch(common, input, limit, offset)

  public open fun relProject(
    common: Common,
    input: Rel,
    rexs: List<Binding>
  ) = Rel.Project(common, input, rexs)

  public open fun relJoin(
    common: Common,
    lhs: Rel,
    rhs: Rel,
    condition: RexNode?,
    type: Rel.Join.Type
  ) = Rel.Join(common, lhs, rhs, condition, type)

  public open fun relAggregate(
    common: Common,
    input: Rel,
    calls: List<Binding>,
    groups: List<RexNode>
  ) = Rel.Aggregate(common, input, calls, groups)

  public open fun binding(name: String, `value`: RexNode) = Binding(name, value)

  public companion object {
    public val DEFAULT: RelFactory = object : RelFactory() {}
  }
}
