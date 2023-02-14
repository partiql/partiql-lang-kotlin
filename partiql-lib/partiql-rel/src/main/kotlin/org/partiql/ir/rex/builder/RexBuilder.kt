package org.partiql.ir.rex.builder

import com.amazon.ionelement.api.IonElement
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.MutableList
import kotlin.jvm.JvmStatic
import org.partiql.ir.rex.RexNode
import org.partiql.ir.rex.StructPart

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
      return factory.rexAgg(id = b.id!!, args = b.args, modifier = b.modifier!!)
    }

    public fun rexLit(`value`: IonElement? = null, block: _RexLit.() -> Unit = {}):
        org.partiql.ir.rex.Rex.Lit {
      val b = _RexLit(value)
      b.block()
      return factory.rexLit(value = b.value!!)
    }

    public fun rexCollection(
      type: org.partiql.ir.rex.Rex.Collection.Type? = null,
      values: MutableList<org.partiql.ir.rex.Rex> = mutableListOf(),
      block: _RexCollection.() -> Unit = {}
    ): org.partiql.ir.rex.Rex.Collection {
      val b = _RexCollection(type, values)
      b.block()
      return factory.rexCollection(type = b.type!!, values = b.values)
    }

    public fun rexStruct(fields: MutableList<StructPart> = mutableListOf(), block: _RexStruct.() ->
        Unit = {}): org.partiql.ir.rex.Rex.Struct {
      val b = _RexStruct(fields)
      b.block()
      return factory.rexStruct(fields = b.fields)
    }

    public fun structPartFields(rex: org.partiql.ir.rex.Rex? = null, block: _StructPartFields.() ->
        Unit = {}): StructPart.Fields {
      val b = _StructPartFields(rex)
      b.block()
      return factory.structPartFields(rex = b.rex!!)
    }

    public fun structPartField(
      name: org.partiql.ir.rex.Rex? = null,
      rex: org.partiql.ir.rex.Rex? = null,
      block: _StructPartField.() -> Unit = {}
    ): StructPart.Field {
      val b = _StructPartField(name, rex)
      b.block()
      return factory.structPartField(name = b.name!!, rex = b.rex!!)
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

    public class _RexLit(
      public var `value`: IonElement? = null
    )

    public class _RexCollection(
      public var type: org.partiql.ir.rex.Rex.Collection.Type? = null,
      public var values: MutableList<org.partiql.ir.rex.Rex> = mutableListOf()
    )

    public class _RexStruct(
      public var fields: MutableList<StructPart> = mutableListOf()
    )

    public class _StructPartFields(
      public var rex: org.partiql.ir.rex.Rex? = null
    )

    public class _StructPartField(
      public var name: org.partiql.ir.rex.Rex? = null,
      public var rex: org.partiql.ir.rex.Rex? = null
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
