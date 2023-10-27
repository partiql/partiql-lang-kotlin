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

### Contributors
Thank you to all who have contributed!
- @<your-username>

-->

## [Unreleased]

### Added
- Adds top-level IR node creation functions.
- Adds `componentN` functions (destructuring) to IR nodes via Kotlin data classes
- Adds public `tag` field to IR nodes for associating metadata

### Changed
- StaticTypeInferencer and PlanTyper will not raise an error when an expression is inferred to `NULL` or `unionOf(NULL, MISSING)`. In these cases the StaticTypeInferencer and PlanTyper will still raise the Problem Code `ExpressionAlwaysReturnsNullOrMissing` but the severity of the problem has been changed to warning. In the case an expression always returns `MISSING`, problem code `ExpressionAlwaysReturnsMissing` will be raised, which will have problem severity of error.

### Deprecated

### Fixed

### Removed
- [Breaking] Removed IR factory in favor of static top-level functions. Change `Ast.foo()`
  to `foo()`

### Security

### Contributors
Thank you to all who have contributed!
- @rchowell

## [0.13.2-alpha] - 2023-09-29

### Added
- Adds overridden `toString()` method for Sprout-generated code.
- Adds CURRENT_DATE session variable to PartiQL.g4 and PartiQLParser
- Adds configurable AST to SQL pretty printer. Usage in Java `AstKt.sql(ast)` or in Kotlin `ast.sql()`.
- Support parsing, planning, and evaluation of Bitwise AND operator (&).
  - The Bitwise And Operator only works for integer operands.
  - The operator precedence may change based on the pending operator precedence [RFC](https://github.com/partiql/partiql-docs/issues/50).
- **EXPERIMENTAL** Adds `EXCLUDE` to parser, ast, plan, and plan schema inferencer
  - This feature is marked as experimental until an RFC is added https://github.com/partiql/partiql-spec/issues/39
  - NOTE: this feature is not currently implemented in the evaluator

### Fixed
- Fixes typing of scalar subqueries in the PartiQLSchemaInferencer. Note that usage of `SELECT *` in subqueries
  is not fully supported. Please make sure to handle InferenceExceptions.
- Fixes schema inferencer behavior for ORDER BY

### Contributors
Thank you to all who have contributed!
- @johnedquinn
- @RCHowell
- @yliuuuu
- @alancai98

## [0.13.1-alpha] - 2023-09-19

### Added
- Adds `isInterruptible` property to `CompileOptions`. The default value is `false`. Please see the KDocs for more information.
- Adds support for thread interruption in compilation and execution. If you'd like to opt-in to this addition, please see
  the `isInterruptible` addition above for more information.
- Adds support for CLI users to use CTRL-C to cancel long-running compilation/execution of queries

### Fixed
- Fix a bug in `FilterScanToKeyLookup` pass wherein it was rewriting primary key equality expressions with references
  to the candidate row on both sides.  Now it will correctly ignore such expressions.
- Fixes build failure for version `0.13.0` by publishing `partiql-plan` as an independent artifact. Please note that `partiql-plan` is experimental.

### Contributors
Thank you to all who have contributed!
- @dlurton
- @yliuuuu
- @am357
- @johnedquinn
- @alancai98

## [0.13.0-alpha] - 2023-09-07

### Added
- Adds `org.partiql.value` (experimental) package for reading/writing PartiQL values
- Adds function overloading to the `CompilerPipeline` and experimental `PartiQLCompilerPipeline`.
- Adds new method `getFunctions()` to `org.partiql.spi.Plugin`.
- Adds `PartiQLFunction` interface.
- Adds `FunctionSignature` and `FunctionParameter` class to `org/partiql/types/function`.
- Adds a new flag `--plugins` to PartiQL CLI to allow users to specify the root of their plugins directory.
  The default is `~/.partiql/plugins` . Each implementer of a plugin should place a directory under the
  plugins root containing the JAR corresponding with their plugin implementation.
  Example: `~/.partiql/plugins/customPlugin/customPlugin.jar`
- Adds serialization and deserialization between IonValue and `org.partiql.value`.
- Adds `org.partiql.ast` package and usage documentation
- Adds `org.partiql.parser` package and usage documentation
- Adds PartiQL's Timestamp Data Model.
- Adds support for Timestamp constructor call in Parser.
- Parsing of label patterns within node and edge graph patterns now supports
  disjunction `|`, conjunction `&`, negation `!`, and grouping.
- Adds default `equals` and `hashCode` methods for each generated abstract class of Sprout. This affects the generated
classes in `:partiql-ast` and `:partiql-plan`.
- Adds README to `partiql-types` package.
- Initializes PartiQL's Code Coverage library
  - Adds support for BRANCH and BRANCH-CONDITION Coverage
  - Adds integration with JUnit5 for ease-of-use
  - For more information, see the "Writing PartiQL Unit Tests" article in our GitHub Wiki.
- Adds new constructor parameters to all variants of `PartiQLResult`.
- Adds two new methods to `PartiQLResult`: `getCoverageData` and `getCoverageStructure`.

### Changed
- **Breaking**: all product types defined by the internal Sprout tool no longer generate interfaces. They are now abstract
  classes due to the generation of `equals` and `hashCode` methods. This change impacts many generated interfaces exposed
  in `:partiql-ast` and `:partiql-plan`.
- Standardizes `org/partiql/cli/functions/QueryDDB` and other built-in functions in `org/partiql/lang/eval/builtins` by the new `ExprFunction` format.
- **Breaking**: Redefines `org/partiql/lang/eval/ExprFunctionkt.call()` method by only invoking `callWithRequired` function.
- **Breaking**: Redefines `org/partiql/lang/eval/builtins/DynamicLookupExprFunction` by merging `variadicParameter` into `requiredParameters` as a `StaticType.LIST`. `callWithVariadic` is now replaced by `callWithRequired`.
- Upgrades ion-java to 1.10.2.
- **Breaking** (within experimental graph features): As part of extending
  the language of graph label patterns:
  - Changed the type of the field `label` in AST nodes
    `org.partiql.lang.domains.PartiqlAst.GraphMatchPatternPart.{Node,Edge}`,
    from `SymbolPrimitive` to new `GraphLabelSpec`.
  - Changed the names of subclasses of ` org.partiql.lang.graph.LabelSpec`,
    from `OneOf` to `Name`, and from `Whatever` to `Wildcard`.
- **Breaking** the package `org.partiql.lang.errors` has been moved to `org.partiql.errors`, moved classes include
  - `org.partiql.lang.errors.ErrorCategory` -> `org.partiql.errors.ErrorCategory`
  - `org.partiql.lang.errors.Property` -> `org.partiql.errors.Property`
  - `org.partiql.lang.errors.PropertyValue` -> `org.partiql.errors.PropertyValue`
  - `org.partiql.lang.errors.PropertyType` -> `org.partiql.errors.PropertyType`
  - `org.partiql.lang.errors.PropertyValueMap` -> `org.partiql.errors.PropertyValueMap`
  - `org.partiql.lang.errors.ErrorCode` -> `org.partiql.errors.ErrorCode`
  - `org.partiql.lang.errors.Problem` -> `org.partiql.errors.Problem`
  - `org.partiql.lang.errors.ProblemDetails` -> `org.partiql.errors.ProblemDetails`
  - `org.partiql.lang.errors.ProblemSeverity` -> `org.partiql.errors.ProblemSeverity`
  - `org.partiql.lang.errors.ProblemHandler` -> `org.partiql.errors.ProblemHandler`
- **Breaking** the `sourceLocation` field of `org.partiql.errors.Problem` was changed from `org.partiql.lang.ast.SoureceLocationMeta` to `org.partiql.errors.ProblemLocation`.
- Removed `Nullable<Value` implementations of PartiQLValue and made the standard implementations nullable.
- Using PartiQLValueType requires optin; this was a miss from an earlier commit.
- **Breaking** removed redundant ValueParameter from FunctionParameter as all parameters are values.
- Introduces `isNullCall` and `isNullable` properties to FunctionSignature.
- Removed `Nullable...Value` implementations of PartiQLValue and made the standard implementations nullable.
- Using PartiQLValueType requires optin; this was a miss from an earlier commit.
- Modified timestamp static type to model precision and time zone. 

### Deprecated
- **Breaking**: Deprecates the `Arguments`, `RequiredArgs`, `RequiredWithOptional`, and `RequiredWithVariadic` classes, 
  along with the `callWithOptional()`, `callWithVariadic()`, and the overloaded `call()` methods in the `ExprFunction` class, 
  marking them with a Deprecation Level of ERROR. Now, it's recommended to use 
  `call(session: EvaluationSession, args: List<ExprValue>)` and `callWithRequired()` instead.
- **Breaking**: Deprecates `optionalParameter` and `variadicParameter` in the `FunctionSignature` with a Deprecation
  Level of ERROR. Please use multiple implementations of ExprFunction and use the LIST ExprValue to
  represent variadic parameters instead.

### Fixed

### Removed
- **Breaking**: Removes `optionalParameter` and `variadicParameter` from `org.partiql.lang.types.FunctionSignature`. To continue support for evaluation of `optionalParameters`, please create another same-named function. To continue support for evaluation of `variadicParameter`, please use a `StaticType.LIST` to hold all previously variadic parameters.
  As this changes coincides with the addition of function overloading, only `callWithRequired` will be invoked upon execution of an `ExprFunction`. Note: Function overloading is now allowed, which is the reason for the removal of `optionalParameter` and `variadicParameter`.
- **Breaking**: Removes unused class `Arguments` from `org.partiql.lang.eval`.
- **Breaking**: Removes unused parameter `args: Arguments` from `org.partiql.lang.eval.ExprFunctionkt.call()` method.

### Security

### Contributors
Thank you to all who have contributed!
- @howero
- @yuxtang-amazon
- @yliuuuu
- @johqunn
- @<your-username>

## [0.12.0-alpha] - 2023-06-14

### Added

- Adds support for using EXCLUDED within DML ON-CONFLICT-ACTION conditions. Closes #1111.

### Changed

- Updates Kotlin target from 1.4 (DEPRECATED) to 1.6
- Moves PartiqlAst, PartiqlLogical, PartiqlLogicalResolved, and PartiqlPhysical (along with the transforms)
  to a new project, `partiql-ast`. These are still imported into `partiql-lang` with the `api` annotation. Therefore,
  no action is required to consume the migrated classes. However, this now gives consumers of the AST, Experimental Plans,
  Visitors, and VisitorTransforms the option of importing them directly using: `org.partiql:partiql-ast:${VERSION}`. 
  The file `partiql.ion` is still published in the `partiql-lang-kotlin` JAR.
- Moves internal class org.partiql.lang.syntax.PartiQLParser to org.partiql.lang.syntax.impl.PartiQLPigParser as we refactor for explicit API.
- Moves ANTLR grammar to `partiql-parser` package. The files `PartiQL.g4` and `PartiQLTokens.g4` are still published in the `partiql-lang-kotlin` JAR.
- **Breaking**: Adds new property, `rowAlias`, to experimental `PartiqlLogical.DmlOperation.DmlUpdate`,
  `PartiqlLogical.DmlOperation.DmlReplace`, `PartiqlLogicalResolved.DmlOperation.DmlUpdate`,
  `PartiqlLogicalResolved.DmlOperation.DmlReplace`, `PartiqlPhysical.DmlOperation.DmlUpdate`, and
  `PartiqlPhysical.DmlOperation.DmlReplace`.

### Deprecated

### Fixed

### Removed
- **Breaking**: Removes deprecated `org.partiql.annotations.PartiQLExperimental`
- **Breaking**: Removes deprecated/unused `blacklist()` and `denyList()` from `org.partiql.lang.eval`
- **Breaking**: Removes deprecated enum `LEGACY` in `org.partiql.lang.eval.CompileOptions`
- **Breaking**: Removes deprecated `org.partiql.lang.eval.ExprValueFactory`, as well as all methods that had its instance
  among arguments. The counterparts of these methods without an ExprValueFactory are still available. The affected methods
  include: `ofIonStruct()` in `org.partiql.lang.eval.Bindings`, a constructor of `org.partiql.lang.CompilerPipeline`,
  `convert()` in `org.partiql.lang.eval.io.DelimitedValues.ConversionMode`, `exprValue()` from
  `org.partiql.lang.eval.io.DelimitedValues`, a constructor for `org.partiql.lang.eval.physical.EvaluatorState`, and
  `valueFactory`, `build`, `builder`, `standard` in `org.partiql.lang.CompilerPipeline`
- **Breaking**: Removes deprecated `org.partiql.lang.eval.visitors.GroupKeyReferencesVisitorTransform`

- **Breaking**: Removes `org.partiql.lang.mappers.StaticTypeMapper`
- **Breaking**: Removes `org.partiql.lang.mappers.IonSchemaMapper`
- **Breaking**: Removes `org.partiql.lang.mappers.TypeNotFoundException`
- **Breaking**: Removes `org.partiql.lang.mappers.getBaseTypeName()`
- **Breaking**: Removes unused/deprecated enums `KEYWORD`, `TOKEN_TYPE`, `EXPECTED_TOKEN_TYPE`, `EXPECTED_TOKEN_TYPE_1_OF_2`,
  `EXPECTED_TOKEN_TYPE_2_OF_2`, `TIMESTAMP_STRING`, `NARY_OP` from `org.partiql.lang.errors.Property`
- **Breaking**: Removes unused `tokenTypeValue()` from `org.partiql.lang.errors.PropertyValue`
- **Breaking**: Removes unused `TOKEN_CLASS` from `org.partiql.lang.errors.PropertyType`
- **Breaking**: Removes unused `set(Property, TokenType)` from `org.partiql.lang.errors.PropertyValueMap`
- **Breaking**: Removes unused/deprecated enums `LEXER_INVALID_NAME`, `LEXER_INVALID_OPERATOR`, `LEXER_INVALID_ION_LITERAL`,
  `PARSE_EXPECTED_KEYWORD`, `PARSE_EXPECTED_TOKEN_TYPE`, `PARSE_EXPECTED_2_TOKEN_TYPES`, `PARSE_EXPECTED_TYPE_NAME`,
  `PARSE_EXPECTED_WHEN_CLAUSE`, `PARSE_EXPECTED_WHERE_CLAUSE`, `PARSE_EXPECTED_CONFLICT_ACTION`, `PARSE_EXPECTED_RETURNING_CLAUSE`,
  `PARSE_UNSUPPORTED_RETURNING_CLAUSE_SYNTAX`, `PARSE_UNSUPPORTED_TOKEN`, `PARSE_EXPECTED_MEMBER`, `PARSE_UNSUPPORTED_SELECT`,
  `PARSE_UNSUPPORTED_CASE`, `PARSE_UNSUPPORTED_CASE_CLAUSE`, `PARSE_UNSUPPORTED_ALIAS`, `PARSE_UNSUPPORTED_SYNTAX`,
  `PARSE_UNSUPPORTED_SYNTAX`, `PARSE_INVALID_PATH_COMPONENT`, `PARSE_MISSING_IDENT_AFTER_AT`, `PARSE_UNEXPECTED_OPERATOR`,
  `PARSE_UNEXPECTED_TERM`, `PARSE_UNEXPECTED_KEYWORD`, `PARSE_EXPECTED_EXPRESSION`, `PARSE_EXPECTED_LEFT_PAREN_AFTER_CAST`,
  `PARSE_EXPECTED_LEFT_PAREN_VALUE_CONSTRUCTOR`, `PARSE_EXPECTED_LEFT_PAREN_BUILTIN_FUNCTION_CALL`,
  `PARSE_EXPECTED_RIGHT_PAREN_BUILTIN_FUNCTION_CALL`, `PARSE_EXPECTED_ARGUMENT_DELIMITER`, `PARSE_CAST_ARITY`,
  `PARSE_INVALID_TYPE_PARAM`, `PARSE_EMPTY_SELECT`, `PARSE_SELECT_MISSING_FROM`, `PARSE_MISSING_OPERATION`,
  `PARSE_MISSING_SET_ASSIGNMENT`, `PARSE_EXPECTED_IDENT_FOR_GROUP_NAME`, `PARSE_EXPECTED_IDENT_FOR_ALIAS`,
  `PARSE_EXPECTED_KEYWORD_FOR_MATCH`, `PARSE_EXPECTED_IDENT_FOR_MATCH`, `PARSE_EXPECTED_LEFT_PAREN_FOR_MATCH_NODE`,
  `PARSE_EXPECTED_RIGHT_PAREN_FOR_MATCH_NODE`, `PARSE_EXPECTED_LEFT_BRACKET_FOR_MATCH_EDGE`,
  `PARSE_EXPECTED_RIGHT_BRACKET_FOR_MATCH_EDGE`, `PARSE_EXPECTED_PARENTHESIZED_PATTERN`, `PARSE_EXPECTED_EDGE_PATTERN_MATCH_EDGE`,
  `PARSE_EXPECTED_EQUALS_FOR_MATCH_PATH_VARIABLE`, `PARSE_EXPECTED_AS_FOR_LET`, `PARSE_UNSUPPORTED_CALL_WITH_STAR`,
  `PARSE_NON_UNARY_AGREGATE_FUNCTION_CALL`, `PARSE_NO_STORED_PROCEDURE_PROVIDED`, `PARSE_MALFORMED_JOIN`,
  `PARSE_EXPECTED_IDENT_FOR_AT`, `PARSE_INVALID_CONTEXT_FOR_WILDCARD_IN_SELECT_LIST`,
  `PARSE_CANNOT_MIX_SQB_AND_WILDCARD_IN_SELECT_LIST`, `PARSE_ASTERISK_IS_NOT_ALONE_IN_SELECT_LIST`,
  `SEMANTIC_DUPLICATE_ALIASES_IN_SELECT_LIST_ITEM`, `SEMANTIC_NO_SUCH_FUNCTION`, `SEMANTIC_INCORRECT_ARGUMENT_TYPES_TO_FUNC_CALL`,
  `EVALUATOR_NON_TEXT_STRUCT_KEY`, `SEMANTIC_INCORRECT_NODE_ARITY`, `SEMANTIC_ASTERISK_USED_WITH_OTHER_ITEMS`,
  `getKeyword()` from `org.partiql.lang.errors.ErrorCode`
- **Breaking**: Removes unused `fillErrorContext()` from `org.partiql.lang.eval`
- **Breaking**: Removes deprecated `isNull()` from `org.partiql.lang.eval.ExprValueType`
- **Breaking**: Remove unused `fromTypeName()`, `fromSqlDataType()`, `fromSqlDataTypeOrNull()` from `org.partiql.lang.eval.ExprValueType`
- **Breaking**: Removes deprecated `org.partiql.lang.syntax.Lexer`
- **Breaking**: Removes unused `STANDARD_AGGREGATE_FUNCTIONS`, `OperatorPrecedenceGroups` from `org.partiql.lang.syntax`
- **Breaking**: Removes deprecated `org.partiql.lang.syntax.SourcePosition`
- **Breaking**: Removes deprecated `org.partiql.lang.syntax.SourceSpan`
- **Breaking**: Removes deprecated `org.partiql.lang.syntax.Token`
- **Breaking**: Removes deprecated `org.partiql.lang.syntax.TokenType`
- **Breaking**: Stops publishing PartiQL ISL to Maven Central. The last published version is https://central.sonatype.com/artifact/org.partiql/partiql-isl-kotlin/0.11.0
- **Breaking**: Removes unused package `org.partiql.lang.schemadiscovery` which included unused classes of:
  `SchemaInferencerFromExample`, `SchemaInferencerFromExampleImpl`, `TypeConstraint`, `NormalizeNullableVisitorTransform`,
  `NormalizeDecimalPrecisionsToUpToRange`, and `IonExampleParser`.
- **Breaking**: Removes unused package `org.partiql.lang.partiqlisl` which includes unused classes/methods: `ResourceAuthority`,
  `getResourceAuthority()`, and `loadPartiqlIsl()`.
- **Breaking**: Plan nodes cannot be directly instantiated. To instantiate, use the `Plan` (DEFAULT) factory.
- **Breaking**: PlanRewriter has been moved from `org.partiql.plan.visitor.PlanRewriter` to `org.partiql.plan.util.PlanRewriter`

### Security

### Contributors
Thank you to all who have contributed!
- @johnedquinn
- @RCHowell
- @vgapeyev

## [0.11.0-alpha] - 2023-05-22

### Added

- Adds an initial implementation of GPML (Graph Pattern Matching Language), following 
  PartiQL [RFC-0025](https://github.com/partiql/partiql-docs/blob/main/RFCs/0025-graph-data-model.md) 
  and [RFC-0033](https://github.com/partiql/partiql-docs/blob/main/RFCs/0033-graph-query.md).
  This initial implementation includes:
  - A file format for external graphs, defined as a schema in ISL (Ion Schema Language), 
    as well as an in-memory graph data model and a reader for loading external graphs into it.
  - CLI shell commands `!add_graph` and `!add_graph_from_file` for bringing 
    externally-defined graphs into the evaluation environment. 
  - Evaluation of straight-path patterns with simple label matching and 
    all directed/undirected edge patterns.
- Adds new `TupleConstraint` variant, `Ordered`, to represent ordering in `StructType`. See the KDoc for more information.

### Changed

- **Breaking**: The `fields` attribute of `org.partiql.types.StructType` is no longer a `Map<String, StaticType>`. It is
  now a `List<org.partiql.types.StructType.Field>`, where `Field` contains a `key (String)` and `value (StaticType)`. This
  is to allow duplicates within the `StructType`.

### Deprecated

### Fixed

- Fixes the ability for JOIN predicates to access the FROM source aliases and corresponding attributes.

### Removed

### Security

## [0.10.0-alpha] - 2023-05-05

### Added
- Added numeric builtins ABS, SQRT, EXP, LN, POW, MOD.
- Added standard SQL built-in functions POSITION, OVERLAY, LENGTH, BIT_LENGTH, OCTET_LENGTH, CARDINALITY,
  an additional builtin TEXT_REPLACE, and standard SQL aggregations on booleans EVERY, ANY, SOME.
- **Breaking** Added coercion of SQL-style subquery to a single value, as defined in SQL for
  subqueries occurring in a single-value context and outlined in Chapter 9 of the PartiQL specification.
  This is backward incompatible with the prior behavior (which left the computed collection as is),
  but brings it in conformance with the specification.
- Added `partiql-plan` package which contains experimental PartiQL Plan data structures.
- Initializes SPI Framework under `partiql-spi`.
- Models experimental `Schema` with constraints.
  With this change, we're introducing `Tuple` and `Collection` constraints to be able to model the shape of data as
  constraints.
- Introduces the PartiQLSchemaInferencer and PlannerSession
  - The PlannerSession describes the current session and is used by the PartiQLSchemaInferencer.
  - The PartiQLSchemaInferencer provides a function, `infer`, to aid in inferring the output `StaticType` of a
    PartiQL Query. See the KDoc for more information and examples.
- Adds back ability to convert an `IonDatagram` to an `ExprValue` using `of(value: IonValue): ExprValue` and `newFromIonValue(value: IonValue): ExprValue`
- Adds support for SQL's CURRENT_USER in the AST, EvaluatingCompiler, experimental planner implementation, and Schema Inferencer.
  - Adds the AST node `session_attribute`.
  - Adds the function `EvaluationSession.Builder::user()` to add the CURRENT_USER to the EvaluationSession
- Adds support for parsing and planning of `INSERT INTO .. AS <alias> ... ON CONFLICT DO [UPDATE|REPLACE] EXCLUDED WHERE <expr>`
- Adds the `statement.dml` and `dml_operation` node to the experimental PartiQL Physical Plan.

### Changed

- Deprecates the project level opt-in annotation `PartiQLExperimental` and split it into feature level. `ExperimentalPartiQLCompilerPipeline` and `ExperimentalWindowFunctions`.
- **Breaking**: Moves StaticType to `partiql-types`.
  - All references to static types need to modify their imports accordingly. For example,
    `org.partiql.lang.types.IntType` is now `org.partiql.types.IntType`.
  - Please modify existing dependencies accordingly. You may need to add dependency `org.partiql:partiql-types:0.10.0`.
  - Also, several methods within StaticType have been moved to a utility class within `partiql-lang-kotln`. See the below list:
    1. `org.partiql.lang.types.StaticType.fromExprValueType` -> `org.partiql.lang.types.StaticTypeUtils.staticTypeFromExprValueType`
    2. `org.partiql.lang.types.StaticType.fromExprValue` -> `org.partiql.lang.types.StaticTypeUtils.staticTypeFromExprValue`
    3. `org.partiql.lang.types.StaticType.isInstance` -> `org.partiql.lang.types.StaticTypeUtils.isInstance`
    4. `org.partiql.lang.types.StaticType.isComparableTo` -> `org.partiql.lang.types.StaticTypeUtils.areStaticTypesComparable`
    5. `org.partiql.lang.types.StaticType.isSubTypeOf` -> `org.partiql.lang.types.StaticTypeUtils.isSubTypeOf`
    5. `org.partiql.lang.types.StaticType.typeDomain` -> `org.partiql.lang.types.StaticTypeUtils.getTypeDomain`
    6. `org.partiql.lang.types.SingleType.getRuntimeType` -> `org.partiql.lang.types.StaticTypeUtils.getRuntimeType`
    7. `org.partiql.lang.types.StringType.StringLengthConstraint.matches` -> `org.partiql.lang.types.StaticTypeUtils.stringLengthConstraintMatches`
- **Breaking**: Removes deprecated `ionSystem()` function from PartiQLCompilerBuilder and PartiQLParserBuilder
- **Breaking**: Adds a new property `as_alias` to the `insert` AST node.
- **Breaking**: Adds new property `condition` to the AST nodes of `do_replace` and `do_update`
- **Breaking**: Adds `target_alias` property to the `dml_insert`, `dml_replace`, and `dml_update` nodes within the
  Logical and Logical Resolved plans
- **Breaking**: Adds `condition` property to the `dml_replace` and `dml_update` nodes within the
  Logical and Logical Resolved plans

### Deprecated

- `ExprValueFactory` interface marked as deprecated. Equivalent `ExprValue` construction methods are implemented in the `ExprValue` interface as static methods.

### Fixed

- Javadoc jar now contains dokka docs (was broken by gradle commit from 0.9.0)
- ANTLR (PartiQL.g4, PartiQLTokens.g4) and PIG (org/partiql/type-domains/partiql.ion) sources
  are back to being distributed with the jar.
- CLI no longer terminates on user errors in submitted PartiQL (when printing out the AST with !!)
  and no longer prints out stack traces upon user errors.
- Constrained Decimal matching logic.
- Parsing INSERT statements with aliases no longer loses the original table name. Closes #1043.
- Parsing INSERT statements with the legacy ON CONFLICT clause is no longer valid. Similarly, parsing the legacy INSERT
  statement with the up-to-date ON CONFLICT clause is no longer valid. Closes #1063.

### Removed

- The deprecated `IonValue` property in `ExprValue` interface is now removed.
- Removed partiql-extensions to partiql-cli `org.partiql.cli.functions`
- Removed IonSystem from PartiQLParserBuilder
- **Breaking**: Removes node `statement.dml_query` from the experimental PartiQL Physical Plan. Please see the added
  `statement.dml` and `dml_operation` nodes.

### Security

## [0.9.4-alpha] - 2023-04-20

This version reverts many accidental breaking changes introduced in v0.9.3. Its contents are equivalent to v0.9.2.

## [0.9.3-alpha] - 2023-04-12

This version accidentally released multiple breaking changes and is not recommended. Please use v0.9.4 to avoid
breaking changes if migrating from v0.9.2. The breaking changes accidentally introduced in v0.9.3 can be found in v0.10.0.

## [0.9.2-alpha] - 2023-01-20

### Added
- Adds ability to pipe queries to the CLI.
- Adds ability to run PartiQL files as executables by adding support for shebangs.
- Adds experimental syntax for CREATE TABLE, towards addressing 
  [#36](https://github.com/partiql/partiql-docs/issues/36) of specifying PartiQL DDL.

### Changed

### Deprecated

### Fixed
- Fixes list/bag ExprValue creation in plan evaluator
- Fixes gradle build issues.

## [0.9.1-alpha] - 2023-01-04

### Added
- Makes the following `PartiQLCompilerBuilder` functions are moved to public
  - `customOperatorFactories`
  - `customFunctions`
  - `customProcedures`

## [0.9.0-alpha] - 2022-12-13

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


## [0.8.2-alpha] - 2022-11-28
### Added
- Adds simple auto-completion to the CLI.

### Changed
- Increases the performance of the PartiQLParser by changing the parsing strategy
  - The PartiQLParser now attempts to parse queries using the SLL Prediction Mode set by ANTLR
  - If unable to parse via SLL Prediction Mode, it attempts to parse using the slower LL Prediction Mode
  - Modifications have also been made to the ANTLR grammar to increase the speed of parsing joined table references
  - Updates how the PartiQLParser handles parameter indexes to remove the double-pass while lexing

## [0.8.1-alpha] - 2022-10-28

### Added
- Extends statement redaction to support `INSERT/REPLACE/UPSERT INTO`.


## [0.8.0-alpha] - 2022-10-14

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

[Unreleased]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.13.2-alpha...HEAD
[0.13.2-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.13.1-alpha...v0.13.2-alpha
[0.13.1-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.13.0-alpha...v0.13.1-alpha
[0.13.0-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.12.0-alpha...v0.13.0-alpha
[0.12.0-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.11.0-alpha...v0.12.0-alpha
[0.11.0-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.10.0-alpha...v0.11.0-alpha
[0.10.0-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.9.4-alpha...v0.10.0-alpha
[0.9.4-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.9.3-alpha...v0.9.4-alpha
[0.9.3-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.9.2-alpha...v0.9.3-alpha
[0.9.2-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.9.1-alpha...v0.9.2-alpha
[0.9.1-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.9.0-alpha...v0.9.1-alpha
[0.9.0-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.8.2-alpha...v0.9.0-alpha
[0.8.2-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.8.1-alpha...v0.8.2-alpha
[0.8.1-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.8.0-alpha...v0.8.1-alpha
[0.8.0-alpha]: https://github.com/partiql/partiql-lang-kotlin/compare/v0.7.0-alpha...v0.8.0-alpha
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
