# This Doc covers the ExprNode AST, which has been deprecated and removed from the Codebase. 

# Introduction to the ExprNode AST

It seems the term "AST" is often extended to mean more things than just "Abstract Syntax Tree" and our use of the term
is no different. A better term might have been "Abstract Semantic Tree" because our AST was defined with the goal of
modeling the intent of the PartiQL source and *not* the exact syntax. Thus, the original SQL from which an AST was
constituted cannot be derived, however the *semantics* of that SQL are guaranteed to be preserved. One example that
demonstrates this is the fact that we model a `CROSS JOIN` in the same way that we model an `INNER JOIN` with a
condition of `TRUE`. Semantically, these have the exact same function and so they also have the same representation in
the AST.

> It **is** Actually a Tree

Language implementations often use the term "Abstract Syntax Tree" to refer to a data structure that is actually a
graph. Our implementation for PartiQL's AST is a tree and *not* a graph. It contains no cycles and each node can only
reference its children.

## It's Immutable

No mechanism has been provided to mutate an instance of the AST after it has been instantiated.  Modifications to an
existing tree must use cloning re-writes--for instance, a visitor pattern that returns a modified version of the input
tree can be utilized.

## Design Patterns Used with the AST

The AST employs a number of patterns and utilizes certain features of Kotlin to aid with the development process.
Without any introduction, the reasoning behind these patterns may not be completely apparent at first glance. What
follows is an is an attempt to document those patterns and features.

### Inheritance Mirrors The PartiQL Grammar

The top-most type of the AST is `org.partiql.lang.ast.ExprNode`. Most of the classes of the AST derive
from this class. Most child nodes are also of type `ExprNode`. However, there are several cases where the types of child
nodes that are allowed are constrained (or extended) by PartiQL's grammar. For example, not every type of `ExprNode`
can exist as components of a path expression (i.e. `a.b.c`). Additionally, some path components are allowed that do not
make sense outside of the context of a path expression (i.e. `a.*.b` and `a[*].b`). If *all* nodes of the AST inherited
from `ExprNode` it would be easy to accidentally construct ASTs which are structurally invalid. Thus, each grammar
context has a different base class.

This pattern enlists the assistance of the Kotlin compiler to ensure that ASTs are constructed in a manner that is
structurally valid.  This works so well that for the most part,
[ExprNodeCompiler][org.partiql.lang.eval.ExprNodeCompiler] needs to include very few structural checks on the AST.
Mostly, it is possible to assume that if the compiler allowed the tree to be instantiated, then it is structurally
valid. (However, that does not mean it is semantically valid.)

The base classes are:

- `org.partiql.lang.ast.ExprNode`, for any expression that is self contained and has a value.
- `org.partiql.lang.ast.SelectListItem`, for expressions that may appear between `SELECT` ... `FROM`.
- `org.partiql.lang.ast.FromSource`, for expressions that are data sources in a `FROM` clause.
- `org.partiql.lang.ast.PathComponent`, for the components of a path expression.
- `org.partiql.lang.ast.SelectProjection`, for the type of projection used by a
`SELECT`,`SELECT VALUE` or `PIVOT` query. This isn't directly related to the grammar but is convenient to represent
in this manner.
- `org.partiql.lang.ast.DataManipulation`, for data manipulation expressions that may optionally be wrapped with
 `FROM ... WHERE ...`.
- `org.partiql.lang.DmlOperation`, for the data manipulation operation itself (e.g. `INSERT INTO ...`)

All base classes of the AST are [sealed](https://kotlinlang.org/docs/reference/sealed-classes.html) classes.

To keep the inheritance hierarchy manageable, the inheritance depth does not exceed 1.

### Data Classes

Kotlin [data classes](https://kotlinlang.org/docs/reference/data-classes.html) have several useful properties which aid
the developer when working with the AST.  Those features are the compiler generated methods shown below.

- `equals()`, can be used to compare two nodes--for instance, during unit testing.
- `toString()`, highly useful during debugging.
- `componentN()`, enables the use of destructuring (see the section on destructuring below).
- `copy()`, performs a shallow copy of the node, useful during cloning re-writes.

### When-As-Expression over Sealed Type Derived Classes

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

### Destructuring

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

### Arbitrary Meta Information Can Be Attached to Any Node

TODO:  leaving this part unspecified for the moment because there is still some uncertainty surrounding the how metas
work.


## Rules for working with the AST

- Always add new properties *before* the `metas: MetaContainer` of a new property.
- When using `when` with a sealed class and branching by derived types as a statement use `case()` and `toUnit()` to
  trick the Kotlin compiler into being helpful.
- When using `when` with the derived types of a sealed class, use destructuring in each branch where possible.

This document covers the s-expression based format of the PartiQL AST version 0, which will soon
be deprecated in favor of version 1.  
 
## Ion SQL AST

The `SqlParser` implementation generates an AST representation in Ion based on
s-expressions.

The general format is as follows:

```
(name ...)
```

Where `name` is the AST node name, which can be one of:

* `(lit <ION VALUE>)` - a verbatim (quoted) Ion value.
* `(missing)` - The literal `MISSING` value.
* `(id <NAME SYMBOL> [case_sensitive | case_insensitive])` - a quoted or unquoted identifier.
* `(<OPERATOR SYMBOL> ...)` - A binary or unary operator.
* `(select ...)` - A `SELECT-FROM-WHERE` expression.
* `(pivot ...)` - Convert a collection into a tuple/struct.
* `(path <VALUE EXPR> <PATH COMPONENT EXPR>...)` - A path (which is used for normal dotted name resolution).
* `(call <NAME SYMBOL> <VALUE EXPR>...)` - A function invocation.
* `(call_agg <NAME SYMBOL> <QUANTIFIER SYMBOL> <VALUE EXPR>)` - An aggregate call.
* `(call_agg_wildcard <NAME SYMBOL>)` - An aggregate call with wildcard,
  specifically for the special form `COUNT(*)`.
* `(struct <NAME EXPR> <VALUE EXPR>...)` - A *constructor* for a tuple/struct that
  is to be evaluated.
* `(list <VALUE EXPR>...)` - A *constructor* for a list/array that is to be evaluated.
* `(bag <VALUE EXPR>...)` - A *constructor* for a bag that is to be evaluated.
* `(unpivot <VALUE EXPR>)` - Treat a struct as a collection.
* `(as <NAME SYMBOL> <VALUE EXPR>)` - A name aliasing/binding for a value.
* `(at <NAME SYMBOL> <VALUE EXPR>)` - An ordinal or attribute name binding.
* `(cast <VALUE EXPR> (type <TYPE NAME> ...))` - `CAST` operator.
* `(is <VALUE EXPR> (type <TYPE NAME> ...))` and `(is_not <VALUE EXPR> (type <TYPE NAME> ...))` -
  Type predicate.  The type node is the same as that defined for `CAST`.
* `(simple_case ...)` and `(searched_case ...)` - `CASE` expression forms.
* `(meta <NODE EXPR> <STRUCT>)` - Metadata about an enclosing AST node, from a semantic perspective
  this is a *no-op*, but can provide diagnostic context such as line/column position.

### Operators
Currently, the following unary operators are defined in the form `(<OPNAME> <VALUE EXPR>)`:

* `+` - Unary plus.
* `-` - Unary negation.

The following binary operators are defined in the form `(<OPNAME> <LEFT EXPR> <RIGHT EXPR>)`:

* `/` - Division.
* `%` - Division remainder.
* `*` - Multiplication.
* `||` - String concatenation.
* `like` - String pattern comparison.
* `<`, `<=`, `>`, `>=`, `=`, `<>` - Comparison operators.
* `and`, `or` - Logical operators.
* `union`, `union_all`, `except`, `except_all`, `intersect`, and `intersect_all` - Set operators.

The following additional operators are defined:

* `(between <VALUE EXPR> <START EXPR> <END EXPR>)` - Interval containment.
* `(not_between <VALUE EXPR> <START EXPR> <END EXPR>)` - Interval non-containment.

### Aggregate Functions
For `(call_agg ...)`, the `<QUANTIFIER SYMBOL>` is one of `all` or `distinct`.
In most contexts, `(call_agg ...)` evaluates similar to `(call ...)` with the
exception that the input arguments are generally collections.  Within the `SELECT` list and
`HAVING` clauses, there are additional, context sensitive, semantics about how aggregate
functions work due to legacy SQL compatibility.  `(call_agg ...)` functions are **always** unary
as per SQL rules.

`(call_agg_wildcard ...)` is a special form that captures the legacy syntax form of `COUNT(*)`

### LIKE operator

The `LIKE` operator can be a binary or ternary operator depending on the input. The grammar 
allows for 

1. `<EXPR1> LIKE <EXPR2>` where `<EXPR1>` is the value we are matching on, `<EXPR2>` is the search pattern
1. `<EXPR1> LIKE <EXPR2> ESCAPE <EXPR3>` where `<EXPR1>` is the value we are matching on, `<EXPR2>` is the search pattern and `<EXPR3>` is the escape character that might be used inside `<EXPR2>` 

On top of these cases for `LIKE` we also have the negation `NOT LIKE` that is treated similarly as the other operators and their negated form. 

In the first case `LIKE` translates to an binary operator 

* `(like <NODE EXPR1> <NODE EXPR2>)` 
* `(not_like <NODE EXPR1> <NODE EXPR2>)` 

In the second case where an escape expression is provided `LIKE` translates to a ternary operator 

* `(like <NODE EXPR1> <NODE EXPR2> <NODE EXPR3>)`
* `(not_like <NODE EXPR1> <NODE EXPR2> <NODE EXPR3>)`

### Identifiers

The `id` node may include as its last element a `case_sensitive` or `case_insensitive` symbol to indicate if the 
binding is to be looked up with consideration of the identifier case or not.  If not specified, default is  
`case_sensitive`.  

### Path Component Expressions

In addition to any normal expression, a path component can be the special form `(*)` which
is the wildcard that is syntactically equivalent to the path component `[*]` and
`(* unpivot)` which is syntactically equivalent to the path component `.*`.

Only quoted and unquoted identifiers that are used as path component expressions are enclosed within a `case_sensitive` or 
`case_insensitive` annotating s-exp to indicate if the binding is to be looked up considering identifier case.  If not 
enclosed in such an s-exp, the default is case-sensitive.  

Examples:

| Expresssion | AST Representation | Explanation |
| ----------- | ------------------ | ----------- |
| `foo.bar` | `(path (id foo case_insensitive) (case_insensitive (lit "bar")))` | Case insensitive lookup of up field `bar` within `foo`. | 
| `foo."bar"` | `(path (id foo case_insensitive) (case_sensitive (lit "bar")))` | Case sensitive lookup of up field `bar` within `foo`. |
| `foo['bar']` | `(path (id foo case_insensitive) (lit "bar"))` | Case sensitive lookup of up field `bar` within `foo`. |
| `foo[bar]` | `(path (id foo case_insensitive) (id bar case_insensitive))` | If `bar` is a string, case sensitive lookup of the field identified by variable `bar` within `foo`.  If `bar` is an integer, lookup of the value at index `bar`. |
| `foo[1]` | `(path (id foo case_insensitive) (lit 1))` | Lookup of the value at index `1` within `bar`. |

Note that in the last three cases above, the path component expressions (`(lit "bar")`, `(id bar case_insensitive)` 
and `(lit 1)`) are not wrapped in a `case_[in]sensitive` node because they are expressions that have values.  When the 
value of the path component expression is a string, lookup will be case sensitiive.

### `SELECT` Expressions
The first position of the `select` node is the projection node which is marked by
`(project <PROJECT-EXPR>)` or `(project_distinct <PROJECT-EXPR>)` which must be one of:

* `(list <ITEM EXPR>...)` - Projection tuple-list, the expression node could have
  column names defined with an `(as ...)` node.
* `(value <VALUE EXPR>)` - Projects a direct value.

The second position is a `(from <SOURCE EXPR>)` which is the `FROM` clause that sources and
joins data to operate on.

All other nodes are optional and not positionally defined.  Possible nodes:

* `(where <CONDITIONAL EXPR>)` - The `WHERE` clause filter expression.
* `(group ...)` or `(group_partial ...)` - The `GROUP BY` or `GROUP PARTIAL BY` clause.
* `(having <CONDITIONAL EXPR>)` - The `HAVING` clause filter expression.
* `(limit <EXPR>)` - The `LIMIT` clause expression. 

### `SELECT *, alias.*`

Wildcard projection such as `SELECT *` or `SELECT alias.*` is reperesented with the `(project_all [<EXPR>])` node).

Examples:

| Query | AST Representation | 
| ----- | ------------------ |
| `SELECT * FROM foo` | `(select (project (list (project_all))) (from (id foo case_insensitive)))` | 
| `SELECT foo.* FROM foo` | `(select (project (list (project_all (id foo case_insensitive)))) (from (id foo case_insensitive)))` |
| `SELECT foo.bar.* FROM foo` | `(select (project (list (project_all (path (id foo case_insensitive) (case_insensitive (lit "bar")))))) (from (id foo case_insensitive)))` |

Note that the PartiQL reference parser does not allow a single `*` without dot notation to be used in 
combination with other expressions.  For example, the following are prevented: 

```sql
    SELECT *, * FROM foo
    SELECT *, 1 + 1 as c FROM foo
    SELECT *, colunm_a FROM foo
    SELECT *, bar.* FROM foo, bar
```

#### `FROM` Clause
The single `<SOURCE EXPR>` in this clause is as follows:

* Any top-level expression, where the source can be aliased with the `(as ...)` node.
  The node could also be wrapped with an `(at ...)` node.  If both `(as ...)` and `(at ...)` exist
  they are to be wrapped as `(at <NAME SYMBOL> (as ...))`
* A join expression node, has the form `(<JOIN OP SYMBOL> <SOURCE EXPR> <EXPR> <COND EXPR>)`.
  `<EXPR>` is a top-level expression node, `<COND EXPR>` is a join condition similar to the `WHERE`
  clause and is optional; not specifying it is as if `(lit true)` was provided.
  `<JOIN OP SYMBOL>` is one of:
  * `inner_join`
  * `left_join`
  * `right_join`
  * `outer_join`

For implicit cross joins (e.g. `FROM A, B`) and explicit `CROSS JOIN`, `inner_join` without a
condition is the way it is represented in the AST.

For example, the clause `FROM a, b CROSS JOIN c LEFT JOIN d ON x = y` would be translated as:

```
(from
  (left_join
    (inner_join
      (inner_join (id a) (id b))
      (id c)
    )
    (id d)
    (= (id x) (id y))
  )
)
```

#### `GROUP BY` Clause
The `(group ...)` and `(group_partial ...)` clause have one mandatory element:

* `(by <VALUE EXPR>...)` - the expressions to group by.  Each clause may be wrapped in an
  `(as ...)` node to alias the group component.

There is an optional second element that binds the group itself as a collection to a name
visible in the `HAVING` clause and the `SELECT` projection clause.  It is specified as:

* `(name <NAME SYMBOL>)` - the name to bind the group collections to.

For example, the clause `GROUP PARTIAL BY age GROUP AS age_group`:

```
(group_partial
  (by (id age))
  (name age_group)
)
```

### `PIVOT` Expressions
The `(pivot ...)` is very similar to the `(select ...)` form with the only difference that the 
first element is **not** a `(project ...)` node.  Instead, the first node is a
`(member <NAME EXPR> <VALUE EXPR>)` form, where `<NAME EXPR>` is the computed
field name of the struct and `<VALUE EXPR>` is the computed value.

The semantics of `PIVOT` are essentially that of `SELECT`, but with the projection being
struct members.

For example, the expression `PIVOT value AT name FROM data`, translates to:

```
(pivot
  (member (id name) (id value))
  (from (id data))
)
```

### `CAST` Expressions
The first position of a `cast` node is the value expression node to be casted.  The
second position is a `type` node which consists of a name and integral parameters for said
type specification.

Example:

```
(cast
  (lit 5)
  (type character_varying 10)
)
```

### `CASE` Expressions
There are two supported forms of `CASE` expressions, the first is the `simple_case` which is
similar to `switch` in C-like languages, and the second is `searched_case` which is
more like a sequence of `if`/`else if`/`else` branches.

Both forms have at least one `(when <VALUE EXPR> <RESULT EXPR>)` nodes and an optional
`(else <RESULT EXPR>)` node.

Simple `CASE` expressions have a common expression at the first position, the result
of which are compared to the result of each `<VALUE EXPR>` in the `when` nodes.

Example, for `CASE name WHEN 'zoe' THEN 1 END`:

```
(simple_case
  (id name)
  (when
    (lit "zoe")
    (lit 1)
  )
)
```

Another example, for `CASE name WHEN 'zoe' THEN 1 WHEN 'kumo' THEN 2 ELSE 0 END`:

```
(simple_case
  (id name)
  (when
    (lit "zoe")
    (lit 1)
  )
  (when
    (lit "kumo')
    (lit 2)
  )
  (else (lit 0))
)
```

Searched `CASE` expressions have no such common expression and each `<VALUE EXPR>` in the `when`
nodes are Boolean expressions for the condition.

Example for `CASE WHEN name = 'zoe' THEN 1 END`:

```
(searched_case
  (when
    (= (id name) (lit "zoe"))
    (lit 1)
  )
)
```
Another example, for `CASE WHEN name = 'zoe' THEN 1 WHEN name = 'kumo' THEN 2 ELSE 0 END`:

```
(searched_case
  (when
    (= (id name) (lit "zoe"))
    (lit 1)
  )
  (when
    (= (id name) (lit "kumo"))
    (lit 2)
  )    
  (else (lit 0))
)
```


### Meta Nodes
Meta nodes are no-ops from a semantic perspective.  They provide meta-data about node the
encapsulate.  Meta nodes have a single child node at the first position and a single struct
providing the contextual metadata.  These nodes can appear anywhere, and currently provide
information about source line/column position.

The following fields within a meta node's struct is defined currently:

* `line` - The one-based line number of where the enclosed node is located in the program source.
* `column` - The one-based column number of where the enclosed node is located in the program source.

Example:

```
(meta
  (lit 5)
  {line: 1, column: 1}
)
```

The above would indicate the the integer literal `5` was located at line 1, column 1.

### TODO
* Support `ORDER BY`.

# PartiQL AST

By AST in this document we refer to the data structure used to represent an PartiQL query. 
This is also the version of the AST provided to clients that consume the PartiQL AST. 

## Notation

### Abusing Grammar notation

We borrow notation used for grammars to denote data structures and the alternatives for each data structure. 
You can think of production rule as a sum type, e.g., 

```
LIST ::= `null` | `(` `cell` INTEGER LIST `)`
```

defines a linked list of `INTEGER`s as being one of 

* the empty list, denoted by `null`, or, 
* the list of 1 or more integers, e.g., `(cell 1 (cell 2 null))` is the list holding the numbers 1 and 2 in that order.  

Terminals are in lowercase and surrounded with backticks (\`). Terminals denote literals. 

Non-Terminals are in all caps and denote abstract type names. 

The parallel bar `|` denotes alternatives. 

Square brackets `[ ]` denote optional elements 

### Ellipsis

Ellipsis `...` mean 0 or more of the element *preceding* the ellipsis. We
use parenthesis to denote a composite element 

* `X ...`  0 or more `X`
* `(op X Y) ...` 0 or more `(op X Y)`, i.e., `(op X Y) (op X Y) (op X Y)`

### Referencing the Ion Text Specification

`ITS(<type>)` refers to the [Ion Text Specification](https://amzn.github.io/ion-docs/spec.html)
for the Ion `type` passed as an argument. For example, `ITS(boolean)`
means lookup the section on `boolean` in the Ion Text Specification. 

The AST is a valid Ion S-expression, `SEXP`. For the purposes of this
documentation we use the following grammar for Ion and it's `SEXP`.
The grammar *should* be a refactoring of Ion's grammar that allows us to create more convenient
groupings of the Ion Text grammar for our purposes.

```
ION_VALUE ::=
              ATOM
            | SEXP
            | LIST
            | STRUCT

ATOM      ::=
              BOOL
            | NUMBER
            | TIMESTAMP
            | STRING
            | SYMBOL
            | BLOB
            | CLOB
            | NULL


BOOL      ::= ITS(boolean)
NUMBER    ::=
              INTEGER
            | FLOAT
            | DECIMAL

INTEGER   ::= ITS(integer)
FLOAT     ::= ITS(float)
DECIMAL   ::= ITS(decimal)

TIMESTAMP ::= ITS(timestamp)
STRING    ::= ITS(string)
SYMBOL    ::= ITS(symbol)
BLOB      ::= ITS(blob)
CLOB      ::= ITS(clob)
NULL      ::= ITS(null)

LIST      ::= ITS(list)
STRUCT    ::= ITS(structure)

SEXP      ::= `(` ION_VALUE ... `)`
```

In the case of `SEXP` we refer to the name immediately following the `SEXP` 's open parenthesis as its **tag**.
For example the `SEXP` `(lit 2)` is tagged with the symbol `lit`.

## PartiQL AST data definition 

Starting with version 1 of the PartiQL AST, each AST must be wrapped in an `ast` node:

```
(ast (version 1)
     (root EXP))
```

If the top-most node of the AST does not have the tag `ast` then we assume that it is a 
[legacy version 0 AST](README-AST-V0.md).

Within `(root ...)` we represent a valid PartiQL expression as a tree (`SEXP`) of **optionally** wrapped nodes called 
**term**s.

```
TERM ::= `(` `term` `(` `exp` EXP `)`
                    `(` `meta` META ...`)` `)`
```

The wrapper `term` contains 2 sub `SEXP` 

* the PartiQL expression or expression fragment being wrapped tagged with `exp` 
* the meta information for the PartiQL expression or expression fragment tagged with `meta`

The `meta` child node of `term` is optional but usually holds information on the location of the expression 
in the original query. The definition of `META` is purposely left open to allow for the addition of meta 
information on the AST by consumers of the PartiQL such as query rewriters.

The PartiQL implementation shall use the `$` prefix for all of its meta node tags.  A naming convention such
as reverse domain name notation should be used for meta node tags introduced by consumers of the PartiQL AST
to avoid naming conflicts.

```
META ::= LOC | META_INFO

LOC  ::= `(` `$source_location` `(` `{` `line_num` `:` INTEGER`,` `char_offset` `:` INTEGER `}` `)` `)`

META_INFO ::= `(` SYMBOL `(` ION_VALUE ... `)` `)`
```

For example:

```
(term (exp (lit customerId))
      (meta ($source_location ({line_num: 10, char_offset: 12}))
            (type_env ({ customerId : Symbol })))
```

Captures the literal `customerId` that appears on line `10` at character offset `12` in the original 
query. The term's meta information also captures the type environment
for this expression. In this example the values bound to `customerId`
are expected to be of type `Symbol`.

`EXP` defines the alternatives for an `SEXP` that that can appear inside a `term` 's `exp` tagged `SEXP`. 
**For readability, any nested `term`-wrapped sub components of `EXP` are not shown in this definition.**

The `term` wrapper is optional and so the previous example can be stripped down to the semantically equivalent:

```
    (lit customerId)
```

### PartiQL AST "Grammar" 

```
EXP ::= 
        `(` `lit` ION_VALUE `)`
      | `(` `missing` `)`
      | `(` `id` SYMBOL CASE_SENSITIVITY `)`
      
      | `(` `struct` KEY_VALUE ... `)`
      | `(` `list` EXP ... `)`
      | `(` `bag`  EXP ... `)`
      
      | `(` NARY_OP EXP EXP ... `)`
      | `(` TYPED_OP EXP TYPE `)` `)`
      
      | `(` `path` EXP PATH_ELEMENT... `)`
      
      | `(` `call_agg` SYMBOL QSYMBOL EXP `)`
      | `(` `call_agg_wildcard` `count` `)` 
      
      | `(` `simple_case` EXP WHEN [WHEN ...] [ELSE] `)`
      | `(` `searched_case` WHEN [WHEN ...] [ELSE] `)`
      
      | `(` `select` SELECT_LIST
                     FROM_SOURCE
                     [ '(' `where' EXP `)` ]
                     [ GROUP_CLAUSE ]
                     [ `(` `having` EXP `)` ]
                     [ `(` `limit` EXP `)` ]

      | `(` `pivot` MEMBER
                    FROM_SOURCE
                    [ '(' `where' EXP `)` ]
                    [ GROUP_CLAUSE ]
                    [ `(` `having` EXP `)` ]
                    [ `(` `limit` EXP `)` ]

LENGTH ::= INTEGER


TYPE ::= `(` `type` TYPE_NAME [LENGTH [LENGTH]] `)` 
//NOTE: the two optional length arguments above are meant to capture the length or precision and scale
//arguments as defined by the SQL-92 spcification for certain data types.  While space exists in the AST
//for them, they do not apply to the Ion type system and therefore are ignored at during compilation and
//evaluation times.

CASE_SENSITIVITY ::= `case_sensitive` | `case_insensitive` 

KEY_VALUE ::= '(' EXP EXP ')'

TYPE_NAME ::= `boolean` | `smallint` | `int`  | `float` | `decimal`
              | `numeric` | `double_precision` | `character` | `character_varying` | `string` 
              | `symbol` | `varchar` | `list` | `bag` | `struct` 
              | `clob` | `blob` | `timestamp` | `symbol` | `null` 
              | `missing` | `null` | `missing`

TYPED_OP ::= `cast` | `is`

NARY_OP ::= // NOTE:  When used with an arity of 1, the operators +, - and NOT function as unary operators.
          `+` | `-` | `not` | `/` | `*` | `%` 
        | `<` | `<=` | `>` | `>=` | `<>` 
        | `and | `or` | `||` | `in` | `call` 
        | 'between` | `like` | `is`
        | `union | `union_all` 
        | `intersect` | `intersect_all` 
        | `except` | `except_all` 

PATH_ELEMNT ::= `(` `path_element` PATH_EXP CASE_SENSITIVITY `)`
PATH_EXP ::= EXP 
    | `(` `star` `)`
    | `(` `star `unpivot` `)`

MEMBER ::= `(` `member` EXP EXP `)`

QSYMBOL ::= `distinct` | `all`

SELECT_LIST ::= `(` PROJECT SELECT_PROJECTION `)`

PROJECT ::= `project` | `project_distinct` 

SELECT_PROJECTION ::= 
           | `(` `value` EXP `)`
           | `(` `list` SELECT_LIST_ITEM... `)` 
           | `(` `list` STAR `)` 

SELECT_LIST_ITEM ::= EXP
    | `(` `as` SYMBOL EXP `)`
    | `(` `path_project_all` EXP `)`

FROM_SOURCES ::= `(` `from` FROM_SOURCE `)`

JOIN_TYPE ::= `inner_join` | `outer_join` | `left_join` | `right_join`

FROM_SOURCE_TABLE ::= EXP
    | `(` `as` SYMBOL EXP `)`
    | `(` `at` SYMBOL `(` `as` SYMBOL EXP `)` `)`
    | `(` `unpivot` EXP `)`
    
FROM_SOURCE ::= FROM_SOURCE_TABLE
    | `(` JOIN_TYPE FROM_SOURCE EXP `)`

GROUP_FULL ::= `group` 
GROUP_PARTIAL ::= `group_partial`
GROUP_ALL ::= `group_all`
GROUP_BY_EXP ::= EXP | `(` `as` SYMBOL EXP `)`
GROUP_KIND ::= GROUP_FULL | GROUP_PARTIAL

GROUP_CLAUSE  ::= 
    `(`  GROUP_KIND
         //NOTE:  the `by` node cannot be wrapped in a term (GROUP_BY_EXPs however *can* be wrapped in a term).
        `(` `by` GROUP_BY_EXP GROUP_BY_EXP... `)` 
         //NOTE:  the `name` node *can* be wrapped in a term
        [ `(` `name` SYMBOL `)` ] ')'
    | `(` GROUP_ALL SYMBOL `)`
    
                    
WHEN ::= `(` `when` EXP EXP `)`
ELSE ::= `(` `else` EXP `)`

```

## Examples

Each example lists:

1. The query as a string as the title 
1. The version 0 AST 
1. `=>`
1. The version 1 AST 

The examples show some `meta`/`term` nodes based on what meta information is available today. 
The goal is to annotate all nodes with meta information. 

### E1 : `select * from a`

```
(select (project (*))
    (from (meta (id a case_insensitive)
                    {line:1, column:15})))
```

`=>`

```
(ast
    (version 1)
    (root
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp (star))
                                (meta ($source_location ({line_num:1,char_offset:8}))))))
                    (from
                        (term
                            (exp (id a case_insensitive))
                            (meta ($source_location ({line_num:1,char_offset:15}))))))))))


```

### E2 : `select a as x from a`
```
(select (project (list (meta (as x (meta (id a case_insensitive) 
                                         {line:1, column:8 }))
                             { line:1, column:13 })))
        (from (meta (id a case_insensitive) 
                    { line:1, column:20 })))
```

`=>`

```
(ast
    (version 1)
    (root
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (as
                                        x
                                        (term (exp (id a case_insensitive))
                                            (meta ($source_location ({line_num:1,char_offset:8}))))))
                                (meta ($source_location ({line_num:1,char_offset:13}))))))
                    (from
                        (term
                            (exp (id a case_insensitive))
                            (metaJ ($source_location ({line_num:1,char_offset:20}))))))))))
```

### E3 : `select x from a as x`

```
(select (project (list (meta (id x case_insensitive)
                             { line:1, column:8 })))
        (from (meta (as x (meta (id a case_insensitive) 
                                { line:1, column:15 })) 
                    { line:1, column:20 })))

```

`=>`

```
(ast
    (version 1)
    (root
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp (id x case_insensitive))
                                (meta ($source_location ({line_num:1,char_offset:8}))))))
                    (from
                        (term
                            (exp
                                (as
                                    x
                                    (term
                                        (exp (id a case_insensitive))
                                        (meta ($source_location ({line_num:1,char_offset:15}))))))
                            (meta
                                ($source_location ({line_num:1,char_offset:20}))))))))))

```


### E4 : `select AVG(a.id) from a`

```
(select (project (list (meta (call_agg avg all (path (meta (id a case_insensitive)
                                                           { line:1, column:12 }) 
                                                     (meta (case_insensitive (meta (lit "price")
                                                                                   { line:1, column:14 })) 
                                                           { line:1, column:14 })))
                             { line:1, column:8 })))
        (from (meta (id a case_insensitive)
                    { line:1, column:26 })))
```

`=>`

```
(ast (version 1)
    (root
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp
                                    (call_agg avg all
                                        (term
                                            (exp
                                                (path
                                                    (term
                                                        (exp (id a case_insensitive))
                                                        (meta ($source_location ({line_num:1,char_offset:12}))))
                                                    (path_element
                                                        (term
                                                            (exp (lit "id"))
                                                            (meta ($source_location ({line_num:1,char_offset:14}))))
                                                        case_insensitive)))
                                            (meta ($source_location ({line_num:1,char_offset:12}))))))
                                (meta ($source_location ({line_num:1,char_offset:8}))))))
                    (from
                        (term
                            (exp (id a case_insensitive))
                            (meta ($source_location ({line_num:1,char_offset:23}))))))))))
```


### E5 : `select * from a where a.price > 100`

```
(select (project ( *)) 
        (from (meta (id a case_insensitive)
                    { line:1, column:15 }))
        (where (meta (> ( path (meta (id a case_insensitive)
                                     { line:1, column:23 }) 
                               (meta (case_insensitive (meta (lit "price")
                                                             { line:1, column:25 })) 
                                     { line:1, column:25 })) 
                        (meta (lit 100) 
                              { line:1, column:33 }))
                     { line:1, column:31 })))
```

`=>`

```
(ast (version 1)
    (root
        (term
            (exp
                (select
                    (project
                        (list
                            (term
                                (exp (star))
                                (meta
                                    ($source_location ({line_num:1,char_offset:8} ))))))
                    (from
                        (term
                            (exp (id a case_insensitive))
                            (meta ($source_location ({line_num:1,char_offset:15} )))))
                    (where
                        (term
                            (exp
                                (>
                                    (term
                                        (exp
                                            (path
                                                (term
                                                    (exp (id a case_insensitive))
                                                    (meta ($source_location ({line_num:1,char_offset:23} ))))
                                                (path_element
                                                    (term
                                                        (exp (lit "price"))
                                                        (meta ($source_location ({line_num:1,char_offset:25} ))))
                                                    case_insensitive)))
                                        (meta ($source_location ({line_num:1,char_offset:23} ))))
                                    (term
                                        (exp (lit 100))
                                        (meta ($source_location ({line_num:1,char_offset:33} ))))))
                            (meta ($source_location ({line_num:1,char_offset:31} ))))))))))
```

### E6 : `SELECT a, b FROM data GROUP BY a, b`
```
(term
    (exp
        (select
            (project
                (list
                    (term
                        (exp
                            (id a case_insensitive)))))
            (from
                (term
                    (exp
                        (id data case_insensitive))))
            (group
                (by
                    (term
                        (exp
                            (id a case_insensitive)))
                    (term
                        (exp
                            (id b case_insensitive))))))))
```
                            
### E7 : `SELECT a FROM data GROUP BY a as x GROUP BY g`

```
(term
    (exp
        (select
            (project
                (list
                    (term
                        (exp
                            (id a case_insensitive)))))
            (from
                (term
                    (exp
                        (id data case_insensitive))))
            (group
                (by
                    (term
                        (exp
                            (as
                                x
                                (term
                                    (exp
                                        (id a case_insensitive)
                                    ))))))
                (term
                    (exp 
                        (name g)))))))
```
                             


## Callouts 

Important modifications/additions regarding changes from version 0: 

1. Some nodes had 2 versions one for the node e.g., `is` and one for the nodes complement (negation) e.g. `is_not` 
    * Removed the complements in favour of a nested `(not ..)` expression. The nodes affected are `like`, `is`,
1. The `*` was previously used in multiple places to denote different semantic meanings.  This is no longer the case.
    * `SELECT *` is now denoted with the tag `star`
    * `SELECT exp.*` is now denoted with the tag `path_project_all`
    * `exp[*]` and `exp.*` is still denoted with the `(*)` and `(* unpivot)` s-expressions.  This is unchanged from version 0.
1. Path elements (arguments after the first in a path expression) are now contained in a `path_element` node following this 
pattern: `(path_element EXP CASE_SENSITIVITY)`.  

## ToDo

Need to address/flesh out

1. Order by (https://github.com/partiql/partiql-lang-kotlin/issues/47)

