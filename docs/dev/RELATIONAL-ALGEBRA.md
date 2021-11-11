# PartiQL’s Relational Algebra

This document is a work-in-progress document that describes the PartiQL’s equivalent of the relational algebra that will
be used by all implementations of PartiQL owned by the PartiQL team. This document is early in its life, and currently
only lays out some basic details of the relational algebra and 4 preliminary operators.

## Notation

We will use a Ion text syntax with two extensions:

* `<< >>` will be used to denote a bag.
* `<{ variable_1: value_1, variable_2: value_2, variable_n: value_n, ... }>` will be used to denote a *bindings tuple*,
  described later in this document.

We will also wrap binding tuples in Ion’s bag or list notation, i..e  `<< <{ ... }>, ... >>`, `[ <{ ... }>, ... ]`, to
denote a bag or list of binding tuples, respectively.

## What is a Bindings Collection?

_PartiQL considers a bindings collection to_ _be a_ _bag or list of_ *_binding tuples_ and a binding tuple is
a map of variable names and their values. Bindings collections take the place of 
[relations](https://en.wikipedia.org/wiki/Relation_(database)), which are employed heavily by traditional RDBMs. The
departure is needed to account for schema-less and schema-optional scenarios, where assumptions cannot be made about the
presence of certain fields.

It is important to know that binding tuples are *distinct* from values. It is not possible to directly substitute a
binding tuple for a value or vice-versa, although facilities to convert between them are provided (see 
[Operator: scan](#operator-scan) and [Operator: map_values](#operator-map_values)).

The distinction between binding tuples and values is needed because it simplifies rewriting the algebra by reducing
total number of operators that are applicable within a given context, and constrains the possible inputs and outputs of
those operators.

## What is a Relational Algebra?

The definition used by PartiQL is: a well-defined set of operators (i.e. functions) that manipulate bindings
collections. Such functions can include: projection, selection (filtering), Cartesian products, joins, and more. The
algebra defined here is based on [Codd’s relational algebra](https://en.wikipedia.org/wiki/Relational_algebra).

## Requirements for PartiQL’s Relational Algebra

A relational algebra must:

* Be able to express any PartiQL query.
* Be easily rewritten to effect query optimization and planning.

## Logical vs Physical Operators

This document does not (yet) attempt to define any physical operators to perform operations such
as [full table scans, range scans, indexed range scans](https://code.amazon.com/packages/PenningPlaygroundJavaKotlin/blobs/c238aed25a72372315ef70c24bc2706ef470bfc6/--/schemas/penning.ion#L475-L489)
, etc. However, it is worthwhile to describe some properties of physical operators, given their similarities to logical
operators.

Like logical operators, physical operators will always return bindings collections. Unlike logical operators, physical
operators may:

* Access a storage layer or other data source.
* Be semantically redundant with logical or other physical operators while specifying different implementation
  strategies, allowing the best one to be selected for a given scenario.
* Combine multiple logical operations for the purpose of enhanced performance.

## Algebra Overview

The following operators are defined:

* [Operator: scan](#operator-scan): converts a PartiQL value to a binding collection.
* [Operator: cross_join](#operator-cross_join): computes a Cartesian product of two bindings collections.
* [Operator: filter](#operator-filter): selects the subset of rows of an input binding collection that satisfies a
certain condition.
* [Operator: map_values](#operator-map_values): converts a binding collection into a value collection.

**This document does not yet define all the operators that we will need.**

See [Appendix - Other Operators To Be Defined During Steel Threading](#appendix---other-operators-to-be-defined-during-steel-threading) 
for a preliminary list.

### Relational Algebra Grammar

```
// Nodes that work on bindings collections.
bindings_expr ::=
      (scan <value_expr> <as_alias> <at_alias> | null <by_alias> | null)
    | (cross_join <bindings_expr> <bindings_expr>))
    | (filter <value_expr> <bindings_expr>)
    | ... future operators go here ...

// Nodes that work on values.
value_expr ::=
      ... arithmetic, function calls, `when` expressions, etc go here ...
      ... see note ...
    | (map_values <value_expr> <bindings_expr>)    
```

Note: a detailed discussion of `value_expr` is beyond the scope of this document, but some explanation is
required:  `value_expr` will contain constructs to represent all PartiQL expressions that appear in the PartiQL AST
today. The key difference is that there will be no SFW queries (i.e. no `(select ...)` node) in the `value_expr` as the
query semantics of that are now represented by the far easier to rewrite  `bindings_expr`.

## Schema Used With Examples Below

The examples used below assume a database created with this script exists:

```
CREATE TABLE Foo
(
    FooId INT NOT NULL PRIMARY KEY,
    FooName VARCHAR(50)
);

INSERT INTO Foo VALUES(100, 'Foo #1');
INSERT INTO Foo VALUES(200, 'Foo #2');

CREATE TABLE Bar
(
    BarId INT NOT NULL PRIMARY KEY,
    BarName VARCHAR(50)
);

INSERT INTO Bar VALUES(300, 'Bar #1');
INSERT INTO Bar VALUES(400, 'Bar #2');
```

## Bridges Between Bindings Collections and Values

### Operator: `scan`

```
bindings_expr ::=
      ... 
    | (scan <value_expr> <as_alias> <at_alias> | null <by_alias> | null)
```

Evaluates a `value_expr` and converts the result to a bindings collection. The returned collection’s type (bag or
struct) is bag if the expression returned a bag or struct of values. The result is a bindings list if the expression
returned a list of values. If the expression returns a value of any other type, a binding list containing a single
binding tuple will be returned.

#### `scan` Elements

* `<value_expr>`: The expression to be evaluated.
* `<as_alias>`: Every value in the collection is bound to this name in each binding tuple in the returned bindings
  collection.
* `<at_alias>`: If specified, the integer index or symbol name of the current value in the containing is bound to this
  name. This applies to values contained within lists, but not values in bags. If this is specified and `<value_expr>`
  returns a bag, the name is bound to `MISSING`.
* `<by_alias>`: If specified, the current unique record identifier is bound to this name. The type of the unique record
  identifier is implementation defined and supplied. It is typically a UUID or similar value that is assigned by the
  storage layer to the record on insertion. If this is specified and the implementation has not supplied a unique record
  identifier, the name will be bound to `MISSING`.

#### Example #1 - Conversion of a values bag into a bindings bag

```
// Equivalent to: FROM Foo AS f AT idx BY uid
(scan (id Foo) f idx uid)
```

Results in the following bindings bag:

```
<<
  <{ f: { FooId: 100, FooName: 'Foo #1' }, idx: MISSING, uid: 'c7559f84-3768' }>,
  <{ f: { FooId: 200, FooName: 'Foo #2' }, idx: MISSING, uid: 'd2bdb79e-4432' }>,
>>
```

Note that `idx` is bound to `MISSING` in this case because `(id Foo)` returns a bag, which does not have a notion of an
index. However, `uid` is bound to a unique id assigned by the storage layer for the given record.

#### Example #2 - Conversion of a values list into a bindings list

```
// Equivalent to: 
// FROM `[{x: 1}, {x: 2}]` AS f AT idx BY uid
(scan (lit [{x: 1}, {x: 2}]) f idx uid) 
```

The returned bindings collection is below.

```
[
    <{ f: { x: 1 }, idx: 0, uid: MISSING }>,
    <{ f: { x: 2 }, idx: 1, uid: MISSING }>
]
```

Note that `idx` is bound to the index of the current value, and that `uid` is bound to `MISSING` because the values in
the list returned by `<value_expr>` do not have unique record identifiers.

#### Example #3 - Conversion of a scalar into a bindings list

```
// Equivalent to: SELECT * FROM 42 AS f AT idx BY uid
(scan (lit 42) f idx uid)
```

The returned bindings bag is below.

```
<<
    <{ f: 42, idx: MISSING, uid: MISSING }>
>>
```

Note that the scalar has been coerced to a singleton bag before being converted into a bindings collection. Also note
that, as demonstrated here, the value bound to `f` need not be a struct--it can be any value.

TODO: add example showing `scan` with null `AT` and `BY` variables.

### Operator: `map_values`

```
value_expr ::=
       ...
    | (map_values <value_expr> <bindings_expr>)  
```

`map_values` performs a map operation over the `bindings_expr`, invoking `value_expr` within the context of each binding
tuple in succession. The type of bindings collection returned by `map_values` (list or bag) is the same as the
collection returned from `bindings_expr`.

#### Example #1 - `SELECT VALUE`

The query:

```
SELECT VALUE f.FooId
FROM Foo AS f
```

Is represented with the following logical algebra:

```
(map_values
    (path (id f) (lit FooId))
    (scan (id Foo) f null null))
```

And returns:

```
<< 100, 200 >>
```

#### Example #2 - `SELECT` (SQL-92)

The query:

```
SELECT
    f.FooId AS fid
    f.FooName AS fname
FROM Foo AS f
```

Is represented with the following logical algebra:

```
(map_values
    (struct
        (expr_pair (lit fid) (path (id f) (lit FooId)))
        (expr_pair (lit fname) (path (id f) (lit FooName))))
    (scan (id Foo) f null null))
```

And returns:

```
<< 
    { fid: 100, fname: "Foo #1" },
    { fid: 200, fname: "Foo #2" }
>>
```

#### Example #3: `SELECT *` - single from source

The query:

```
SELECT * FROM Foo AS f
```

Is represented with the following logical algebra:

```
(map_values
    (id f)
   (scan (id Foo) f null null))
```

The output is the entire `Foo` table contents shown earlier in the document and is omitted for brevity.

#### Example #4: `SELECT *` - multiple from sources

To accomplish this, a `struct_concat` function is required that merges two struct values, i.e.:

```
struct_concat({'a': 1}, {'a': 2, b: 3}) → {'a': 1, 'a': 2, b: 3 }
```

With that defined we can represent the query:

```
SELECT * FROM Foo AS f, Bar AS b
```

With the following relational algebra:

```
(map_values
    (call struct_concat (id f) (id b))
    (cross_join 
       (scan (id Foo) f null null)
       (scan (id Bar) b null null)))
```

The output is the Cartesian product of the `Foo` and `Bar` tables defined earlier in the document and is omitted for
brevity.

## Operators That Accept and Return Bindings Collections

### Operator: `cross_join`

```
bindings_expr ::=
       ... 
    | (cross_join <bindings_expr> <bindings_expr>))
```

Computes the Cartesian product of both binding expressions, returning a single bindings tuple for every pairwise
combination. In pseudo-code:

```
for l in bindings_expr_1
    for r in bindings_expr_2
        output the concatenation of binding tuples l and r
    next
next
```

Since binding tuples must not contain duplicate names, a compile-time exception is raised if `l` or `r` contain the same
binding name.

```
// Equivalent to: FROM Foo AS f, Bar AS b
(cross_join
    (scan (id Foo) f)
    (scan (id Bar) b)
)
```

The resulting bindings collection is the Cartesian product:

```
<<
    <{ 
        f: { FooId: 100, FooName: 'Foo #1' }, 
        b: { BarId: 300, BarName: 'Bar #1' } 
    }>,
    <{ 
        f: { FooId: 100, FooName: 'Foo #1' }, 
        b: { BarId: 400, BarName: 'Bar #2' } 
    }>,
    <{ 
        f: { FooId: 200, FooName: 'Foo #2' }, 
        b: { BarId: 300, BarName: 'Bar #1' } 
    }>,
    <{
        f: { FooId: 200, FooName: 'Foo #2' }, 
        b: { BarId: 400, BarName: 'Bar #2' } 
    }>
>>
```

### Operator: `filter`

```
bindings_expr ::=
      ...
    | (filter <value_expr> <bindings_expr>)
```

Inputs a bindings collection returned from `<bindings_expr>` and outputs a bindings collection containing only those
binding tuples that satisfy `<value_expr>`. If `<value_expr>` returns a value that is not a boolean value: in strict mode
a runtime exception occurs, in permissive mode, the value is coerced into `MISSING`, which is then considered the same
as if it had evaluated to `false`.  The output is a bindings bag if `<bindings_expr>` returns a bag, a bindings list if 
`<bindings_expr>` returns a list.

#### Example - Filtering `Foo`

```
// Equivalent to: FROM Foo AS f WHERE f.FooId = 200
(filter
    (eq (path (id f) (lit FooId) (lit 200)))
    (scan (id Foo) f null null))
```

First, `(scan (id Foo) f null null)` bindings expression returns the following bindings bag:

```
<<
    <{ f: { FooId: 100, FooName: 'Foo #1' } }>,
    <{ f: { FooId: 200, FooName: 'Foo #2' }}>
>>
```

Then, the bindings bag is iterated over. The `condition` expression is evaluated within the context of each bindings
tuple, and only those bindings tuples causing `condition` to return true are included:

```
<<
    <{ f: { FooId: 200, FooName: 'Foo #2' }}>
>>
```

## Appendix - Other Operators To Be Defined During Steel Threading

We are punting for now on defining the following operators:

* Something for: `pivot`
* Something for: `unpivot`
* `map_let`: like `map_values`, but returns a bindings collection instead of values.
* `limit`: truncates a bindings collection after `n` rows.
* `offset`: skips `n` rows of a bindings collection.
* `join`: performs a theta join
* other join types: left, right, outer, full
* `group_by`: grouping & aggregation
* `order_by`: sorting
* All the physical operators.

## Appendix - Further Reading

An introduction to the relational theory is beyond the scope of this document. The author of this document has found the
following books to be helpful to introduce the concepts here.

* [Database Systems: The Complete Book](https://www.amazon.com/Database-Systems-Complete-Book-2nd/dp/0131873253), Hector
  Molina-Garcia, et al, chapters 2, 5, 15 and 16.
* [Relational Theory for Computer Professionals](https://www.amazon.com/Relational-Theory-Computer-Professionals-Databases/dp/144936943X)
  , C.J. Date, chapters 2, 5, 7, 11, 12, 14 and appendix D.

Also see:

* The original [IonSQL++ paper](https://arxiv.org/pdf/1405.3631.pdf) by Kian Win Ong, Yannis Papakonstantinou, and
  Romain Vernoux.

