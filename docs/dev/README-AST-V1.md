
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

