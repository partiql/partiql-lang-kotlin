# Ion SQL AST
The `IonSqlParser` implementation generates an AST representation in Ion based on
s-expressions.

The general format is as follows:

```
(name ...)
```

Where `name` is the AST node name, which can be one of:

* `(lit <ION VALUE>)` - a verbatim (quoted) Ion value.
* `(missing)` - The literal `MISSING` value.
* `(id <NAME SYMBOL>)` - an identifier.
* `(<OPERATOR SYMBOL> ...)` - A binary or unary operator.
* `(select ...)` - A `SELECT-FROM-WHERE` expression.
* `(path <VALUE EXPR> <PATH COMPONENT EXPR>...)` - A path (which is used for normal dotted name resolution).
* `(call <NAME SYMBOL> <VALUE EXPR>...)` - A function invocation.
* `(struct <NAME EXPR> <VALUE EXPR>...)` - A *comprehension* for a tuple/struct that
  is to be evaluated.
* `(list <VALUE EXPR>...)` - A *comprehension* for a list/array that is to be evaluated.
* `(as <NAME SYMBOL> <VALUE EXPR>)` - A name aliasing/binding.
* `(cast <VALUE EXPR> (type <TYPE NAME> ...))` - `CAST` operator.
* `(simple_case ...)` and `(searched_case ...)` - `CASE` expression forms.
* `(meta <NODE EXPR> <STRUCT>)` - Metadata about an enclosing AST node, from a semantic perspective
  this is a *no-op*, but can provide diagnostic context such as line/column position.

## Operators
Currently, the following unary operators are defined in the form `(<OPNAME> <VALUE EXPR>)`:

* `+` - Unary plus.
* `-` - Unary negation.

The following binary operators are defined in the form `(<OPNAME> <LEFT EXPR> <RIGHT EXPR>)`:

* `/` - Division.
* `%` - Division remainder.
* `*` - Multplication.
* `||` - String concatenation.
* `like` - String pattern comparison.
* `<`, `<=`, `>`, `>=`, `=`, `<>` - Comparison operators.
* `and`, `or` - Logical operators.
* `is`, `is_not` - Identity comparison.
* `union`, `union_all`, `except`, `except_all`, `intersect`, and `intersect_all` - Set operators.

The following additional operators are defined:

* `(between <VALUE EXPR> <START EXPR> <END EXPR>)` - Interval containment.

## `SELECT` Expressions
The first position of the `select` node is the projection node which is marked by
`(project <PROJECT-EXPR>)` or `(project_distinct <PROJECT-EXPR>)` which must be one of:

* `(*)` - Tuple wildcard projection, mapping to `SELECT * ...`
* `(list <ITEM EXPR>...)` - Projection tuple-list, the expression node could have
  column names defined with an `(as ...)` node.
* `(values <VALUE EXPR>)` - Projects a direct value.

The second position is a `(from <SOURCE EXPR>)` which is the `FROM` list, each element could have
source names defined with the `(as ...)` node.

All other nodes are optional and not positionally defined.  Possible nodes:

* `(where <CONDITIONAL EXPR>)` - The `WHERE` clause filter expression.
* `(limit <EXPR>)` - The `LIMIT` clause expression. 

## `CAST` Expressions
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

## `CASE` Expressions
There are two supported forms of `CASE` expressions, the first is the `simple_case` which is
similar to `switch` in C-like languages, and the second is `searched_case` which is
more like a sequence of `if`/`else if`/`else` branches.

Both forms have at least one `(when <VALUE EXPR> <RESULT EXPR>)` nodes and an optional
`(else <RESULT EXPR>)` node.

Simple `CASE` expressions have a common expression at the first position, the result
of which are compared to the result of each `<VALUE EXPR>` in the `when` nodes.

Example for `CASE name WHEN 'zoe' THEN 1 END`:

```
(simple_case
  (id name)
  (when
    (lit "zoe")
    (lit 1)
  )
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

## Meta Nodes
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

## TODO
* Support `JOIN` constructs.
* Support `GROUP BY` and `HAVING`.
* Support `SELECT` projection modifiers (e.g. `DISTINCT`)
* Support `ORDER BY`
