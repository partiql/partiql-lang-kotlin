# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

<!-- Template: after a release, copy and paste out below
## [Unreleased]

### Added

### Changed

### Deprecated

### Fixed

### Removed

### Security
-->


## [Unreleased]

### Added

### Changed

### Deprecated

### Fixed

### Removed

### Security

## [0.9.2] - 2023-01-20

### Added
- Adds ability to pipe queries to the CLI
- Adds ability to run PartiQL files as executables by adding support for shebangs

### Changed

### Deprecated

### Fixed
- Fixes list/bag ExprValue creation in plan evaluator
- Fixes gradle build issues.

### Removed

### Security

## [0.9.1] - 2023-01-04

### Added
- Makes the following `PartiQLCompilerBuilder` functions are moved to public
  - `customOperatorFactories`
  - `customFunctions`
  - `customProcedures`

## [0.9.0] - 2022-12-13

### Added
- Adds simple auto-completion to the CLI.
- Adds the IsListParenthesizedMeta meta to aid in differentiating between parenthesized and non-parenthesized lists
- Adds support for HAVING clause in planner
- Adds support for collection aggregation functions in the EvaluatingCompiler and experimental planner
- Adds support for the syntactic sugar of using aggregations functions in place of their collection aggregation function
  counterparts (in the experimental planner)
- Experimental implementation for window function `Lag` and `Lead`.
- Adds support for EXPLAIN
- Adds continuous performance benchmarking to the CI for existing JMH benchmarks
  - Benchmark results can be seen on the project's GitHub Pages site
- Adds the `pipeline` flag to the CLI to provide experimental usage of the PartiQLCompilerPipeline
- Added `ExprValue.toIonValue(ion: IonSystem)` in kotlin, and `ExprValueExtensionKt.toIonValue(value: ExprValue, ion: IonSystem)` in Java to transform one `ExprValue` to a corresponding `IonValue`.
- Added `ExprValue.of(value: IonValue)` method to construct an `ExprValue` from an `IonValue`. 

### Changed
- Now `CompileOption` uses `TypedOpParameter.HONOR_PARAMETERS` as default.
- Updates the CLI Shell Highlighter to use the ANTLR generated lexer/parser for highlighting user queries
- PartiQL MISSING in Ion representation now becomes ion null with annotation of `$missing`, instead of `$partiql_missing`
- PartiQL BAG in Ion representation now becomes ion list with annotation of `$bag`, instead of `$partiql_bag`
- PartiQL DATE in Ion representation now becomes ion timestamp with annotation of `$date`, instead of `$partiql_date`
- PartiQL TIME in Ion representation now becomes ion struct with annotation of `$time`, instead of `$partiql_time`
- Simplifies the aggregation operator in the experimental planner by removing the use of metas
- Increases the performance of the PartiQLParser by changing the parsing strategy
  - The PartiQLParser now attempts to parse queries using the SLL Prediction Mode set by ANTLR
  - If unable to parse via SLL Prediction Mode, it attempts to parse using the slower LL Prediction Mode
  - Modifications have also been made to the ANTLR grammar to increase the speed of parsing joined table references
  - Updates how the PartiQLParser handles parameter indexes to remove the double-pass while lexing
- Changes the expected `Property`'s of `TOKEN_INFO` to use `Property.TOKEN_DESCRIPTION` instead of `Property.TOKEN_TYPE`

### Deprecated
- Marks the GroupKeyReferencesVisitorTransform as deprecated. There is no functionally equivalent class.
- Marks `ionValue` property in `ExprValue` interface as deprecated. The functional equivalent method is `ExprValue.toIonValue(ion: IonSystem)` in kotlin, and `ExprValueKt.toIonValue(value: ExprValue, ion: IonSystem)` in Java.
- Marks `Lexer`, `Token`, `TokenType`, `SourcePosition`, and `SourceSpan` as deprecated. These will be removed without
any replacement.
- Marks approximately 60 `ErrorCode`'s as deprecated. These will be removed without any replacement.
- Marks `Property.TOKEN_TYPE` as deprecated. Please use `Property.TOKEN_DESCRIPTION`.

### Fixed
- Fixes the ThreadInterruptedTests by modifying the time to interrupt parses. Also adds better exception exposure to
  facilitate debugging.

### Removed
- Removes the deprecated V0 AST in the codebase.
- Removes the deprecated MetaContainer in the codebase, removed interfaces and classes include:
  - [MetaContainer] Interface
  - [MetaContainerImpl]
  - [MetaDeserialize]
  - [MemoizedMetaDeserializer]
- Removes the deprecated Rewriter/AstWalker/AstVisitor in the code base, removed interfaces and classes include:
  - [AstRewriter] Interface & [AstRewriterBase] class
  - [AstVisitor] Interface & [AstVisitorBase] class
  - [AstWalker] class
  - [MetaStrippingRewriter] class
- Removes the deprecated ExprNode and related files in the code base.
  - [Parser] API `parseExprNode(source: String): ExprNode` has been removed.
  - [CompilerPipeline] API `compile(query: ExprNode): Expression` has been removed.
  - [ExprNode] and [AstNode] have been removed.
  - Functions related to conversions between ExprNode and PartiqlAst have been removed.
- Removes the deprecated SqlParser and SqlLexer
- **Breaking**: Removes the `CallAgg` node from the Logical, LogicalResolved, and Physical plans.
- Removes the experimental `PlannerPipeline` and replaces it with `PartiQLCompilerPipeline`.

### Security


## [0.8.2] - 2022-11-28
### Added
- Adds simple auto-completion to the CLI.

### Changed
- Increases the performance of the PartiQLParser by changing the parsing strategy
  - The PartiQLParser now attempts to parse queries using the SLL Prediction Mode set by ANTLR
  - If unable to parse via SLL Prediction Mode, it attempts to parse using the slower LL Prediction Mode
  - Modifications have also been made to the ANTLR grammar to increase the speed of parsing joined table references
  - Updates how the PartiQLParser handles parameter indexes to remove the double-pass while lexing

## [0.8.1] - 2022-10-28

### Added
- Extends statement redaction to support `INSERT/REPLACE/UPSERT INTO`.


## [0.8.0] - 2022-10-14

### Added
- `CHANGELOG.md` with back-filling of the previous releases to the change log to provide more visibility on unreleased
  changes and make the release process easier by using the `unreleased` section of change log. The `CONTRIBUTING.md`
  has also been updated to ensure this is part of the process.
- backward-incompatiblity and dependency questions are added to the project's PR process to provide more context
  on the changes that include these and the alternatives that have been considered.
- README.md badges for GitHub Actions status, codecov, and license
- An experimental (pending [#15](https://github.com/partiql/partiql-docs/issues/15)) embedding of a subset of
  the [GPML (Graph Pattern Matching Language)](https://arxiv.org/abs/2112.06217) graph query, as a new expression
  form `<expr> MATCH <gpml_pattern>`, which can be used as a bag-of-structs data source in the `FROM` clause.   
  The use within the grammar is based on the assumption of a new graph data type being added to the
  specification of data types within PartiQL, and should be considered experimental until the semantics of the graph
  data type are specified.
  - basic and abbreviated node and edge patterns (section 4.1 of the GPML paper)
  - concatenated path patterns  (section 4.2 of the GPML paper)
  - path variables  (section 4.2 of the GPML paper)
  - graph patterns (i.e., comma separated path patterns)  (section 4.3 of the GPML paper)
  - parenthesized patterns (section 4.4 of the GPML paper)
  - path quantifiers  (section 4.4 of the GPML paper)
  - restrictors and selector  (section 5.1 of the GPML paper)
  - pre-filters and post-filters (section 5.2 of the GPML paper)
- Added EvaluatonSession.context: A string-keyed map of arbitrary values which provides a way to make  
  session state such as current user and transaction details available to custom [ExprFunction] implementations
  and custom physical operator implementations.
- Replaces `union`, `intersect`, `except` IR nodes with common `bag_op` IR node
- Add support for CallAgg in Type Inferencer.
- A GitHub Workflow to automatically sync the `docs` directory with the GitHub Wiki
- Introduces the `PartiQLParser`, an implementation of `Parser` using `ANTLR`
  - Matches the functionality of the existing `SqlParser`
  - Now catches a StackOverflowError and throws a ParserException
  - Support for DQL, DDL, DML, GPML, and EXEC
  - Handles consistency and precedence issues seen in SqlParser
    - See GitHub Issues [#709](https://github.com/partiql/partiql-lang-kotlin/issues/709), [#708](https://github.com/partiql/partiql-lang-kotlin/issues/708),
      [#707](https://github.com/partiql/partiql-lang-kotlin/issues/707), [#683](https://github.com/partiql/partiql-lang-kotlin/issues/683),
      and [#730](https://github.com/partiql/partiql-lang-kotlin/issues/730)
- Parsing of `INSERT` DML with `ON CONFLICT DO REPLACE EXCLUDED` based on [RFC-0011](https://github.com/partiql/partiql-docs/blob/main/RFCs/0011-partiql-insert.md)
- Adds a subset of `REPLACE INTO` and `UPSERT INTO` parsing based on [RFC-0030](https://github.com/partiql/partiql-docs/blob/main/RFCs/0030-partiql-upsert-replace.md)
  - Parsing of target attributes is not supported yet and is pending [#841](https://github.com/partiql/partiql-lang-kotlin/issues/841)
- Logical plan representation and evaluation support for `INSERT` DML with `ON CONFLICT DO REPLACE EXCLUDED` and `REPLACE INTO` based on [RFC-0011](https://github.com/partiql/partiql-docs/blob/main/RFCs/0011-partiql-insert.md)
- Logical plan representation of `INSERT` DML with `ON CONFLICT DO UPDATE EXCLUDED` and `UPSERT INTO` based on [RFC-0011](https://github.com/partiql/partiql-docs/blob/main/RFCs/0011-partiql-insert.md)
- Enabled projection alias support for ORDER BY clause
- Adds support for PIVOT in the planner consistent with `EvaluatingCompiler`

#### Experimental Planner Additions

- Renamed `PassResult` to PlannerPassResult for clarity. (This is part of the experimental query planner API.)
- The `PlannerPipeline` API now has experimental and partial support for `INSERT` and `DELETE` DML statements—
  tracking PartiQL specification issues are [partiql-docs/#4](https://github.com/partiql/partiql-docs/issues/4) (only
  a subset has been implemented--see examples below) and
  [partiql-docs/#19](https://github.com/partiql/partiql-docs/issues/19).
  - Examples of supported statements include:
    - `INSERT INTO foo << { 'id': 1, 'name': 'bob' }, { 'id': 2, 'name' : 'sue' } >>` (multi record insert)
    - `INSERT INTO foo SELECT c.id, c.name FROM customer AS c` (insert the results of a query into another table)
    - `DELETE FROM foo` (delete all records in a table)
    - `DELETE FROM foo AS f WHERE f.zipCode = '90210'` (delete all records matching a predicate)
- Introduced planner event callbacks as a means to provide a facility that allows the query to be visualized at every
  stage in the `PlannerPipeline` and to generate performance metrics for the individual phases of query planning.  See
  `PlannerPipe.Builder.plannerEventCallback` for details.
- Adds the following optimization passes, none of which are enabled by default:
  - `FilterScanToKeyLookupPass` which performs a simple optimization common to most databases: it converts a filter
    predicate covering a table's complete primary key into a single get-by-key operation, thereby avoiding a full table
    scan.  This may pass leave behind some useless `and` expressions if more `and` operands exist in the filter predicate
    other than primary key field equality expressions.
  - `RemoveUselessAndsPass`, which removes any useless `and` expressions introduced by the previous pass or by the
    query author, e.g. `true and x.id = 42` -> `x.id = 42`), `true and true` -> `true`, etc.
  - `RemoveUselessFiltersPass`, which removes useless filters introduced by the previous pass or by the query author
    (e.g. `(filter (lit true) <bexpr>))` -> `<bexpr>`.
- Add support for `UNPIVOT`, the behavior is expected to be compatible with the `evaluating compiler`.
- Adds support for GROUP BY (aggregations, group keys, etc)
- Adds support for ORDER BY in Planner

### Changed
- The default parser for all components of PartiQL is now the PartiQLParser -- see the deprecation of `SqlParser`
- Parsing of `ORDER BY` clauses will no longer populate the AST with defaults for the 'sort specification'
  (i.e., `ASC` or `DESC`) or 'nulls specification' (i.e., `NULLS FIRST` or `NULLS LAST`) when the are not provided in
  the query text. Defaulting of sort order is moved to the evaluator.

### Deprecated
- Deprecates `SqlLexer` and `SqlParser` to be replaced with the `PartiQLParserBuilder`.
- Deprecates helper method, `blacklist`, within `org.partiql.lang.eval` and introduced a functionally equivalent
  `org.partiql.lang.eval.denyList` method.
- Deprecates `TypedOpParameter.LEGACY` to be replaced with `TypedOpParameter.HONOR_PARAMETERS`

### Fixed
- Codecov report uploads in GitHub Actions workflow
- GitHub Actions capability to run on forks
- Negation overflow caused by minimum INT8
- Type mismatch error caused by evaluator's integer overflow check
- Cast function's behavior on positive_infinity, negative_infinity, and NaN explicitly defined and handled.
- Changed Trim Function Specification handling(fixed error message, and now can take case-insensitive trim spec)

### Removed
- README.md badge for travisci
- **Breaking Change**: removed [ExprValueType.typeNames] as needed by the future work of legacy parser removal and OTS
- **Breaking Change**: [PartiqlPhysical.Type.toTypedOpParameter()] now becomes an internal function
- **Breaking Change**: [PartiqlAst.Type.toTypedOpParameter()] is removed
- **Breaking Change**: [PartiqlAstSanityValidator] now becomes an internal class
- **Breaking Change**: [PartiqlPhysicalSanityValidator] is removed
- **Breaking Change**: the following custom type AST nodes are removed from `partiql.ion` file: `es_boolean`, `es_integer`, `es_float`,
  `es_text`, `es_any`, `spark_short`, `spark_integer`, `spark_long`, `spark_double`, `spark_boolean`, `spark_float`,
  `rs_varchar_max`, `rs_integer`, `rs_bigint`, `rs_boolean`, `rs_real`, `rs_double_precision`.
  The related visitor transform `CustomTypeVisitorTransform` is also removed.
  See [Issue 510](https://github.com/partiql/partiql-lang-kotlin/issues/510) for more details.


### Security

## [0.7.0-alpha] - 2022-06-23
### Added
- An experimental query planner API along with logical and physical plans structures with the support of non-default
  physical operator implementations.
- An optional flag, `--wrap-ion`, to give users the old functionality of reading multiple Ion values (previous behavior).
- Benchmark framework and benchmark implementation for `LIKE` performance
- Convenience `StaticType` for `TEXT` and `NUMERIC`
- Enable `MIN` and `MAX` to work with all the data-types.
- Introduction of `extensions` and addition of the `query_ddb` function to allow querying AWS DynamoDB from the CLI.
- Replacement of REPL with [JLine](https://jline.github.io/) shell
- Syntax highlighting for CLI
- Three additional CLI flags:
  - `-r --projection-iter-behavior:` Controls the behavior of ExprValue.iterator in the projection result:
    (default: FILTER_MISSING) [FILTER_MISSING, UNFILTERED]
  - `-t --typed-op-behavior`: indicates how CAST should behave: (default: HONOR_PARAMETERS) [LEGACY, HONOR_PARAMETERS]
  - `-v --undefined-variable-behavior`: Defines the behavior when a non-existent variable is referenced:
    (default: ERROR) [ERROR, MISSING]
- `--input-format` flag to the CLI
- `CEIL` and `FLOOR` functions
- `DATE/TIME` formatting and the support for `DATE/TIME` in Ion data format

### Changed
- `LIKE` matching via compilation to `java.util.regex.Pattern`
- Run `ktlint` before tests.

### Removed
- [breaking change] Removal of Field `EVALUATOR_SQL_EXCEPTION` from `ErrorCode` class:
  A client program may be interrupted by `NoSuchFieldError` exception.
- [breaking change] Removal of `NodeMetadata` from `org.partiql.lang.eval`:
  A client program may be interrupted by `NoClassDefFoundError` exception.

### Fixed
- Fix `write_file` CLI function; the old function required the input to be a `string`, but it must be a generic type.
- Add `ktlint` task dependency to enable execution optimizations and reducing he build time by ~ `30%`.
- Adjust handling of Ion input (requiring single value)
- Adjust handling of Ion output (outputting the real value)
- Adds missing metas to `ORDER BY` `ExprNode` and `PartiqlAst` (E.g. source location), which limits error message
  reporting.

## [0.6.0-alpha] - 2022-04-06
### Added
- [cli] Add permissive mode evaluation option to CLI/REPL [#545](https://github.com/partiql/partiql-lang-kotlin/pull/545)
- `ORDER BY` implementation in evaluator [#554](https://github.com/partiql/partiql-lang-kotlin/pull/554)
- [build] Adds `ktlint` to gradle build [#542](https://github.com/partiql/partiql-lang-kotlin/pull/542)

### Changed
- For `ExprFunction`, replace `Environment` with `EvaluationSession` [#559](https://github.com/partiql/partiql-lang-kotlin/pull/559)
- Migrate to PIG `v0.5.0` [#563](https://github.com/partiql/partiql-lang-kotlin/pull/563)
- [build] Increase build performance w/ Gradle upgrade to 7.4 [#539](https://github.com/partiql/partiql-lang-kotlin/pull/539)
- [build] Upgrade `dokka` to `1.6.10`, set `org.gradle.jvmargs` [#568](https://github.com/partiql/partiql-lang-kotlin/pull/568)
- Changed `Path` AST node to use its root node source location [#527](https://github.com/partiql/partiql-lang-kotlin/pull/527)
- Improve the `CAST` assertion assertEval [#523](https://github.com/partiql/partiql-lang-kotlin/pull/523)
- [build] Bump Kotlin version to `1.4.32` from `1.4.0` [#548](https://github.com/partiql/partiql-lang-kotlin/pull/548)
- [breaking-change] changing `ExprFunction`'s usage of `Environment` to `EvaluationSession` along with some other
  classes containing implementation details made internal as part of [#559](https://github.com/partiql/partiql-lang-kotlin/pull/559).

### Deprecated
- Deprecate `ExprNode` [#535](https://github.com/partiql/partiql-lang-kotlin/pull/535)

### Fixed
- Fix all compiler warnings [#562](https://github.com/partiql/partiql-lang-kotlin/pull/562)

### Removed
- Clean up `ExprFunction` test [#529](https://github.com/partiql/partiql-lang-kotlin/pull/529)

## [0.5.0-alpha] - 2022-02-11
### Added
- Adds a static type inferencer for static query checks and query type inference
- Adds multiple exception logging and severity level API
- Adds the dataguide API which can be used to infer Ion schema from Ion data
  - Also adds mappers to and from PartiQL’s static type and ISL
- Refactor of PartiQL’s `StaticType`
- Refactors `ExprFunction` interface
- Adds evaluator option for `PERMISSIVE` mode
- Adds support for `CAN_CAST` and `CAN_LOSSLESS_CAST`
- Adds evaluation-time function call (`ExprFunction`) argument type checks
- Adds `integer8`, `int8`, `bigint`, `int2`, and `integer2` as type names
- Adds support for `OFFSET` [#451](https://github.com/partiql/partiql-lang-kotlin/pull/451)
- [cli] Uses Apache's CSVParser for file reading [#474](https://github.com/partiql/partiql-lang-kotlin/pull/474) and
- ability to read custom CSV configurations [#480](https://github.com/partiql/partiql-lang-kotlin/pull/480)

### Changed
- Upgrades Kotlin version to `1.4`
- Modeled `NULLIF` and `COALESCE` as `PartiqlAst` nodes rather than `ExprFunctions`
- Started parameterization of evaluation tests

### Deprecated
- Deprecate `ExprNode` in parser [#464](https://github.com/partiql/partiql-lang-kotlin/pull/464)

### Fixed
- Fixes evaluator behavior to error for structs with non-text keys
- Corrects the parser error for unexpected reserved keywords in a select list
- Fixes static initializing cycle with lazy initialization of `SqlDataType`
- Fixes unknown propagation for `IN` operator
- Fixes bug in precision check for `NUMERIC`
- Makes unknown simple `CASE WHEN` predicate the same as `false`
- Make unknown branch predicates the same as false for searched `CASE WHEN`
- Disallows duplicate projected fields in select list query
- Fixes `EXTRACT` `ExprFunction` to return a `decimal` instead of `float`
- Fixes `EXISTS` and `DATE_DIFF` function signatures
- Fixes `GROUP BY` for more than 2 nested path expressions [#461](https://github.com/partiql/partiql-lang-kotlin/pull/461)
- [cli] Fixes `CLI` command bug when input data is empty [#478](https://github.com/partiql/partiql-lang-kotlin/pull/478)
- [cli] Fixes `CLI` bug when outputting `IONTEXT` to file [#479](https://github.com/partiql/partiql-lang-kotlin/pull/479)

### Removed
- Removes wildcard imports in cli [#483](https://github.com/partiql/partiql-lang-kotlin/pull/483) and
  lang [#488](https://github.com/partiql/partiql-lang-kotlin/pull/488)
- Removes `DateTimeType` `sealed` class [#489](https://github.com/partiql/partiql-lang-kotlin/pull/489)
- Renames `DateTimePart` type to `DatePart` [#506](https://github.com/partiql/partiql-lang-kotlin/pull/506)

## [0.4.0-alpha] - 2021-10-07

### Added
- Sets up JMH for PartiQL [#427](https://github.com/partiql/partiql-lang-kotlin/pull/427)
- Allows for default timezone configuration [#449](https://github.com/partiql/partiql-lang-kotlin/pull/449)

### Changed
- Uses new PIG major version `v0.4.0` [#454](https://github.com/partiql/partiql-lang-kotlin/pull/454)
- Moves usage of default timezone from parser to evaluator [#448](https://github.com/partiql/partiql-lang-kotlin/pull/448)
- [breaking-change] changes related to imported builders.

### Fixed
- Fixes struct handling of non-text struct field keys [#450](https://github.com/partiql/partiql-lang-kotlin/pull/450)

## [0.2.7-alpha] - 2021-09-13

### Fixed
- Cherry picks "Fix bug causing multiple nested nots to parse very slowly [#436](https://github.com/partiql/partiql-lang-kotlin/pull/436)
  for `v0.2.7` release [#439](https://github.com/partiql/partiql-lang-kotlin/pull/439)
- Cherry picks "Use LazyThreadSafteyMode.PUBLICATION instead of NONE [#433](https://github.com/partiql/partiql-lang-kotlin/pull/433)
  for `v0.2.7` release [#440](https://github.com/partiql/partiql-lang-kotlin/pull/440)

## [0.1.7-alpha] - 2021-09-13

### Fixed
- Cherry picks "Fix bug causing multiple nested nots to parse very slowly [#436](https://github.com/partiql/partiql-lang-kotlin/pull/436)
  for `v0.1.7` release [#441](https://github.com/partiql/partiql-lang-kotlin/pull/441)
- Cherry picks "Use LazyThreadSafteyMode.PUBLICATION instead of NONE [#433](https://github.com/partiql/partiql-lang-kotlin/pull/433)
  for `v0.1.7` release [#442](https://github.com/partiql/partiql-lang-kotlin/pull/442)

## [0.3.4-alpha] - 2021-09-10
### Fixed
- Bug causing multiple nested nots to parse very slowly [#436](https://github.com/partiql/partiql-lang-kotlin/pull/436)

## [0.3.3-alpha] - 2021-09-09
### Changed
- Uses `LazyThreadSafteyMode.PUBLICATION` instead of NONE

## [0.3.1-alpha] - 2021-06-18
### Fixed
- Prevent the `ORDER BY` clause from being dropped in visitor transforms [#420](https://github.com/partiql/partiql-lang-kotlin/pull/420)

## [0.3.0-alpha] - 2021-06-09
### Added
- `DATE` and `TIME` data types
- Enhancements made by/for DynamoDB
- Compile-time `Thread.interrupted()` checks were added to help mitigate the impact of compiling extremely large SQL
  queries.
- Various performance improvements to the compiler were added.

### Changed
- The modeling of `ExprNode` and `PartiqlAst` APIs has changed as needed to account for the enhancements to DML statements
  and `ORDER BY`. Customers using these APIs may be affected.
- Other minor API changes.

### Fixed
- Fixes parser for the top level tokens [#369](https://github.com/partiql/partiql-lang-kotlin/pull/369)
- Make `SIZE` function work with s-expressions. [#379](https://github.com/partiql/partiql-lang-kotlin/pull/379)
- A number of other minor bug fixes and technical debt has been addressed. For a complete list of PRs that made it into
  this release, please see the v0.3.0 GitHub milestone.

## [0.1.6-alpha] - 2021-05-13

### Fixed
- Adds Compile-Time Thread.interrupted() checks [#398](https://github.com/partiql/partiql-lang-kotlin/pull/398)

## [0.1.5-alpha] - 2021-04-27

### Fixed
- Fixes a severe performance issue relating the sanity checks performed on very large queries before compilation. [#391](https://github.com/partiql/partiql-lang-kotlin/pull/391)

## [0.2.6-alpha] - 2021-02-18

### Added
- Functions to convert from UNIX epoch to TIMESTAMP and TIMESTAMP to UNIX epoch. [#330](https://github.com/partiql/partiql-lang-kotlin/pull/330)
- Adds a Rewriter to VisitorTransform [guide](https://github.com/partiql/partiql-lang-kotlin/blob/feb84730c64a2ad0f12c57bef3b1c45e21279538/docs/dev/RewriterToVisitorTransformGuide.md)

### Changed
- Migrates existing `AstRewriters` to PIG’s `VisitorTransform`. [#356](https://github.com/partiql/partiql-lang-kotlin/pull/356)

### Deprecated
- Deprecates AstRewriter, AstRewriterBase, MetaStrippingRewriter, RewriterTestBase

## [0.2.5-alpha] - 2021-01-12

### Added
- System stored procedure calls (`EXEC`) [#345](https://github.com/partiql/partiql-lang-kotlin/pull/345).
  More details on usage can be found [here](https://github.com/partiql/partiql-spec/issues/17)
- CLI: version number and commit hash in REPL [#339](https://github.com/partiql/partiql-lang-kotlin/pull/339)
- CLI: `PARTIQL_PRETTY` output-format for non-interactive use [#349](https://github.com/partiql/partiql-lang-kotlin/pull/349)
- Document thread safety of `CompilerPipeline` [#334](https://github.com/partiql/partiql-lang-kotlin/pull/334)

### Fixed
- Parsing of `TRIM` specification keywords (`BOTH`, `LEADING`, and `TRAILING`) [#326](https://github.com/partiql/partiql-lang-kotlin/pull/326)
- Build failure of `TimestampTemporalAccessorTests` when given a negative year [#346](https://github.com/partiql/partiql-lang-kotlin/pull/346)
- Running of parameterized tests and other test targets [#338](https://github.com/partiql/partiql-lang-kotlin/pull/338) and [#351](https://github.com/partiql/partiql-lang-kotlin/pull/351)

## [0.2.4-alpha] - 2020-11-08

### Fixed
- Fix `LIMIT` clause execution order #300
- Stop treating date parts as if they are string literals #317

## [0.2.3-alpha] - 2020-10-09

### Added
- `LET` (fom `FROM` clauses) implementation.

### Fixed
- fix: bigDecimalOf no-ops when given an Ion decimal [#293](https://github.com/partiql/partiql-lang-kotlin/pull/293)

## [0.1.4-alpha] - 2020-09-30

### Fixed
- This release is a backport of [#286](https://github.com/partiql/partiql-lang-kotlin/pull/286) which was applied on top
  of v0.1.3-alpha.

## [0.2.2-alpha] - 2020-09-29

### Changed
- Improvements to LIKE pattern compilation performance. [#284](https://github.com/partiql/partiql-lang-kotlin/pull/284)

## [0.2.1-alpha] - 2020-06-09

### Fixed
- Fixes [#246](https://github.com/partiql/partiql-lang-kotlin/pull/246)

## [0.2.0-alpha] - 2020-03-26

### Added
- Adds support for `DISTINCT`
- Initial set of DML features.  See [this](https://github.com/partiql/partiql-lang-kotlin/commit/16fefe0f096175a6a7b284313634dfad23858a38) for details.
- New error codes for division by `0` and modulo `0`

### Changed
- [breaking-change] `JOIN` is now **required** to provide an `ON` clause. In previous version an `ON` clause was optional
  which caused ambiguous parsing of multiple `JOIN` for which some had `ON` clause and some had not. The old behaviour
  was also out of Spec.

### Fixed
- Close CLI Stream correctly
- Preserve negative zero when writing values to the console in the REPL/CLI.
- Fix float negative zero equality

### Removed
- Removes invalid syntax check on case expressions with type parameters, e.g., `CAST(a AS DECIMAL(1, 2))` now does not
  throw

## [0.1.3-alpha] - 2020-03-26

### Fixed
- Fix [#228](https://github.com/partiql/partiql-lang-kotlin/pull/228) by removing invalid sanity check.

## [0.1.2-alpha] - 2020-01-10

### Changed
- Optimizes performance of IN operator when right side is used with many literals.

### Fixed
- Fix issue causing the REPL's output stream to be prematurely closed

## [0.1.1-alpha] - 2019-11-21

### Added
- Better printing support

### Changed
- Refactors code in CLI

### Fixed
- Fixes treatment of null values in JOIN conditions

## [0.1.0-alpha] - 2019-07-30

### Added
Initial alpha release of PartiQL.

[Unreleased]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.7.0-alpha...HEAD
[0.7.0-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.6.0-alpha...v0.7.0-alpha
[0.6.0-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.5.0-alpha...v0.6.0-alpha
[0.5.0-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.4.0-alpha...v0.5.0-alpha
[0.4.0-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.3.4-alpha...v0.4.0-alpha
[0.3.4-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.3.3-alpha...v0.3.4-alpha
[0.3.3-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.3.1-alpha...v0.3.3-alpha
[0.3.1-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.3.0-alpha...v0.3.1-alpha
[0.3.0-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.2.6-alpha...v0.3.0-alpha
[0.2.7-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.2.6-alpha...v0.2.7-alpha
[0.2.6-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.2.5-alpha...v0.2.6-alpha
[0.2.5-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.2.4-alpha...v0.2.5-alpha
[0.2.4-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.2.3-alpha...v0.2.4-alpha
[0.2.3-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.2.2-alpha...v0.2.3-alpha
[0.2.2-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.2.1-alpha...v0.2.2-alpha
[0.2.1-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.2.0-alpha...v0.2.1-alpha
[0.2.0-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.1.7-alpha...v0.2.0-alpha
[0.1.7-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.1.6-alpha...v0.1.7-alpha
[0.1.6-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.1.5-alpha...v0.1.6-alpha
[0.1.5-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.1.4-alpha...v0.1.5-alpha
[0.1.4-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.1.3-alpha...v0.1.4-alpha
[0.1.3-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.1.2-alpha...v0.1.3-alpha
[0.1.2-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.1.1-alpha...v0.1.2-alpha
[0.1.1-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.1.0-alpha...v0.1.1-alpha
[0.1.0-alpha]: https://github.com/partiql/partiql-lang-kotlin/releases/tag/v0.1.0-alpha
