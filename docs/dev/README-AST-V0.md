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
