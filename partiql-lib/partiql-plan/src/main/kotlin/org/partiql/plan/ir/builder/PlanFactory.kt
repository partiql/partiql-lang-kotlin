package org.partiql.plan.ir.builder

import com.amazon.ionelement.api.IonElement
import kotlin.Any
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.Set
import org.partiql.plan.ir.Binding
import org.partiql.plan.ir.Common
import org.partiql.plan.ir.Property
import org.partiql.plan.ir.Rel
import org.partiql.plan.ir.Rex
import org.partiql.plan.ir.SortSpec
import org.partiql.plan.ir.StructPart

public abstract class PlanFactory {
  public open fun common(
    schema: Map<String, Rel.Join.Type>,
    properties: Set<Property>,
    metas: Map<String, Any>
  ) = Common(schema, properties, metas)

  public open fun relScan(
    common: Common,
    rex: Rex,
    alias: String?,
    at: String?,
    `by`: String?
  ) = Rel.Scan(common, rex, alias, at, by)

  public open fun relFilter(
    common: Common,
    input: Rel,
    condition: Rex
  ) = Rel.Filter(common, input, condition)

  public open fun relSort(
    common: Common,
    input: Rel,
    specs: List<SortSpec>
  ) = Rel.Sort(common, input, specs)

  public open fun relBag(
    common: Common,
    lhs: Rel,
    rhs: Rel,
    op: Rel.Bag.Op
  ) = Rel.Bag(common, lhs, rhs, op)

  public open fun relFetch(
    common: Common,
    input: Rel,
    limit: Rex,
    offset: Rex
  ) = Rel.Fetch(common, input, limit, offset)

  public open fun relProject(
    common: Common,
    input: Rel,
    bindings: List<Binding>
  ) = Rel.Project(common, input, bindings)

  public open fun relJoin(
    common: Common,
    lhs: Rel,
    rhs: Rel,
    condition: Rex?,
    type: Rel.Join.Type
  ) = Rel.Join(common, lhs, rhs, condition, type)

  public open fun relAggregate(
    common: Common,
    input: Rel,
    calls: List<Binding>,
    groups: List<Binding>,
    strategy: Rel.Aggregate.Strategy
  ) = Rel.Aggregate(common, input, calls, groups, strategy)

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

  public open fun rexSubqueryTuple(rel: Rel) = Rex.Subquery.Tuple(rel)

  public open fun rexSubqueryScalar(rel: Rel) = Rex.Subquery.Scalar(rel)

  public open fun structPartFields(rex: Rex) = StructPart.Fields(rex)

  public open fun structPartField(name: Rex, rex: Rex) = StructPart.Field(name, rex)

  public open fun sortSpec(
    rex: Rex,
    dir: SortSpec.Dir,
    nulls: SortSpec.Nulls
  ) = SortSpec(rex, dir, nulls)

  public open fun binding(name: String, rex: Rex) = Binding(name, rex)

  public companion object {
    public val DEFAULT: PlanFactory = object : PlanFactory() {}
  }
}
