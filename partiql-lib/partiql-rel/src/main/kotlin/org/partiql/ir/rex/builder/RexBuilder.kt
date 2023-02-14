package org.partiql.ir.rex.builder

import com.amazon.ionelement.api.AnyElement
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.MutableList
import kotlin.jvm.JvmStatic
import org.partiql.ir.rex.RexNode

/**
 * The Builder is inside this private final class for DSL aesthetics
 */
public class Rex private constructor() {
  @Suppress("ClassName")
  public class Builder(
    private val factory: RexFactory
  ) {
    public fun rexId(name: String? = null, block: _RexId.() -> Unit = {}):
        org.partiql.ir.rex.Rex.Id {
      val b = _RexId(name)
      b.block()
      return factory.rexId(name = b.name!!)
    }

    public fun rexPath(root: org.partiql.ir.rex.Rex? = null, block: _RexPath.() -> Unit = {}):
        org.partiql.ir.rex.Rex.Path {
      val b = _RexPath(root)
      b.block()
      return factory.rexPath(root = b.root!!)
    }

    public fun rexUnary(
      rex: org.partiql.ir.rex.Rex? = null,
      op: org.partiql.ir.rex.Rex.Unary.Op? = null,
      block: _RexUnary.() -> Unit = {}
    ): org.partiql.ir.rex.Rex.Unary {
      val b = _RexUnary(rex, op)
      b.block()
      return factory.rexUnary(rex = b.rex!!, op = b.op!!)
    }

    public fun rexBinary(
      lhs: org.partiql.ir.rex.Rex? = null,
      rhs: org.partiql.ir.rex.Rex? = null,
      op: org.partiql.ir.rex.Rex.Binary.Op? = null,
      block: _RexBinary.() -> Unit = {}
    ): org.partiql.ir.rex.Rex.Binary {
      val b = _RexBinary(lhs, rhs, op)
      b.block()
      return factory.rexBinary(lhs = b.lhs!!, rhs = b.rhs!!, op = b.op!!)
    }

    public fun rexCall(
      id: String? = null,
      args: MutableList<org.partiql.ir.rex.Rex> = mutableListOf(),
      block: _RexCall.() -> Unit = {}
    ): org.partiql.ir.rex.Rex.Call {
      val b = _RexCall(id, args)
      b.block()
      return factory.rexCall(id = b.id!!, args = b.args)
    }

    public fun rexAgg(
      id: String? = null,
      args: MutableList<org.partiql.ir.rex.Rex> = mutableListOf(),
      modifier: org.partiql.ir.rex.Rex.Agg.Modifier? = null,
      block: _RexAgg.() -> Unit = {}
    ): org.partiql.ir.rex.Rex.Agg {
      val b = _RexAgg(id, args, modifier)
      b.block()
      return factory.rexAgg(id = b.id!!, args = b.args, modifier = b.modifier)
    }

    public fun rexLitCollection(
      type: org.partiql.ir.rex.Rex.Lit.Collection.Type? = null,
      values: MutableList<org.partiql.ir.rex.Rex> = mutableListOf(),
      block: _RexLitCollection.() -> Unit = {}
    ): org.partiql.ir.rex.Rex.Lit.Collection {
      val b = _RexLitCollection(type, values)
      b.block()
      return factory.rexLitCollection(type = b.type!!, values = b.values)
    }

    public fun rexLitScalar(`value`: AnyElement? = null, block: _RexLitScalar.() -> Unit = {}):
        org.partiql.ir.rex.Rex.Lit.Scalar {
      val b = _RexLitScalar(value)
      b.block()
      return factory.rexLitScalar(value = b.value!!)
    }

    public class _RexId(
      public var name: String? = null
    )

    public class _RexPath(
      public var root: org.partiql.ir.rex.Rex? = null
    )

    public class _RexUnary(
      public var rex: org.partiql.ir.rex.Rex? = null,
      public var op: org.partiql.ir.rex.Rex.Unary.Op? = null
    )

    public class _RexBinary(
      public var lhs: org.partiql.ir.rex.Rex? = null,
      public var rhs: org.partiql.ir.rex.Rex? = null,
      public var op: org.partiql.ir.rex.Rex.Binary.Op? = null
    )

    public class _RexCall(
      public var id: String? = null,
      public var args: MutableList<org.partiql.ir.rex.Rex> = mutableListOf()
    )

    public class _RexAgg(
      public var id: String? = null,
      public var args: MutableList<org.partiql.ir.rex.Rex> = mutableListOf(),
      public var modifier: org.partiql.ir.rex.Rex.Agg.Modifier? = null
    )

    public class _RexLitCollection(
      public var type: org.partiql.ir.rex.Rex.Lit.Collection.Type? = null,
      public var values: MutableList<org.partiql.ir.rex.Rex> = mutableListOf()
    )

    public class _RexLitScalar(
      public var `value`: AnyElement? = null
    )
  }

  public companion object {
    @JvmStatic
    public fun <T : RexNode> build(factory: RexFactory = RexFactory.DEFAULT, block: Builder.() -> T)
        = Builder(factory).block()

    @JvmStatic
    public fun <T : RexNode> create(block: RexFactory.() -> T) = RexFactory.DEFAULT.block()
  }
}
