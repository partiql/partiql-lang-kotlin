package org.partiql.plan.ir.builder

import com.amazon.ionelement.api.IonElement
import kotlin.Any
import kotlin.String
import kotlin.Unit
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import org.partiql.plan.ir.Attribute
import org.partiql.plan.ir.Binding
import org.partiql.plan.ir.Case
import org.partiql.plan.ir.Common
import org.partiql.plan.ir.Field
import org.partiql.plan.ir.PartiQLPlan
import org.partiql.plan.ir.PlanNode
import org.partiql.plan.ir.Property
import org.partiql.plan.ir.Rel
import org.partiql.plan.ir.Rex
import org.partiql.plan.ir.SortSpec
import org.partiql.plan.ir.Step
import org.partiql.spi.types.StaticType

public fun <T : PlanNode> plan(factory: PlanFactory = PlanFactory.DEFAULT, block: PlanBuilder.() ->
    T) = PlanBuilder(factory).block()

public class PlanBuilder(
  private val factory: PlanFactory = PlanFactory.DEFAULT
) {
  public fun partiQLPlan(
    version: PartiQLPlan.Version? = null,
    root: Rex? = null,
    block: PartiQlPlanBuilder.() -> Unit = {}
  ): PartiQLPlan {
    val builder = PartiQlPlanBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun common(
    schema: MutableList<Attribute> = mutableListOf(),
    properties: MutableSet<Property> = mutableSetOf(),
    metas: MutableMap<String, Any> = mutableMapOf(),
    block: CommonBuilder.() -> Unit = {}
  ): Common {
    val builder = CommonBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun attribute(
    name: String? = null,
    type: StaticType? = null,
    block: AttributeBuilder.() -> Unit = {}
  ): Attribute {
    val builder = AttributeBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun binding(
    name: String? = null,
    `value`: Rex? = null,
    block: BindingBuilder.() -> Unit = {}
  ): Binding {
    val builder = BindingBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun `field`(
    name: Rex? = null,
    `value`: Rex? = null,
    block: FieldBuilder.() -> Unit = {}
  ): Field {
    val builder = FieldBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun stepKey(
    `value`: Rex? = null,
    case: Case? = null,
    block: StepKeyBuilder.() -> Unit = {}
  ): Step.Key {
    val builder = StepKeyBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun stepWildcard(block: StepWildcardBuilder.() -> Unit = {}): Step.Wildcard {
    val builder = StepWildcardBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun stepUnpivot(block: StepUnpivotBuilder.() -> Unit = {}): Step.Unpivot {
    val builder = StepUnpivotBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun sortSpec(
    `value`: Rex? = null,
    dir: SortSpec.Dir? = null,
    nulls: SortSpec.Nulls? = null,
    block: SortSpecBuilder.() -> Unit = {}
  ): SortSpec {
    val builder = SortSpecBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun relScan(
    common: Common? = null,
    `value`: Rex? = null,
    alias: String? = null,
    at: String? = null,
    `by`: String? = null,
    block: RelScanBuilder.() -> Unit = {}
  ): Rel.Scan {
    val builder = RelScanBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun relUnpivot(
    common: Common? = null,
    `value`: Rex? = null,
    alias: String? = null,
    at: String? = null,
    `by`: String? = null,
    block: RelUnpivotBuilder.() -> Unit = {}
  ): Rel.Unpivot {
    val builder = RelUnpivotBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun relFilter(
    common: Common? = null,
    input: Rel? = null,
    condition: Rex? = null,
    block: RelFilterBuilder.() -> Unit = {}
  ): Rel.Filter {
    val builder = RelFilterBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun relSort(
    common: Common? = null,
    input: Rel? = null,
    specs: MutableList<SortSpec> = mutableListOf(),
    block: RelSortBuilder.() -> Unit = {}
  ): Rel.Sort {
    val builder = RelSortBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun relBag(
    common: Common? = null,
    lhs: Rel? = null,
    rhs: Rel? = null,
    op: Rel.Bag.Op? = null,
    block: RelBagBuilder.() -> Unit = {}
  ): Rel.Bag {
    val builder = RelBagBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun relFetch(
    common: Common? = null,
    input: Rel? = null,
    limit: Rex? = null,
    offset: Rex? = null,
    block: RelFetchBuilder.() -> Unit = {}
  ): Rel.Fetch {
    val builder = RelFetchBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun relProject(
    common: Common? = null,
    input: Rel? = null,
    bindings: MutableList<Binding> = mutableListOf(),
    block: RelProjectBuilder.() -> Unit = {}
  ): Rel.Project {
    val builder = RelProjectBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun relJoin(
    common: Common? = null,
    lhs: Rel? = null,
    rhs: Rel? = null,
    condition: Rex? = null,
    type: Rel.Join.Type? = null,
    block: RelJoinBuilder.() -> Unit = {}
  ): Rel.Join {
    val builder = RelJoinBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun relAggregate(
    common: Common? = null,
    input: Rel? = null,
    calls: MutableList<Binding> = mutableListOf(),
    groups: MutableList<Binding> = mutableListOf(),
    strategy: Rel.Aggregate.Strategy? = null,
    block: RelAggregateBuilder.() -> Unit = {}
  ): Rel.Aggregate {
    val builder = RelAggregateBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun rexId(
    name: String? = null,
    case: Case? = null,
    qualifier: Rex.Id.Qualifier? = null,
    block: RexIdBuilder.() -> Unit = {}
  ): Rex.Id {
    val builder = RexIdBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun rexPath(
    root: Rex? = null,
    steps: MutableList<Step> = mutableListOf(),
    block: RexPathBuilder.() -> Unit = {}
  ): Rex.Path {
    val builder = RexPathBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun rexUnary(
    `value`: Rex? = null,
    op: Rex.Unary.Op? = null,
    block: RexUnaryBuilder.() -> Unit = {}
  ): Rex.Unary {
    val builder = RexUnaryBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun rexBinary(
    lhs: Rex? = null,
    rhs: Rex? = null,
    op: Rex.Binary.Op? = null,
    block: RexBinaryBuilder.() -> Unit = {}
  ): Rex.Binary {
    val builder = RexBinaryBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun rexCall(
    id: String? = null,
    args: MutableList<Rex> = mutableListOf(),
    block: RexCallBuilder.() -> Unit = {}
  ): Rex.Call {
    val builder = RexCallBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun rexAgg(
    id: String? = null,
    args: MutableList<Rex> = mutableListOf(),
    modifier: Rex.Agg.Modifier? = null,
    block: RexAggBuilder.() -> Unit = {}
  ): Rex.Agg {
    val builder = RexAggBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun rexLit(`value`: IonElement? = null, block: RexLitBuilder.() -> Unit = {}): Rex.Lit {
    val builder = RexLitBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun rexCollection(
    type: Rex.Collection.Type? = null,
    values: MutableList<Rex> = mutableListOf(),
    block: RexCollectionBuilder.() -> Unit = {}
  ): Rex.Collection {
    val builder = RexCollectionBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun rexTuple(fields: MutableList<Field> = mutableListOf(), block: RexTupleBuilder.() ->
      Unit = {}): Rex.Tuple {
    val builder = RexTupleBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun rexQueryScalarCoerce(query: Rex.Query.Collection? = null,
      block: RexQueryScalarCoerceBuilder.() -> Unit = {}): Rex.Query.Scalar.Coerce {
    val builder = RexQueryScalarCoerceBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun rexQueryScalarPivot(
    rel: Rel? = null,
    `value`: Rex? = null,
    at: Rex? = null,
    block: RexQueryScalarPivotBuilder.() -> Unit = {}
  ): Rex.Query.Scalar.Pivot {
    val builder = RexQueryScalarPivotBuilder()
    builder.block()
    return builder.build(factory)
  }

  public fun rexQueryCollection(
    rel: Rel? = null,
    `constructor`: Rex? = null,
    block: RexQueryCollectionBuilder.() -> Unit = {}
  ): Rex.Query.Collection {
    val builder = RexQueryCollectionBuilder()
    builder.block()
    return builder.build(factory)
  }
}
