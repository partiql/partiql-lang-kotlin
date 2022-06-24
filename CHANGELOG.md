- TODO add list of commits
- TODO Ensure breaking changes are included under CHANGED
- TODO Add the process to README.md

# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.7.0-alpha] - 2022-06-23
### Added
- An experimental query planner API along with logical and physical plans structures with the support of non-default physical operator implementations.
- An optional flag, `--wrap-ion`, to give users the old functionality of reading multiple Ion values (previous behavior).
- Benchmark framework and benchmark implementation for `LIKE` performance
- Convenience `StaticType` for `TEXT` and `NUMERIC`
- Enable `MIN` and `MAX` to work with all the data-types.
- Introduction of `extensions` and addition of the `query_ddb` function to allow querying AWS DynamoDB from the CLI.
- Replacement of REPL with [JLine](https://jline.github.io/) shell
- Syntax highlighting for CLI
- Three additional CLI flags:
  - `-r --projection-iter-behavior:` Controls the behavior of ExprValue.iterator in the projection result: (default: FILTER_MISSING) [FILTER_MISSING, UNFILTERED]
  - `-t --typed-op-behavior`: indicates how CAST should behave: (default: HONOR_PARAMETERS) [LEGACY, HONOR_PARAMETERS]
  - `-v --undefined-variable-behavior`: Defines the behavior when a non-existent variable is referenced: (default: ERROR) [ERROR, MISSING]
- `--input-format` flag to the CLI
- `CEIL` and `FLOOR` functions
- `DATE/TIME` formatting and the support for `DATE/TIME` in Ion data format

### Fixed
- Fix `write_file` CLI function; the old function required the input to be a `string`, but it must be a generic type.
- Add `ktlint` task dependency to enable execution optimizations and reducing he build time by ~ `30%`.
- Adjust handling of Ion input (requiring single value)
- Adjust handling of Ion output (outputting the real value)
- Adds missing metas to `ORDER BY` `ExprNode` and `PartiqlAst` (E.g. source location), which limits error message reporting.

### Changed
- `LIKE` matching via compilation to `java.util.regex.Pattern`
- Run `ktlint` before tests.

## [0.6.0-alpha] - 2022-04-06
### Added
- [cli] Add permissive mode evaluation option to CLI/REPL (#545)
- `ORDER BY` implementation in evaluator (#554)
- [build] Adds `ktlint` to gradle build (#542)

### Fixed
- Fix all compiler warnings (#562)

### Changed
- For `ExprFunction`, replace `Environment` with `EvaluationSession` (#559)
- Migrate to PIG `v0.5.0` (#563)
- [build] Increase build performance w/ Gradle upgrade to 7.4 (#539)
- [build] Upgrade `dokka` to `1.6.10`, set `org.gradle.jvmargs` (#568)

### Changed
- Changed `Path` AST node to use its root node source location (#527)
- Improve the `CAST` assertion assertEval (#523)
- [build] Bump Kotlin version to `1.4.32` from `1.4.0` (#548)

### Deprecated
- Deprecate `ExprNode` (#535)

### Removed
- Clean up `ExprFunction` test (#529)

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
- Adds support for `OFFSET` (#451)
- [cli] Uses Apache's CSVParser for file reading (#474) and ability to read custom CSV configurations (#480)

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
- Fixes `GROUP BY` for more than 2 nested path expressions (#461)
- [cli] Fixes `CLI` command bug when input data is empty (#478)
- [cli] Fixes `CLI` bug when outputting `IONTEXT` to file (#479)

### Changed
- Upgrades Kotlin version to `1.4`
- Modeled `NULLIF` and `COALESCE` as `PartiqlAst` nodes rather than `ExprFunctions`
- Started parameterization of evaluation tests

### Deprecated
- Deprecate `ExprNode` in parser (#464)

### Removed
- Removes wildcard imports in cli (#483) and lang (#488)
- Removes `DateTimeType` `sealed` class (#489)
- Renames `DateTimePart` type to `DatePart` (#506)

## [0.4.0-alpha] - 2021-10-07

### Added
- Sets up JMH for PartiQL (#427)
- Allows for default timezone configuration (#449)

### Fixed
- Fixes struct handling of non-text struct field keys (#450)

### Changed

- Uses new PIG major version `v0.4.0` (#454)
- Moves usage of default timezone from parser to evaluator (#448)

## [0.2.7-alpha] - 2021-09-13

### Fixed
- Cherry picks "Fix bug causing multiple nested nots to parse very slowly (#436)" for `v0.2.7` release #439 
- Cherry picks "Use LazyThreadSafteyMode.PUBLICATION instead of NONE (#433)" for `v0.2.7` release #440

## [0.1.7-alpha] - 2021-09-13

### Fixed
- Cherry picks "Fix bug causing multiple nested nots to parse very slowly (#436)" for `v0.1.7` release #441
- Cherry picks "Use LazyThreadSafteyMode.PUBLICATION instead of NONE (#433)" for `v0.1.7` release #442

## [0.3.4-alpha] - 2021-09-10
### Fixed
- Bug causing multiple nested nots to parse very slowly (#436)

## [0.3.3-alpha] - 2021-09-09
### Changed
- Uses `LazyThreadSafteyMode.PUBLICATION` instead of NONE

## [0.3.1-alpha] - 2021-06-18
### Fixed
- Prevent the `ORDER BY` clause from being dropped in visitor transforms #420

## [0.3.0-alpha] - 2021-06-09
### Added
- `DATE` and `TIME` data types
- Enhancements made by/for DynamoDB
- Compile-time `Thread.interrupted()` checks were added to help mitigate the impact of compiling extremely large SQL 
  queries.
- Various performance improvements to the compiler were added.

### Fixed
- Fixes parser for the top level tokens (#369)
- Make `SIZE` function work with s-expressions. (#379)
- A number of other minor bug fixes and technical debt has been addressed. For a complete list of PRs that made it into 
  this release, please see the v0.3.0 GitHub milestone.

### Changed
- The modeling of `ExprNode` and `PartiqlAst` APIs has changed as needed to account for the enhancements to DML statements 
  and `ORDER BY`. Customers using these APIs may be affected.
- Other minor API changes.

## [0.1.6-alpha] - 2021-05-13

## [0.1.5-alpha] - 2021-04-27

## [0.2.6-alpha] - 2021-02-18

## [0.2.5-alpha] - 2021-01-12

## [0.2.4-alpha] - 2020-11-08

## [0.2.3-alpha] - 2020-10-09

## [0.1.4-alpha] - 2020-09-30

## [0.2.2-alpha] - 2020-09-29

## [0.2.1-alpha] - 2020-06-09

## [0.2.0-alpha] - 2020-03-26

## [0.1.3-alpha] - 2020-03-26

## [0.1.2-alpha] - 2020-01-10

## [0.1.1-alpha] - 2019-11-21

## [0.1.0-alpha] - 2019-07-30


[Unreleased]: https://github.com/olivierlacan/keep-a-changelog/compare/v1.0.0...HEAD
[0.7.0]: https://github.com/olivierlacan/keep-a-changelog/compare/v0.0.8...v0.1.0
[0.6.0]: https://github.com/olivierlacan/keep-a-changelog/compare/v0.0.7...v0.0.8
[0.5.0]: https://github.com/olivierlacan/keep-a-changelog/compare/v0.0.6...v0.0.7
[0.4.0]: https://github.com/olivierlacan/keep-a-changelog/compare/v0.0.5...v0.0.6
[0.2.7]: https://github.com/olivierlacan/keep-a-changelog/compare/v0.0.4...v0.0.5
[0.1.7]: https://github.com/olivierlacan/keep-a-changelog/compare/v0.0.3...v0.0.4
[0.3.4]: https://github.com/olivierlacan/keep-a-changelog/compare/v0.0.2...v0.0.3
[0.3.3]: https://github.com/olivierlacan/keep-a-changelog/compare/v0.0.1...v0.0.2
[0.3.1]: https://github.com/olivierlacan/keep-a-changelog/releases/tag/v0.0.1
[0.3.0]: https://github.com/olivierlacan/keep-a-changelog/releases/tag/v0.0.1
[0.1.6]: https://github.com/olivierlacan/keep-a-changelog/releases/tag/v0.0.1
[0.1.5]: https://github.com/olivierlacan/keep-a-changelog/releases/tag/v0.0.1
[0.2.6]: https://github.com/olivierlacan/keep-a-changelog/releases/tag/v0.0.1
[0.2.5]: https://github.com/olivierlacan/keep-a-changelog/releases/tag/v0.0.1
[0.2.4]: https://github.com/olivierlacan/keep-a-changelog/releases/tag/v0.0.1
[0.2.3]: https://github.com/olivierlacan/keep-a-changelog/releases/tag/v0.0.1
[0.1.4]: https://github.com/olivierlacan/keep-a-changelog/releases/tag/v0.0.1
[0.2.2]: https://github.com/olivierlacan/keep-a-changelog/releases/tag/v0.0.1
[0.2.1]: https://github.com/olivierlacan/keep-a-changelog/releases/tag/v0.0.1
[0.2.0]: https://github.com/olivierlacan/keep-a-changelog/releases/tag/v0.0.1
[0.1.3]: https://github.com/olivierlacan/keep-a-changelog/releases/tag/v0.0.1
[0.1.2]: https://github.com/olivierlacan/keep-a-changelog/releases/tag/v0.0.1
[0.1.1]: https://github.com/olivierlacan/keep-a-changelog/releases/tag/v0.0.1
[0.1.0]: https://github.com/olivierlacan/keep-a-changelog/releases/tag/v0.0.1
