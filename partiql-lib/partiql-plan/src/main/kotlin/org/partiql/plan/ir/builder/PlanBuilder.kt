package org.partiql.plan.ir.builder

import com.amazon.ionelement.api.IonElement
import kotlin.Any
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.jvm.JvmStatic
import org.partiql.plan.ir.Binding
import org.partiql.plan.ir.Common
import org.partiql.plan.ir.PlanNode
import org.partiql.plan.ir.Property
import org.partiql.plan.ir.Rel
import org.partiql.plan.ir.Rex
import org.partiql.plan.ir.SortSpec
import org.partiql.plan.ir.StructPart

/**
 * The Builder is inside this private final class for DSL aesthetics
 */
public class Plan private constructor() {
  @Suppress("ClassName")
  public class Builder(
    private val factory: PlanFactory
  ) {
    public fun common(
      schema: MutableMap<String, Rel.Join.Type> = mutableMapOf(),
      properties: MutableSet<Property> = mutableSetOf(),
      metas: MutableMap<String, Any> = mutableMapOf(),
      block: _Common.() -> Unit = {}
    ): Common {
      val b = _Common(schema, properties, metas)
      b.block()
      return factory.common(schema = b.schema, properties = b.properties, metas = b.metas)
    }

    public fun relScan(
      common: Common? = null,
      rex: Rex? = null,
      alias: String? = null,
      at: String? = null,
      `by`: String? = null,
      block: _RelScan.() -> Unit = {}
    ): Rel.Scan {
      val b = _RelScan(common, rex, alias, at, by)
      b.block()
      return factory.relScan(common = b.common!!, rex = b.rex!!, alias = b.alias, at = b.at, by =
          b.by)
    }

    public fun relFilter(
      common: Common? = null,
      input: Rel? = null,
      condition: Rex? = null,
      block: _RelFilter.() -> Unit = {}
    ): Rel.Filter {
      val b = _RelFilter(common, input, condition)
      b.block()
      return factory.relFilter(common = b.common!!, input = b.input!!, condition = b.condition!!)
    }

    public fun relSort(
      common: Common? = null,
      input: Rel? = null,
      specs: MutableList<SortSpec> = mutableListOf(),
      block: _RelSort.() -> Unit = {}
    ): Rel.Sort {
      val b = _RelSort(common, input, specs)
      b.block()
      return factory.relSort(common = b.common!!, input = b.input!!, specs = b.specs)
    }

    public fun relBag(
      common: Common? = null,
      lhs: Rel? = null,
      rhs: Rel? = null,
      op: Rel.Bag.Op? = null,
      block: _RelBag.() -> Unit = {}
    ): Rel.Bag {
      val b = _RelBag(common, lhs, rhs, op)
      b.block()
      return factory.relBag(common = b.common!!, lhs = b.lhs!!, rhs = b.rhs!!, op = b.op!!)
    }

    public fun relFetch(
      common: Common? = null,
      input: Rel? = null,
      limit: Rex? = null,
      offset: Rex? = null,
      block: _RelFetch.() -> Unit = {}
    ): Rel.Fetch {
      val b = _RelFetch(common, input, limit, offset)
      b.block()
      return factory.relFetch(common = b.common!!, input = b.input!!, limit = b.limit!!, offset =
          b.offset!!)
    }

    public fun relProject(
      common: Common? = null,
      input: Rel? = null,
      rexs: MutableList<Binding> = mutableListOf(),
      block: _RelProject.() -> Unit = {}
    ): Rel.Project {
      val b = _RelProject(common, input, rexs)
      b.block()
      return factory.relProject(common = b.common!!, input = b.input!!, rexs = b.rexs)
    }

    public fun relJoin(
      common: Common? = null,
      lhs: Rel? = null,
      rhs: Rel? = null,
      condition: Rex? = null,
      type: Rel.Join.Type? = null,
      block: _RelJoin.() -> Unit = {}
    ): Rel.Join {
      val b = _RelJoin(common, lhs, rhs, condition, type)
      b.block()
      return factory.relJoin(common = b.common!!, lhs = b.lhs!!, rhs = b.rhs!!, condition =
          b.condition, type = b.type!!)
    }

    public fun relAggregate(
      common: Common? = null,
      input: Rel? = null,
      calls: MutableList<Binding> = mutableListOf(),
      groups: MutableList<Binding> = mutableListOf(),
      strategy: Rel.Aggregate.Strategy? = null,
      block: _RelAggregate.() -> Unit = {}
    ): Rel.Aggregate {
      val b = _RelAggregate(common, input, calls, groups, strategy)
      b.block()
      return factory.relAggregate(common = b.common!!, input = b.input!!, calls = b.calls, groups =
          b.groups, strategy = b.strategy!!)
    }

    public fun rexId(name: String? = null, block: _RexId.() -> Unit = {}): Rex.Id {
      val b = _RexId(name)
      b.block()
      return factory.rexId(name = b.name!!)
    }

    public fun rexPath(root: Rex? = null, block: _RexPath.() -> Unit = {}): Rex.Path {
      val b = _RexPath(root)
      b.block()
      return factory.rexPath(root = b.root!!)
    }

    public fun rexUnary(
      rex: Rex? = null,
      op: Rex.Unary.Op? = null,
      block: _RexUnary.() -> Unit = {}
    ): Rex.Unary {
      val b = _RexUnary(rex, op)
      b.block()
      return factory.rexUnary(rex = b.rex!!, op = b.op!!)
    }

    public fun rexBinary(
      lhs: Rex? = null,
      rhs: Rex? = null,
      op: Rex.Binary.Op? = null,
      block: _RexBinary.() -> Unit = {}
    ): Rex.Binary {
      val b = _RexBinary(lhs, rhs, op)
      b.block()
      return factory.rexBinary(lhs = b.lhs!!, rhs = b.rhs!!, op = b.op!!)
    }

    public fun rexCall(
      id: String? = null,
      args: MutableList<Rex> = mutableListOf(),
      block: _RexCall.() -> Unit = {}
    ): Rex.Call {
      val b = _RexCall(id, args)
      b.block()
      return factory.rexCall(id = b.id!!, args = b.args)
    }

    public fun rexAgg(
      id: String? = null,
      args: MutableList<Rex> = mutableListOf(),
      modifier: Rex.Agg.Modifier? = null,
      block: _RexAgg.() -> Unit = {}
    ): Rex.Agg {
      val b = _RexAgg(id, args, modifier)
      b.block()
      return factory.rexAgg(id = b.id!!, args = b.args, modifier = b.modifier!!)
    }

    public fun rexLit(`value`: IonElement? = null, block: _RexLit.() -> Unit = {}): Rex.Lit {
      val b = _RexLit(value)
      b.block()
      return factory.rexLit(value = b.value!!)
    }

    public fun rexCollection(
      type: Rex.Collection.Type? = null,
      values: MutableList<Rex> = mutableListOf(),
      block: _RexCollection.() -> Unit = {}
    ): Rex.Collection {
      val b = _RexCollection(type, values)
      b.block()
      return factory.rexCollection(type = b.type!!, values = b.values)
    }

    public fun rexStruct(fields: MutableList<StructPart> = mutableListOf(), block: _RexStruct.() ->
        Unit = {}): Rex.Struct {
      val b = _RexStruct(fields)
      b.block()
      return factory.rexStruct(fields = b.fields)
    }

    public fun rexSubqueryTuple(rel: Rel? = null, block: _RexSubqueryTuple.() -> Unit = {}):
        Rex.Subquery.Tuple {
      val b = _RexSubqueryTuple(rel)
      b.block()
      return factory.rexSubqueryTuple(rel = b.rel!!)
    }

    public fun rexSubqueryScalar(rel: Rel? = null, block: _RexSubqueryScalar.() -> Unit = {}):
        Rex.Subquery.Scalar {
      val b = _RexSubqueryScalar(rel)
      b.block()
      return factory.rexSubqueryScalar(rel = b.rel!!)
    }

    public fun structPartFields(rex: Rex? = null, block: _StructPartFields.() -> Unit = {}):
        StructPart.Fields {
      val b = _StructPartFields(rex)
      b.block()
      return factory.structPartFields(rex = b.rex!!)
    }

    public fun structPartField(
      name: Rex? = null,
      rex: Rex? = null,
      block: _StructPartField.() -> Unit = {}
    ): StructPart.Field {
      val b = _StructPartField(name, rex)
      b.block()
      return factory.structPartField(name = b.name!!, rex = b.rex!!)
    }

    public fun sortSpec(
      rex: Rex? = null,
      dir: SortSpec.Dir? = null,
      nulls: SortSpec.Nulls? = null,
      block: _SortSpec.() -> Unit = {}
    ): SortSpec {
      val b = _SortSpec(rex, dir, nulls)
      b.block()
      return factory.sortSpec(rex = b.rex!!, dir = b.dir!!, nulls = b.nulls!!)
    }

    public fun binding(
      name: String? = null,
      rex: Rex? = null,
      block: _Binding.() -> Unit = {}
    ): Binding {
      val b = _Binding(name, rex)
      b.block()
      return factory.binding(name = b.name!!, rex = b.rex!!)
    }

    public class _Common(
      public var schema: MutableMap<String, Rel.Join.Type> = mutableMapOf(),
      public var properties: MutableSet<Property> = mutableSetOf(),
      public var metas: MutableMap<String, Any> = mutableMapOf()
    )

    public class _RelScan(
      public var common: Common? = null,
      public var rex: Rex? = null,
      public var alias: String? = null,
      public var at: String? = null,
      public var `by`: String? = null
    )

    public class _RelFilter(
      public var common: Common? = null,
      public var input: Rel? = null,
      public var condition: Rex? = null
    )

    public class _RelSort(
      public var common: Common? = null,
      public var input: Rel? = null,
      public var specs: MutableList<SortSpec> = mutableListOf()
    )

    public class _RelBag(
      public var common: Common? = null,
      public var lhs: Rel? = null,
      public var rhs: Rel? = null,
      public var op: Rel.Bag.Op? = null
    )

    public class _RelFetch(
      public var common: Common? = null,
      public var input: Rel? = null,
      public var limit: Rex? = null,
      public var offset: Rex? = null
    )

    public class _RelProject(
      public var common: Common? = null,
      public var input: Rel? = null,
      public var rexs: MutableList<Binding> = mutableListOf()
    )

    public class _RelJoin(
      public var common: Common? = null,
      public var lhs: Rel? = null,
      public var rhs: Rel? = null,
      public var condition: Rex? = null,
      public var type: Rel.Join.Type? = null
    )

    public class _RelAggregate(
      public var common: Common? = null,
      public var input: Rel? = null,
      public var calls: MutableList<Binding> = mutableListOf(),
      public var groups: MutableList<Binding> = mutableListOf(),
      public var strategy: Rel.Aggregate.Strategy? = null
    )

    public class _RexId(
      public var name: String? = null
    )

    public class _RexPath(
      public var root: Rex? = null
    )

    public class _RexUnary(
      public var rex: Rex? = null,
      public var op: Rex.Unary.Op? = null
    )

    public class _RexBinary(
      public var lhs: Rex? = null,
      public var rhs: Rex? = null,
      public var op: Rex.Binary.Op? = null
    )

    public class _RexCall(
      public var id: String? = null,
      public var args: MutableList<Rex> = mutableListOf()
    )

    public class _RexAgg(
      public var id: String? = null,
      public var args: MutableList<Rex> = mutableListOf(),
      public var modifier: Rex.Agg.Modifier? = null
    )

    public class _RexLit(
      public var `value`: IonElement? = null
    )

    public class _RexCollection(
      public var type: Rex.Collection.Type? = null,
      public var values: MutableList<Rex> = mutableListOf()
    )

    public class _RexStruct(
      public var fields: MutableList<StructPart> = mutableListOf()
    )

    public class _RexSubqueryTuple(
      public var rel: Rel? = null
    )

    public class _RexSubqueryScalar(
      public var rel: Rel? = null
    )

    public class _StructPartFields(
      public var rex: Rex? = null
    )

    public class _StructPartField(
      public var name: Rex? = null,
      public var rex: Rex? = null
    )

    public class _SortSpec(
      public var rex: Rex? = null,
      public var dir: SortSpec.Dir? = null,
      public var nulls: SortSpec.Nulls? = null
    )

    public class _Binding(
      public var name: String? = null,
      public var rex: Rex? = null
    )
  }

  public companion object {
    @JvmStatic
    public fun <T : PlanNode> build(factory: PlanFactory = PlanFactory.DEFAULT, block: Builder.() ->
        T) = Builder(factory).block()

    @JvmStatic
    public fun <T : PlanNode> create(block: PlanFactory.() -> T) = PlanFactory.DEFAULT.block()
  }
}
