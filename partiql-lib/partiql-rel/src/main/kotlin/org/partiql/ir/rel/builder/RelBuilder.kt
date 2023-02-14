package org.partiql.ir.rel.builder

import kotlin.Any
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.jvm.JvmStatic
import org.partiql.ir.rel.Binding
import org.partiql.ir.rel.Common
import org.partiql.ir.rel.Property
import org.partiql.ir.rel.RelNode
import org.partiql.ir.rex.RexNode

/**
 * The Builder is inside this private final class for DSL aesthetics
 */
public class Rel private constructor() {
  @Suppress("ClassName")
  public class Builder(
    private val factory: RelFactory
  ) {
    public fun common(
      schema: MutableMap<String, org.partiql.ir.rel.Rel.Join.Type> = mutableMapOf(),
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
      `value`: RexNode? = null,
      alias: String? = null,
      at: String? = null,
      `by`: String? = null,
      block: _RelScan.() -> Unit = {}
    ): org.partiql.ir.rel.Rel.Scan {
      val b = _RelScan(common, value, alias, at, by)
      b.block()
      return factory.relScan(common = b.common!!, value = b.value!!, alias = b.alias, at = b.at, by
          = b.by)
    }

    public fun relCross(
      common: Common? = null,
      lhs: org.partiql.ir.rel.Rel? = null,
      rhs: org.partiql.ir.rel.Rel? = null,
      block: _RelCross.() -> Unit = {}
    ): org.partiql.ir.rel.Rel.Cross {
      val b = _RelCross(common, lhs, rhs)
      b.block()
      return factory.relCross(common = b.common!!, lhs = b.lhs!!, rhs = b.rhs!!)
    }

    public fun relFilter(
      common: Common? = null,
      input: org.partiql.ir.rel.Rel? = null,
      condition: RexNode? = null,
      block: _RelFilter.() -> Unit = {}
    ): org.partiql.ir.rel.Rel.Filter {
      val b = _RelFilter(common, input, condition)
      b.block()
      return factory.relFilter(common = b.common!!, input = b.input!!, condition = b.condition!!)
    }

    public fun relSort(
      common: Common? = null,
      rex: RexNode? = null,
      dir: org.partiql.ir.rel.Rel.Sort.Dir? = null,
      nulls: org.partiql.ir.rel.Rel.Sort.Nulls? = null,
      block: _RelSort.() -> Unit = {}
    ): org.partiql.ir.rel.Rel.Sort {
      val b = _RelSort(common, rex, dir, nulls)
      b.block()
      return factory.relSort(common = b.common!!, rex = b.rex!!, dir = b.dir!!, nulls = b.nulls!!)
    }

    public fun relBag(
      common: Common? = null,
      lhs: org.partiql.ir.rel.Rel? = null,
      rhs: org.partiql.ir.rel.Rel? = null,
      op: org.partiql.ir.rel.Rel.Bag.Op? = null,
      block: _RelBag.() -> Unit = {}
    ): org.partiql.ir.rel.Rel.Bag {
      val b = _RelBag(common, lhs, rhs, op)
      b.block()
      return factory.relBag(common = b.common!!, lhs = b.lhs!!, rhs = b.rhs!!, op = b.op!!)
    }

    public fun relFetch(
      common: Common? = null,
      input: org.partiql.ir.rel.Rel? = null,
      limit: Long? = null,
      offset: Long? = null,
      block: _RelFetch.() -> Unit = {}
    ): org.partiql.ir.rel.Rel.Fetch {
      val b = _RelFetch(common, input, limit, offset)
      b.block()
      return factory.relFetch(common = b.common!!, input = b.input!!, limit = b.limit!!, offset =
          b.offset!!)
    }

    public fun relProject(
      common: Common? = null,
      input: org.partiql.ir.rel.Rel? = null,
      rexs: MutableList<Binding> = mutableListOf(),
      block: _RelProject.() -> Unit = {}
    ): org.partiql.ir.rel.Rel.Project {
      val b = _RelProject(common, input, rexs)
      b.block()
      return factory.relProject(common = b.common!!, input = b.input!!, rexs = b.rexs)
    }

    public fun relJoin(
      common: Common? = null,
      lhs: org.partiql.ir.rel.Rel? = null,
      rhs: org.partiql.ir.rel.Rel? = null,
      condition: RexNode? = null,
      type: org.partiql.ir.rel.Rel.Join.Type? = null,
      block: _RelJoin.() -> Unit = {}
    ): org.partiql.ir.rel.Rel.Join {
      val b = _RelJoin(common, lhs, rhs, condition, type)
      b.block()
      return factory.relJoin(common = b.common!!, lhs = b.lhs!!, rhs = b.rhs!!, condition =
          b.condition, type = b.type!!)
    }

    public fun relAggregate(
      common: Common? = null,
      input: org.partiql.ir.rel.Rel? = null,
      calls: MutableList<Binding> = mutableListOf(),
      groups: MutableList<RexNode> = mutableListOf(),
      block: _RelAggregate.() -> Unit = {}
    ): org.partiql.ir.rel.Rel.Aggregate {
      val b = _RelAggregate(common, input, calls, groups)
      b.block()
      return factory.relAggregate(common = b.common!!, input = b.input!!, calls = b.calls, groups =
          b.groups)
    }

    public fun binding(
      name: String? = null,
      `value`: RexNode? = null,
      block: _Binding.() -> Unit = {}
    ): Binding {
      val b = _Binding(name, value)
      b.block()
      return factory.binding(name = b.name!!, value = b.value!!)
    }

    public class _Common(
      public var schema: MutableMap<String, org.partiql.ir.rel.Rel.Join.Type> = mutableMapOf(),
      public var properties: MutableSet<Property> = mutableSetOf(),
      public var metas: MutableMap<String, Any> = mutableMapOf()
    )

    public class _RelScan(
      public var common: Common? = null,
      public var `value`: RexNode? = null,
      public var alias: String? = null,
      public var at: String? = null,
      public var `by`: String? = null
    )

    public class _RelCross(
      public var common: Common? = null,
      public var lhs: org.partiql.ir.rel.Rel? = null,
      public var rhs: org.partiql.ir.rel.Rel? = null
    )

    public class _RelFilter(
      public var common: Common? = null,
      public var input: org.partiql.ir.rel.Rel? = null,
      public var condition: RexNode? = null
    )

    public class _RelSort(
      public var common: Common? = null,
      public var rex: RexNode? = null,
      public var dir: org.partiql.ir.rel.Rel.Sort.Dir? = null,
      public var nulls: org.partiql.ir.rel.Rel.Sort.Nulls? = null
    )

    public class _RelBag(
      public var common: Common? = null,
      public var lhs: org.partiql.ir.rel.Rel? = null,
      public var rhs: org.partiql.ir.rel.Rel? = null,
      public var op: org.partiql.ir.rel.Rel.Bag.Op? = null
    )

    public class _RelFetch(
      public var common: Common? = null,
      public var input: org.partiql.ir.rel.Rel? = null,
      public var limit: Long? = null,
      public var offset: Long? = null
    )

    public class _RelProject(
      public var common: Common? = null,
      public var input: org.partiql.ir.rel.Rel? = null,
      public var rexs: MutableList<Binding> = mutableListOf()
    )

    public class _RelJoin(
      public var common: Common? = null,
      public var lhs: org.partiql.ir.rel.Rel? = null,
      public var rhs: org.partiql.ir.rel.Rel? = null,
      public var condition: RexNode? = null,
      public var type: org.partiql.ir.rel.Rel.Join.Type? = null
    )

    public class _RelAggregate(
      public var common: Common? = null,
      public var input: org.partiql.ir.rel.Rel? = null,
      public var calls: MutableList<Binding> = mutableListOf(),
      public var groups: MutableList<RexNode> = mutableListOf()
    )

    public class _Binding(
      public var name: String? = null,
      public var `value`: RexNode? = null
    )
  }

  public companion object {
    @JvmStatic
    public fun <T : RelNode> build(factory: RelFactory = RelFactory.DEFAULT, block: Builder.() -> T)
        = Builder(factory).block()

    @JvmStatic
    public fun <T : RelNode> create(block: RelFactory.() -> T) = RelFactory.DEFAULT.block()
  }
}
