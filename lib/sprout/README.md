# Sprout

Sprout is a graphical IR generator. It is inspired by PIG, ANTLR, and Protobuf.

## Installation

> Note, all commands in this document are run from the partiql-lang-kotlin root

```shell
./gradlew :lib:sprout:install  
```

## Usage

```shell
$ ./lib/sprout/build/install/sprout/bin/sprout generate kotlin --help

Usage: sprout generate kotlin [-hV] [-m=<modifier>] [-o=<out>]
                              [-p=<packageRoot>] [-u=<id>] [--poems=<poems>]...
                              <file>
Generates Kotlin sources from type universe definitions
      <file>            Type definition file
  -h, --help            Show this help message and exit.
  -m, --modifier=<modifier>
                        Generated node class modifier. Options FINAL, DATA, OPEN
  -o, --out=<out>       Generated source output directory
  -p, --package=<packageRoot>
                        Package root
      --poems=<poems>   Poem templates to apply
  -u, --universe=<id>   Universe identifier
```

**Example**
```shell
$ ./lib/sprout/build/install/sprout/bin/sprout generate kotlin \
     -o ./generated\
     -p org.partiql.example\
     -u Example\
     -m OPEN\
     -poems visitor example.ion
  
# sources are generated in partiql-lang-kotlin/generated/
```

## Example

Here is a short example which shows some features such as
- Sum types
- Product types
- Enum types
- Inline type definitions
- Local and absolute type references
- Builtin scalar types

**Ion**
```ion
expr::[
  unary::{
    expr: expr,
    op: [ ADD, SUB ]
  },
  binary::{
    lhs: expr,
    rhs: expr,
    op: [ ADD, SUB, MULT, DIV ]
  },
  call::{
    id: '.expr.id.path',
    args: list::[expr]
  },
  id::[
    relative::{
      id: string
    },
    path::{
      id: list::[string]
    }
  ]
]
```

**Kotlin**

> This is the basic template with no additional poems — visitors, listeners, factories, serde, etc.

```kotlin
public abstract class ExampleNode

public sealed class Expr : ExampleNode() {
  public data class Unary(
    public val expr: Expr,
    public val op: Op
  ) : Expr() {
    public enum class Op {
      ADD,
      SUB,
    }
  }

  public data class Binary(
    public val lhs: Expr,
    public val rhs: Expr,
    public val op: Op
  ) : Expr() {
    public enum class Op {
      ADD,
      SUB,
      MULT,
      DIV,
    }
  }

  public data class Call(
    public val id: Id.Path,
    public val args: List<Expr>
  ) : Expr()

  public sealed class Id : Expr() {
    public data class Relative(
      public val id: String
    ) : Id()

    public data class Path(
      public val id: List<String>
    ) : Id()
  }
}
```

## Guide

### Modeling Language

The modeling language is an Ion DSL.

### Types

https://en.wikipedia.org/wiki/Algebraic_data_type

> A general algebraic data type is a possibly recursive sum type of product types.

**Sum Type**

A [sum](https://en.wikipedia.org/wiki/Tagged_union) type takes one of several defined forms.
The wiki page has some nice examples in a variety of languages.

```ion
// sum named `x` with variants `a` and `b`
x::[
  a::{ ... },
  b::{ ... }
]
```

**Product Type**

A [product](https://en.wikipedia.org/wiki/Product_type) type is some structure with a fixed set of fields.

```ion
// product named `x` with fields `a` and `b` of type int, string respectively
x::{
  a: int,
  b: string
}
```

Fields of a product type can be marked as `optional`. For example,
```ion
x::{
  a: optional::int,
  b: map::[int,optional::string], // Map<Int, String?> what does this actually mean? I don't know but it's an example
  c: optional::foo
}

foo::[ ... ]
```

**Enum Type**

This is a special case of the sum type. Each variant is a named value.
A sum type becomes an enum if all variants are symbols matching the regex `[A-Z][A-Z0-9_]*`.

```ion
// enum named `x` with values A, B, and C
x::[ A, B, C ]
```

**Scalar Types**

```ion
bool
int         // Int32
long        // Int64
float       // IEEE 754 (32 bit)
double      // IEEE 754 (64 bit)
bytes       // Array of unsigned bytes
string      // Unicode char sequenc
```

**Collection Types**

```ion
list::[t]   // List<T>
set::[t]    // Set<T>
map::[k,v]  // Map<K,V>
```

**Import Type**

At the top of each definition file, you can specify imports for your generation target.
Values with a target's import are target specific. Since Kotlin compiles to Java bytecode, an import uses the [canonical
Java binary name](https://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.1) to reference an external type.

```ion
imports::{
  kotlin: [
    timestamp::'com.amazon.ionelement.api.TimestampElement'
  ]
}

// -- `timestamp` can now be referenced
// -- `bounds` is an inline enum definition with implicit id `my_interval.bounds`
my_interval::{
  start: timestamp,
  end: timestamp,
  bounds: [
    INCLUSIVE,
    EXCLUSIVE,
    L_EXCLUSIVE,
    R_EXCLUSIVE
  ]
}
```

### Type References

Names need not be globally unique. You can refer to a type by its symbol or an absolute path (`.` delimited, starting with a `.`). If a raw symbol is used,
the type definitions will be searched for the _nearest_ type with that symbol. Scalars are match first, then definitions, and finally imports.

Here's an example where absolute references are required to achieve the desire behavior
```ion
imports::{
  kotlin: [
    ion::'com.amazon.ionelement.api.IonElement'
  ]
}

range::{
  start: int,
  end: int,
  bounds: bounds // relative reference, forward declaration not required
}

bounds::[
  OPEN,
  CLOSED
]

value::[
  ion::{
    value: '.ion' // use '.' for root reference so this isn't self-referential
  },
  range::{
    value: '.range' // ..
  }
]
```

### Inline Definitions

A product type can contain inline definitions. If the definition does not have an identifier symbol, the field name is used.

```ion
foo::{
  a: [...],               // inline sum foo.a
  b: v::[...],            // inline sum foo.v
  c: optional::[...],     // inline sum foo.c, optional field of foo
  d: optional::x::[...],  // inline sum foo.v, optional field of foo
  e: {...},               // inline product foo.e
  f: y::{...},            // inline product foo.y
  g: optional::{...},     // inline product foo.g, optional field of foo
  h: optional::z::{...},  // inline product foo.z, optional field of foo
 }
```

## Code Generation

### Kotlin

This library was written to generate Kotlin code with _some_ thought of other targets. Code is generated by applying
multiple "poems". You can also provide your own, but the story around that hasn't been fully fleshed out. The builtin
poems are below. By default, there is no runtime library. If you import a type or use the Jackson poem, then you'll need
a runtime dep on the appropriate types.

**Visitor**

This generates an ANTLR-like visitor — which is the same style as [Trino](https://github.com/trinodb/trino/blob/master/core/trino-parser/src/main/java/io/trino/sql/tree/AstVisitor.java) and [Calcite](https://github.com/apache/calcite/blob/b9c2099ea92a575084b55a206efc5dd341c0df62/core/src/main/java/org/apache/calcite/rex/RexBiVisitor.java).

Each node gets a `children` field and an `<R, C> accept(visitor: Visitor<R,C>, ctx: C): R` method.

```kotlin
public interface ExampleVisitor<R, C> {
  public fun visit(node: ExampleNode, ctx: C): R

  public fun visitExpr(node: Expr, ctx: C): R

  public fun visitExprUnary(node: Expr.Unary, ctx: C): R

  public fun visitExprBinary(node: Expr.Binary, ctx: C): R

  public fun visitExprCall(node: Expr.Call, ctx: C): R

  public fun visitExprId(node: Expr.Id, ctx: C): R

  public fun visitExprIdRelative(node: Expr.Id.Relative, ctx: C): R

  public fun visitExprIdPath(node: Expr.Id.Path, ctx: C): R
}

public abstract class ExampleBaseVisitor<R, C> : ExampleVisitor<R, C> {
    public override fun visit(node: ExampleNode, ctx: C): R = node.accept(this, ctx)

    public override fun visitExpr(node: Expr, ctx: C): R = when (node) {
        is Expr.Unary -> visitExprUnary(node, ctx)
        is Expr.Binary -> visitExprBinary(node, ctx)
        is Expr.Call -> visitExprCall(node, ctx)
        is Expr.Id -> visitExprId(node, ctx)
    }

    public override fun visitExprUnary(node: Expr.Unary, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprBinary(node: Expr.Binary, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprCall(node: Expr.Call, ctx: C): R = defaultVisit(node, ctx)

    public override fun visitExprId(node: Expr.Id, ctx: C): R = when (node) {
        is Expr.Id.Relative -> visitExprIdRelative(node, ctx)
        is Expr.Id.Path -> visitExprIdPath(node, ctx)
    }

    public override fun visitExprIdRelative(node: Expr.Id.Relative, ctx: C): R = defaultVisit(
        node,
        ctx
    )

    public override fun visitExprIdPath(node: Expr.Id.Path, ctx: C): R = defaultVisit(node, ctx)

    public open fun defaultVisit(node: ExampleNode, ctx: C): R {
        for (child in node.children) {
            child.accept(this, ctx)
        }
        return defaultReturn(node, ctx)
    }

    public abstract fun defaultReturn(node: ExampleNode, ctx: C): R
}
```

**Listener**

Like ANTLR's listener. This could be improved by making walker abstract and having a default. Or making ExampleListener
take some parameterized types, but to be honest the visitor is mainly used.

```kotlin
public object ExampleWalker {
    public fun walk(listener: ExampleListener, node: ExampleNode): Unit {
        listener.enterEveryNode(node)
        node.enter(listener)
        node.children.forEach { walk(listener, it) }
        node.exit(listener)
        listener.exitEveryNode(node)
    }
}

public abstract class ExampleListener {
  public open fun enterExpr(node: Expr): Unit {
  }

  public open fun exitExpr(node: Expr): Unit {
  }

  public open fun enterExprUnary(node: Expr.Unary): Unit {
  }

  public open fun exitExprUnary(node: Expr.Unary): Unit {
  }

  public open fun enterExprBinary(node: Expr.Binary): Unit {
  }

  public open fun exitExprBinary(node: Expr.Binary): Unit {
  }
    
  // ... redacted

  public open fun enterEveryNode(node: ExampleNode): Unit {
  }

  public open fun exitEveryNode(node: ExampleNode): Unit {
  }
}
```

**Builder**

This generates an abstract factory and a DSL.

```kotlin
public abstract class ExampleFactory {
  public open fun exprUnary(expr: Expr, op: Expr.Unary.Op) = Expr.Unary(expr, op)

  public open fun exprBinary(
    lhs: Expr,
    rhs: Expr,
    op: Expr.Binary.Op
  ) = Expr.Binary(lhs, rhs, op)

  public open fun exprCall(id: Expr.Id.Path, args: List<Expr>) = Expr.Call(id, args)

  public open fun exprIdRelative(id: String) = Expr.Id.Relative(id)

  public open fun exprIdPath(id: List<String>) = Expr.Id.Path(id)

  public companion object {
    public val DEFAULT: ExampleFactory = object : ExampleFactory() {}
  }
}

/**
 * The Builder is inside this private final class for DSL aesthetics
 */
public class Example private constructor() {
    @Suppress("ClassName")
    public class Builder(
        private val factory: ExampleFactory
    ) {
        public fun exprUnary(
            expr: Expr? = null,
            op: Expr.Unary.Op? = null,
            block: _ExprUnary.() -> Unit = {}
        ): Expr.Unary {
            val b = _ExprUnary(expr, op)
            b.block()
            return factory.exprUnary(expr = b.expr!!, op = b.op!!)
        }

        public fun exprBinary(
            lhs: Expr? = null,
            rhs: Expr? = null,
            op: Expr.Binary.Op? = null,
            block: _ExprBinary.() -> Unit = {}
        ): Expr.Binary {
            val b = _ExprBinary(lhs, rhs, op)
            b.block()
            return factory.exprBinary(lhs = b.lhs!!, rhs = b.rhs!!, op = b.op!!)
        }

        public fun exprCall(
            id: Expr.Id.Path? = null,
            args: MutableList<Expr> = mutableListOf(),
            block: _ExprCall.() -> Unit = {}
        ): Expr.Call {
            val b = _ExprCall(id, args)
            b.block()
            return factory.exprCall(id = b.id!!, args = b.args)
        }

        public fun exprIdRelative(id: String? = null, block: _ExprIdRelative.() -> Unit = {}):
            Expr.Id.Relative {
            val b = _ExprIdRelative(id)
            b.block()
            return factory.exprIdRelative(id = b.id!!)
        }

        public fun exprIdPath(id: MutableList<String> = mutableListOf(), block: _ExprIdPath.() -> Unit =
            {}): Expr.Id.Path {
            val b = _ExprIdPath(id)
            b.block()
            return factory.exprIdPath(id = b.id)
        }

        public class _ExprUnary(
            public var expr: Expr? = null,
            public var op: Expr.Unary.Op? = null
        )

        public class _ExprBinary(
            public var lhs: Expr? = null,
            public var rhs: Expr? = null,
            public var op: Expr.Binary.Op? = null
        )

        public class _ExprCall(
            public var id: Expr.Id.Path? = null,
            public var args: MutableList<Expr> = mutableListOf()
        )

        public class _ExprIdRelative(
            public var id: String? = null
        )

        public class _ExprIdPath(
            public var id: MutableList<String> = mutableListOf()
        )
    }

    public companion object {
        @JvmStatic
        public fun <T : ExampleNode> build(factory: ExampleFactory = ExampleFactory.DEFAULT,
            block: Builder.() -> T) = Builder(factory).block()

        @JvmStatic
        public fun <T : ExampleNode> create(block: ExampleFactory.() -> T) =
            ExampleFactory.DEFAULT.block()
    }
}
```

**Jackson**

The Sprout Jackson poem generates a [Jackson Module](https://fasterxml.github.io/jackson-databind/javadoc/2.7/com/fasterxml/jackson/databind/module/SimpleModule.html) which you can install to arbitrary ObjectMappers. This enables serde
to and from Jackson supported formats such as xml, yaml, json, ion, etc. The generated module enables you do deserialize
to any node (including abstract nodes) or to the root level node (like PIGs fromIonElement).

Usage
```kotlin
// build with the DSL
// Note that the DSL takes a factory, so you can control instantiation from the DSL
val tree = Example.build { 
    exprCall {
         // DSL receivers can be invoked like factory methods
         id = exprIdPath(listOf("foo", "bar"))
         // DSL blocks!
         args += exprLit(1)
         args += exprLit(2)
     }
 }

// Use any mapper IonObjectMapper, YAMLMapper, XmlMapper (with defaultUseWrapper false), etc.
val mapper = ObjectMapper()

// Register the generated module, which you can provide a factory to
mapper.registerModule(ExampleModule())

val serialized = mapper.writeValueAsString(tree)
println("--- Serialized")
println(serialized)

// Deserialize to top-level abstract type
// Note that we don't have any type information other than the top-level type
val deserializedTree: ExampleNode = mapper.readValue(serialized, ExampleNode::class.java)
println("--- Deserialized Tree")
println(deserializedTree)

// Deserialize to some given abstract type (Expr::class.java)
// This is normally not possible with Jackson unless a deserializer module is installed
// There is no default constructor for an abstract type because .. abstract, hence the module must tell Jackson how to create the object
// But we know how to create the object because we have a factory! So you can inject your own factory to the Jackson
// module which means you can control instantiation upon deserialization by only overriding the relevant factory method.
// This is a nice example of how these patterns compose.
val deserializedExpr = mapper.readValue(serialized, Expr::class.java)
println("--- Deserialized Expr")
println(deserializedTree)
```

Mapper
```kotlin
public class ExampleModule(
  private val factory: ExampleFactory = ExampleFactory.DEFAULT
) : SimpleModule() {
  private val _base: Mapping<ExampleNode> = Mapping {
    when (val id = it.id()) {
      "expr" -> _expr(it)
      "expr.unary" -> _exprUnary(it)
      "expr.binary" -> _exprBinary(it)
      "expr.call" -> _exprCall(it)
      "expr.id" -> _exprId(it)
      "expr.id.relative" -> _exprIdRelative(it)
      "expr.id.path" -> _exprIdPath(it)
      else -> err(id)
    }
  }


  private val _expr: Mapping<Expr> = Mapping {
    when (val id = it.id()) {
      "expr.unary" -> _exprUnary(it)
      "expr.binary" -> _exprBinary(it)
      "expr.call" -> _exprCall(it)
      "expr.id" -> _exprId(it)
      else -> err(id)
    }
  }


  private val _exprUnary: Mapping<Expr.Unary> = Mapping {
    factory.exprUnary(
    expr = _expr(it["expr"]),
    op = org.partiql.sprout.test.generated.Expr.Unary.Op.valueOf(it["op"].asText().uppercase()),
    )
  }


  private val _exprBinary: Mapping<Expr.Binary> = Mapping {
    factory.exprBinary(
    lhs = _expr(it["lhs"]),
    rhs = _expr(it["rhs"]),
    op = org.partiql.sprout.test.generated.Expr.Binary.Op.valueOf(it["op"].asText().uppercase()),
    )
  }


  private val _exprCall: Mapping<Expr.Call> = Mapping {
    factory.exprCall(
    id = _exprIdPath(it["id"]),
    args = it["args"].map { n -> _expr(n) },
    )
  }


  private val _exprId: Mapping<Expr.Id> = Mapping {
    when (val id = it.id()) {
      "expr.id.relative" -> _exprIdRelative(it)
      "expr.id.path" -> _exprIdPath(it)
      else -> err(id)
    }
  }


  private val _exprIdRelative: Mapping<Expr.Id.Relative> = Mapping {
    factory.exprIdRelative(
    id = it["id"].asText(),
    )
  }


  private val _exprIdPath: Mapping<Expr.Id.Path> = Mapping {
    factory.exprIdPath(
    id = it["id"].map { n -> n.asText() },
    )
  }


  init {
    addDeserializer(ExampleNode::class.java, map(_base))
    addDeserializer(Expr::class.java, map(_expr))
    addDeserializer(Expr.Unary::class.java, map(_exprUnary))
    addDeserializer(Expr.Binary::class.java, map(_exprBinary))
    addDeserializer(Expr.Call::class.java, map(_exprCall))
    addDeserializer(Expr.Id::class.java, map(_exprId))
    addDeserializer(Expr.Id.Relative::class.java, map(_exprIdRelative))
    addDeserializer(Expr.Id.Path::class.java, map(_exprIdPath))
  }

  private fun JsonNode.id(): String = get("_id").asText()

  private inline fun err(id: String): Nothing =
      error("""no deserializer registered for _id `$id`""")

  private fun <T : ExampleNode> map(mapping: Mapping<T>): JsonDeserializer<T> = object :
      JsonDeserializer<T>() {
    public override fun deserialize(p: JsonParser, ctxt: DeserializationContext): T =
        mapping(ctxt.readTree(p)!!)
  }

  private fun interface Mapping<out T : ExampleNode> {
    public operator fun invoke(node: JsonNode): T
  }
}
```
