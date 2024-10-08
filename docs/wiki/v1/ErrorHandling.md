# Usage Guide: Error Handling

## Introduction

The PartiQL library provides a robust error reporting mechanism, and this usage guide aims to show
how you can leverage the exposed APIs.

## Who is this for?

This usage guide is aimed at developers who use any one of [PartiQL's components](TODO) for their
application. If you are looking for how to change how errors are reported in the CLI, please run: `partiql --help`.

To elaborate on why this usage guide may be useful to you, the developer, let us assume that your
company provides a CLI to enable your customers to execute PartiQL queries. When a user is typing a query and
references a table that doesn't exist, your CLI might want to highlight that error and halt processing of the
query to save on cost. Or, your CLI might want to highlight the error but continue processing the query to accumulate
errors to better enable the developer to see all of their mistakes at once. In any case, the PartiQL
library allows developers to register their own error listeners to 

## Error Listeners

TODO

## Registering Error Listeners

If you have read other PartiQL usage guides, you might be aware that the PartiQL library comprises
[multiple components](TODO).

## Abort, by default

If you have opted to not register your own error listener, each component opts to use its own error listener which
throws an exception at the first encountered error. This behavior aims to protect developers who might have decided
to avoid reading this documentation. However, as seen above, this is easy to override.

## Output Structures

Each of PartiQL's components produce a structure for future use. The parser outputs an AST, the planner outputs a plan,
and the compiler outputs an executable. What happens when any of the components experience an error/warning?

The answer, as is often in software, depends. Since this error reporting mechanism allows developers to register error
listeners that accumulate all errors, the PartiQL components still continue processing until terminated by an error
listener. That being said, when error listeners receive an error, one must assume that the output of the component
is a dud and is incorrect. Therefore, if the parser has produced errors with a malformed AST, you shouldn't pass
the AST to the planner to continue evaluation.

However, if warnings have been emitted, the output can still be safely relied upon.

## Reference Implementations

The PartiQL CLI offers multiple ways to process warnings/errors. See the flags `-Werror`, `-w`,
`--max-errors`, and more when you run `partiql --help`. See the CLI Usage Guide [here](TODO). The
implementation details can be found in the [CLI subproject](TODO).
