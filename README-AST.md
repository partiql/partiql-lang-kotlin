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
* `(pivot ...)` - Convert a collection into a tuple/struct.
* `(path <VALUE EXPR> <PATH COMPONENT EXPR>...)` - A path (which is used for normal dotted name resolution).
* `(call <NAME SYMBOL> <VALUE EXPR>...)` - A function invocation.
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

## Operators
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

## Path Component Expressions
In addition to any normal expression, a path component can be the special form `(*)` which
is the wildcard that is syntactically equivalent to the path component `[*]` and
`(* unpivot)` which is syntactically equivalent to the path component `.*`. 

## `SELECT` Expressions
The first position of the `select` node is the projection node which is marked by
`(project <PROJECT-EXPR>)` or `(project_distinct <PROJECT-EXPR>)` which must be one of:

* `(*)` - Tuple wildcard projection, mapping to `SELECT * ...`
* `(list <ITEM EXPR>...)` - Projection tuple-list, the expression node could have
  column names defined with an `(as ...)` node.
* `(value <VALUE EXPR>)` - Projects a direct value.

The second position is a `(from <SOURCE EXPR>...)` which is the `FROM` list, each element could have
source names defined with the `(as ...)`.  The `<SOURCE EXPR>` could also be wrapped with
an `(at ...)` node.  If bot `(as ...)` and `(at ...)` exist they are to be wrapped as
`(at <NAME SYMBOL> (as ...))`

All other nodes are optional and not positionally defined.  Possible nodes:

* `(where <CONDITIONAL EXPR>)` - The `WHERE` clause filter expression.
* `(group ...)` or `(group_partial ...)` - The `GROUP BY` or `GROUP PARTIAL BY` clause.
* `(having <CONDITIONAL EXPR>)` - The `HAVING` clause filter expression.
* `(limit <EXPR>)` - The `LIMIT` clause expression. 

### `GROUP BY` Clause
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

## `PIVOT` Expressions
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
* Support `ORDER BY`.
