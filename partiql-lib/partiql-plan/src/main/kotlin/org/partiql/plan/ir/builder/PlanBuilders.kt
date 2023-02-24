package org.partiql.plan.ir.builder

import com.amazon.ionelement.api.IonElement
import kotlin.Any
import kotlin.String
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import org.partiql.plan.ir.Attribute
import org.partiql.plan.ir.Binding
import org.partiql.plan.ir.Case
import org.partiql.plan.ir.Common
import org.partiql.plan.ir.Field
import org.partiql.plan.ir.Plan
import org.partiql.plan.ir.Property
import org.partiql.plan.ir.Rel
import org.partiql.plan.ir.Rex
import org.partiql.plan.ir.SortSpec
import org.partiql.plan.ir.Step
import org.partiql.spi.types.StaticType

public class PlanBuilder {
  public var version: Plan.Version? = null

  public var root: Rex? = null

  public fun version(version: Plan.Version?): PlanBuilder = this.apply {
    this.version = version
  }

  public fun root(root: Rex?): PlanBuilder = this.apply {
    this.root = root
  }

  public fun build(): Plan = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Plan = factory.plan(version =
      version!!, root = root!!)
}

public class CommonBuilder {
  public var schema: MutableList<Attribute> = mutableListOf()

  public var properties: MutableSet<Property> = mutableSetOf()

  public var metas: MutableMap<String, Any> = mutableMapOf()

  public fun schema(schema: MutableList<Attribute>): CommonBuilder = this.apply {
    this.schema = schema
  }

  public fun properties(properties: MutableSet<Property>): CommonBuilder = this.apply {
    this.properties = properties
  }

  public fun metas(metas: MutableMap<String, Any>): CommonBuilder = this.apply {
    this.metas = metas
  }

  public fun build(): Common = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Common = factory.common(schema =
      schema, properties = properties, metas = metas)
}

public class AttributeBuilder {
  public var name: String? = null

  public var type: StaticType? = null

  public fun name(name: String?): AttributeBuilder = this.apply {
    this.name = name
  }

  public fun type(type: StaticType?): AttributeBuilder = this.apply {
    this.type = type
  }

  public fun build(): Attribute = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Attribute = factory.attribute(name =
      name!!, type = type!!)
}

public class BindingBuilder {
  public var name: String? = null

  public var `value`: Rex? = null

  public fun name(name: String?): BindingBuilder = this.apply {
    this.name = name
  }

  public fun `value`(`value`: Rex?): BindingBuilder = this.apply {
    this.`value` = `value`
  }

  public fun build(): Binding = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Binding = factory.binding(name =
      name!!, value = value!!)
}

public class FieldBuilder {
  public var name: Rex? = null

  public var `value`: Rex? = null

  public fun name(name: Rex?): FieldBuilder = this.apply {
    this.name = name
  }

  public fun `value`(`value`: Rex?): FieldBuilder = this.apply {
    this.`value` = `value`
  }

  public fun build(): Field = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Field = factory.field(name = name!!,
      value = value!!)
}

public class StepKeyBuilder {
  public var `value`: Rex? = null

  public var case: Case? = null

  public fun `value`(`value`: Rex?): StepKeyBuilder = this.apply {
    this.`value` = `value`
  }

  public fun case(case: Case?): StepKeyBuilder = this.apply {
    this.case = case
  }

  public fun build(): Step.Key = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Step.Key = factory.stepKey(value =
      value!!, case = case)
}

public class StepWildcardBuilder {
  public fun build(): Step.Wildcard = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Step.Wildcard =
      factory.stepWildcard()
}

public class StepUnpivotBuilder {
  public fun build(): Step.Unpivot = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Step.Unpivot = factory.stepUnpivot()
}

public class SortSpecBuilder {
  public var `value`: Rex? = null

  public var dir: SortSpec.Dir? = null

  public var nulls: SortSpec.Nulls? = null

  public fun `value`(`value`: Rex?): SortSpecBuilder = this.apply {
    this.`value` = `value`
  }

  public fun dir(dir: SortSpec.Dir?): SortSpecBuilder = this.apply {
    this.dir = dir
  }

  public fun nulls(nulls: SortSpec.Nulls?): SortSpecBuilder = this.apply {
    this.nulls = nulls
  }

  public fun build(): SortSpec = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): SortSpec = factory.sortSpec(value =
      value!!, dir = dir!!, nulls = nulls!!)
}

public class RelScanBuilder {
  public var common: Common? = null

  public var `value`: Rex? = null

  public var alias: String? = null

  public var at: String? = null

  public var `by`: String? = null

  public fun common(common: Common?): RelScanBuilder = this.apply {
    this.common = common
  }

  public fun `value`(`value`: Rex?): RelScanBuilder = this.apply {
    this.`value` = `value`
  }

  public fun alias(alias: String?): RelScanBuilder = this.apply {
    this.alias = alias
  }

  public fun at(at: String?): RelScanBuilder = this.apply {
    this.at = at
  }

  public fun `by`(`by`: String?): RelScanBuilder = this.apply {
    this.`by` = `by`
  }

  public fun build(): Rel.Scan = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rel.Scan = factory.relScan(common =
      common!!, value = value!!, alias = alias, at = at, by = by)
}

public class RelUnpivotBuilder {
  public var common: Common? = null

  public var `value`: Rex? = null

  public var alias: String? = null

  public var at: String? = null

  public var `by`: String? = null

  public fun common(common: Common?): RelUnpivotBuilder = this.apply {
    this.common = common
  }

  public fun `value`(`value`: Rex?): RelUnpivotBuilder = this.apply {
    this.`value` = `value`
  }

  public fun alias(alias: String?): RelUnpivotBuilder = this.apply {
    this.alias = alias
  }

  public fun at(at: String?): RelUnpivotBuilder = this.apply {
    this.at = at
  }

  public fun `by`(`by`: String?): RelUnpivotBuilder = this.apply {
    this.`by` = `by`
  }

  public fun build(): Rel.Unpivot = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rel.Unpivot =
      factory.relUnpivot(common = common!!, value = value!!, alias = alias, at = at, by = by)
}

public class RelFilterBuilder {
  public var common: Common? = null

  public var input: Rel? = null

  public var condition: Rex? = null

  public fun common(common: Common?): RelFilterBuilder = this.apply {
    this.common = common
  }

  public fun input(input: Rel?): RelFilterBuilder = this.apply {
    this.input = input
  }

  public fun condition(condition: Rex?): RelFilterBuilder = this.apply {
    this.condition = condition
  }

  public fun build(): Rel.Filter = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rel.Filter =
      factory.relFilter(common = common!!, input = input!!, condition = condition!!)
}

public class RelSortBuilder {
  public var common: Common? = null

  public var input: Rel? = null

  public var specs: MutableList<SortSpec> = mutableListOf()

  public fun common(common: Common?): RelSortBuilder = this.apply {
    this.common = common
  }

  public fun input(input: Rel?): RelSortBuilder = this.apply {
    this.input = input
  }

  public fun specs(specs: MutableList<SortSpec>): RelSortBuilder = this.apply {
    this.specs = specs
  }

  public fun build(): Rel.Sort = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rel.Sort = factory.relSort(common =
      common!!, input = input!!, specs = specs)
}

public class RelBagBuilder {
  public var common: Common? = null

  public var lhs: Rel? = null

  public var rhs: Rel? = null

  public var op: Rel.Bag.Op? = null

  public fun common(common: Common?): RelBagBuilder = this.apply {
    this.common = common
  }

  public fun lhs(lhs: Rel?): RelBagBuilder = this.apply {
    this.lhs = lhs
  }

  public fun rhs(rhs: Rel?): RelBagBuilder = this.apply {
    this.rhs = rhs
  }

  public fun op(op: Rel.Bag.Op?): RelBagBuilder = this.apply {
    this.op = op
  }

  public fun build(): Rel.Bag = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rel.Bag = factory.relBag(common =
      common!!, lhs = lhs!!, rhs = rhs!!, op = op!!)
}

public class RelFetchBuilder {
  public var common: Common? = null

  public var input: Rel? = null

  public var limit: Rex? = null

  public var offset: Rex? = null

  public fun common(common: Common?): RelFetchBuilder = this.apply {
    this.common = common
  }

  public fun input(input: Rel?): RelFetchBuilder = this.apply {
    this.input = input
  }

  public fun limit(limit: Rex?): RelFetchBuilder = this.apply {
    this.limit = limit
  }

  public fun offset(offset: Rex?): RelFetchBuilder = this.apply {
    this.offset = offset
  }

  public fun build(): Rel.Fetch = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rel.Fetch = factory.relFetch(common
      = common!!, input = input!!, limit = limit!!, offset = offset!!)
}

public class RelProjectBuilder {
  public var common: Common? = null

  public var input: Rel? = null

  public var bindings: MutableList<Binding> = mutableListOf()

  public fun common(common: Common?): RelProjectBuilder = this.apply {
    this.common = common
  }

  public fun input(input: Rel?): RelProjectBuilder = this.apply {
    this.input = input
  }

  public fun bindings(bindings: MutableList<Binding>): RelProjectBuilder = this.apply {
    this.bindings = bindings
  }

  public fun build(): Rel.Project = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rel.Project =
      factory.relProject(common = common!!, input = input!!, bindings = bindings)
}

public class RelJoinBuilder {
  public var common: Common? = null

  public var lhs: Rel? = null

  public var rhs: Rel? = null

  public var condition: Rex? = null

  public var type: Rel.Join.Type? = null

  public fun common(common: Common?): RelJoinBuilder = this.apply {
    this.common = common
  }

  public fun lhs(lhs: Rel?): RelJoinBuilder = this.apply {
    this.lhs = lhs
  }

  public fun rhs(rhs: Rel?): RelJoinBuilder = this.apply {
    this.rhs = rhs
  }

  public fun condition(condition: Rex?): RelJoinBuilder = this.apply {
    this.condition = condition
  }

  public fun type(type: Rel.Join.Type?): RelJoinBuilder = this.apply {
    this.type = type
  }

  public fun build(): Rel.Join = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rel.Join = factory.relJoin(common =
      common!!, lhs = lhs!!, rhs = rhs!!, condition = condition, type = type!!)
}

public class RelAggregateBuilder {
  public var common: Common? = null

  public var input: Rel? = null

  public var calls: MutableList<Binding> = mutableListOf()

  public var groups: MutableList<Binding> = mutableListOf()

  public var strategy: Rel.Aggregate.Strategy? = null

  public fun common(common: Common?): RelAggregateBuilder = this.apply {
    this.common = common
  }

  public fun input(input: Rel?): RelAggregateBuilder = this.apply {
    this.input = input
  }

  public fun calls(calls: MutableList<Binding>): RelAggregateBuilder = this.apply {
    this.calls = calls
  }

  public fun groups(groups: MutableList<Binding>): RelAggregateBuilder = this.apply {
    this.groups = groups
  }

  public fun strategy(strategy: Rel.Aggregate.Strategy?): RelAggregateBuilder = this.apply {
    this.strategy = strategy
  }

  public fun build(): Rel.Aggregate = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rel.Aggregate =
      factory.relAggregate(common = common!!, input = input!!, calls = calls, groups = groups,
      strategy = strategy!!)
}

public class RexIdBuilder {
  public var name: String? = null

  public var case: Case? = null

  public var qualifier: Rex.Id.Qualifier? = null

  public fun name(name: String?): RexIdBuilder = this.apply {
    this.name = name
  }

  public fun case(case: Case?): RexIdBuilder = this.apply {
    this.case = case
  }

  public fun qualifier(qualifier: Rex.Id.Qualifier?): RexIdBuilder = this.apply {
    this.qualifier = qualifier
  }

  public fun build(): Rex.Id = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rex.Id = factory.rexId(name =
      name!!, case = case, qualifier = qualifier!!)
}

public class RexPathBuilder {
  public var root: Rex? = null

  public var steps: MutableList<Step> = mutableListOf()

  public fun root(root: Rex?): RexPathBuilder = this.apply {
    this.root = root
  }

  public fun steps(steps: MutableList<Step>): RexPathBuilder = this.apply {
    this.steps = steps
  }

  public fun build(): Rex.Path = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rex.Path = factory.rexPath(root =
      root!!, steps = steps)
}

public class RexUnaryBuilder {
  public var `value`: Rex? = null

  public var op: Rex.Unary.Op? = null

  public fun `value`(`value`: Rex?): RexUnaryBuilder = this.apply {
    this.`value` = `value`
  }

  public fun op(op: Rex.Unary.Op?): RexUnaryBuilder = this.apply {
    this.op = op
  }

  public fun build(): Rex.Unary = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rex.Unary = factory.rexUnary(value =
      value!!, op = op!!)
}

public class RexBinaryBuilder {
  public var lhs: Rex? = null

  public var rhs: Rex? = null

  public var op: Rex.Binary.Op? = null

  public fun lhs(lhs: Rex?): RexBinaryBuilder = this.apply {
    this.lhs = lhs
  }

  public fun rhs(rhs: Rex?): RexBinaryBuilder = this.apply {
    this.rhs = rhs
  }

  public fun op(op: Rex.Binary.Op?): RexBinaryBuilder = this.apply {
    this.op = op
  }

  public fun build(): Rex.Binary = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rex.Binary = factory.rexBinary(lhs =
      lhs!!, rhs = rhs!!, op = op!!)
}

public class RexCallBuilder {
  public var id: String? = null

  public var args: MutableList<Rex> = mutableListOf()

  public fun id(id: String?): RexCallBuilder = this.apply {
    this.id = id
  }

  public fun args(args: MutableList<Rex>): RexCallBuilder = this.apply {
    this.args = args
  }

  public fun build(): Rex.Call = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rex.Call = factory.rexCall(id =
      id!!, args = args)
}

public class RexAggBuilder {
  public var id: String? = null

  public var args: MutableList<Rex> = mutableListOf()

  public var modifier: Rex.Agg.Modifier? = null

  public fun id(id: String?): RexAggBuilder = this.apply {
    this.id = id
  }

  public fun args(args: MutableList<Rex>): RexAggBuilder = this.apply {
    this.args = args
  }

  public fun modifier(modifier: Rex.Agg.Modifier?): RexAggBuilder = this.apply {
    this.modifier = modifier
  }

  public fun build(): Rex.Agg = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rex.Agg = factory.rexAgg(id = id!!,
      args = args, modifier = modifier!!)
}

public class RexLitBuilder {
  public var `value`: IonElement? = null

  public fun `value`(`value`: IonElement?): RexLitBuilder = this.apply {
    this.`value` = `value`
  }

  public fun build(): Rex.Lit = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rex.Lit = factory.rexLit(value =
      value!!)
}

public class RexCollectionBuilder {
  public var type: Rex.Collection.Type? = null

  public var values: MutableList<Rex> = mutableListOf()

  public fun type(type: Rex.Collection.Type?): RexCollectionBuilder = this.apply {
    this.type = type
  }

  public fun values(values: MutableList<Rex>): RexCollectionBuilder = this.apply {
    this.values = values
  }

  public fun build(): Rex.Collection = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rex.Collection =
      factory.rexCollection(type = type!!, values = values)
}

public class RexTupleBuilder {
  public var fields: MutableList<Field> = mutableListOf()

  public fun fields(fields: MutableList<Field>): RexTupleBuilder = this.apply {
    this.fields = fields
  }

  public fun build(): Rex.Tuple = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rex.Tuple = factory.rexTuple(fields
      = fields)
}

public class RexQueryScalarCoerceBuilder {
  public var query: Rex.Query.Collection? = null

  public fun query(query: Rex.Query.Collection?): RexQueryScalarCoerceBuilder = this.apply {
    this.query = query
  }

  public fun build(): Rex.Query.Scalar.Coerce = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rex.Query.Scalar.Coerce =
      factory.rexQueryScalarCoerce(query = query!!)
}

public class RexQueryScalarPivotBuilder {
  public var rel: Rel? = null

  public var `value`: Rex? = null

  public var at: Rex? = null

  public fun rel(rel: Rel?): RexQueryScalarPivotBuilder = this.apply {
    this.rel = rel
  }

  public fun `value`(`value`: Rex?): RexQueryScalarPivotBuilder = this.apply {
    this.`value` = `value`
  }

  public fun at(at: Rex?): RexQueryScalarPivotBuilder = this.apply {
    this.at = at
  }

  public fun build(): Rex.Query.Scalar.Pivot = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rex.Query.Scalar.Pivot =
      factory.rexQueryScalarPivot(rel = rel!!, value = value!!, at = at!!)
}

public class RexQueryCollectionBuilder {
  public var rel: Rel? = null

  public var `constructor`: Rex? = null

  public fun rel(rel: Rel?): RexQueryCollectionBuilder = this.apply {
    this.rel = rel
  }

  public fun `constructor`(`constructor`: Rex?): RexQueryCollectionBuilder = this.apply {
    this.`constructor` = `constructor`
  }

  public fun build(): Rex.Query.Collection = build(PlanFactory.DEFAULT)

  public fun build(factory: PlanFactory = PlanFactory.DEFAULT): Rex.Query.Collection =
      factory.rexQueryCollection(rel = rel!!, constructor = constructor)
}
