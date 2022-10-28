
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.partiql/partiql-lang-kotlin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.partiql/partiql-lang-kotlin)
[![License](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/partiql/partiql-lang-kotlin/blob/main/LICENSE)
[![CI Build](https://github.com/partiql/partiql-lang-kotlin/actions/workflows/build.yml/badge.svg)](https://github.com/partiql/partiql-lang-kotlin/actions?query=workflow%3A%22Build+and+Report+Generation%22)
[![codecov](https://codecov.io/gh/partiql/partiql-lang-kotlin/branch/main/graph/badge.svg)](https://codecov.io/gh/partiql/partiql-lang-kotlin)

# PartiQL Lang Kotlin

This is a Kotlin implementation of the [PartiQL specification](https://partiql.org/assets/PartiQL-Specification.pdf).
PartiQL is based on SQL-92 and has added support for working with schemaless hierarchical data.
PartiQL’s extensions to SQL are easy to understand, treat nested data as first class citizens and
compose seamlessly with each other and SQL.

This repository contains an embeddable reference interpreter, test framework, and tests for PartiQL in Kotlin.

## About

Check out the [PartiQL Lang Kotlin Wiki](https://github.com/partiql/partiql-lang-kotlin/wiki) for documentation,
tutorials, migration guides, and more!

## Status

Users of PartiQL should consider PartiQL to be in "preview" status. It has been leveraged within a number of Amazon internal
systems and AWS products for over a year. The behavior of the language itself is mostly stable, however,
the public API of the interpreter is slated to undergo significant improvements in the near term. (See the
GitHub issues list for details.)

## Using In Your Project

This project is published to [Maven Central](https://search.maven.org/artifact/org.partiql/partiql-lang-kotlin).

| Group ID | Artifact ID | Recommended Version |
|----------|-------------|---------------------|
| `org.partiql` | `partiql-lang-kotlin` | `0.8.1`             | 


For Maven builds, add the following to your `pom.xml`:

```xml
<dependency>
  <groupId>org.partiql</groupId>
  <artifactId>partiql-lang-kotlin</artifactId>
  <version>${version}</version>
</dependency>
```

For Gradle 5+, add the following to your `build.gradle`:

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation "org.partiql:partiql-lang-kotlin:${version}"
}
```

Be sure to replace `${version}` with the desired version.

## Building

**Pre-requisite**: Building this project requires Java 11+.

This project uses a [git submodule](https://git-scm.com/book/en/v2/Git-Tools-Submodules) to pull in 
[partiql-tests](https://github.com/partiql/partiql-tests). The easiest way to pull everything in is to clone the 
repository recursively:

```bash
$ git clone --recursive https://github.com/partiql/partiql-lang-kotlin.git
```

You can also initialize the submodules as follows:

```bash
$ git submodule update --init --recursive
```

To build this project, from the root directory execute:

```shell
./gradlew build
```

This will build the reference interpreter and test framework, then run all unit and integration tests.

## Directory Structure

- `docs` source code for the GitHub Wiki
- `partiql-grammar` contains the ANTLR files to generate the PartiQL Parser.
- `lang` contains the source code of the library containing the interpreter.
- `lang/jmh` contains the JMH benchmarks for PartiQL.
- `cli` contains the source code of the command-line interface and interactive prompt. (CLI/REPL)

### Running JMH Benchmarks

To run JMH benchmarks located in `lang/jmh`, build the entire project first and then run 
the following command:

```shell
./gradlew jmh
```

### Examples

See the [examples](examples) project in this repository for examples covering
use of the PartiQL interpreter in your project.

## Contributing

See [CONTRIBUTING](CONTRIBUTING.md)

## License

The works contained within this repository are licensed under the Apache 2.0 License.

See the [LICENSE](LICENSE) file.
