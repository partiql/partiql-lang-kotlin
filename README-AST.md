# Ion SQL AST
The `IonSqlParser` implementation generates an AST representation in Ion based on
s-expressions.

The general format is as follows:

```
(name ...)
```

Where `name` is the AST node name, which can be one of:

* `(lit <ION VALUE>)` - a verbatim (quoted) Ion value.
* `(id <NAME>)` - an identifier.
* `(<OPERATOR> ...)` - A binary or unary operator.
* `(select ...)` - A `SELECT-FROM-WHERE` expression.
* `(path <VALUE EXPR> <PATH COMPONENT>...)` - A path (which is used for normal dotted name resolution).
* `(call <NAME> <VALUE EXPR>...)` - A function invocation.
* `(struct <NAME EXPR> <VALUE EXPR> ...)` - A *comprehension* for a tuple/struct that
  is to be evaluated.
* `(list <VALUE EXPR>...)` - A *comprehension* for a list/array that is to be evaluated.
* `(as <NAME> <VALUE EXPR>)` - A name aliasing/binding.
* `(meta <NODE> ...)` - Metadata about an enclosing AST node, from a semantic perspective
  this is a *no-op*, but can provide diagnostic context such as line/column position.
