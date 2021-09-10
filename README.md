
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.partiql/partiql-lang-kotlin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.partiql/partiql-lang-kotlin)
[![Build Status](https://travis-ci.org/partiql/partiql-lang-kotlin.svg?branch=master)](https://travis-ci.org/partiql/partiql-lang-kotlin)
[![codecov](https://codecov.io/gh/partiql/partiql-lang-kotlin/branch/master/graph/badge.svg)](https://codecov.io/gh/partiql/partiql-lang-kotlin)



# PartiQL Lang Kotlin

This is a Kotlin implementation of the [PartiQL specification](https://partiql.org/assets/PartiQL-Specification.pdf).
PartiQL is based on SQL-92 and has added support for working with schemaless hierarchical data.
PartiQL’s extensions to SQL are easy to understand, treat nested data as first class citizens and
compose seamlessly with each other and SQL.


This repository contains an embeddable reference interpreter, test framework, and tests for PartiQL in Kotlin.

The easiest way to get started with PartiQL is to clone this repository locally, build, then
[run the REPL](./docs/user/CLI.md).

## Status

PartiQL should be considered to be in "preview" status. It has been in use within a number of Amazon internal
systems and an AWS product for over one year. The behavior of the language itself is mostly stable however
the public API of the interpreter is slated to undergo significant improvements in the near term. (See the
GitHub issues list for details.)

## Using In Your Project

This project is published to [Maven Central](https://search.maven.org/artifact/org.partiql/partiql-lang-kotlin).

| Group ID | Artifact ID | Recommended Version |
|----------|-------------|---------------------|
| `org.partiql` | `partiql-lang-kotlin` | `0.3.4`| 


For Maven builds, add this to your `pom.xml`:

```
<dependency>
  <groupId>org.partiql</groupId>
  <artifactId>partiql-lang-kotlin</artifactId>
  <version>{version}</version>
</dependency>
```

For Gradle 5 and later, add this to your `build.gradle`:

```
repositories {
    mavenCentral()
}

dependencies {
    implementation "org.partiql:partiql-lang-kotlin:{version}"
}
```

Be sure to replace `{version}` with the desired version.

## Building

To build this project, clone this repository and from its root directory execute:

```
$./gradlew build
```

This will build the reference interpreter and test framework, then run all unit and integration tests.

### Building the documentation

[Instructions on how to build PartiQL's documentation](docs/Docker/README.md)

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
