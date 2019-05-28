# PartiQL Lang Kotlin

PartiQL is an implementation of [SQL++](http://db.ucsd.edu/wp-content/uploads/pdfs/375.pdf) that uses Amazon 
[Ion](http://amzn.github.io/ion-docs/) as its underlying type system.  SQL++/PartiQL is based on SQL92 and has 
added support for working with schemaless hierarchical data.  The support for hierarchical data is different from that 
of most SQL implementations in that support is directly built into syntax of the language instead of having been 
"tacked on" with a set of functions.

This repository contains an embeddable reference interpreter, test framework, and tests for PartiQL in Kotlin.

The easiest way to get started with PartiQL is to clone this repository locally, build, then 
[run the REPL](./docs/user/CLI.md).

## Status

PartiQL should be considered to be in "preview" status.  It has been in use within a number of Amazon
internal systems and an AWS product for over one year.  The behavior of the language itself is mostly stable 
however the public API of the interpreter is slated to undergo significant improvements in the near term.
(See the GitHub issues list for details.)

After the public API has been improved it will be released to Maven Central and will be available for general
production use.

## Building

To build this project, clone this repository and from its root directory execute:

```
$./gradlew build
```

This will build the reference interpreter and test framework, then run all unit and integration tests.

## Directory Structure

- `docs/user` documentation for developers embedding the interpreter in an application.
- `docs/dev` documentation for developers of the interpreter library.
- `lang` contains the source code of the library containing the interpreter.
- `cli` contains the source code of the command-line interface and interactive prompt. (CLI/REPL)
- `testframework` contains the source code of the integration test framework.
- `integration-test/test-scripts` contains the test scripts executed by the test framework as part of the
Gradle build.
- `integration-test/test-scripts-ignored` contains test scripts which cannot be executed during the Gradle build.

### Examples 

See the [examples](examples) project in this repository for examples covering
use of the PartiQL interpreter in your project.

## Contributing

See [CONTRIBUTING](CONTRIBUTING.md)

## License

This the works contained within this repository are licensed under the Apache 2.0 License.

See the [LICENSE](LICENSE) file.
