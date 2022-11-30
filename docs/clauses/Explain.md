# Syntax

See the EXPLAIN syntax
```antlrv4
grammar PartiQL;

explain
  : EXPLAIN explainParameters? statement;

explainParameters
  : '(' explainParameter (',' explainParameter)* ')';

explainParameter
  : TYPE identifier
  | FORMAT identifier
  ;
```

## CLI Allowed Parameters/Values

Currently, PartiQL allows `type` to be one of the following:
- `AST`, `AST_NORMALIZED`, `LOGICAL_RESOLVED`, `PHYSICAL`, `PHYSICAL_TRANSFORMED`

The PartiQL CLI allows `FORMAT` to be one of the following:
- `ION_SEXP`, `TREE`, `DOT`, `DOT_URL`

# Example Usages

The following queries will show the `AST` as an Ion S-Expression.

```postgresql
-- Using the default values
EXPLAIN
    SELECT t.a FROM t;

-- Explicit parameters
EXPLAIN (TYPE AST, FORMAT ION_SEXP)
    SELECT t.a FROM t;
```

The following will show the `PHYSICAL` plan in `TREE` format:

```postgresql
EXPLAIN (TYPE PHYSICAL, FORMAT TREE)
    SELECT t.a FROM t;
```

While the following typically creates extremely long URLs -- to get a URL to a rendered Dot graph of a `LOGICAL` plan,
execute:

```postgresql
EXPLAIN (TYPE LOGICAL, FORMAT DOT_URL)
    SELECT t.a FROM t;
```

Or, alternatively, if you have [viu](https://github.com/atanunq/viu), [dot](https://graphviz.org/doc/info/command.html),
and [rsvg-convert](https://manpages.ubuntu.com/manpages/bionic/man1/rsvg-convert.1.html), and you'd like to render the DOT
graph directly from the console, execute:

```shell
./cli/build/install/partiql-cli/bin/partiql \
  --pipeline EXPERIMENTAL \
  --query "EXPLAIN (TYPE LOGICAL, FORMAT DOT) SELECT t.a FROM t" \
  | dot -Tsvg | rsvg-convert | viu -
```
