# Introduction to PartiQL AST

## Design concerns

### Abstract Semantic Tree
It seems the term "AST" is often extended to mean more things than just "Abstract Syntax Tree" and our use of the term is no different. A better term might have been "Abstract Semantic Tree" because our AST was defined with the goal of modeling the intent of the PartiQL source and *not* the exact syntax. Thus, the original SQL from which an AST was constituted cannot be derived, however the *semantics* of that SQL are guaranteed to be preserved. One example that demonstrates this is the fact that we model a `CROSS JOIN` in the same way that we model an `INNER JOIN` with a condition of `TRUE`. Semantically, these have the exact same functionality, so they have the same representation in the AST.

### Tree Structure
> It **is** Actually a Tree

Language implementations often use the term "Abstract Syntax Tree" to refer to a data structure that is actually a
graph. Our implementation for PartiQL's AST is a tree and *not* a graph. It contains no cycles and each node can only
reference its children.

## Implementation
PartiQL uses [PIG](https://github.com/partiql/partiql-ir-generator/wiki) to concisely defines the structure of our ASTs. [partiql.ion](https://github.com/partiql/partiql-lang-kotlin/blob/main/lang/resources/org/partiql/type-domains/partiql.ion) contains the type universe, which specified named constraints for every node and its attributes in the generated AST. 

The rest of this doc describes some points of the implementation of PIG-generated code. 

### Immutable 

No mechanism has been provided to mutate an instance of the AST after it has been instantiated.  Modifications to an existing tree must use cloning re-writes--for instance, a visitor transform that returns a modified version of the input tree can be utilized.

### Design Patterns Used with the AST

The AST employs a number of patterns and utilizes certain features of Kotlin to aid with the development process.
Without any introduction, the reasoning behind these patterns may not be completely apparent at first glance. What
follows is an attempt to document those patterns and features.

1. When-As-Expression over Sealed Type Derived Classes

Kotlin's [when](https://kotlinlang.org/docs/reference/control-flow.html#when-expression) can be used as a statement or
as an expression.

```Kotlin
    sealed class Foo
    class Bar : Foo(val i: Int)
    class Bat : Foo(val n: String)
    class Bonk : Foo(val o: Boolean)
    val foo = //... an instance of Foo ...
    
    // This a statement because the value is not consumed...
    when(foo) -> {
        is Bar -> { println("It's a bar!" }
        is Bat -> { println("It's a bat!" }
        //A compiler warning is issued because no case for Bonk exists.
    }
    
    // This is an expression because the value is assigned to variable foo.
    val foo = when(bar) { 
        is Bat -> "It's a bat!"
        is Baz -> "It's a baz!"
        //A compile-time error is generated because there is no case for Bonk -- when branches must be exhaustive.
    }
```

When `when` is used as an expression Kotlin requires that the cases are exhaustive, meaning that all possible branches
are included or it has an `else` clause. Unfortunately, the Kotlin compiler issues a warning instead of an error when
the result of the `when` expression is not consumed. We have developed a simple way to gain these compile-time checks
for `when` statements as well. This method involves treating them as expressions.

Consider the following:

```Kotlin
    when(expr) {
        is VariableReference -> case {
            ...
        }
        is Literal -> case {
            ...
        }
        // and so on for all types derived from ExprNode
    }.toUnit()
```

In order to help make sense of this, the definitions of `case` and `toUnit` follow:

```Kotlin
    inline fun case(block: () -> Unit): WhenAsExpressionHelper {
        block()
        return WhenAsExpressionHelper.Instance
    }


    class WhenAsExpressionHelper private constructor() {
        fun toUnit() {}
        companion object {
            val Instance = WhenAsExpressionHelper()
        }
    }
```

Every branch of the `when` expression calls the `case()` function whose first argument is a literal lambda. `case()`
invokes the lambda and returns the sentinel instance of `WhenAsExpressionHelper`. This forces `when` to have a result.
`WhenAsExpressionHelper` then has a single method, `toUnit()`, which does nothing--its purpose however is to consume of
result the `when` expression.

When `case()` and `toUnit()` are used together in this fashion the Kotlin compiler considers the `when` an expression
and will require that a branch exists for all derived types or that an `else` branch is present.

This helps improve maintainability of code that uses the AST because when a new type that inherits from
`ExprNode` is added then those `when` expressions which do not include an `else` branch will generate compiler errors
and the developer will know they need to be updated to include the new node type. For this reason, the developer should
carefully consider the use of `else` branches and instead should consider explicit empty branches for each of the
derived types instead.

Also note that the use of `case()` and `toUnit()` is *not* needed when the value of the `when` expression is consumed by
other means.  For example, the compiler will still require a branch for every derived type in this scenario because the
result of the `when` becomes the function's return value:

```Kotlin
   fun transformNode(exprNode: ExprNode): ExprNode = when(exprNode) {
       is Literal -> { // case() is not needed
           //Perform cloning transform
           ...
       }
       is VariableReference -> { // case() is not needed
           //Perform cloning transform
           ...
       }
       //and so on for all nodes
       ...
   } //.toUnit() is not needed
```

2. Destructuring

Another potential maintainability issue can arise when new properties are added to existing node types.  Knowing the
locations of the code which must be modified to account for the new property can be a challenge.

Another way in which the Kotlin compiler can be enlisted to help improve code maintainability is with the use of 
[destructuring](https://kotlinlang.org/docs/reference/multi-declarations.html).  There is also a shortcoming in Kotlin's
destructuring feature which we solve almost by accident.

Consider the following:

```Kotlin
    when(expr) {
        //...
        is VariableReference -> {
            val (id, caseSensitivity) = expr
        }
        //...
    }
```

Unlike some languages, Kotlin's destructuring feature doesn't require that all the properties are mapped to variables on
the left side of `=`.  Unfortunately, this means that if a new property is added to `VariableReference`, the above
example of destructuring will not result in an compile error of any kind.

By chance, all of the node types in the AST contain an additional property:  `metas: MetaContainer`.  The fact that this
is *always* the last property defined in a node type is intentional.  In fact, any and all new properties should be
added immediately *before* the `metas: MetaContainer`.  Consider:

```Kotlin
    val (id, caseSensitivity, m) = expr
```

If a new property is added to `VariableReference` *before* `metas`, the type of varialbe `m` will be the type of that
new property and *this* will result in compile-time errors from the Kotlin compiler at the locations where `m` is
referenced. This is great for circumstances where `m` is needed, but there are also many cases where a node's
`metas` are ignored, and if `m` is unused, the Kotlin compiler will issue a warning. For that scenario use the
following:

```Kotlin
    val (id, caseSensitivity, _: MetaContainer) = varRef
```

The `_: MetaContainer` component here simply causes a compile-time assertion that the third property of
`VariableReference` is of type `MetaContainer`, resulting in a compile-time error whenever a new property is added.

3. Arbitrary Meta Information Can Be Attached to Any Node

TODO:  leaving this part unspecified for the moment because there is still some uncertainty surrounding how metas work.

4. Rules for working with the AST

- Always add new properties *before* the `metas: MetaContainer` of a new property.
- When using `when` with a sealed class and branching by derived types as a statement use `case()` and `toUnit()` to
  trick the Kotlin compiler into being helpful.
- When using `when` with the derived types of a sealed class, use destructuring in each branch where possible.
