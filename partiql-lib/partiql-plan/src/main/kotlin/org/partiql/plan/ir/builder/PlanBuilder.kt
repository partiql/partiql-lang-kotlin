package org.partiql.plan.ir.builder

import com.amazon.ionelement.api.IonElement
<<<<<<< HEAD
<<<<<<< HEAD
import kotlin.Any
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.jvm.JvmStatic
=======
import org.partiql.lang.types.StaticType
=======
>>>>>>> 916ec3df (Moves StaticType into new SPI project)
import org.partiql.plan.ir.Attribute
>>>>>>> 780d0657 (Adds SPI framework, schema inference, cli tool, localdb connector, and plan typing)
import org.partiql.plan.ir.Binding
import org.partiql.plan.ir.Case
import org.partiql.plan.ir.Common
import org.partiql.plan.ir.Field
import org.partiql.plan.ir.PlanNode
import org.partiql.plan.ir.Property
import org.partiql.plan.ir.Rel
import org.partiql.plan.ir.Rex
import org.partiql.plan.ir.SortSpec
import org.partiql.plan.ir.Step
import org.partiql.spi.types.StaticType
import kotlin.Any
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.jvm.JvmStatic

/**
 * The Builder is inside this private final class for DSL aesthetics
 */
<<<<<<< HEAD
public class Plan private constructor() {
  @Suppress("ClassName")
  public class Builder(
    private val factory: PlanFactory
  ) {
    public fun plan(
      version: org.partiql.plan.ir.Plan.Version? = null,
      root: Rex? = null,
      block: _Plan.() -> Unit = {}
    ): org.partiql.plan.ir.Plan {
      val b = _Plan(version, root)
      b.block()
      return factory.plan(version = b.version!!, root = b.root!!)
=======
public class PlanBuilder private constructor() {
    @Suppress("ClassName")
    public class Builder(
        private val factory: PlanFactory
    ) {
        public fun plan(
            version: org.partiql.plan.ir.Plan.Version? = null,
            root: Rex? = null,
            block: _Plan.() -> Unit = {}
        ): org.partiql.plan.ir.Plan {
            val b = _Plan(version, root)
            b.block()
            return factory.plan(version = b.version!!, root = b.root!!)
        }

        public fun common(
            schema: MutableList<Attribute> = mutableListOf(),
            properties: MutableSet<Property> = mutableSetOf(),
            metas: MutableMap<String, Any> = mutableMapOf(),
            block: _Common.() -> Unit = {}
        ): Common {
            val b = _Common(schema, properties, metas)
            b.block()
            return factory.common(schema = b.schema, properties = b.properties, metas = b.metas)
        }

        public fun attribute(
            name: String? = null,
            type: StaticType? = null,
            block: _Attribute.() -> Unit = {}
        ): Attribute {
            val b = _Attribute(name, type)
            b.block()
            return factory.attribute(name = b.name!!, type = b.type!!)
        }

        public fun binding(
            name: String? = null,
            `value`: Rex? = null,
            block: _Binding.() -> Unit = {}
        ): Binding {
            val b = _Binding(name, value)
            b.block()
            return factory.binding(name = b.name!!, value = b.value!!)
        }

        public fun `field`(
            name: Rex? = null,
            `value`: Rex? = null,
            block: _Field.() -> Unit = {}
        ): Field {
            val b = _Field(name, value)
            b.block()
            return factory.field(name = b.name!!, value = b.value!!)
        }

        public fun stepRex(
            index: Rex? = null,
            case: Case? = null,
            block: _StepRex.() -> Unit = {}
        ): Step.Rex {
            val b = _StepRex(index, case)
            b.block()
            return factory.stepRex(index = b.index!!, case = b.case)
        }

        public fun stepWildcard(block: _StepWildcard.() -> Unit = {}): Step.Wildcard {
            val b = _StepWildcard()
            b.block()
            return factory.stepWildcard()
        }

        public fun stepUnpivot(block: _StepUnpivot.() -> Unit = {}): Step.Unpivot {
            val b = _StepUnpivot()
            b.block()
            return factory.stepUnpivot()
        }

        public fun sortSpec(
            `value`: Rex? = null,
            dir: SortSpec.Dir? = null,
            nulls: SortSpec.Nulls? = null,
            block: _SortSpec.() -> Unit = {}
        ): SortSpec {
            val b = _SortSpec(value, dir, nulls)
            b.block()
            return factory.sortSpec(value = b.value!!, dir = b.dir!!, nulls = b.nulls!!)
        }

        public fun relScan(
            common: Common? = null,
            `value`: Rex? = null,
            alias: String? = null,
            at: String? = null,
            `by`: String? = null,
            block: _RelScan.() -> Unit = {}
        ): Rel.Scan {
            val b = _RelScan(common, value, alias, at, by)
            b.block()
            return factory.relScan(
                common = b.common!!, value = b.value!!, alias = b.alias, at = b.at, by = b.by
            )
        }

        public fun relUnpivot(
            common: Common? = null,
            `value`: Rex? = null,
            alias: String? = null,
            at: String? = null,
            `by`: String? = null,
            block: _RelUnpivot.() -> Unit = {}
        ): Rel.Unpivot {
            val b = _RelUnpivot(common, value, alias, at, by)
            b.block()
            return factory.relUnpivot(
                common = b.common!!, value = b.value!!, alias = b.alias, at = b.at, by = b.by
            )
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
            return factory.relFetch(
                common = b.common!!, input = b.input!!, limit = b.limit!!, offset = b.offset!!
            )
        }

        public fun relProject(
            common: Common? = null,
            input: Rel? = null,
            bindings: MutableList<Binding> = mutableListOf(),
            block: _RelProject.() -> Unit = {}
        ): Rel.Project {
            val b = _RelProject(common, input, bindings)
            b.block()
            return factory.relProject(common = b.common!!, input = b.input!!, bindings = b.bindings)
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
            return factory.relJoin(
                common = b.common!!, lhs = b.lhs!!, rhs = b.rhs!!, condition = b.condition, type = b.type!!
            )
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
            return factory.relAggregate(
                common = b.common!!, input = b.input!!, calls = b.calls, groups = b.groups, strategy = b.strategy!!
            )
        }

        public fun rexId(
            name: String? = null,
            case: Case? = null,
            qualifier: Rex.Id.Qualifier? = null,
            block: _RexId.() -> Unit = {}
        ): Rex.Id {
            val b = _RexId(name, case, qualifier)
            b.block()
            return factory.rexId(name = b.name!!, case = b.case, qualifier = b.qualifier!!)
        }

        public fun rexPath(
            root: Rex? = null,
            steps: MutableList<Step> = mutableListOf(),
            block: _RexPath.() -> Unit = {}
        ): Rex.Path {
            val b = _RexPath(root, steps)
            b.block()
            return factory.rexPath(root = b.root!!, steps = b.steps)
        }

        public fun rexUnary(
            `value`: Rex? = null,
            op: Rex.Unary.Op? = null,
            block: _RexUnary.() -> Unit = {}
        ): Rex.Unary {
            val b = _RexUnary(value, op)
            b.block()
            return factory.rexUnary(value = b.value!!, op = b.op!!)
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

        public fun rexTuple(
            fields: MutableList<Field> = mutableListOf(),
            block: _RexTuple.() -> Unit = {}
        ): Rex.Tuple {
            val b = _RexTuple(fields)
            b.block()
            return factory.rexTuple(fields = b.fields)
        }

        public fun rexQueryScalarCoerce(
            query: Rex.Query.Collection? = null,
            block: _RexQueryScalarCoerce.() -> Unit = {}
        ): Rex.Query.Scalar.Coerce {
            val b = _RexQueryScalarCoerce(query)
            b.block()
            return factory.rexQueryScalarCoerce(query = b.query!!)
        }

        public fun rexQueryScalarPivot(
            rel: Rel? = null,
            `value`: Rex? = null,
            at: Rex? = null,
            block: _RexQueryScalarPivot.() -> Unit = {}
        ): Rex.Query.Scalar.Pivot {
            val b = _RexQueryScalarPivot(rel, value, at)
            b.block()
            return factory.rexQueryScalarPivot(rel = b.rel!!, value = b.value!!, at = b.at!!)
        }

        public fun rexQueryCollection(
            rel: Rel? = null,
            `constructor`: Rex? = null,
            block: _RexQueryCollection.() -> Unit = {}
        ): Rex.Query.Collection {
            val b = _RexQueryCollection(rel, constructor)
            b.block()
            return factory.rexQueryCollection(rel = b.rel!!, constructor = b.constructor)
        }

        public class _Plan(
            public var version: org.partiql.plan.ir.Plan.Version? = null,
            public var root: Rex? = null
        )

        public class _Common(
            public var schema: MutableList<Attribute> = mutableListOf(),
            public var properties: MutableSet<Property> = mutableSetOf(),
            public var metas: MutableMap<String, Any> = mutableMapOf()
        )

        public class _Attribute(
            public var name: String? = null,
            public var type: StaticType? = null
        )

        public class _Binding(
            public var name: String? = null,
            public var `value`: Rex? = null
        )

        public class _Field(
            public var name: Rex? = null,
            public var `value`: Rex? = null
        )

        public class _StepRex(
            public var index: Rex? = null,
            public var case: Case? = null
        )

        public class _StepWildcard

        public class _StepUnpivot

        public class _SortSpec(
            public var `value`: Rex? = null,
            public var dir: SortSpec.Dir? = null,
            public var nulls: SortSpec.Nulls? = null
        )

        public class _RelScan(
            public var common: Common? = null,
            public var `value`: Rex? = null,
            public var alias: String? = null,
            public var at: String? = null,
            public var `by`: String? = null
        )

        public class _RelUnpivot(
            public var common: Common? = null,
            public var `value`: Rex? = null,
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
            public var bindings: MutableList<Binding> = mutableListOf()
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
            public var name: String? = null,
            public var case: Case? = null,
            public var qualifier: Rex.Id.Qualifier? = null
        )

        public class _RexPath(
            public var root: Rex? = null,
            public var steps: MutableList<Step> = mutableListOf()
        )

        public class _RexUnary(
            public var `value`: Rex? = null,
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

        public class _RexTuple(
            public var fields: MutableList<Field> = mutableListOf()
        )

        public class _RexQueryScalarCoerce(
            public var query: Rex.Query.Collection? = null
        )

        public class _RexQueryScalarPivot(
            public var rel: Rel? = null,
            public var `value`: Rex? = null,
            public var at: Rex? = null
        )

        public class _RexQueryCollection(
            public var rel: Rel? = null,
            public var `constructor`: Rex? = null
        )
>>>>>>> 780d0657 (Adds SPI framework, schema inference, cli tool, localdb connector, and plan typing)
    }

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

    public fun binding(
      name: String? = null,
      `value`: Rex? = null,
      block: _Binding.() -> Unit = {}
    ): Binding {
      val b = _Binding(name, value)
      b.block()
      return factory.binding(name = b.name!!, value = b.value!!)
    }

    public fun `field`(
      name: Rex? = null,
      `value`: Rex? = null,
      block: _Field.() -> Unit = {}
    ): Field {
      val b = _Field(name, value)
      b.block()
      return factory.field(name = b.name!!, value = b.value!!)
    }

    public fun stepKey(
      `value`: Rex? = null,
      case: Case? = null,
      block: _StepKey.() -> Unit = {}
    ): Step.Key {
      val b = _StepKey(value, case)
      b.block()
      return factory.stepKey(value = b.value!!, case = b.case)
    }

    public fun stepWildcard(block: _StepWildcard.() -> Unit = {}): Step.Wildcard {
      val b = _StepWildcard()
      b.block()
      return factory.stepWildcard()
    }

    public fun stepUnpivot(block: _StepUnpivot.() -> Unit = {}): Step.Unpivot {
      val b = _StepUnpivot()
      b.block()
      return factory.stepUnpivot()
    }

    public fun sortSpec(
      `value`: Rex? = null,
      dir: SortSpec.Dir? = null,
      nulls: SortSpec.Nulls? = null,
      block: _SortSpec.() -> Unit = {}
    ): SortSpec {
      val b = _SortSpec(value, dir, nulls)
      b.block()
      return factory.sortSpec(value = b.value!!, dir = b.dir!!, nulls = b.nulls!!)
    }

    public fun relScan(
      common: Common? = null,
      `value`: Rex? = null,
      alias: String? = null,
      at: String? = null,
      `by`: String? = null,
      block: _RelScan.() -> Unit = {}
    ): Rel.Scan {
      val b = _RelScan(common, value, alias, at, by)
      b.block()
      return factory.relScan(common = b.common!!, value = b.value!!, alias = b.alias, at = b.at, by
          = b.by)
    }

    public fun relUnpivot(
      common: Common? = null,
      `value`: Rex? = null,
      alias: String? = null,
      at: String? = null,
      `by`: String? = null,
      block: _RelUnpivot.() -> Unit = {}
    ): Rel.Unpivot {
      val b = _RelUnpivot(common, value, alias, at, by)
      b.block()
      return factory.relUnpivot(common = b.common!!, value = b.value!!, alias = b.alias, at = b.at,
          by = b.by)
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
      bindings: MutableList<Binding> = mutableListOf(),
      block: _RelProject.() -> Unit = {}
    ): Rel.Project {
      val b = _RelProject(common, input, bindings)
      b.block()
      return factory.relProject(common = b.common!!, input = b.input!!, bindings = b.bindings)
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

    public fun rexId(
      name: String? = null,
      case: Case? = null,
      qualifier: Rex.Id.Qualifier? = null,
      block: _RexId.() -> Unit = {}
    ): Rex.Id {
      val b = _RexId(name, case, qualifier)
      b.block()
      return factory.rexId(name = b.name!!, case = b.case, qualifier = b.qualifier!!)
    }

    public fun rexPath(
      root: Rex? = null,
      steps: MutableList<Step> = mutableListOf(),
      block: _RexPath.() -> Unit = {}
    ): Rex.Path {
      val b = _RexPath(root, steps)
      b.block()
      return factory.rexPath(root = b.root!!, steps = b.steps)
    }

    public fun rexUnary(
      `value`: Rex? = null,
      op: Rex.Unary.Op? = null,
      block: _RexUnary.() -> Unit = {}
    ): Rex.Unary {
      val b = _RexUnary(value, op)
      b.block()
      return factory.rexUnary(value = b.value!!, op = b.op!!)
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

    public fun rexTuple(fields: MutableList<Field> = mutableListOf(), block: _RexTuple.() -> Unit =
        {}): Rex.Tuple {
      val b = _RexTuple(fields)
      b.block()
      return factory.rexTuple(fields = b.fields)
    }

    public fun rexQueryScalarCoerce(query: Rex.Query.Collection? = null,
        block: _RexQueryScalarCoerce.() -> Unit = {}): Rex.Query.Scalar.Coerce {
      val b = _RexQueryScalarCoerce(query)
      b.block()
      return factory.rexQueryScalarCoerce(query = b.query!!)
    }

    public fun rexQueryScalarPivot(
      rel: Rel? = null,
      `value`: Rex? = null,
      at: Rex? = null,
      block: _RexQueryScalarPivot.() -> Unit = {}
    ): Rex.Query.Scalar.Pivot {
      val b = _RexQueryScalarPivot(rel, value, at)
      b.block()
      return factory.rexQueryScalarPivot(rel = b.rel!!, value = b.value!!, at = b.at!!)
    }

    public fun rexQueryCollection(
      rel: Rel? = null,
      `constructor`: Rex? = null,
      block: _RexQueryCollection.() -> Unit = {}
    ): Rex.Query.Collection {
      val b = _RexQueryCollection(rel, constructor)
      b.block()
      return factory.rexQueryCollection(rel = b.rel!!, constructor = b.constructor)
    }

    public class _Plan(
      public var version: org.partiql.plan.ir.Plan.Version? = null,
      public var root: Rex? = null
    )

    public class _Common(
      public var schema: MutableMap<String, Rel.Join.Type> = mutableMapOf(),
      public var properties: MutableSet<Property> = mutableSetOf(),
      public var metas: MutableMap<String, Any> = mutableMapOf()
    )

    public class _Binding(
      public var name: String? = null,
      public var `value`: Rex? = null
    )

    public class _Field(
      public var name: Rex? = null,
      public var `value`: Rex? = null
    )

    public class _StepKey(
      public var `value`: Rex? = null,
      public var case: Case? = null
    )

    public class _StepWildcard

    public class _StepUnpivot

    public class _SortSpec(
      public var `value`: Rex? = null,
      public var dir: SortSpec.Dir? = null,
      public var nulls: SortSpec.Nulls? = null
    )

    public class _RelScan(
      public var common: Common? = null,
      public var `value`: Rex? = null,
      public var alias: String? = null,
      public var at: String? = null,
      public var `by`: String? = null
    )

    public class _RelUnpivot(
      public var common: Common? = null,
      public var `value`: Rex? = null,
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
      public var bindings: MutableList<Binding> = mutableListOf()
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
      public var name: String? = null,
      public var case: Case? = null,
      public var qualifier: Rex.Id.Qualifier? = null
    )

    public class _RexPath(
      public var root: Rex? = null,
      public var steps: MutableList<Step> = mutableListOf()
    )

    public class _RexUnary(
      public var `value`: Rex? = null,
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

    public class _RexTuple(
      public var fields: MutableList<Field> = mutableListOf()
    )

    public class _RexQueryScalarCoerce(
      public var query: Rex.Query.Collection? = null
    )

    public class _RexQueryScalarPivot(
      public var rel: Rel? = null,
      public var `value`: Rex? = null,
      public var at: Rex? = null
    )

    public class _RexQueryCollection(
      public var rel: Rel? = null,
      public var `constructor`: Rex? = null
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
