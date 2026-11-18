# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Changelog entries for `0.x` (alpha) releases have been archived to [archive/CHANGELOG-0.x.md](./archive/CHANGELOG-0.x.md).

## [Unreleased]

### Added


### Changed



### Deprecated



### Fixed



### Removed



### Security



### Contributors

## [1.7.0] - 2026-11-16

### Added

- **EXPERIMENTAL**: Added support for the `PERCENT_RANK` and `CUME_DIST` window functions.
- Added the `PError.INVALID_WINDOW_FUNCTION` error code, with the `FN_NAME`, `WINDOW_NAME`, and `REASON` properties. It is reported as a warning for a missing window `ORDER BY`, and as an error for `DISTINCT` with a window `ORDER BY` or for an outer reference or a subquery in a window function or `WINDOW` clause definition (SQL:2023 Subclause 9.23, Syntax Rule 3).
- `RexError.create(PError)` and `RexError.getError()` in `partiql-plan`, and `Operators.error(PType, PError)`, so an error node keeps the problem the planner reported. `getError()` returns a copy, and is `null` for a node made with `RexError.create()`.
- `PError.copy()` in `partiql-spi`, which returns a new error with the same code, severity, kind, location and properties.
- **EXPERIMENTAL**: Added support for the aggregates `COUNT`, `SUM`, `AVG`, `MIN`, and `MAX` as window functions.
- **EXPERIMENTAL**: Added `OperatorRewriter.visitWindowFunction`, `visitWindowPartition`, and `visitMapEntry`, and `Operators.window`.
- Added `date_trunc` and `to_char` to the `partiql-function-extensions` artifact.

### Changed

- Java classes that implement `Operators` directly must implement the new `error(PType, PError)` and `window(...)` methods; Kotlin implementations inherit the defaults.
- Aggregate window functions resolve to the built-in aggregate when the session has the default system catalog, even if a catalog on the session path defines an aggregate with the same name.
- The planner now reports an `INVALID_WINDOW_FUNCTION` warning for `RANK`, `DENSE_RANK`, `PERCENT_RANK`, `CUME_DIST`, `LEAD`, and `LAG` over a window without an `ORDER BY` clause (SQL:2023 Subclause 9.23, Syntax Rule 5a). Such queries still plan and run.

### Deprecated



### Fixed

- Fixed `OperatorRewriter` not rewriting a `RelWindow` (its input, window function arguments, partitions, and collations), a `RexMap`, a `RexMapDynamic`, or a sort key ([#2007](https://github.com/partiql/partiql-lang-kotlin/issues/2007)).
  - Rewriters built on `OperatorRewriter` now also visit `RelWindow` subtrees, `RexMap`/`RexMapDynamic` entries, and every `Collation` column, including `ORDER BY` keys. A rewriter that relied on these being skipped can override the visit to return the node unchanged.
- Fixed `SqlDialect` dropping the `PARTITION BY`/`ORDER BY` of a window that refines a named window, e.g. `OVER (w ORDER BY y)` printed as `OVER w`, and printing `WINDOW w2 AS (w1)` without parentheses.
- Fixed incorrect window ordering-group (peer group) bounds passed to window functions.
- Supported `IGNORE NULLS` for `LAG` and `LEAD` window functions; previously it was rejected by the planner and ignored by the evaluator.
- Fixed a re-opened window operator emitting a stale row from a previous, partially consumed run.
- Fixed `SUM` failing on statically typed `BIGINT` arguments.
- Fixed path index on `VARIANT` always failing ([#1983](https://github.com/partiql/partiql-lang-kotlin/issues/1983)). An Ion list or S-expression is now indexed like an array: ``` `[1, 2]`[0] ``` returns `1` instead of `MISSING` in permissive mode or an error in strict mode. A `VARIANT` index also works, as in ``` [10, 20][`1`] ```.
  - Results change for queries that index Ion data and relied on `MISSING`.
  - A null element is returned as an Ion null, the same as ``` `{a: null}`.a ```, so ``` `[null]`[0] IS NULL ``` is now false. [#1994](https://github.com/partiql/partiql-lang-kotlin/issues/1994) tracks `IS NULL` on Ion nulls.
- Fixed an error in an `ORDER BY` key, including a window `ORDER BY` key, failing the query in permissive mode ([#1982](https://github.com/partiql/partiql-lang-kotlin/issues/1982)). It now evaluates to `MISSING`, as it does in a projection. Strict mode still fails. This covers every data error in a sort key, such as a failed `CAST`, a path that fails, and a `MAP` index whose key is missing or of the wrong type (`MAP_KEY_NOT_FOUND`, `MAP_KEY_TYPE_MISMATCH`). A path index is now wrapped for permissive mode in every position, like a path key or symbol.
- Fixed a `BIGINT` path index being narrowed to 32 bits, which returned the wrong element: `[10, 20][4294967296]` returned `10`. It is now out of range: `MISSING` in permissive mode, `PATH_INDEX_NEVER_SUCCEEDS` in strict mode. An index that is not an integer at runtime now also fails with `PATH_INDEX_NEVER_SUCCEEDS` instead of `TYPE_UNEXPECTED`.
- Fixed indexing a null array, such as `CAST(NULL AS LIST)[0]`, failing with an internal `NullPointerException` in both modes. A null array, including Ion `null.list` and `null.sexp`, now gives `NULL` (SQL:2023 section 6.25).
- Fixed strict mode raising `INTERNAL_ERROR` for an expression the planner proved will fail ([#1988](https://github.com/partiql/partiql-lang-kotlin/issues/1988)). It now raises the planner's error code, such as `PATH_SYMBOL_NEVER_SUCCEEDS`, `PATH_KEY_NEVER_SUCCEEDS`, `PATH_INDEX_NEVER_SUCCEEDS`, `FUNCTION_TYPE_MISMATCH` or `TYPE_UNEXPECTED`, with the planner's location and properties. The error has severity `ERROR` and kind `EXECUTION`, like errors from checks that run only at runtime. Permissive mode is unchanged.
  - Hosts that match on `INTERNAL_ERROR` for these queries now see the specific code.
- Fixed a non-numeric `OFFSET` being planned as a second `LIMIT`.

### Removed

- **BREAKING**: For the _experimental_ window functions, removed support for an outer reference or a subquery in a window function, including its `OVER` specification and `WINDOW` clause definition (SQL:2023 Subclause 9.23, Syntax Rule 3). Such queries now fail to plan with `INVALID_WINDOW_FUNCTION`.

### Security



### Contributors

- @xd1313113
- @XuechunHHH

## [1.6.1] - 2026-09-03

### Added

- Added support for a (non-recursive) `WITH` list element (CTE) referencing sibling elements defined earlier. See [#1868](https://github.com/partiql/partiql-lang-kotlin/issues/1868).

### Changed



### Deprecated



### Fixed



### Removed



### Security



### Contributors

- @xd1313113

## [1.6.0] - 2026-09-01

### Added

- Added `RoutineProvider`, `MemRoutineProvider`, `RoutineBinding`, and `RoutineCatalog` APIs for explicit scalar and aggregate routine registration with exact catalog-local lookup.
- Added the separately published `partiql-function-extensions` artifact with executable `contains`, `date_add`, `utcnow`, `to_unixtime`, `pow`, and `hex_to_bigint` functions.
- Added namespace-aware scalar and aggregate routine resolution with configurable session paths, qualified catalog names, ambiguity diagnostics, and canonical `RoutineRef` plan identity.

### Changed



### Deprecated



### Fixed



### Removed



### Security



### Contributors

- @XuechunHHH

## [1.5.1] - 2026-08-18

### Added

- Added the `REPLACE` and `SPLIT` string functions
- Added `CHAR` and `VARCHAR` support to the built-in text functions: `BIT_LENGTH`, `CHAR_LENGTH`, `OCTET_LENGTH`, `POSITION`, `SUBSTRING`, `TRIM` (and its `LEADING`/`TRAILING`/character-set variants), `LOWER`, `UPPER`, and `||`.
- Added Ion timestamp support to `IonDatumReader`.

### Changed

- `VARCHAR(n)` values are no longer space-padded to their declared length; a value shorter than `n` keeps its own length (only `CHAR(n)` pads). Values longer than `n` are still truncated.
- The result length of the "shrink-only" text functions (`TRIM` and its `LEADING`/`TRAILING`/character-set variants, `SUBSTRING`, and `SPLIT` elements) now preserves the input's declared length, since these functions can only remove characters: `CHAR(n)`/`VARCHAR(n)` -> `VARCHAR(n)`, `CLOB(n)` -> `CLOB(n)`.
- `REPLACE` now returns an unbounded result type (`VARCHAR`/`CLOB` of maximum length), because its result may be longer than the input and its length is not computable at plan time. Previously it defaulted to `VARCHAR(255)` and silently truncated longer results.
- `||` (`concat`) now clamps the result length of the variable-length result types (`VARCHAR`/`CLOB`) to the maximum length instead of raising an overflow error when `L1 + L2` exceeds it; fixed-length `CHAR` results still raise. This also lets `CLOB || CLOB` and concatenation of an unbounded function result (such as `REPLACE`) resolve rather than fail.

### Deprecated



### Fixed

- Fixed `NullPointerException` during AST traversal by guarding nullable child fields in `getChildren()` for `WindowFunctionType.Lead`/`.Lag`, `AttributeConstraint`, and `TableConstraint.Unique`.
- Fixed writing `CLOB` values in the CLI text-pretty output as `CAST('<value>' AS CLOB)` until `CLOB` literal is defined.
- Fixed the built-in text functions failing to resolve (returning `MISSING`, with an `Undefined function` warning) when an argument is an untyped `NULL`/`MISSING`, instead of propagating `NULL`/`MISSING`. Affects `BIT_LENGTH`, `CHAR_LENGTH`, `OCTET_LENGTH`, `POSITION`, `SUBSTRING`, `TRIM` (and its `LEADING`/`TRAILING`/character-set variants), `LOWER`, `UPPER`, `LIKE`, `LIKE ... ESCAPE ...`, and `||`.
- Fixed `LIKE` and `LIKE ... ESCAPE ...` rejecting `CLOB` arguments.
- Fixed `||` throwing `Operation "getString" is not valid for type CLOB(n)` when either operand is a `CLOB`.
- Fixed decimal division precision and scale reduction when computed precision exceeds 38, preventing invalid result types and missing projected fields
- Fixed `VARCHAR` casts padding values shorter than the declared maximum length.

### Removed



### Security



### Contributors

- @xd1313113
- @XuechunHHH

## [1.5.0] - 2026-07-01

### Added

- Added SqlDialect support for MAP expression and type serialization
- Added MAP functions: map_contains_key, map_get, map_keys, map_values, map_entries, size, exists, cardinality and `IS MAP<K, V>
- Added implicit cast for `MAP` lookup key type if needed for bracket notation, map_contains_key function and map_get function.
- Added MAP type operation support: CAST and UNPIVOT
- Added `MAP` type parsing in DatumIonReader for CLI and Conformance tests
- Added Support for correlated Join.

### Changed



### Deprecated



### Fixed

- Fixed NullPointerException in RelOpUnpivot when UNPIVOT encounters a typed null value
- Fixed SELECT * with UNPIVOT to use named aliases instead of positional names

### Removed



### Security



### Contributors

- @xd1313113
- @yliuuuu

## [1.4.0] - 2026-06-05

### Added

- Added `fmt` subcommand to the CLI (`partiql fmt`) for pretty-printing PartiQL statements with width-aware formatting.
- **EXPERIMENTAL** Add interfaces to support `MAP` data type
- Added thread-safe, cacheable execution plans via `PartiQLVM` and `ExecutionPlan`. Plans compiled with `PartiQLCompiler.compile(plan, mode)` use integer-referenced table nodes (`RexTableRef`) resolved lazily through user-provided `ExecutionCatalog` instances. Each `PartiQLVM.execute()` call builds a fresh operator tree with no shared mutable state.
- Added `ExecutionCatalog` interface (single method: `getTable(int id)`) for providing table data at execution time.
- Added `SymbolTable` to planner results, mapping plan-assigned integer IDs to catalog/table names.
- Added `PartiQLPlanner.builder().useRefs()` to opt into ref-based table nodes for thread-safe plans.
- Added `Strategy.applyFactory()` for thread-safe custom operator strategies that return `Supplier<Expr>`.

### Changed

- `StandardCompiler` now uses a unified internal compilation path (`PlanToExecTransform` + `OperatorCompiler`) for all plans.

### Deprecated

- `Statement` — use `PartiQLVM.execute()` with `ExecutionPlan` for thread-safe execution.
- `PartiQLCompiler.prepare(plan, mode)` — use `compile(plan, mode)` + `PartiQLVM` instead.
- `RexTable` — use `RexTableRef` with `useRefs()` for thread-safe plans.
- `Strategy.apply()` — override `applyFactory()` for thread-safe strategies.

### Fixed



### Removed



### Security



### Contributors

- @XuechunHHH
- @xd1313113

## [1.3.14] - 2026-05-26

### Added

- Re-applied thread interruption support for the V1 evaluation engine, added `PError.INTERRUPTED` error code for identifying query cancellation via `Thread.interrupt()`

### Changed



### Deprecated



### Fixed



### Removed



### Security



### Contributors

- @AugustineFu

## [1.3.13] - 2026-05-07

### Added



### Changed

- Reverted [Add interruption check for V1](https://github.com/partiql/partiql-lang-kotlin/pull/1908) due to wrong exception handling

### Deprecated



### Fixed



### Removed



### Security



### Contributors

- @xd1313113

## [1.3.12] - 2026-05-05

### Added

- Added thread interruption support to the V1 evaluation engine, planner, and compiler for cooperative query cancellation

### Changed

- Route `NULLIF` and `COALESCE` to standard function call when parsing; 
this change enables SLL parsing, allowing us to see significant performance improvement when parsing complicated `NULLIF`/`COALESCE` call.

### Deprecated



### Fixed



### Removed



### Security



### Contributors

- @yliuuuu
- @AugustineFu

## [1.3.11] - 2026-04-21

### Added

- Implemented `--dir` CLI option to load a directory of data files (.ion, .json, .csv, .tsv, .pql, .parquet) as tables with lazy loading

### Changed



### Deprecated



### Fixed

- Fixed path expression on missing field causing errors in permissive mode instead of returning MISSING
- Fixed extraneous struct materialization in plan when `UNION/INTERSECT/EXCEPT` requires type coercion
- Fixed when `WITH` statement is present, the relproject is omitted in OperatorRewriter
- Fixed path expression on missing field
- Fixed parameter type mismatch for `BETWEEN` `AND` statement

### Removed



### Security



### Contributors

- @austnwil
- @AugustineFu
- @xd1313113
- @johnedquinn

## [1.3.10] - 2026-04-01

### Added



### Changed

- Reverted: Redefine DATE_ADD and DATE_DIFF to be special syntax for the standard SQL datetime arithmetic operators. See [#1819](https://github.com/partiql/partiql-lang-kotlin/pull/1819).

### Deprecated

## [1.3.9] - 2026-03-20

### Added



### Changed



### Deprecated



### Fixed

- Fixed the issue that Set Op throws "Not Implemented error" SELECT DISTINCT

### Removed



### Security



### Contributors

- @xd1313113

## [1.3.8] - 2026-03-19

### Added



### Changed

- Relax type match check for set operations `UNION/INTERSECT/EXCEPT`

### Deprecated



### Fixed

- Fixed `AVG` and `SUM` aggregation functions throwing IllegalStateException on VARIANT-typed inputs
- Fixed path navigation errors for `COALESCE` and `NULLIF` function

### Removed



### Security



### Contributors

- @xd1313113

## [1.3.7] - 2026-03-09

### Fixed

- Fixed the `--format` flag in partiql-cli, which was non-functional
- Removed coercion of root expressions when applying wildcard path expressions
- Fixed in-predicate behavior when RHS is a SQL-style SFW query
g

### Contributors

- @jobarr-amzn
- @johnedquinn

## [1.3.6] - 2026-03-05

### Added



### Changed



### Deprecated



### Fixed

- Fixed comparison operators returning wrong type when one argument is NULL/MISSING, breaking boolean expressions.

### Removed



### Security



### Contributors

- @XuechunHHH

## [1.3.5] - 2026-03-04

### Added



### Changed



### Deprecated



### Fixed

- Incorrectly throwing errors in PERMISSIVE mode
- String functions unexpectedly fail with evaluation errors

### Removed



### Security



### Contributors

- @XuechunHHH

## [1.3.4] - 2026-02-16

### Added



### Changed



### Deprecated



### Fixed

- Fixed `SIZE` function compilation to properly handle `SELECT` expressions instead of coercing them to scalars

### Removed



### Security



### Contributors

- @XuechunHHH

## [1.3.3] - 2026-01-16

### Added

- Added ExprError AST node and error handling for datetime field keywords (YEAR, MONTH, DAY, HOUR, MINUTE, SECOND) used in expression contexts

### Changed



### Deprecated



### Fixed



### Removed



### Security



### Contributors

- @XuechunHHH

## [1.3.2] - 2025-11-19

### Added

- Add auto cast support for `TIME` ↔ `TIME WITH TIME ZONE`, `DATE` ↔ `TIMESTAMP` ↔ `TIMESTAMP WITH TIME ZONE`
- Implement DATE_DIFF function
- Add `TIME WITH TIME ZONE` and `TIMESTAMP WITH TIME ZONE` support to some datetime operators `+` and `-`.

### Changed



### Deprecated



### Fixed



### Removed



### Security



### Contributors

- @xd1313113

## [1.3.1] - 2025-11-06

### Added

- Support aggregation function calls in the `ORDER BY` and `HAVING` clause
- Add Support to transcribe `Overlaps` function
- Add planner support to compare DATE and TIMESTAMP(Z)

### Changed



### Deprecated



### Fixed



### Removed



### Security



### Contributors

- @xd1313113

## [1.3.0] - 2025-10-13

### Added

- **EXPERIMENTAL**: Adds end-to-end support for window functions including RANK, DENSE_RANK, LAG, LEAD, and ROW_NUMBER functions alongside the WINDOW clause.
- `UPPER`, `LOWER`, `TRIM` functions support for `CHAR/VARCHAR`
- `LIKE` and `LIKE_ESCAPE` functions support for `CHAR/VARCHAR/CLOB/STRING` with dynamic type coercion
- `CLOB/BLOB` now supports length parameters with `CLOB(max_length)/BLOB(max_length)` syntax

### Changed

- Formalize `UPPER`, `LOWER`, `TRIM` and `CONCAT` length and type handling on string types

### Deprecated

- Deprecated previous modeling of window functions.

### Fixed

- Datum return type for `LOWER` on a CLOB value

### Removed



### Security



### Contributors

- @johnedquinn
- @XuechunHHH

## [1.2.3] - 2025-09-29

### Added

- partiql-ast: add `With` and `WithListElement` to `AstVisitor` and `SqlDialect`
- **EXPERIMENTAL** partiql-plan: add representation of `RelWith` and `WithListElement` to the plan and the
`OperatorVisitor`
- **EXPERIMENTAL** partiql-planner: add planner builder function to control whether `With` table references are
rewritten to their query representation
  - **NOTE** evaluating plans without the inlined `With` rewrite will not work as expected. Users trying to evaluate
  `With` should use the default planner.
- Signed `INTERVAL` literals support
- Metas attached to `INTERVAL` `PType`s if an `INTERVAL` has an unspecified precision or fractional precision
- `EXTRACT` function support on `INTERVAL` datum
- `ABS` function support on `INTERVAL` values
- Support for `OVERLAPS` predicate
- Add cli option -e to load PartiQL literal data as default catalog data
- Add toString() support for all datum types
- Add optional `distinguishNullMissing` parameter to `Datum.comparator()` to control whether `null` and `missing` are treated as equivalent or distinct in comparisons
- Remove the extra non-character Unicode in the function name when displaying cli errors
- **EXPERIMENTAL** Add `Datum.getTotalMonths` to retrieve the total number of months from a year-month `Datum`
- **EXPERIMENTAL** Add `Datum.getTotalSeconds` to retrieve the total number of seconds from a day-time `Datum`
- fix the parsing of DATE_ADD/DATE_DIFF with an uncapitalized datetime field argument.
- fix that wrong offset is set when parsing Ion timestamp with time zone into Datum.
- Fix ORDER BY statement does not recognize alias from SELECT statement

### Changed



### Deprecated



### Fixed

- Specify the correct qualifier for `INTERVAL` datum with `MINUTE TO SECOND`
- Handle the formatting when the nanoseconds of `INTERVAL` datum underflow
- CLI printing of negative interval literals
- Fix alias collision problem when multiple table references need auto-generated aliases
- Display of `Ion` in PartiQL CLI output
- Support `INTERVAL` times and divide with numerics
- Prioritize `missing` propagation when both `null` and `missing` exist in function args
- Return `missing` instead of `null` for `NULLIF(MISSING, MISSING)`
- CLI printing of mixed sign literals
- Parsing of `TIMESTAMP` and `TIME` literals with timezone offsets
- Variable resolution for qualified references with `GROUP BY`

### Removed



### Security



### Contributors

- @zyfy29
- @alancai98
- @XuechunHHH
- @xd1313113

## [1.2.2] - 2025-06-26

### Added

- **EXPERIMENTAL** add `metas` map for `PType`
- partiql-planner: `ROW` or collection types with excluded fields resulting from `RelExclude` will include a meta
`CONTAINS_EXCLUDED_FIELD` mapping to `true`
- Static factories for creating `PType.decimal` and `PType.numeric` types with a precision and default scale
- Metas attached to `PType`s indicating if a parameter (e.g. precision, scale, length) was specified

### Changed



### Deprecated



### Fixed

- `BETWEEN` operator function overloads in the partiql-planner
- Mistyping of some Rex plan nodes
- `OperatorRewriter` omitting types for rewritten nodes
- fixed Aggregation function lookup so that custom overloads from `Catalog` are found
- `SqlDialect` printing of AST literal string single quotes
- Parsing of `LIMIT`, `OFFSET`, and `ORDER BY` on set operators

### Removed



### Security



### Contributors

- @alancai98

## [1.2.1] - 2025-05-07

### Added

- added missing `NotNull` annotation for `RexPathIndex`'s `getIndex` function

### Changed



### Deprecated



### Fixed

- fixed plan typing of function arguments which were previously typed the same as the function's return type
- fixed pretty-printing of `TRIM` and binary operator parens
- fixed the AST to plan conversion of `PathStep.Field` case-sensitivity
- fixed the AST to plan resolving of `IS <type>` functions for `<type>`s with parameters

### Removed

- redundant `CAST`s added for `CASE WHEN` branches

### Security



### Contributors

- @alancai98

## [1.2.0] - 2025-03-12

### Added

- Added parsing, planning, and execution of the interval type and interval values.
- Added multiple operations involving datetime and interval values.
- Added planning and evaluation of the LET clause

### Changed



### Deprecated



### Fixed



### Removed



### Security



### Contributors

- @johnedquinn
- @lucaxchen
- @mrugeshgharat

## [1.1.0] - 2025-02-14

### Added

- Added parsing, planning, and execution of basic Common Table Expressions (CTEs). These CTEs do not
  allow for the planning/execution of RECURSIVE nor the planning/execution of the column list. This addition
  also does not support the ability to reference CTEs from other CTEs. These may be added in a subsequent release.

### Fixed

- Fixed the parsing of select statements and bag operations as they pertain to the expression tree.
- Properly returns properties from `Session`

### Contributors

- @johnedquinn
- @jpschorr

**Full Changelog**: [v1.0.0...v1.1.0](https://github.com/partiql/partiql-lang-kotlin/compare/v1.0.0...v1.1.0)

## [1.0.0] - 2025-01-23

### Added

- This is our biggest release yet, and we are incredibly excited to announce 1.0. Please refer to the [1.0 documentation](https://partiql.org/plk/1.0/) for more information.

### Contributors

- @alancai98
- @am357
- @johnedquinn
- @rchowell
- @yliuuuu

**Full Changelog**: [v0.14.9...v1.0.0](https://github.com/partiql/partiql-lang-kotlin/compare/v0.14.9...v1.0.0)

## [1.0.0-rc.3] - 2024-12-10

## [1.0.0-rc.2] - 2024-11-04

## [1.0.0-rc.1] - 2024-10-02

[Unreleased]: https://github.com/partiql/partiql-lang-kotlin/compare/v1.7.0...main
[1.7.0]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.7.0
[1.6.1]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.6.1
[1.6.0]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.6.0
[1.5.1]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.5.1
[1.5.0]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.5.0
[1.4.0]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.4.0
[1.3.14]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.3.14
[1.3.13]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.3.13
[1.3.12]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.3.12
[1.3.11]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.3.11
[1.3.10]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.3.10
[1.3.9]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.3.9
[1.3.8]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.3.8
[1.3.7]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.3.7
[1.3.6]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.3.6
[1.3.5]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.3.5
[1.3.4]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.3.4
[1.3.3]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.3.3
[1.3.2]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.3.2
[1.3.1]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.3.1
[1.3.0]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.3.0
[1.2.3]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.2.3
[1.2.2]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.2.2
[1.2.1]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.2.1
[1.2.0]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.2.0
[1.1.0]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.1.0
[1.0.0]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.0.0
[1.0.0-rc.3]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.0.0-rc.3
[1.0.0-rc.2]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.0.0-rc.2
[1.0.0-rc.1]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v1.0.0-rc.1
